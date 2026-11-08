package dev.etran.ultimate.compat

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** A material plus the legacy data value needed to express it on pre-1.13 servers (0 on modern ones). */
data class ResolvedMaterial(val material: Material, val data: Short = 0) {
    fun toItem(amount: Int = 1): ItemStack {
        val item = ItemStack(material, amount)
        if (data.toInt() != 0) Items.setLegacyData(item, data)
        return item
    }
}

/**
 * Cross-version material lookup.
 *
 * Config files may use modern names (`GOLDEN_SWORD`, `RED_WOOL`, `FIRE_CHARGE`) or legacy names
 * (`GOLD_SWORD`, `WOOL`, `FIREBALL`); both resolve on every server version. Colored blocks written
 * with the modern `<COLOR>_WOOL` style are translated to `WOOL` + data on pre-1.13 servers.
 */
object Materials {

    internal val aliases = AliasGroups.of(
        "GOLDEN_SWORD, GOLD_SWORD", "GOLDEN_AXE, GOLD_AXE", "GOLDEN_PICKAXE, GOLD_PICKAXE",
        "GOLDEN_SHOVEL, GOLD_SPADE", "GOLDEN_HOE, GOLD_HOE", "GOLDEN_HELMET, GOLD_HELMET",
        "GOLDEN_CHESTPLATE, GOLD_CHESTPLATE", "GOLDEN_LEGGINGS, GOLD_LEGGINGS", "GOLDEN_BOOTS, GOLD_BOOTS",
        "WOODEN_SWORD, WOOD_SWORD", "WOODEN_AXE, WOOD_AXE", "WOODEN_PICKAXE, WOOD_PICKAXE",
        "WOODEN_SHOVEL, WOOD_SPADE", "WOODEN_HOE, WOOD_HOE",
        "DIAMOND_SHOVEL, DIAMOND_SPADE", "IRON_SHOVEL, IRON_SPADE", "STONE_SHOVEL, STONE_SPADE",
        "FIRE_CHARGE, FIREBALL",
        "BRICKS, BRICK",
        "BRICK, CLAY_BRICK",
        "CREEPER_SPAWN_EGG, MONSTER_EGG",
        "PLAYER_HEAD, SKULL_ITEM",
        "GRASS_BLOCK, GRASS",
        "OAK_PLANKS, WOOD",
        "OAK_LOG, LOG",
        "COBWEB, WEB",
        "SNOWBALL, SNOW_BALL",
        "CLOCK, WATCH",
        "GUNPOWDER, SULPHUR",
        "ENDER_EYE, EYE_OF_ENDER",
        "FIREWORK_ROCKET, FIREWORK",
        "WRITABLE_BOOK, BOOK_AND_QUILL",
        "ENCHANTING_TABLE, ENCHANTMENT_TABLE",
        "CRAFTING_TABLE, WORKBENCH",
        "IRON_BARS, IRON_FENCE",
        "GLASS_PANE, THIN_GLASS",
        "EXPERIENCE_BOTTLE, EXP_BOTTLE",
        "COMPARATOR, REDSTONE_COMPARATOR",
        "REPEATER, DIODE",
        "LEAD, LEASH",
        "OAK_SIGN, SIGN",
        "COMMAND_BLOCK, COMMAND",
        "MUSHROOM_STEW, MUSHROOM_SOUP",
        "SPLASH_POTION, POTION",
        "SHEARS", "BARRIER", "NAME_TAG", "NOTE_BLOCK", "BLAZE_POWDER", "BOOK", "PAPER", "CHEST"
    )

    // Order matches the legacy wool/glass/clay data values 0..15.
    private val COLORS = listOf(
        "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
        "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    )

    /** Modern suffix -> legacy base material that stores the colour in data. */
    private val COLORED = listOf(
        "_STAINED_GLASS_PANE" to "STAINED_GLASS_PANE",
        "_STAINED_GLASS" to "STAINED_GLASS",
        "_TERRACOTTA" to "STAINED_CLAY",
        "_CARPET" to "CARPET",
        "_WOOL" to "WOOL",
        "_CONCRETE" to "CONCRETE"
    )

    /**
     * The resolution logic with the server lookup abstracted away, so it can be verified against the material list of any
     * Minecraft version. Returns the exact material name plus legacy data value.
     */
    internal fun resolveName(rawName: String, exists: (String) -> Boolean, modern: Boolean): Pair<String, Int>? {
        val name = AliasGroups.normalize(rawName)

        // Pre-flattening spawn eggs are MONSTER_EGG with the entity id (creeper = 50) as data.
        if (name == "CREEPER_SPAWN_EGG" && !modern && exists("MONSTER_EGG")) return "MONSTER_EGG" to 50

        for (candidate in aliases.candidates(name)) {
            if (exists(candidate)) return candidate to 0
        }

        // Modern coloured block names on a server that predates the flattening.
        if (!modern) {
            for ((suffix, legacyBase) in COLORED) {
                if (name.endsWith(suffix)) {
                    val colorIndex = COLORS.indexOf(name.removeSuffix(suffix))
                    if (colorIndex >= 0 && exists(legacyBase)) return legacyBase to colorIndex
                }
            }
        }
        // Legacy "WOOL" on a modern server: white wool is the closest thing.
        if (name == "WOOL" && exists("WHITE_WOOL")) return "WHITE_WOOL" to 0
        return null
    }

    fun resolve(rawName: String?): ResolvedMaterial? {
        if (rawName == null || rawName.isBlank()) return null
        val (exact, data) = resolveName(rawName, { lookup(it) != null }, McVersion.supports(13)) ?: return null
        val material = lookup(exact) ?: return null
        return ResolvedMaterial(material, data.toShort())
    }

    fun isValid(name: String?): Boolean = resolve(name) != null

    fun isWool(material: Material): Boolean = material.name.endsWith("WOOL")

    fun isWool(item: ItemStack?): Boolean = item != null && isWool(item.type)

    /** Plain `Material.getMaterial`; null on a server that doesn't have it (never throws). */
    fun lookup(exactName: String): Material? = try {
        Material.getMaterial(exactName)
    } catch (ignored: Throwable) {
        null
    }

    fun air(): Material = Material.AIR
}
