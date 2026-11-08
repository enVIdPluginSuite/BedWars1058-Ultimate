package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.GameState
import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.config.ConfigFile
import dev.etran.ultimate.config.ItemOpts
import dev.etran.ultimate.config.Opt
import dev.etran.ultimate.config.Specs
import dev.etran.ultimate.core.Text
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

/**
 * Base class of every ultimate. Subclasses declare their options as properties (in declaration order, which is also
 * the order they appear in the YAML file) and implement the behaviour.
 *
 * Lifecycle for a player holding this ultimate: [activate] when ultimates are on in their arena and they picked it
 * or respawned; [deactivate] when they switch, die, leave or the game ends. All per-player state must be dropped in
 * [onDeactivate] so nothing leaks between lives or games.
 */
abstract class Ultimate(
    val plugin: UltimatePlugin,
    val id: String,
    val defaultName: String,
    iconMaterial: String,
    iconSlot: Int,
    iconLore: List<String>,
    placeholderHelp: String
) : Listener {

    val specs = Specs()
    lateinit var configFile: ConfigFile

    init {
        specs.section("icon", "MENU ICON - lore placeholders: {status}$placeholderHelp")
        specs.section("messages", "MESSAGES SENT BY THIS ULTIMATE")
    }

    val enabled: Opt<Boolean> = specs.bool(
        "enabled", true,
        "Turn this ultimate on or off. A disabled ultimate disappears from the menu, and players holding it lose it on /bwu reload."
    )
    val displayName: Opt<String> = specs.text("display-name", defaultName, "Name shown in menus and chat messages.")
    val icon = ItemOpts(specs, "icon", iconMaterial, "&a$defaultName", iconLore, defSlot = iconSlot, what = "menu icon")

    /** Values for `{placeholders}` in this ultimate's icon lore, read live from the config so lore never goes stale. */
    open fun placeholders(): Map<String, String> = emptyMap()

    protected fun ctx(player: Player): UltimateManager.Context? = plugin.manager.context(player, this)

    fun isHolder(player: Player): Boolean = plugin.manager.selected(player) === this

    /** Makes this ultimate effective for [player]: clears old state, then applies items/effects. */
    fun activate(player: Player, arena: IArena) {
        deactivate(player)
        onActivate(player, arena)
    }

    /** Removes everything this ultimate gave the player and forgets all its per-player state. */
    fun deactivate(player: Player) {
        onDeactivate(player)
        plugin.cooldowns.clearAbilities(player)
        dev.etran.ultimate.core.UltimateItems.clear(player)
    }

    /**
     * The player is done with this ultimate for good (switched away, left the arena, game ended, final death).
     * Unlike [deactivate] - which also runs on every respawn/refresh - this is where state that must survive a
     * death (saved resources, a virtual ender chest, ...) is finally released. [switching] is true when the
     * player stays in the game, so items can be handed back instead of discarded.
     */
    fun relinquish(player: Player, switching: Boolean) {
        deactivate(player)
        onRelinquish(player, switching)
    }

    protected abstract fun onActivate(player: Player, arena: IArena)
    protected open fun onDeactivate(player: Player) {}
    protected open fun onRelinquish(player: Player, switching: Boolean) {}

    /** The holder died (they lose abilities state until they respawn). */
    open fun onHolderDeath(player: Player, arena: IArena) {
        plugin.cooldowns.clearAbilities(player)
    }

    /** The holder got a kill. */
    open fun onKill(killer: Player, victim: Player, arena: IArena) {}

    /** Called once per server tick. */
    open fun tick() {}

    /** After config files were reloaded. */
    open fun onReload() {}

    /** Game-wide state (zones, entities, ...) for [arena] must be dropped. */
    open fun onArenaDeactivated(arena: IArena) {}

    /** Iterates every holder of this ultimate in running ultimate arenas. */
    protected inline fun forEachHolder(action: (Player, UltimateManager.Context) -> Unit) {
        for (arena in plugin.arenas.active()) {
            if (arena.status != GameState.playing) continue
            for (player in arena.players.toList()) {
                val c = plugin.manager.context(player, this) ?: continue
                action(player, c)
            }
        }
    }

    /** Menu icon for [player]; `{status}` and this ultimate's placeholders are filled in. */
    fun menuIcon(status: String, glow: Boolean): ItemStack {
        val ph = HashMap(placeholders())
        ph["status"] = Text.color(status)
        ph["name"] = displayName.get()
        return icon.build(ph, glowOverride = glow)
    }

    fun displayNameColored(): String = Text.color(displayName.get())
}
