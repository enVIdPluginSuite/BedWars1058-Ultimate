package dev.etran.ultimate.ultimate

import com.andrei1058.bedwars.api.arena.GameState
import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.arena.team.ITeam
import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.core.Text
import dev.bw1058.ultimate.core.UltimateItems
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/** Registry of ultimates plus every player's current pick. */
class UltimateManager(private val plugin: UltimatePlugin) {

    class Context(val ultimate: Ultimate, val arena: IArena, val team: ITeam?)

    enum class SelectResult { OK, ALREADY, DISABLED, NOT_IN_GAME, NOT_ULTIMATE_ARENA, COOLDOWN, NO_PERMISSION, SWITCHING_DISABLED, RESPAWNING }

    private val ultimates = LinkedHashMap<String, Ultimate>()
    private val selection = HashMap<UUID, String>()
    private val everPicked = HashSet<UUID>()

    private companion object {
        const val SWITCH_KEY = "core:switch"
    }

    fun register(ultimate: Ultimate) {
        ultimates[ultimate.id] = ultimate
        plugin.configs.registerUltimate(ultimate)
        plugin.server.pluginManager.registerEvents(ultimate, plugin)
    }

    fun all(): List<Ultimate> = ultimates.values.toList()
    fun enabled(): List<Ultimate> = ultimates.values.filter { it.enabled.get() }
    fun get(id: String): Ultimate? = ultimates[id.lowercase()]

    fun selected(player: Player): Ultimate? = selection[player.uniqueId]?.let { ultimates[it] }

    fun hasSelection(player: Player): Boolean = selection.containsKey(player.uniqueId)

    /** Non-null only when [player] holds [ultimate] *and* it is currently in effect for them (see class docs). */
    fun context(player: Player, ultimate: Ultimate): Context? {
        if (selection[player.uniqueId] != ultimate.id) return null
        if (!ultimate.enabled.get()) return null
        val arena = plugin.arenas.activeArenaOf(player) ?: return null
        if (arena.status != GameState.playing) return null
        if (arena.isSpectator(player) || arena.isReSpawning(player)) return null
        return Context(ultimate, arena, arena.getTeam(player))
    }

    /** Context for whichever ultimate the player holds, if any is in effect. */
    fun context(player: Player): Context? {
        val u = selected(player) ?: return null
        return context(player, u)
    }

    // ---- selection --------------------------------------------------------------------------------------------

    fun select(player: Player, ultimate: Ultimate, force: Boolean = false): SelectResult {
        val general = plugin.configs.general
        val msgs = plugin.configs.messages
        val ph = arrayOf("ultimate" to ultimate.displayNameColored())

        fun deny(result: SelectResult, text: String): SelectResult {
            plugin.tell(player, text, *ph)
            Sounds.play(player, general.deniedSound.get())
            return result
        }

        if (!ultimate.enabled.get() && !force) return deny(SelectResult.DISABLED, msgs.disabled.get())

        val arena = plugin.arenas.arenaOf(player)
        if (!force) {
            if (arena == null) return deny(SelectResult.NOT_IN_GAME, msgs.notInGame.get())
            if (!plugin.arenas.qualifies(arena)) return deny(SelectResult.NOT_ULTIMATE_ARENA, msgs.notUltimateArena.get())
            if (general.permissionPerUltimate.get() && !player.hasPermission("bw1058ultimate.use.${ultimate.id}") &&
                !player.hasPermission("bw1058ultimate.use.*")
            ) return deny(SelectResult.NO_PERMISSION, msgs.noUltimatePermission.get())
            if (selected(player) === ultimate) return deny(SelectResult.ALREADY, msgs.alreadySelected.get())
            if (hasSelection(player) && !general.allowSwitching.get()) return deny(SelectResult.SWITCHING_DISABLED, msgs.switchingDisabled.get())
            if (arena.isReSpawning(player) && !general.allowWhileRespawning.get()) return deny(SelectResult.RESPAWNING, msgs.cantWhileRespawning.get())
            if (plugin.cooldowns.isActive(player.uniqueId, SWITCH_KEY)) {
                val left = Math.ceil(plugin.cooldowns.remainingSeconds(player.uniqueId, SWITCH_KEY)).toInt()
                plugin.tell(player, msgs.switchCooldown.get(), "seconds" to left.toString())
                Sounds.play(player, general.deniedSound.get())
                return SelectResult.COOLDOWN
            }
        }

        selected(player)?.relinquish(player, switching = true)
        selection[player.uniqueId] = ultimate.id

        val firstPick = everPicked.add(player.uniqueId)
        if (!force && general.switchCooldown.get() > 0 && !(firstPick && general.firstPickFree.get())) {
            plugin.cooldowns.start(player, SWITCH_KEY, general.switchCooldown.get().toDouble(), hud = false)
        }

        val active = arena != null && plugin.arenas.isActive(arena)
        if (active) {
            applyActive(player, arena!!)
            plugin.tell(player, msgs.selected.get(), *ph)
        } else {
            plugin.tell(player, msgs.selectedPending.get(), *ph)
        }
        Sounds.play(player, general.selectSound.get())
        return SelectResult.OK
    }

