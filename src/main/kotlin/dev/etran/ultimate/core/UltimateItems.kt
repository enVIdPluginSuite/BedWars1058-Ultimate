package dev.etran.ultimate.core

import dev.bw1058.ultimate.UltimatePlugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Items belonging to an ultimate carry an NBT tag `ultimate = "<ultimate-id>:<kind>"`, written and read through
 * BedWars1058's own version support layer (so it works wherever BedWars1058 itself does).
 */
object UltimateItems {

    const val KEY = "ultimate"

    /** Tag kind for the shop button (not owned by an ultimate). */
    const val SHOP_BUTTON = "core:shop-button"
    const val SELECTOR = "core:selector"

    fun tag(item: ItemStack, value: String): ItemStack = try {
        UltimatePlugin.instance.bedWars.versionSupport.setTag(item, KEY, value)
    } catch (ignored: Throwable) {
        item
    }

    fun tagOf(item: ItemStack?): String? {
        if (item == null || item.type == Material.AIR) return null
        return try {
            UltimatePlugin.instance.bedWars.versionSupport.getTag(item, KEY)
        } catch (ignored: Throwable) {
            null
        }
    }

    /** Ultimate-owned item (excludes the shop button / selector, which are plain UI items). */
    fun isUltimateItem(item: ItemStack?): Boolean {
        val tag = tagOf(item) ?: return false
        return !tag.startsWith("core:")
    }

    fun isProtected(item: ItemStack?): Boolean = tagOf(item) != null

    /** `kind` if the item belongs to ultimate [id], else null. */
    fun kindOf(item: ItemStack?, id: String): String? {
        val tag = tagOf(item) ?: return null
        return if (tag.startsWith("$id:")) tag.substring(id.length + 1) else null
    }

    fun tagFor(ultimateId: String, kind: String): String = "$ultimateId:$kind"

    /** Removes every ultimate-owned item from the player's inventory (and cursor). */
    fun clear(player: Player) {
        val inv = player.inventory
        for (slot in 0 until inv.size) {
            val item = inv.getItem(slot) ?: continue
            if (isUltimateItem(item)) inv.setItem(slot, null)
        }
        val cursor = player.itemOnCursor
        if (cursor != null && isUltimateItem(cursor)) player.setItemOnCursor(null)
        player.updateInventory()
    }

    fun count(player: Player, id: String, kind: String): Int {
        var total = 0
        for (item in player.inventory.contents) {
            if (item != null && kindOf(item, id) == kind) total += item.amount
        }
        return total
    }

    fun holds(player: Player, id: String, kind: String): Boolean = count(player, id, kind) > 0
}
