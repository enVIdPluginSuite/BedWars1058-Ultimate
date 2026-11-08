package dev.etran.ultimate.listener

import com.andrei1058.bedwars.api.arena.GameState
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent
import com.andrei1058.bedwars.api.events.player.PlayerReSpawnEvent
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent
import com.andrei1058.bedwars.api.events.server.ArenaRestartEvent
import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.core.UltimateItems
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerQuitEvent

/** BedWars1058 game lifecycle plus the global rules of Ultimate mode (fall damage, item protection). */
class ArenaListener(private val plugin: UltimatePlugin) : Listener {

    // ---- lifecycle -----------------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    fun onStateChange(event: GameStateChangeEvent) {
        val arena = event.arena
        if (event.newState == GameState.playing) {
            plugin.arenas.scheduleActivation(arena)
        } else if (event.newState == GameState.restarting || event.newState == GameState.waiting) {
            plugin.arenas.deactivate(arena)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGameEnd(event: GameEndEvent) {
        plugin.arenas.deactivate(event.arena)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onArenaRestart(event: ArenaRestartEvent) = deactivateByName(event.arenaName)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onArenaDisable(event: ArenaDisableEvent) = deactivateByName(event.arenaName)

    private fun deactivateByName(name: String?) {
        if (name == null) return
        for (arena in plugin.arenas.active()) if (name.equals(arena.arenaName, ignoreCase = true)) plugin.arenas.deactivate(arena)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onLeave(event: PlayerLeaveArenaEvent) {
        if (!plugin.arenas.isActive(event.arena)) return
        plugin.manager.onLeaveArena(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onKill(event: PlayerKillEvent) {
        val arena = event.arena
        if (!plugin.arenas.isActive(arena)) return
        val victim = event.victim
        val killer = event.killer

        // The killer's reward logic runs while the victim's state is still intact.
        if (killer != null && killer != victim) {
            plugin.manager.context(killer)?.let { it.ultimate.onKill(killer, victim, arena) }
        }
        plugin.manager.onDeath(victim, arena, event.cause.isFinalKill)
    }

    @EventHandler
    fun onRespawn(event: PlayerReSpawnEvent) {
        plugin.manager.onRespawn(event.player, event.arena)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        plugin.manager.selected(player)?.relinquish(player, switching = false)
        plugin.removeSelectorItem(player)
        plugin.manager.forget(player.uniqueId)
        plugin.blocking.forget(player.uniqueId)
        plugin.prompts.forget(player.uniqueId)
    }

    // ---- rules ---------------------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFall(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return
        val player = event.entity as? Player ?: return
        if (plugin.arenas.activeArenaOf(player) == null) return

        if (plugin.isFallImmune(player.uniqueId)) {
            event.isCancelled = true
            return
        }
        val general = plugin.configs.general
        if (general.fallOnlyUltimatePlayers.get() && !plugin.manager.hasSelection(player)) return
        if (general.fallDisable.get()) {
            event.isCancelled = true
        } else if (general.fallMultiplier.get() != 1.0) {
            event.damage = event.damage * general.fallMultiplier.get()
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        if (!plugin.configs.general.preventDrop.get()) return
        if (plugin.arenas.arenaOf(event.player) == null) return
        if (UltimateItems.isProtected(event.itemDrop.itemStack)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathDrops(event: PlayerDeathEvent) {
        if (!plugin.configs.general.removeOnDeath.get()) return
        if (plugin.arenas.arenaOf(event.entity) == null) return
        event.drops.removeAll { UltimateItems.isProtected(it) }
    }

    /** Ultimate items stand in for blocks (bricks, ender chest...) but must never be placed. */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        if (plugin.arenas.arenaOf(event.player) == null) return
        if (UltimateItems.isProtected(event.itemInHand)) event.isCancelled = true
    }
}
