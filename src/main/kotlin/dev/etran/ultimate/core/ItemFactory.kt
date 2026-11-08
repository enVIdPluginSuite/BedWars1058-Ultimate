package dev.etran.ultimate.core

import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Items
import dev.bw1058.ultimate.compat.Materials
import dev.bw1058.ultimate.compat.Reflect
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect

/** Builds every item the plugin hands out, in a way that works from 1.8 to 26.x. */
object ItemFactory {

    private val getEnchantByName = Reflect.method(Enchantment::class.java, "getByName", String::class.java)

    /** Legacy (pre-1.9) potion data ids, used so the 1.8 item *looks* right; the actual effect is added as a custom effect. */
    private val LEGACY_POTION_IDS = mapOf(
        "REGENERATION" to 1, "SPEED" to 2, "FIRE_RESISTANCE" to 3, "POISON" to 4, "HEAL" to 5,
        "NIGHT_VISION" to 6, "WEAKNESS" to 8, "INCREASE_DAMAGE" to 9, "SLOW" to 10, "JUMP" to 11,
        "HARM" to 12, "WATER_BREATHING" to 13, "INVISIBILITY" to 14
    )

    fun fallbackMaterial(): Material = Materials.lookup("BARRIER") ?: Material.STONE

    fun create(
        materialName: String, amount: Int = 1, name: String? = null, lore: List<String> = emptyList(),
        glow: Boolean = false, placeholders: Map<String, String> = emptyMap(),
        unbreakable: Boolean = false, tag: String? = null, tweak: ((ItemMeta) -> Unit)? = null
    ): ItemStack {
        val base = Materials.resolve(materialName)?.toItem(amount.coerceIn(1, 64)) ?: ItemStack(fallbackMaterial(), amount.coerceIn(1, 64))
        return decorate(base, name, lore, glow, placeholders, unbreakable, tag, tweak)
    }

    fun decorate(
        base: ItemStack, name: String?, lore: List<String>, glow: Boolean,
        placeholders: Map<String, String> = emptyMap(), unbreakable: Boolean = false,
        tag: String? = null, tweak: ((ItemMeta) -> Unit)? = null
    ): ItemStack {
        var item = base
        val meta = item.itemMeta
        if (meta != null) {
            if (name != null) meta.displayName = Text.color(Text.apply(name, placeholders))
            if (lore.isNotEmpty()) meta.lore = Text.colorAll(lore, placeholders)
            addFlag(meta, "HIDE_ATTRIBUTES")
            addFlag(meta, "HIDE_POTION_EFFECTS")
            addFlag(meta, "HIDE_ADDITIONAL_TOOLTIP")
            if (glow) {
                val ench = glowEnchantment()
                if (ench != null) {
                    meta.addEnchant(ench, 1, true)
                    addFlag(meta, "HIDE_ENCHANTS")
                }
            }
            if (unbreakable) makeUnbreakable(meta)
            tweak?.invoke(meta)
            item.itemMeta = meta
        }
        if (tag != null) item = UltimateItems.tag(item, tag)
        return item
    }

    private fun addFlag(meta: ItemMeta, flag: String) {
        try {
            meta.addItemFlags(ItemFlag.valueOf(flag))
        } catch (ignored: Throwable) {
            // flag doesn't exist on this server version
        }
    }

    private fun glowEnchantment(): Enchantment? {
        for (name in listOf("DURABILITY", "UNBREAKING", "LUCK", "LUCK_OF_THE_SEA")) {
            (Reflect.invoke(getEnchantByName, null, name) as? Enchantment)?.let { return it }
        }
        return null
    }

    fun makeUnbreakable(meta: ItemMeta) {
        try {
            UltimatePlugin.instance.bedWars.versionSupport.setUnbreakable(meta)
            return
        } catch (ignored: Throwable) {
        }
        try {
            meta.javaClass.getMethod("setUnbreakable", java.lang.Boolean.TYPE).invoke(meta, true)
        } catch (ignored: Throwable) {
        }
    }

    /** A bare splash potion item for [effect]; works on 1.8 (POTION + splash data) and 1.9+ (SPLASH_POTION). */
    fun splashPotionBase(effect: PotionEffect?): ItemStack {
        Materials.lookup("SPLASH_POTION")?.let { return ItemStack(it) }
        val potion = Materials.lookup("POTION") ?: fallbackMaterial()
        val item = ItemStack(potion)
        val id = LEGACY_POTION_IDS[effect?.type?.name ?: ""] ?: 0
        Items.setLegacyData(item, (16384 + id).toShort())
        return item
    }

    fun isSimilarKind(a: ItemStack?, b: ItemStack?): Boolean = a != null && b != null && a.isSimilar(b)
}
