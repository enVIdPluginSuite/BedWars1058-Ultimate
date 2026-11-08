package dev.etran.ultimate.core

import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.PlayerCompat
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.metadata.FixedMetadataValue

/**
 * Splash potions thrown from an ultimate item are marked with entity metadata at launch time.
 * (Reading the thrown potion's item back, as `ThrownPotion#getItem`, isn't dependable across versions; the item the
 * player is holding at the moment of the throw is.)
 */
object PotionTools {

    private const val META = "etranultimate-potion"

    /** The `<id>:<kind>` tag of the ultimate item in either hand, if any. */
    fun heldTag(player: Player): String? =
        UltimateItems.tagOf(PlayerCompat.mainHandItem(player)) ?: UltimateItems.tagOf(PlayerCompat.offHandItem(player))

    fun mark(plugin: UltimatePlugin, potion: ThrownPotion, tag: String) {
        potion.setMetadata(META, FixedMetadataValue(plugin, tag))
    }

    fun markOf(potion: ThrownPotion): String? =
        if (potion.hasMetadata(META)) potion.getMetadata(META).firstOrNull()?.asString() else null
}
