package dev.etran.ultimate.gui

import dev.etran.ultimate.UltimatePlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Minimal inventory-menu framework. Each menu is its own [InventoryHolder], so clicks are routed by holder identity
 * rather than by comparing inventory titles (titles can't be read portably across versions, and titles can collide).
 */
abstract class Menu(protected val plugin: UltimatePlugin, val player: Player) : InventoryHolder {

    private lateinit var inv: Inventory
    private val actions = HashMap<Int, (InventoryClickEvent) -> Unit>()

    protected abstract fun title(): String
    protected abstract fun rows(): Int
    protected abstract fun render()

    override fun getInventory(): Inventory = inv

    open fun open() {
        inv = Bukkit.createInventory(this, rows().coerceIn(1, 6) * 9, title().take(32))
        fill()
        player.openInventory(inv)
    }

    /** Rebuilds the contents in place (the open inventory is kept, so there is no flicker). */
    fun refresh() {
        if (!::inv.isInitialized) return
        inv.clear()
        fill()
        player.updateInventory()
    }

    private fun fill() {
        actions.clear()
        render()
    }

    protected fun set(slot: Int, item: ItemStack, action: ((InventoryClickEvent) -> Unit)? = null) {
        if (slot < 0 || slot >= inv.size) return
        inv.setItem(slot, item)
        if (action != null) actions[slot] = action else actions.remove(slot)
    }

    protected fun size(): Int = inv.size

    internal fun handle(event: InventoryClickEvent) {
        val slot = event.rawSlot
        if (slot < 0 || slot >= inv.size) return
        actions[slot]?.invoke(event)
    }

    open fun onClose() {}
}

class MenuListener(private val plugin: UltimatePlugin) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(event: InventoryClickEvent) {
        val menu = event.inventory.holder as? Menu ?: return
        event.isCancelled = true
        menu.handle(event)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is Menu) event.isCancelled = true
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        (event.inventory.holder as? Menu)?.onClose()
    }
}
