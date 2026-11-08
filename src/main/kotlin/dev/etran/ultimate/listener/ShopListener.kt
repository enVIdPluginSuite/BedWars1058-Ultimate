package dev.etran.ultimate.listener

import com.andrei1058.bedwars.api.language.Language
import com.andrei1058.bedwars.api.language.Messages
import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.PlayerCompat
import dev.bw1058.ultimate.core.UltimateItems
import dev.bw1058.ultimate.gui.Menu
import dev.bw1058.ultimate.gui.UltimateMenu
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent

/**
 * Adds the Ultimates button to BedWars1058's item shop and keeps ultimate items from being shuffled around.
 *
 * Inventory titles are read through BedWars1058's own `VersionSupport#getInventoryName`. Calling
 * `InventoryEvent#getView().getTitle()` directly breaks on 1.21+, where `InventoryView` changed from a class into an
 * interface (that exact call crashed the original plugin there).
 */
class ShopListener(private val plugin: UltimatePlugin) : Listener {

    private val shopCategories = listOf(
        "blocks-category", "melee-category", "armor-category", "tools-category",
        "ranged-category", "potions-category", "utility-category"
    )

    @EventHandler(priority = EventPriority.NORMAL)
    fun onOpen(event: InventoryOpenEvent) {
        val general = plugin.configs.general
        if (!general.shopButtonEnabled.get()) return
        val player = event.player as? Player ?: return
        val arena = plugin.arenas.arenaOf(player) ?: return
        if (!plugin.arenas.qualifies(arena)) return
        if (!isShop(player, event)) return

        val item = general.shopButton.build(tag = UltimateItems.SHOP_BUTTON)
        val slot = general.shopButton.slot!!.get()
        if (slot < event.inventory.size) event.inventory.setItem(slot, item)
    }

    private fun isShop(player: Player, event: InventoryOpenEvent): Boolean {
        val title = try {
            plugin.bedWars.versionSupport.getInventoryName(event)
        } catch (ignored: Throwable) {
            return false
        } ?: return false
        return try {
            title == Language.getMsg(player, Messages.SHOP_INDEX_NAME) ||
                shopCategories.any { title == Language.getMsg(player, Messages.SHOP_PATH + ".$it.inventory-name") }
        } catch (ignored: Throwable) {
            false
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder is Menu) return // our own menus handle themselves
        val player = event.whoClicked as? Player ?: return
        if (plugin.arenas.arenaOf(player) == null) return // nothing of ours can be in this inventory
        val clicked = event.currentItem
        val clickedTag = UltimateItems.tagOf(clicked)

        if (clickedTag == UltimateItems.SHOP_BUTTON) {
            event.isCancelled = true
            Bukkit.getScheduler().runTask(plugin, Runnable { UltimateMenu(plugin, player).open() })
            return
        }
        if (!plugin.configs.general.preventMoving.get()) return

        val protectedClick = clickedTag != null && !clickedTag.startsWith("core:") ||
            UltimateItems.isUltimateItem(event.cursor)
        if (protectedClick && event.inventory.type != InventoryType.CRAFTING) event.isCancelled = true
        // The selector/shop button must never leave the player's own inventory either.
        if (clickedTag != null && clickedTag.startsWith("core:") && event.inventory.type != InventoryType.CRAFTING) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is Menu) {
            event.isCancelled = true
            return
        }
        if (!plugin.configs.general.preventMoving.get()) return
        if (event.inventory.type == InventoryType.CRAFTING) return
        val who = event.whoClicked as? Player ?: return
        if (plugin.arenas.arenaOf(who) == null) return
        if (!UltimateItems.isProtected(event.oldCursor)) return
        if (event.rawSlots.any { it < event.inventory.size }) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onSelectorUse(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (PlayerCompat.isOffHand(event)) return
        if (UltimateItems.tagOf(event.item) != UltimateItems.SELECTOR) return
        event.isCancelled = true
        UltimateMenu(plugin, event.player).open()
    }
}
