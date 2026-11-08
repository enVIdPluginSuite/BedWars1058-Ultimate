package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent
import com.andrei1058.bedwars.api.events.player.PlayerReSpawnEvent
import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.Effects
import dev.etran.ultimate.compat.Materials
import dev.etran.ultimate.compat.Particles
import dev.etran.ultimate.compat.Sounds
import dev.etran.ultimate.core.Text
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * Kangaroo: double jump, keeps part of its resources on death, free milk when a bed is destroyed,
 * and a speed boost on kill.
 */
class Kangaroo(plugin: UltimatePlugin) : Ultimate(
    plugin, "kangaroo", "Kangaroo", "RABBIT_FOOT", 10,
    listOf(
        "&7• Double Jump!",
        "&7• Keep resources on death!",
        "&7• Free milk on bed destroy!",
        "&7• Speed II on kill!",
        "",
        "&7Cooldown: &a{cooldown} Seconds",
        "",
        "{status}"
    ),
    ", {cooldown}, {keep-chance}, {keep-percent}, {speed-seconds}"
) {

    // ---- double jump ----------------------------------------------------------------------------------------
    private val jumpEnabled = specs.bool("double-jump.enabled", true, "Enable the double jump.")
    private val maxJumps = specs.int("double-jump.max-jumps", 1, "Mid-air jumps available before the cooldown starts.", min = 1, max = 10)
    private val cooldown = specs.double("double-jump.cooldown-seconds", 10.0, "Cooldown after the last jump. 0 = no cooldown.", min = 0.0, max = 600.0, step = 1.0)
    private val horizontalPower = specs.double("double-jump.horizontal-power", 0.5, "Forward push of the jump (multiplies your look direction).", min = 0.0, max = 5.0, step = 0.1)
    private val verticalPower = specs.double("double-jump.vertical-power", 1.0, "Upward push of the jump.", min = 0.0, max = 5.0, step = 0.1)
    private val rejumpDelay = specs.int("double-jump.rejump-delay-ticks", 6, "With max-jumps above 1: ticks before the next mid-air jump becomes available.", min = 1, max = 100)
    private val jumpSound = specs.sound("double-jump.sound", "ENTITY_BAT_TAKEOFF", "Sound when you jump.")
    private val jumpParticle = specs.particle("double-jump.particle", "CLOUD", "Particle puffed under your feet when you jump.")
    private val jumpParticleCount = specs.int("double-jump.particle-count", 8, "Number of particles.", min = 1, max = 100)
    private val readySound = specs.sound("double-jump.ready-sound", "", "Sound when the double jump is available again.")
    private val resetOnKill = specs.bool("double-jump.reset-cooldown-on-kill", false, "A kill instantly refreshes the double jump.")

    // ---- keep resources -------------------------------------------------------------------------------------
    private val keepEnabled = specs.bool("keep-resources.enabled", true, "Keep part of your resources when you die (if your bed is still alive).")
    private val keepChance = specs.int("keep-resources.chance-percent", 50, "Chance (0-100) that the effect triggers on a given death.", min = 0, max = 100, step = 5)
    private val keepFraction = specs.double("keep-resources.keep-fraction", 0.5, "Share of eligible item stacks that are kept (0.5 = half of your stacks, rounded down).", min = 0.0, max = 1.0, step = 0.05)
    private val resourcesOnly = specs.bool("keep-resources.resources-only", true, "Only resource stacks are eligible. If false, every inventory stack is.")
    private val resourceMaterials = specs.materialList(
        "keep-resources.resource-materials", listOf("IRON_INGOT", "GOLD_INGOT", "DIAMOND", "EMERALD"),
        "Materials that count as resources when resources-only is on."
    )

    // ---- milk -----------------------------------------------------------------------------------------------
    private val milkEnabled = specs.bool("milk.enabled", true, "Receive milk buckets when you destroy a bed.")
    private val milkAmount = specs.int("milk.amount", 1, "Milk buckets received.", min = 1, max = 16)

    // ---- speed on kill --------------------------------------------------------------------------------------
    private val speedEnabled = specs.bool("speed-on-kill.enabled", true, "Gain speed after a kill.")
    private val speedAmplifier = specs.int("speed-on-kill.amplifier", 1, "Speed level minus one (1 = Speed II).", min = 0, max = 10)
    private val speedSeconds = specs.double("speed-on-kill.duration-seconds", 5.0, "How long the speed lasts.", min = 0.5, max = 600.0, step = 0.5)
    private val speedParticles = specs.bool("speed-on-kill.show-particles", false, "Show potion particles while the speed is active.")

    // ---- messages -------------------------------------------------------------------------------------------
    private val msgKept = specs.text("messages.kept-resources", "&7Kangaroo kept &e{count} &7stack(s) of your resources!", "Sent after respawn. Placeholder: {count}. Blank = no message.")
    private val msgMilk = specs.text("messages.milk", "", "Sent when you receive milk. Blank = no message.")

    private val jumpsUsed = HashMap<UUID, Int>()
    private val saved = HashMap<UUID, List<Pair<Int, ItemStack>>>()

    private companion object {
        const val JUMP = "kangaroo:jump"
    }

    override fun placeholders(): Map<String, String> = mapOf(
        "cooldown" to Text.seconds(cooldown.get()),
        "keep-chance" to keepChance.get().toString(),
        "keep-percent" to (keepFraction.get() * 100).toInt().toString(),
        "speed-seconds" to Text.seconds(speedSeconds.get())
    )

    override fun onActivate(player: Player, arena: IArena) {
        jumpsUsed.remove(player.uniqueId)
        if (jumpEnabled.get()) {
            // BedWars1058 resets flight around respawn, so grant it one tick later.
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (player.isOnline && ctx(player) != null) player.allowFlight = true
            }, 1L)
        }
    }

    override fun onDeactivate(player: Player) {
        jumpsUsed.remove(player.uniqueId)
        if (player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE) {
            player.isFlying = false
            player.allowFlight = false
        }
    }

    override fun onRelinquish(player: Player, switching: Boolean) {
        saved.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        if (!event.isFlying) return
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return
        if (ctx(player) == null || !jumpEnabled.get()) return

        event.isCancelled = true
        player.allowFlight = false
        player.isFlying = false
        if (plugin.cooldowns.isActive(player.uniqueId, JUMP)) return
        jump(player)
    }

    private fun jump(player: Player) {
        val uuid = player.uniqueId
        val used = (jumpsUsed[uuid] ?: 0) + 1

        player.velocity = player.location.direction.multiply(horizontalPower.get()).setY(verticalPower.get())
        player.fallDistance = 0f
        Sounds.play(player, jumpSound.get())
        Particles.spawn(player.location, jumpParticle.get(), jumpParticleCount.get(), 0.3, 0.1, 0.3, 0.02)

        if (used >= maxJumps.get()) {
            jumpsUsed.remove(uuid)
            plugin.cooldowns.start(player, JUMP, cooldown.get(), "Double Jump") { onJumpReady(player) }
        } else {
            jumpsUsed[uuid] = used
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { onJumpReady(player, silent = true) }, rejumpDelay.get().toLong())
        }
    }

    private fun onJumpReady(player: Player, silent: Boolean = false) {
        if (!player.isOnline || ctx(player) == null || !jumpEnabled.get()) return
        player.allowFlight = true
        if (!silent) Sounds.play(player, readySound.get())
    }

    // ---- keep resources -------------------------------------------------------------------------------------

    private fun isResource(item: ItemStack): Boolean {
        if (!resourcesOnly.get()) return true
        val names = resourceMaterials.get().mapNotNull { Materials.resolve(it)?.material?.name }
        return item.type.name in names
    }

    /** Runs before BedWars1058 processes the death, so kept stacks never reach the killer. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val ctx = ctx(victim) ?: return
        if (!keepEnabled.get()) return
        if (ctx.team?.isBedDestroyed == true) return // no respawn coming, nothing to give back
        val rnd = ThreadLocalRandom.current()
        if (rnd.nextInt(100) >= keepChance.get()) return

        val inventory = victim.inventory
        val candidates = ArrayList<Int>()
        for (slot in 0 until 36) {
            val item = inventory.getItem(slot) ?: continue
            if (item.type == org.bukkit.Material.AIR) continue
            if (dev.etran.ultimate.core.UltimateItems.isProtected(item)) continue
            if (isResource(item)) candidates.add(slot)
        }
        val keepCount = Math.floor(candidates.size * keepFraction.get()).toInt()
        if (keepCount <= 0) return
        candidates.shuffle(rnd)

        val kept = ArrayList<Pair<Int, ItemStack>>()
        for (slot in candidates.take(keepCount)) {
            val item = inventory.getItem(slot) ?: continue
            kept.add(slot to item.clone())
            event.drops.remove(item)
            inventory.setItem(slot, null)
        }
        saved[victim.uniqueId] = kept
    }

    @EventHandler
    fun onRespawn(event: PlayerReSpawnEvent) {
        val player = event.player
        val items = saved.remove(player.uniqueId) ?: return
        val inventory = player.inventory
        var restored = 0
        for ((slot, item) in items) {
            val current = inventory.getItem(slot)
            if (current == null || current.type == org.bukkit.Material.AIR) {
                inventory.setItem(slot, item)
            } else {
                for (left in inventory.addItem(item).values) player.world.dropItemNaturally(player.location, left)
            }
            restored++
        }
        if (restored > 0) plugin.tell(player, msgKept.get(), "count" to restored.toString())
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        saved.remove(event.player.uniqueId)
        jumpsUsed.remove(event.player.uniqueId)
    }

    // ---- bed / kill -----------------------------------------------------------------------------------------

    @EventHandler
    fun onBedBreak(event: PlayerBedBreakEvent) {
        val player = event.player
        if (ctx(player) == null || !milkEnabled.get()) return
        val milk = Materials.resolve("MILK_BUCKET")?.toItem(milkAmount.get()) ?: return
        for (left in player.inventory.addItem(milk).values) player.world.dropItemNaturally(player.location, left)
        plugin.tell(player, msgMilk.get())
    }

    override fun onKill(killer: Player, victim: Player, arena: IArena) {
        if (speedEnabled.get()) {
            Effects.create("SPEED", (speedSeconds.get() * 20).toInt(), speedAmplifier.get(), false, speedParticles.get())
                ?.let { killer.addPotionEffect(it, true) }
        }
        if (resetOnKill.get() && plugin.cooldowns.isActive(killer.uniqueId, JUMP)) {
            plugin.cooldowns.clear(killer.uniqueId, JUMP)
            jumpsUsed.remove(killer.uniqueId)
            onJumpReady(killer)
        }
    }
}
