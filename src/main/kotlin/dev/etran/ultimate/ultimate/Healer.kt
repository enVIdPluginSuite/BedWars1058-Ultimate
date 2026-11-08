package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.arena.team.ITeam
import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Effects
import dev.bw1058.ultimate.compat.Particles
import dev.bw1058.ultimate.compat.PlayerCompat
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.config.ItemOpts
import dev.bw1058.ultimate.core.ItemFactory
import dev.bw1058.ultimate.core.PotionTools
import dev.bw1058.ultimate.core.Text
import dev.bw1058.ultimate.core.UltimateItems
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.PotionMeta
import java.util.UUID

/**
 * Healer: extra max health, a self-heal when holding right-click with a sword, and a throwable potion that leaves a
 * healing zone for the team.
 */
class Healer(plugin: UltimatePlugin) : Ultimate(
    plugin, "healer", "Healer", "GOLDEN_APPLE", 12,
    listOf(
        "&7• Hold right click on a sword",
        "&7to heal yourself!",
        "&7• Throw down potion to heal",
        "&7teammates in a radius!",
        "",
        "&7Cooldown: &a{cooldown} Seconds",
        "",
        "{status}"
    ),
    ", {cooldown}, {potion-cooldown}, {hearts}, {radius}, {zone-seconds}"
) {

    // ---- bonus health ---------------------------------------------------------------------------------------
    private val bonusEnabled = specs.bool("health.bonus-enabled", true, "Raise max health while this ultimate is active.")
    private val maxHearts = specs.double("health.max-hearts", 15.0, "Max health in hearts while active (10 = normal).", min = 1.0, max = 100.0, step = 1.0)
    private val fillOnActivate = specs.bool("health.fill-on-activate", true, "Fill health to the new maximum when the ultimate is applied (selection and each respawn).")
    private val restoreHearts = specs.double("health.restore-hearts", 10.0, "Max health in hearts restored when the ultimate is removed.", min = 1.0, max = 100.0, step = 1.0)

    // ---- self heal ------------------------------------------------------------------------------------------
    private val selfEnabled = specs.bool("self-heal.enabled", true, "Allow healing yourself by holding right-click with a sword.")
    private val selfCooldown = specs.double("self-heal.cooldown-seconds", 20.0, "Cooldown after starting a self-heal.", min = 0.0, max = 600.0, step = 1.0)
    private val selfAmount = specs.double("self-heal.heal-amount", 1.0, "Health restored per pulse (1.0 = half a heart).", min = 0.5, max = 40.0, step = 0.5)
    private val selfInterval = specs.int("self-heal.interval-ticks", 20, "Ticks between pulses.", min = 1, max = 200, step = 5)
    private val selfMaxSeconds = specs.double("self-heal.max-duration-seconds", 0.0, "Stop healing after this long. 0 = until you are at full health.", min = 0.0, max = 600.0, step = 1.0)
    private val selfNeedsDamage = specs.bool("self-heal.require-damage", true, "Don't start (or burn the cooldown) while already at full health.")
    private val selfSound = specs.sound("self-heal.start-sound", "NOTE_PLING", "Sound when the heal starts.")
    private val selfParticle = specs.particle("self-heal.pulse-particle", "HEART", "Particle on each pulse.")

    // ---- potion ---------------------------------------------------------------------------------------------
    private val potionItem = ItemOpts(
        specs, "potion.item", "SPLASH_POTION", "&aHealer Potion",
        listOf("&7Throw to create a healing zone", "&7for your team."), what = "healer potion", withMaterial = false
    )
    private val potionCooldown = specs.double("potion.cooldown-seconds", 20.0, "Seconds until the potion is returned after being thrown.", min = 0.0, max = 600.0, step = 1.0)
    private val potionGiveBack = specs.bool("potion.give-back", true, "Return the potion after the cooldown. If false it is a one-time item per life.")
    private val effectType = specs.effect("potion.zone.effect", "REGENERATION", "Effect applied to players standing in the zone.")
    private val effectAmplifier = specs.int("potion.zone.amplifier", 0, "Effect level minus one.", min = 0, max = 10)
    private val effectTicks = specs.int("potion.zone.effect-duration-ticks", 100, "Duration of each application, in ticks.", min = 1, max = 2400, step = 20)
    private val zoneRadius = specs.double("potion.zone.radius", 2.5, "Zone radius in blocks (a box, like the original plugin).", min = 0.5, max = 20.0, step = 0.5)
    private val zoneSeconds = specs.double("potion.zone.duration-seconds", 30.0, "How long the zone lasts.", min = 1.0, max = 600.0, step = 1.0)
    private val zoneInterval = specs.int("potion.zone.interval-ticks", 20, "Ticks between applications.", min = 1, max = 200, step = 5)
    private val healSelf = specs.bool("potion.zone.affect-thrower", true, "The thrower benefits from their own zone.")
    private val healTeam = specs.bool("potion.zone.affect-teammates", true, "Teammates benefit from the zone.")
    private val zoneParticle = specs.particle("potion.zone.particle", "VILLAGER_HAPPY", "Particle drawn in rings across the zone.")
    private val zoneParticleDensity = specs.double("potion.zone.particle-density", 1.0, "Particle amount multiplier (0.5 = half as many, 0 = none).", min = 0.0, max = 5.0, step = 0.25)
    private val landSound = specs.sound("potion.zone.land-sound", "", "Sound where the potion lands.")

    // ---- messages -------------------------------------------------------------------------------------------
    private val msgPotionReady = specs.text("messages.potion-ready", "", "Sent when the potion comes back. Blank = no message.")

    private class Zone(
        val owner: UUID, val arena: IArena, val team: ITeam?, val center: Location,
        val endMs: Long, var nextMs: Long
    )

    private class SelfHeal(var nextMs: Long, val endMs: Long)

    private val zones = ArrayList<Zone>()
    private val selfHeals = HashMap<UUID, SelfHeal>()

    private companion object {
        const val SELF = "healer:self"
        const val POTION = "healer:potion"
        const val KIND = "potion"
    }

    override fun placeholders(): Map<String, String> = mapOf(
        "cooldown" to Text.seconds(selfCooldown.get()),
        "potion-cooldown" to Text.seconds(potionCooldown.get()),
        "hearts" to Text.seconds(maxHearts.get()),
        "radius" to Text.seconds(zoneRadius.get()),
        "zone-seconds" to Text.seconds(zoneSeconds.get())
    )

    private fun makePotion() = potionItem.buildOn(
        ItemFactory.splashPotionBase(effect()), tag = UltimateItems.tagFor(id, KIND)
    ) { meta -> effect()?.let { (meta as? PotionMeta)?.addCustomEffect(it, true) } }

    private fun effect() = Effects.create(effectType.get(), effectTicks.get(), effectAmplifier.get())

    override fun onActivate(player: Player, arena: IArena) {
        selfHeals.remove(player.uniqueId)
        if (bonusEnabled.get()) {
            val max = maxHearts.get() * 2
            PlayerCompat.setMaxHealth(player, max)
            player.healthScale = max
            if (fillOnActivate.get()) player.health = max
        }
        player.inventory.addItem(makePotion())
    }

    override fun onDeactivate(player: Player) {
        selfHeals.remove(player.uniqueId)
    }

    override fun onRelinquish(player: Player, switching: Boolean) {
        // Restore the normal health bar when the player stops being a Healer.
        if (!bonusEnabled.get()) return
        val max = restoreHearts.get() * 2
        PlayerCompat.setMaxHealth(player, max)
        player.healthScale = max
        player.isHealthScaled = false
        if (player.health > max) player.health = max
    }

    override fun onArenaDeactivated(arena: IArena) {
        zones.removeAll { it.arena === arena }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        selfHeals.remove(event.player.uniqueId)
    }

    // ---- potion throwing ------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    fun onLaunch(event: ProjectileLaunchEvent) {
        val potion = event.entity as? ThrownPotion ?: return
        val player = potion.shooter as? Player ?: return
        if (ctx(player) == null) return
        if (PotionTools.heldTag(player) != UltimateItems.tagFor(id, KIND)) return

        PotionTools.mark(plugin, potion, UltimateItems.tagFor(id, KIND))
        if (potionGiveBack.get()) {
            plugin.cooldowns.start(player, POTION, potionCooldown.get(), "Healer Potion") {
                // One tick later so we never race the server's own "consume the thrown item" step.
                Bukkit.getScheduler().runTaskLater(plugin, Runnable { giveBack(player) }, 1L)
            }
        }
    }

    private fun giveBack(player: Player) {
        if (!player.isOnline || ctx(player) == null) return
        if (UltimateItems.holds(player, id, KIND)) return
        for (left in player.inventory.addItem(makePotion()).values) player.world.dropItemNaturally(player.location, left)
        plugin.tell(player, msgPotionReady.get())
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSplash(event: PotionSplashEvent) {
        val potion = event.potion
        if (PotionTools.markOf(potion) != UltimateItems.tagFor(id, KIND)) return
        event.isCancelled = true // the zone replaces the vanilla splash

        val player = potion.shooter as? Player ?: return
        val c = ctx(player) ?: return
        val now = System.currentTimeMillis()
        val location = potion.location.clone()
        zones.add(Zone(player.uniqueId, c.arena, c.team, location, now + (zoneSeconds.get() * 1000).toLong(), now))
        Sounds.play(location, landSound.get())
    }

    // ---- ticking --------------------------------------------------------------------------------------------

    override fun tick() {
        val now = System.currentTimeMillis()
        tickZones(now)
        tickSelfHeal(now)
    }

    private fun tickZones(now: Long) {
        if (zones.isEmpty()) return
        val iterator = zones.iterator()
        while (iterator.hasNext()) {
            val zone = iterator.next()
            if (now >= zone.endMs || !plugin.arenas.isActive(zone.arena)) {
                iterator.remove()
                continue
            }
            if (now < zone.nextMs) continue
            zone.nextMs = now + zoneInterval.get() * 50L

            val r = zoneRadius.get()
            val effect = effect()
            for (member in zone.arena.players) {
                if (zone.arena.isReSpawning(member) || zone.arena.isSpectator(member)) continue
                val isOwner = member.uniqueId == zone.owner
                val isMate = zone.team != null && zone.team.isMember(member)
                if (!((isOwner && healSelf.get()) || (isMate && !isOwner && healTeam.get()))) continue
                val loc = member.location
                if (loc.world != zone.center.world) continue
                if (Math.abs(loc.x - zone.center.x) > r || Math.abs(loc.y - zone.center.y) > r || Math.abs(loc.z - zone.center.z) > r) continue
                if (effect != null) member.addPotionEffect(effect, true)
            }
            drawRings(zone.center, r)
        }
    }

    private fun drawRings(center: Location, radius: Double) {
        val density = zoneParticleDensity.get()
        if (density <= 0.0) return
        var i = 0.3
        while (i < radius) {
            val step = Math.max(1.0, 30.0 / i / density)
            var degree = 0.0
            while (degree < 360.0) {
                val rad = Math.toRadians(degree)
                Particles.spawn(center.clone().add(Math.cos(rad) * i, 0.1, Math.sin(rad) * i), zoneParticle.get(), 1)
                degree += step
            }
            i += 0.4
        }
    }

    private fun tickSelfHeal(now: Long) {
        forEachHolder { player, _ ->
            val uuid = player.uniqueId
            val running = selfHeals[uuid]
            if (running == null) {
                if (selfEnabled.get() && !plugin.cooldowns.isActive(uuid, SELF) && plugin.blocking.isBlocking(player)) {
                    val max = PlayerCompat.getMaxHealth(player)
                    if (selfNeedsDamage.get() && player.health >= max) return@forEachHolder
                    plugin.cooldowns.start(player, SELF, selfCooldown.get(), "Heal")
                    val end = if (selfMaxSeconds.get() > 0) now + (selfMaxSeconds.get() * 1000).toLong() else Long.MAX_VALUE
                    selfHeals[uuid] = SelfHeal(now, end)
                    Sounds.play(player, selfSound.get())
                }
            } else if (now >= running.nextMs) {
                val max = PlayerCompat.getMaxHealth(player)
                if (player.health >= max || now >= running.endMs) {
                    selfHeals.remove(uuid)
                } else {
                    player.health = Math.min(player.health + selfAmount.get(), max)
                    Particles.spawn(player.location.add(0.0, 1.8, 0.0), selfParticle.get(), 2, 0.3, 0.2, 0.3, 0.0)
                    running.nextMs = now + selfInterval.get() * 50L
                }
            }
        }
    }
}
