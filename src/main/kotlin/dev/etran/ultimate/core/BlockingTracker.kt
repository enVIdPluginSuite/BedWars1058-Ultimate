package dev.etran.ultimate.core

import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.PlayerCompat
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

/**
 * Detects "player is holding right-click with a sword".
 *
 * - On 1.8 a sword genuinely blocks, so `Player#isBlocking` is accurate.
 * - From 1.9 on swords can't block; holding right-click just produces a stream of interact events, so a hold is
 *   considered active for a short window after the last one.
 *
 * Both signals are combined on every version, which also covers 1.8 servers with newer clients (ViaVersion) and
 * 1.9+ servers with 1.8 clients (which see a fake shield block).
 */
class BlockingTracker(private val plugin: UltimatePlugin) : Listener {

    private val lastInteract = HashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (PlayerCompat.isOffHand(event)) return
        val item = event.item ?: return
        if (!matches(item.type)) return
        lastInteract[event.player.uniqueId] = System.currentTimeMillis()
    }

    fun matches(material: Material): Boolean {
        val name = material.name
        return plugin.configs.general.blockingItemSuffixes.get().any { name.endsWith(it.trim().uppercase()) }
    }

    fun isBlocking(player: Player): Boolean {
        val uuid = player.uniqueId
        val last = lastInteract[uuid]
        if (last != null) {
            if (System.currentTimeMillis() - last <= plugin.configs.general.blockingWindowMs.get()) return true
            lastInteract.remove(uuid)
        }
        return try {
            player.isBlocking
        } catch (ignored: Throwable) {
            false
        }
    }

    fun forget(uuid: UUID) {
        lastInteract.remove(uuid)
    }
}
