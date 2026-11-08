package dev.etran.ultimate.compat

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * Cross-version sound playback.
 *
 * Config values look like `NAME`, `NAME:volume` or `NAME:volume:pitch`. Names can be written in
 * any era's spelling (`NOTE_PLING`, `BLOCK_NOTE_PLING`, `BLOCK_NOTE_BLOCK_PLING`) and are mapped to
 * whatever the running server has. A namespaced key (`entity.player.levelup`, `minecraft:...`,
 * or a resource-pack sound) is passed straight through to the string-based API.
 *
 * `org.bukkit.Sound` turned from an enum into an interface on 1.21.3+, so constants are fetched
 * reflectively (`getField` works for both) instead of via `Sound.valueOf`.
 */
object Sounds {

    data class Spec(val name: String, val volume: Float, val pitch: Float)

    private val soundClass: Class<*>? = Reflect.classOrNull("org.bukkit.Sound")
    private val resolved = ConcurrentHashMap<String, Any>()
    private val unresolved = ConcurrentHashMap.newKeySet<String>()

    internal val aliases = AliasGroups.of(
        "NOTE_PLING, BLOCK_NOTE_PLING, BLOCK_NOTE_BLOCK_PLING",
        "NOTE_BASS, BLOCK_NOTE_BASS, BLOCK_NOTE_BLOCK_BASS",
        "NOTE_PIANO, BLOCK_NOTE_HARP, BLOCK_NOTE_BLOCK_HARP",
        "NOTE_BASS_DRUM, BLOCK_NOTE_BASEDRUM, BLOCK_NOTE_BLOCK_BASEDRUM",
        "NOTE_SNARE_DRUM, BLOCK_NOTE_SNARE, BLOCK_NOTE_BLOCK_SNARE",
        "NOTE_STICKS, BLOCK_NOTE_HAT, BLOCK_NOTE_BLOCK_HAT",
        "VILLAGER_NO, ENTITY_VILLAGER_NO",
        "VILLAGER_YES, ENTITY_VILLAGER_YES",
        "VILLAGER_HAGGLE, ENTITY_VILLAGER_TRADE, ENTITY_VILLAGER_TRADING",
        "LEVEL_UP, ENTITY_PLAYER_LEVELUP",
        "FIREWORK_LAUNCH, ENTITY_FIREWORK_LAUNCH, ENTITY_FIREWORK_ROCKET_LAUNCH",
        "FIREWORK_BLAST, ENTITY_FIREWORK_BLAST, ENTITY_FIREWORK_ROCKET_BLAST",
        "ENDERMAN_TELEPORT, ENTITY_ENDERMEN_TELEPORT, ENTITY_ENDERMAN_TELEPORT",
        "BAT_TAKEOFF, ENTITY_BAT_TAKEOFF",
        "CHICKEN_EGG_POP, ENTITY_CHICKEN_EGG",
        "ORB_PICKUP, ENTITY_EXPERIENCE_ORB_PICKUP",
        "CLICK, UI_BUTTON_CLICK",
        "EXPLODE, ENTITY_GENERIC_EXPLODE, ENTITY_DRAGON_FIREBALL_EXPLODE",
        "FIRE_IGNITE, ITEM_FLINTANDSTEEL_USE",
        "GLASS, BLOCK_GLASS_BREAK",
        "ANVIL_USE, BLOCK_ANVIL_USE",
        "ANVIL_LAND, BLOCK_ANVIL_LAND",
        "DRINK, ENTITY_GENERIC_DRINK",
        "BURP, ENTITY_PLAYER_BURP",
        "FIZZ, BLOCK_FIRE_EXTINGUISH",
        "WOOD_CLICK, BLOCK_WOOD_BUTTON_CLICK_ON",
        "ITEM_PICKUP, ENTITY_ITEM_PICKUP",
        "SHOOT_ARROW, ENTITY_ARROW_SHOOT",
        "EAT, ENTITY_GENERIC_EAT",
        "GHAST_FIREBALL, ENTITY_GHAST_SHOOT",
        "PORTAL_TRAVEL, BLOCK_PORTAL_TRAVEL",
        "WITHER_SHOOT, ENTITY_WITHER_SHOOT",
        "LAVA_POP, BLOCK_LAVA_POP",
        "SPLASH, ENTITY_GENERIC_SPLASH",
        "DIG_WOOL, BLOCK_WOOL_BREAK",
        "STEP_WOOL, BLOCK_WOOL_STEP",
        "WOLF_HOWL, ENTITY_WOLF_HOWL",
        "ZOMBIE_UNFECT, ENTITY_ZOMBIE_VILLAGER_CURE",
        "HURT_FLESH, ENTITY_PLAYER_HURT",
        "CREEPER_HISS, ENTITY_CREEPER_PRIMED",
        "ENDERDRAGON_WINGS, ENTITY_ENDERDRAGON_FLAP, ENTITY_ENDER_DRAGON_FLAP",
        "ENDERDRAGON_GROWL, ENTITY_ENDERDRAGON_GROWL, ENTITY_ENDER_DRAGON_GROWL"
    )

    /** Parses `NAME[:volume[:pitch]]`. Returns null for blank input (which means "no sound"). */
    fun parse(text: String?): Spec? {
        if (text == null) return null
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.equals("NONE", ignoreCase = true) || trimmed.equals("OFF", ignoreCase = true)) return null
        val parts = trimmed.split(':')
        // A namespaced key like "minecraft:entity.player.levelup:1:1" has its own colon.
        var nameParts = 1
        if (parts.size >= 2 && parts[0].equals("minecraft", ignoreCase = true)) nameParts = 2
        val name = parts.take(nameParts).joinToString(":")
        val volume = parts.getOrNull(nameParts)?.toFloatOrNull() ?: 1.0f
        val pitch = parts.getOrNull(nameParts + 1)?.toFloatOrNull() ?: 1.0f
        return Spec(name, volume, pitch)
    }

    /** True when [text] is blank (no sound), a namespaced key, or resolves to a Sound on this server. */
    fun isValid(text: String?): Boolean {
        val spec = parse(text) ?: return true
        return isNamespaced(spec.name) || resolve(spec.name) != null
    }

    fun resolve(name: String): Any? {
        val key = AliasGroups.normalize(name)
        resolved[key]?.let { return it }
        if (unresolved.contains(key)) return null
        val cls = soundClass ?: return null
        for (candidate in aliases.candidates(key)) {
            val constant = Reflect.staticField(cls, candidate)
            if (constant != null) {
                resolved[key] = constant
                return constant
            }
        }
        unresolved.add(key)
        return null
    }

    fun play(player: Player, text: String?) {
        val spec = parse(text) ?: return
        play(player, player.location, spec)
    }

    fun play(location: Location, text: String?) {
        val spec = parse(text) ?: return
        val world = location.world ?: return
        for (player in world.players) {
            if (player.location.distanceSquared(location) <= RANGE_SQUARED) play(player, location, spec)
        }
    }

    fun play(player: Player, location: Location, spec: Spec) {
        try {
            if (isNamespaced(spec.name)) {
                player.playSound(location, spec.name, spec.volume, spec.pitch)
                return
            }
            val sound = resolve(spec.name) ?: return
            player.playSound(location, sound as Sound, spec.volume, spec.pitch)
        } catch (ignored: Throwable) {
            // A bad sound must never break gameplay.
        }
    }

    private fun isNamespaced(name: String): Boolean = name.contains('.') || name.contains(':')

    private const val RANGE_SQUARED = 16.0 * 16.0
}
