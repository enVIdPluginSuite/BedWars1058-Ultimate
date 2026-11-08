package dev.etran.ultimate.gui

import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.Sounds
import dev.etran.ultimate.core.Text
import dev.etran.ultimate.ultimate.UltimateManager
import org.bukkit.entity.Player
import java.util.UUID

/** The menu where players pick their ultimate. Every slot, name, lore line and sound comes from the config. */
class UltimateMenu(plugin: UltimatePlugin, player: Player) : Menu(plugin, player) {

    override fun title(): String = Text.color(plugin.configs.general.menuTitle.get())

    override fun rows(): Int = plugin.configs.general.menuRows.get()

    override fun open() {
        super.open()
        Sounds.play(player, plugin.configs.general.menuOpenSound.get())
    }

    override fun render() {
        val general = plugin.configs.general
        val selected = plugin.manager.selected(player)
        val cooldownLeft = plugin.manager.switchCooldownRemaining(player)

        if (general.menuFiller.get()) {
            val filler = general.menuFillerItem.build()
            for (slot in 0 until size()) set(slot, filler)
        }

        for (ultimate in plugin.manager.enabled()) {
            val status = when {
                selected === ultimate -> general.menuStatusSelected.get()
                cooldownLeft > 0 -> Text.apply(general.menuStatusCooldown.get(), mapOf("seconds" to cooldownLeft.toString()))
                else -> general.menuStatusUnselected.get()
            }
            set(ultimate.icon.slot!!.get(), ultimate.menuIcon(status, glow = selected === ultimate)) {
                val result = plugin.manager.select(player, ultimate)
                if (result == UltimateManager.SelectResult.OK && general.menuCloseOnSelect.get()) {
                    player.closeInventory()
                } else {
                    refresh()
                }
            }
        }

        if (general.menuBackEnabled.get() && BedWarsShop.available()) {
            set(general.menuBack.slot!!.get(), general.menuBack.build()) {
                player.closeInventory()
                // Reopen next tick so the close/open pair doesn't fight inside the click event.
                plugin.server.scheduler.runTask(plugin, Runnable { BedWarsShop.open(player) })
            }
        }
    }
}

/**
 * Reopens BedWars1058's item shop. The shop has no public API, so its internals are reached reflectively and the
 * back button simply hides itself if they are not there (keeps the menu working across BedWars1058 versions).
 */
object BedWarsShop {

    private val shopManagerClass = try {
        Class.forName("com.andrei1058.bedwars.shop.ShopManager")
    } catch (ignored: Throwable) {
        null
    }
    private val quickBuyCacheClass = try {
        Class.forName("com.andrei1058.bedwars.shop.quickbuy.PlayerQuickBuyCache")
    } catch (ignored: Throwable) {
        null
    }

    fun available(): Boolean = shopManagerClass != null && quickBuyCacheClass != null

    fun open(player: Player): Boolean {
        return try {
            val shop = shopManagerClass!!.getMethod("getShop").invoke(null) ?: return false
            val cache = quickBuyCacheClass!!.getMethod("getQuickBuyCache", UUID::class.java).invoke(null, player.uniqueId)
            val open = shop.javaClass.methods.first { it.name == "open" && it.parameterTypes.size == 3 }
            open.invoke(shop, player, cache, false)
            true
        } catch (ignored: Throwable) {
            false
        }
    }
}
