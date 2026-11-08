package dev.etran.ultimate.fixture

import org.bukkit.Sound
import org.bukkit.event.inventory.InventoryOpenEvent

/**
 * Deliberately version-fragile code, used only as a negative control for the compatibility checker:
 * the checker MUST flag these on newer servers, otherwise a clean report would mean nothing.
 * (Both patterns are what the original BedWars1058-Ultimate did.)
 */
class KnownPitfalls {
    /** InventoryView is an abstract class up to 1.20 but an interface from 1.21 -> IncompatibleClassChangeError. */
    fun title(event: InventoryOpenEvent): String = event.view.title

    /** Sound is an enum up to 1.21.2 but an interface afterwards -> valueOf no longer exists. */
    fun sound(): Sound = Sound.valueOf("LEVEL_UP")
}
