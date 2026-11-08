package dev.etran.ultimate.compat

import org.bukkit.Location

/**
 * Cross-version particles. 1.9+ uses `World#spawnParticle`; 1.8 falls back to `World#playEffect`.
 * Everything is reflective because `Particle` / `Effect` constants were renamed repeatedly
 * (e.g. `VILLAGER_HAPPY` -> `HAPPY_VILLAGER`), and the 1.8 API has no `Particle` class at all.
 */
object Particles {

    private val particleClass: Class<*>? = Reflect.classOrNull("org.bukkit.Particle")
    private val effectClass: Class<*>? = Reflect.classOrNull("org.bukkit.Effect")
    private val worldClass: Class<*> = org.bukkit.World::class.java

    private val intType: Class<*> = Integer.TYPE
    private val doubleType: Class<*> = java.lang.Double.TYPE

    private val spawnParticle = particleClass?.let {
        Reflect.method(
            worldClass, "spawnParticle", it, Location::class.java, intType,
            doubleType, doubleType, doubleType, doubleType
        )
    }
    private val playEffect = effectClass?.let {
        Reflect.method(worldClass, "playEffect", Location::class.java, it, intType)
    }

    private val resolved = java.util.concurrent.ConcurrentHashMap<String, Any>()
    private val broken = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    internal val aliases = AliasGroups.of(
        "VILLAGER_HAPPY, HAPPY_VILLAGER",
        "CRIT_MAGIC, MAGIC_CRIT, ENCHANTED_HIT",
        "SPELL_WITCH, WITCH_MAGIC, WITCH",
        "SMOKE_NORMAL, SMOKE",
        "SMOKE_LARGE, LARGE_SMOKE",
        "FIREWORKS_SPARK, FIREWORK",
        "SNOWBALL, SNOW_SHOVEL, ITEM_SNOWBALL",
        "DRIP_WATER, WATERDRIP, FALLING_WATER",
        "DRIP_LAVA, LAVADRIP, FALLING_LAVA",
        "EXPLOSION_NORMAL, POOF",
        "EXPLOSION_LARGE, EXPLOSION",
        "EXPLOSION_HUGE, EXPLOSION_EMITTER",
        "WATER_SPLASH, SPLASH",
        "WATER_BUBBLE, BUBBLE",
        "SPELL, EFFECT",
        "SPELL_INSTANT, INSTANT_EFFECT",
        "VILLAGER_ANGRY, ANGRY_VILLAGER, VILLAGER_THUNDERCLOUD",
        "ENCHANTMENT_TABLE, ENCHANT",
        "TOTEM, TOTEM_OF_UNDYING",
        "REDSTONE, DUST, COLOURED_DUST",
        "FLAME, MOBSPAWNER_FLAMES",
        "CLOUD",
        "HEART",
        "NOTE",
        "PORTAL",
        "LAVA"
    )

    fun isValid(name: String?): Boolean {
        if (name == null || name.isBlank() || name.equals("NONE", true) || name.equals("OFF", true)) return true
        return resolve(name) != null
    }

    private fun resolve(name: String): Any? {
        val key = AliasGroups.normalize(name)
        resolved[key]?.let { return it }
        for (candidate in aliases.candidates(key)) {
            // Prefer the modern Particle class, then the legacy Effect class.
            val constant = Reflect.staticField(particleClass, candidate) ?: Reflect.staticField(effectClass, candidate)
            if (constant != null) {
                resolved[key] = constant
                return constant
            }
        }
        return null
    }

    /**
     * Spawns [count] particles at [loc]. Silently does nothing for blank/unknown names and for
     * particles that need extra data on this server (disabled after the first failure).
     */
    fun spawn(
        loc: Location, name: String?, count: Int = 1,
        offsetX: Double = 0.0, offsetY: Double = 0.0, offsetZ: Double = 0.0, speed: Double = 0.0
    ) {
        if (name == null || name.isBlank() || name.equals("NONE", true) || name.equals("OFF", true)) return
        val key = AliasGroups.normalize(name)
        if (broken.contains(key)) return
        val world = loc.world ?: return
        val constant = resolve(key) ?: run { broken.add(key); return }
        try {
            if (particleClass != null && particleClass.isInstance(constant) && spawnParticle != null) {
                spawnParticle.invoke(world, constant, loc, count, offsetX, offsetY, offsetZ, speed)
            } else if (playEffect != null && effectClass != null && effectClass.isInstance(constant)) {
                // playEffect has no count/offset support: emulate with a few scattered plays.
                val rnd = java.util.concurrent.ThreadLocalRandom.current()
                repeat(count.coerceAtLeast(1)) {
                    val at = loc.clone().add(
                        (rnd.nextDouble() - 0.5) * 2 * offsetX,
                        (rnd.nextDouble() - 0.5) * 2 * offsetY,
                        (rnd.nextDouble() - 0.5) * 2 * offsetZ
                    )
                    playEffect.invoke(world, at, constant, 0)
                }
            }
        } catch (ignored: Throwable) {
            broken.add(key)
        }
    }
}
