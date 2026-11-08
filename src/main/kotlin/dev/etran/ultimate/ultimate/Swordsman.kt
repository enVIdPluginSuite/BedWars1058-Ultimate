package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Particles
import dev.bw1058.ultimate.compat.PlayerCompat
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.core.Text
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Swordsman: hold right-click with a sword to charge a dash, release to launch forward, hold again within the
 * recall window to snap back to where you started. A kill refreshes the dash and heals.
 */
class Swordsman(plugin: UltimatePlugin) : Ultimate(
    plugin, "swordsman", "Swordsman", "GOLDEN_SWORD", 11,
    listOf(
        "&7• Hold right click on a sword",
        "&7to dash forward!",
        "&7• Hold right click again to",
        "&7recall within {recall-window} seconds!",
        "&7• Cooldown reset on kill and",
        "&7heals {kill-hearts} heart!",
        "",
        "&7Cooldown: &a{cooldown} Seconds",
        "",
        "{status}"
    ),
    ", {cooldown}, {recall-window}, {kill-hearts}, {max-charge}"
) {

    // ---- charge ---------------------------------------------------------------------------------------------
    private val maxCharge = specs.int("charge.max-ticks", 40, "Ticks of holding right-click for a full-power dash (20 ticks = 1 second).", min = 5, max = 200, step = 5)
    private val chargeSound = specs.sound("charge.sound", "NOTE_PLING", "Sound played while charging.")
    private val chargeSoundInterval = specs.int("charge.sound-interval-ticks", 5, "Ticks between charge sounds.", min = 1, max = 40)
    private val chargeBar = specs.bool("charge.show-xp-bar", true, "Show charge progress on the XP bar.")

    // ---- dash -----------------------------------------------------------------------------------------------
    private val cooldown = specs.double("dash.cooldown-seconds", 10.0, "Cooldown after a dash. 0 = no cooldown.", min = 0.0, max = 600.0, step = 1.0)
    private val speedPerTick = specs.double("dash.speed-per-charge-tick", 0.08, "Forward speed gained per tick of charge.", min = 0.0, max = 1.0, step = 0.01)
    private val liftPerTick = specs.double("dash.lift-per-charge-tick", 0.03, "Upward speed gained per tick of charge.", min = 0.0, max = 1.0, step = 0.01)
    private val maxSpeed = specs.double("dash.max-speed", 3.2, "Upper limit for the forward speed multiplier.", min = 0.1, max = 10.0, step = 0.1)
    private val minCharge = specs.int("dash.min-charge-ticks", 1, "Releasing before this many ticks of charge does nothing.", min = 1, max = 200)
    private val dashSound = specs.sound("dash.sound", "FIREWORK_LAUNCH", "Sound when dashing.")
    private val dashParticle = specs.particle("dash.particle", "CLOUD", "Particle burst at the start of a dash.")
    private val dashParticleCount = specs.int("dash.particle-count", 12, "Number of particles.", min = 1, max = 100)
    private val fallImmunity = specs.double("dash.fall-immunity-seconds", 3.0, "No fall damage for this long after a dash.", min = 0.0, max = 60.0, step = 0.5)

    // ---- recall ---------------------------------------------------------------------------------------------
    private val recallEnabled = specs.bool("recall.enabled", true, "Allow snapping back to the dash origin.")
    private val recallWindow = specs.double("recall.window-seconds", 5.0, "Seconds after a dash during which recall is possible.", min = 0.5, max = 60.0, step = 0.5)
    private val recallSound = specs.sound("recall.sound", "ENDERMAN_TELEPORT", "Sound when recalling.")
    private val recallParticle = specs.particle("recall.particle", "PORTAL", "Particle at the origin and destination of a recall.")
    private val recallParticleCount = specs.int("recall.particle-count", 20, "Number of particles.", min = 1, max = 200)
    private val recallClearVelocity = specs.bool("recall.clear-velocity", true, "Stop all movement on arrival.")

    // ---- on kill --------------------------------------------------------------------------------------------
    private val killReset = specs.bool("on-kill.reset-cooldown", true, "A kill refreshes the dash cooldown.")
    private val killHearts = specs.double("on-kill.heal-hearts", 1.0, "Hearts healed on a kill (never above max health). 0 = none.", min = 0.0, max = 50.0, step = 0.5)
    private val killSound = specs.sound("on-kill.sound", "", "Sound when a kill refreshes you.")

    private class Recall(val origin: Location) {
        var armed = false
    }

    private val charge = HashMap<UUID, Int>()
    private val recalls = HashMap<UUID, Recall>()

    private companion object {
        const val DASH = "swordsman:dash"
    }

    override fun placeholders(): Map<String, String> = mapOf(
        "cooldown" to Text.seconds(cooldown.get()),
        "recall-window" to Text.seconds(recallWindow.get()),
        "kill-hearts" to Text.seconds(killHearts.get()),
        "max-charge" to Text.seconds(maxCharge.get() / 20.0)
    )

    override fun onActivate(player: Player, arena: IArena) {
        charge.remove(player.uniqueId)
        recalls.remove(player.uniqueId)
    }

    override fun onDeactivate(player: Player) {
        if (charge.remove(player.uniqueId) != null) player.exp = 0f
        recalls.remove(player.uniqueId)
    }

    @EventHandler
    fun onHotbarChange(event: PlayerItemHeldEvent) {
        val uuid = event.player.uniqueId
        if (charge.remove(uuid) != null) event.player.exp = 0f
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        charge.remove(event.player.uniqueId)
        recalls.remove(event.player.uniqueId)
    }

    override fun onKill(killer: Player, victim: Player, arena: IArena) {
        if (killReset.get()) {
            plugin.cooldowns.clear(killer.uniqueId, DASH)
            recalls.remove(killer.uniqueId)
            charge.remove(killer.uniqueId)
        }
        if (killHearts.get() > 0) {
            val max = PlayerCompat.getMaxHealth(killer)
            killer.health = Math.min(killer.health + killHearts.get() * 2, max)
        }
        Sounds.play(killer, killSound.get())
    }

    override fun tick() {
        val now = System.currentTimeMillis()
        forEachHolder { player, _ ->
            val uuid = player.uniqueId
            val blocking = plugin.blocking.isBlocking(player)

            if (!plugin.cooldowns.isActive(uuid, DASH)) {
                val current = charge[uuid] ?: 0
                if (blocking) {
                    if (current < maxCharge.get()) {
                        val next = current + 1
                        charge[uuid] = next
                        if (chargeBar.get()) player.exp = (next.toFloat() / maxCharge.get()).coerceIn(0f, 1f)
                        if (next % chargeSoundInterval.get() == 0) Sounds.play(player, chargeSound.get())
                    }
                } else if (current > 0) {
                    charge.remove(uuid)
                    if (chargeBar.get()) player.exp = 0f
                    if (current >= minCharge.get()) dash(player, current)
                }
            } else {
                val recall = recalls[uuid] ?: return@forEachHolder
                val elapsed = plugin.cooldowns.elapsedSeconds(uuid, DASH)
                if (!recallEnabled.get() || elapsed > recallWindow.get()) {
                    recalls.remove(uuid)
                } else if (!blocking) {
                    recall.armed = true // must let go once before a second hold counts as a recall
                } else if (recall.armed) {
                    recalls.remove(uuid)
                    recall(player, recall.origin)
                }
            }
        }
    }

    private fun dash(player: Player, ticks: Int) {
        val origin = player.location.clone()
        val power = Math.min(ticks * speedPerTick.get(), maxSpeed.get())
        val direction: Vector = player.location.direction.multiply(power).setY(ticks * liftPerTick.get())
        player.velocity = direction
        player.fallDistance = 0f
        if (fallImmunity.get() > 0) plugin.grantFallImmunity(player, fallImmunity.get())
        Sounds.play(player, dashSound.get())
        Particles.spawn(origin, dashParticle.get(), dashParticleCount.get(), 0.3, 0.2, 0.3, 0.05)

        plugin.cooldowns.start(player, DASH, cooldown.get(), "Dash")
        if (recallEnabled.get() && cooldown.get() > 0) recalls[player.uniqueId] = Recall(origin)
    }

    private fun recall(player: Player, origin: Location) {
        Particles.spawn(player.location, recallParticle.get(), recallParticleCount.get(), 0.4, 0.8, 0.4, 0.1)
        player.teleport(origin)
        if (recallClearVelocity.get()) player.velocity = Vector(0, 0, 0)
        player.fallDistance = 0f
        Particles.spawn(origin, recallParticle.get(), recallParticleCount.get(), 0.4, 0.8, 0.4, 0.1)
        Sounds.play(player, recallSound.get())
    }
}