    fun switchCooldownRemaining(player: Player): Int =
        Math.ceil(plugin.cooldowns.remainingSeconds(player.uniqueId, SWITCH_KEY)).toInt()

    /** Gives the player's pick its items/effects (if they're in an active arena). */
    fun refresh(player: Player) {
        val arena = plugin.arenas.activeArenaOf(player) ?: return
        applyActive(player, arena)
    }

    private fun applyActive(player: Player, arena: IArena) {
        val ultimate = selected(player)
        ultimate?.let { if (it.enabled.get()) it.activate(player, arena) else it.relinquish(player, switching = true) }
        plugin.giveSelectorItem(player)
    }

    /** Re-gives items to everyone in running ultimate arenas. Returns how many players were refreshed. */
    fun refreshAll(): Int {
        var count = 0
        for (arena in plugin.arenas.active()) {
            for (player in arena.players.toList()) {
                val u = selected(player)
                if (u != null && !u.enabled.get()) {
                    // Disabled by config: take it away from the player entirely.
                    u.relinquish(player, switching = true)
                    selection.remove(player.uniqueId)
                    count++
                } else if (u != null) {
                    if (!arena.isReSpawning(player) && !arena.isSpectator(player)) {
                        u.activate(player, arena)
                        count++
                    }
                }
                plugin.giveSelectorItem(player)
            }
        }
        return count
    }

    // ---- lifecycle events -------------------------------------------------------------------------------------

    fun onArenaActivated(player: Player, arena: IArena) {
        val general = plugin.configs.general
        if (!hasSelection(player)) assignDefault(player)
        if (!arena.isSpectator(player)) applyActive(player, arena)
    }

    private fun assignDefault(player: Player) {
        val setting = plugin.configs.general.defaultUltimate.get().trim()
        val picked: Ultimate? = when {
            setting.isEmpty() || setting.equals("NONE", true) -> null
            setting.equals("RANDOM", true) -> enabled().let { if (it.isEmpty()) null else it[ThreadLocalRandom.current().nextInt(it.size)] }
            else -> get(setting)?.takeIf { it.enabled.get() }
        }
        val choice = picked ?: return
        selection[player.uniqueId] = choice.id
        everPicked.add(player.uniqueId)
        plugin.tell(player, plugin.configs.messages.defaultAssigned.get(), "ultimate" to choice.displayNameColored())
    }

    fun onRespawn(player: Player, arena: IArena) {
        if (!plugin.arenas.isActive(arena)) return
        val u = selected(player)
        if (u != null && u.enabled.get()) u.activate(player, arena)
        plugin.giveSelectorItem(player)
    }

    fun onDeath(player: Player, arena: IArena, finalKill: Boolean) {
        val u = selected(player)
        u?.onHolderDeath(player, arena)
        if (finalKill) {
            u?.relinquish(player, switching = false)
            clearSelection(player)
        }
    }

    /** Player left an arena or the arena ended: drop their state; keep the pick only if configured. */
    fun onArenaDeactivated(player: Player) {
        resetPlayer(player)
        if (!plugin.configs.general.rememberSelection.get()) selection.remove(player.uniqueId)
        plugin.cooldowns.clear(player)
    }

    fun onLeaveArena(player: Player) {
        resetPlayer(player)
        if (!plugin.configs.general.rememberSelection.get()) selection.remove(player.uniqueId)
    }

    private fun resetPlayer(player: Player) {
        selected(player)?.relinquish(player, switching = false)
        UltimateItems.clear(player)
        plugin.removeSelectorItem(player)
        plugin.cooldowns.clear(player)
    }

    fun clearSelection(player: Player) {
        selection.remove(player.uniqueId)
    }

    fun forget(uuid: UUID) {
        selection.remove(uuid)
        everPicked.remove(uuid)
        plugin.cooldowns.forget(uuid)
    }

    fun selectionSnapshot(): Map<UUID, String> = HashMap(selection)

    fun shutdown() {
        selection.clear()
        everPicked.clear()
    }
}
