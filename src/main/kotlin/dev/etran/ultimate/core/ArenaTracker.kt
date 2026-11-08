package dev.etran.ultimate.core

import com.andrei1058.bedwars.api.arena.GameState
import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.Sounds
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Knows which BedWars1058 arenas are currently running Ultimate mode.
 * An arena qualifies by group/name (config), becomes *active* after the activation delay, and stops being active
 * when the game ends or the arena is reset.
 */
class ArenaTracker(private val plugin: UltimatePlugin) {

    private val active: MutableSet<IArena> = Collections.newSetFromMap(IdentityHashMap())
    private val pending = IdentityHashMap<IArena, BukkitTask>()

    fun qualifies(arena: IArena): Boolean {
        val general = plugin.configs.general
        if (general.allArenas.get()) return true
        val group = arena.group
        if (group != null && general.arenaGroups.get().any { it.equals(group, ignoreCase = true) }) return true
        val name = arena.arenaName
        return name != null && general.arenaNames.get().any { it.equals(name, ignoreCase = true) }
    }

    fun isActive(arena: IArena?): Boolean = arena != null && active.contains(arena)

    /** Snapshot, safe to iterate while arenas activate/deactivate. */
    fun active(): List<IArena> = if (active.isEmpty()) emptyList() else ArrayList(active)

    fun activeCount(): Int = active.size

    fun arenaOf(player: Player): IArena? = plugin.bedWars.arenaUtil.getArenaByPlayer(player)

    /** The player's arena, only if it is running Ultimate mode right now. */
    fun activeArenaOf(player: Player): IArena? {
        val arena = arenaOf(player) ?: return null
        return if (active.contains(arena)) arena else null
    }

    /** Schedules activation after the configured delay (immediately when the delay is 0). */
    fun scheduleActivation(arena: IArena) {
        if (active.contains(arena) || pending.containsKey(arena) || !qualifies(arena)) return
        val delayTicks = plugin.configs.general.activationDelay.get() * 20L
        if (delayTicks <= 0L) {
            activate(arena)
            return
        }
        pending[arena] = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            pending.remove(arena)
            if (arena.status == GameState.playing && qualifies(arena)) activate(arena)
        }, delayTicks)
    }

    fun activate(arena: IArena) {
        if (!active.add(arena)) return
        plugin.debug("Ultimate mode activated in arena ${arena.arenaName}")
        val general = plugin.configs.general
        val players = arena.players.toList()

        if (general.activationChat.get()) {
            val lines = plugin.configs.messages.ultimatesEnabled.get()
            for (p in players) for (line in lines) p.sendMessage(Text.color(line))
        }
        for (p in players) {
            Sounds.play(p, general.activationSound.get())
            if (general.titleEnabled.get()) {
                plugin.title(
                    p, Text.color(general.titleText.get()), Text.color(general.titleSubtitle.get()),
                    general.titleFadeIn.get(), general.titleStay.get(), general.titleFadeOut.get()
                )
            }
            plugin.manager.onArenaActivated(p, arena)
        }
    }

    fun deactivate(arena: IArena) {
        pending.remove(arena)?.cancel()
        if (!active.remove(arena)) return
        plugin.debug("Ultimate mode deactivated in arena ${arena.arenaName}")
        for (p in (arena.players + arena.spectators).distinct()) plugin.manager.onArenaDeactivated(p)
        for (u in plugin.manager.all()) u.onArenaDeactivated(arena)
    }

    /** Called after a reload: arenas may now qualify (or stop qualifying). */
    fun syncWithConfig() {
        val general = plugin.configs.general
        if (general.reloadDeactivateRemoved.get()) {
            for (arena in active()) if (!qualifies(arena)) deactivate(arena)
        }
        if (general.reloadActivateRunning.get()) {
            for (arena in plugin.bedWars.arenaUtil.arenas) {
                if (arena.status == GameState.playing && qualifies(arena) && !active.contains(arena)) activate(arena)
            }
        }
    }

    fun shutdown() {
        pending.values.forEach { it.cancel() }
        pending.clear()
        for (arena in active()) deactivate(arena)
        active.clear()
    }
}
