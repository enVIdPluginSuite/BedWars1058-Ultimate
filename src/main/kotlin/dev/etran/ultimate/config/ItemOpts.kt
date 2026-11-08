package dev.etran.ultimate.config

import dev.etran.ultimate.core.ItemFactory
import org.bukkit.inventory.ItemStack

/**
 * A reusable group of options describing one item (material, name, lore, glow, optionally slot/amount),
 * so every item in the plugin is customisable the same way.
 */
class ItemOpts(
    specs: Specs, private val prefix: String,
    defMaterial: String, defName: String, defLore: List<String> = emptyList(),
    defGlow: Boolean = false, defSlot: Int? = null, defAmount: Int? = null, what: String = "item",
    withMaterial: Boolean = true
) {
    /** Null for items whose material is fixed by the game (e.g. splash potions). */
    val material: Opt<String>? =
        if (withMaterial) specs.material("$prefix.material", defMaterial, "Material of the $what. Modern and legacy names both work.") else null
    private val fixedMaterial = defMaterial
    val name = specs.text("$prefix.name", defName, "Display name of the $what. Supports & colour codes.")
    val lore = specs.textList("$prefix.lore", defLore, "Lore lines of the $what. Supports & colour codes and the placeholders listed in the section header.")
    val glow = specs.bool("$prefix.glow", defGlow, "Adds an enchantment glint to the $what.")
    val slot: Opt<Int>? = defSlot?.let { specs.int("$prefix.slot", it, "Inventory slot of the $what (0 = top-left).", min = 0, max = 53) }
    val amount: Opt<Int>? = defAmount?.let { specs.int("$prefix.amount", it, "Stack size of the $what.", min = 1, max = 64) }

    fun build(
        placeholders: Map<String, String> = emptyMap(), tag: String? = null,
        unbreakable: Boolean = false, glowOverride: Boolean? = null, tweak: ((org.bukkit.inventory.meta.ItemMeta) -> Unit)? = null
    ): ItemStack = ItemFactory.create(
        material?.get() ?: fixedMaterial, amount?.get() ?: 1, name.get(), lore.get(), glowOverride ?: glow.get(),
        placeholders, unbreakable, tag, tweak
    )

    /** Same as [build] but on top of a caller-supplied base item (e.g. a splash potion). */
    fun buildOn(
        base: ItemStack, placeholders: Map<String, String> = emptyMap(), tag: String? = null,
        tweak: ((org.bukkit.inventory.meta.ItemMeta) -> Unit)? = null
    ): ItemStack = ItemFactory.decorate(base, name.get(), lore.get(), glow.get(), placeholders, false, tag, tweak)
}
