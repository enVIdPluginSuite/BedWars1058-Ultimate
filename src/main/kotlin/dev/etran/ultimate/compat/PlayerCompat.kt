package dev.etran.ultimate.compat

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * Player-related calls whose availability or shape differs between server versions.
 *
 * `setMaxHealth`/`getMaxHealth` are deprecated (and may disappear) on modern servers, so they are
 * tried reflectively with the attribute API as a fallback. The off-hand only exists on 1.9+.
 */
object PlayerCompat {

    private val doubleType: Class<*> = java.lang.Double.TYPE

    private val setMaxHealthMethod = Reflect.method(Player::class.java, "setMaxHealth", doubleType)
    private val getMaxHealthMethod = Reflect.method(Player::class.java, "getMaxHealth")

    private val attributeClass = Reflect.classOrNull("org.bukkit.attribute.Attribute")
    private val attributeInstanceClass = Reflect.classOrNull("org.bukkit.attribute.AttributeInstance")
    private val getAttribute = attributeClass?.let { Reflect.method(Player::class.java, "getAttribute", it) }
    private val setBaseValue = Reflect.method(attributeInstanceClass, "setBaseValue", doubleType)
    private val getValue = Reflect.method(attributeInstanceClass, "getValue")

    /** GENERIC_MAX_HEALTH was renamed to MAX_HEALTH on 1.21.3+. */
    private val maxHealthAttribute: Any? by lazy {
        Reflect.staticField(attributeClass, "MAX_HEALTH") ?: Reflect.staticField(attributeClass, "GENERIC_MAX_HEALTH")
    }

    private val getHand = Reflect.method(PlayerInteractEvent::class.java, "getHand")
    private val getItemInOffHand = Reflect.method(PlayerInventory::class.java, "getItemInOffHand")

    fun setMaxHealth(player: Player, value: Double) {
        if (setMaxHealthMethod != null) {
            try {
                setMaxHealthMethod.invoke(player, value)
                return
            } catch (ignored: Throwable) {
                // fall through to the attribute API
            }
        }
        val instance = attributeInstance(player) ?: return
        Reflect.invoke(setBaseValue, instance, value)
    }

    fun getMaxHealth(player: Player): Double {
        (Reflect.invoke(getMaxHealthMethod, player) as? Number)?.let { return it.toDouble() }
        val instance = attributeInstance(player)
        (Reflect.invoke(getValue, instance) as? Number)?.let { return it.toDouble() }
        return 20.0
    }

    private fun attributeInstance(player: Player): Any? {
        val attribute = maxHealthAttribute ?: return null
        return Reflect.invoke(getAttribute, player, attribute)
    }

    /** True when the interact event came from the off-hand (1.9+), which would otherwise double-fire every click. */
    fun isOffHand(event: PlayerInteractEvent): Boolean {
        val hand = Reflect.invoke(getHand, event) ?: return false
        return hand.toString() == "OFF_HAND"
    }

    fun offHandItem(player: Player): ItemStack? = Reflect.invoke(getItemInOffHand, player.inventory) as? ItemStack

    fun mainHandItem(player: Player): ItemStack? = player.inventory.getItem(player.inventory.heldItemSlot)
}
