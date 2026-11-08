package dev.etran.ultimate.compat

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Potion effect lookup that accepts every era's naming (`SLOW` / `SLOWNESS`, `INCREASE_DAMAGE` /
 * `STRENGTH`, ...). Lookup goes through `PotionEffectType.getByName` reflectively because that type
 * changed shape (abstract class -> registry-backed interface) on newer servers.
 */
object Effects {

    private val typeClass: Class<*> = PotionEffectType::class.java
    private val getByName = Reflect.method(typeClass, "getByName", String::class.java)
    private val cache = java.util.concurrent.ConcurrentHashMap<String, PotionEffectType>()

    internal val aliases = AliasGroups.of(
        "SLOW, SLOWNESS", "SPEED", "REGENERATION", "INCREASE_DAMAGE, STRENGTH", "JUMP, JUMP_BOOST",
        "FAST_DIGGING, HASTE", "SLOW_DIGGING, MINING_FATIGUE", "DAMAGE_RESISTANCE, RESISTANCE",
        "CONFUSION, NAUSEA", "HEAL, INSTANT_HEALTH", "HARM, INSTANT_DAMAGE", "FIRE_RESISTANCE",
        "WATER_BREATHING", "INVISIBILITY", "BLINDNESS", "NIGHT_VISION", "HUNGER", "WEAKNESS", "POISON",
        "WITHER", "HEALTH_BOOST", "ABSORPTION", "SATURATION", "GLOWING", "LEVITATION", "LUCK",
        "UNLUCK, BAD_LUCK", "SLOW_FALLING", "CONDUIT_POWER", "DOLPHINS_GRACE"
    )

    fun resolve(name: String?): PotionEffectType? {
        if (name == null || name.isBlank()) return null
        val key = AliasGroups.normalize(name)
        cache[key]?.let { return it }
        for (candidate in aliases.candidates(key)) {
            val type = Reflect.invoke(getByName, null, candidate) as? PotionEffectType
            if (type != null) {
                cache[key] = type
                return type
            }
        }
        return null
    }

    fun isValid(name: String?): Boolean = resolve(name) != null

    /** Builds an effect; durations are in ticks. Returns null when the effect doesn't exist on this server. */
    fun create(
        name: String?, durationTicks: Int, amplifier: Int,
        ambient: Boolean = false, particles: Boolean = true
    ): PotionEffect? {
        val type = resolve(name) ?: return null
        return try {
            PotionEffect(type, durationTicks.coerceAtLeast(1), amplifier.coerceAtLeast(0), ambient, particles)
        } catch (ignored: Throwable) {
            PotionEffect(type, durationTicks.coerceAtLeast(1), amplifier.coerceAtLeast(0))
        }
    }
}
