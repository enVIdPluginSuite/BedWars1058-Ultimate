package dev.etran.ultimate.compat

import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/** Legacy item data (pre-1.13 colours, potion ids, spawn egg types). The setter is gone on newer servers, so it is reflective. */
object Items {

    private val setDurability = Reflect.method(ItemStack::class.java, "setDurability", java.lang.Short.TYPE)

    fun setLegacyData(item: ItemStack, data: Short) {
        Reflect.invoke(setDurability, item, data)
    }

    private val getDurability = Reflect.method(ItemStack::class.java, "getDurability")

    fun legacyData(item: ItemStack): Short = (Reflect.invoke(getDurability, item) as? Number)?.toShort() ?: 0
}

/** Block placement that works both before and after the 1.13 flattening. */
object Blocks {

    private val setData = Reflect.method(Block::class.java, "setData", java.lang.Byte.TYPE)

    /** Turns [block] into the block form of [item] (material, plus the colour data value on pre-1.13 servers). */
    fun setFrom(block: Block, item: ItemStack) {
        block.type = item.type
        if (!McVersion.supports(13) && Items.legacyData(item).toInt() != 0) {
            Reflect.invoke(setData, block, Items.legacyData(item).toByte())
        }
    }
}
