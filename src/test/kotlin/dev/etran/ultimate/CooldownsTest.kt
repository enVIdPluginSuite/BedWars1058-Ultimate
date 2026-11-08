package dev.etran.ultimate

import dev.bw1058.ultimate.core.Cooldowns
import org.bukkit.entity.Player
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CooldownsTest {

    private fun player(id: UUID): Player =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getUniqueId" -> id
                "toString" -> "FakePlayer"
                "hashCode" -> id.hashCode()
                "equals" -> false
                else -> null
            }
        } as Player

    private fun cooldowns(): Cooldowns {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val f = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val plugin = unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(f.get(null), UltimatePlugin::class.java) as UltimatePlugin
        return Cooldowns(plugin)
    }

    /** Regression: respawning resets abilities, and must NOT let a player dodge the ultimate switch cooldown. */
    @Test fun clearingAbilitiesKeepsTheSwitchCooldown() {
        val id = UUID.randomUUID()
        val p = player(id)
        val cd = cooldowns()
        cd.start(p, "core:switch", 20.0, hud = false)
        cd.start(p, "kangaroo:jump", 10.0)
        cd.start(p, "swordsman:dash", 10.0)

        cd.clearAbilities(p)

        assertTrue(cd.isActive(id, "core:switch"), "switch cooldown must survive an ability reset")
        assertFalse(cd.isActive(id, "kangaroo:jump"))
        assertFalse(cd.isActive(id, "swordsman:dash"))

        cd.clear(p)
        assertFalse(cd.isActive(id, "core:switch"), "a full clear (leaving the game) removes everything")
    }

    @Test fun remainingAndElapsedTrackTime() {
        val id = UUID.randomUUID()
        val p = player(id)
        val cd = cooldowns()
        cd.start(p, "x", 5.0)
        assertTrue(cd.isActive(id, "x"))
        assertTrue(cd.remainingSeconds(id, "x") in 4.0..5.0)
        assertTrue(cd.elapsedSeconds(id, "x") in 0.0..1.0)
        cd.start(p, "zero", 0.0)
        assertFalse(cd.isActive(id, "zero"), "a zero-second cooldown is never active")
    }

    @Test fun zeroSecondCooldownFiresReadyCallbackImmediately() {
        val p = player(UUID.randomUUID())
        var fired = false
        cooldowns().start(p, "k", 0.0) { fired = true }
        assertTrue(fired)
    }
}
