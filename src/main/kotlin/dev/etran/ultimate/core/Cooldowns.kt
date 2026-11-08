package dev.etran.ultimate.core

import dev.etran.ultimate.UltimatePlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Central cooldown book-keeping plus the cooldown HUD (XP bar / action bar).
 * Cooldowns are keyed per player and per ability and are cleared whenever a player's ultimate state is reset,
 * so nothing can leak across games or players (the reference implementation kept Player objects in maps forever).
 */
class Cooldowns(private val plugin: UltimatePlugin) {

    private class Entry(
        val startMs: Long, val durationMs: Long, val label: String,
        val hud: Boolean, val onReady: (() -> Unit)?
    ) {
        fun remainingMs(now: Long): Long = startMs + durationMs - now
    }

    private val entries = HashMap<UUID, HashMap<String, Entry>>()
    private val hudShown = HashSet<UUID>()
    private var tickCount = 0L

    fun start(
        player: Player, key: String, seconds: Double, label: String = key,
        hud: Boolean = true, onReady: (() -> Unit)? = null
    ) {
        if (seconds <= 0.0) {
            clear(player.uniqueId, key)
            onReady?.invoke()
            return
        }
        entries.getOrPut(player.uniqueId) { HashMap() }[key] =
            Entry(System.currentTimeMillis(), (seconds * 1000).toLong(), label, hud, onReady)
    }

    fun isActive(uuid: UUID, key: String): Boolean {
        val e = entries[uuid]?.get(key) ?: return false
        return e.remainingMs(System.currentTimeMillis()) > 0
    }

    fun remainingSeconds(uuid: UUID, key: String): Double {
        val e = entries[uuid]?.get(key) ?: return 0.0
        return (e.remainingMs(System.currentTimeMillis()) / 1000.0).coerceAtLeast(0.0)
    }

    /** Seconds elapsed since the cooldown started (0 if inactive). */
    fun elapsedSeconds(uuid: UUID, key: String): Double {
        val e = entries[uuid]?.get(key) ?: return 0.0
        return ((System.currentTimeMillis() - e.startMs) / 1000.0).coerceAtLeast(0.0)
    }

    fun clear(uuid: UUID, key: String) {
        entries[uuid]?.remove(key)
    }

    /** Clears every cooldown of a player (including the switch cooldown) without firing ready callbacks, and resets the HUD. */
    fun clear(player: Player) {
        entries.remove(player.uniqueId)
        resetHud(player)
    }

    /**
     * Clears ability cooldowns only. Keys starting with `core:` (the ultimate switch cooldown) are kept, because
     * abilities are reset on every death/respawn and dying must not be a way around the switch cooldown.
     */
    fun clearAbilities(player: Player) {
        val map = entries[player.uniqueId]
        if (map != null) {
            map.keys.removeAll { !it.startsWith("core:") }
            if (map.isEmpty()) entries.remove(player.uniqueId)
        }
        resetHud(player)
    }

    fun forget(uuid: UUID) {
        entries.remove(uuid)
        hudShown.remove(uuid)
    }

    fun clearAll() {
        entries.clear()
        hudShown.clear()
    }

    private fun resetHud(player: Player) {
        if (hudShown.remove(player.uniqueId)) {
            player.exp = 0f
            player.level = 0
        }
    }

    /** Runs once per server tick. */
    fun tick() {
        tickCount++
        val now = System.currentTimeMillis()
        val hudEvery = plugin.configs.general.hudUpdateTicks.get().coerceAtLeast(1)
        val updateHud = tickCount % hudEvery == 0L
        val useXp = plugin.configs.general.hudXpBar.get()
        val useBar = plugin.configs.general.hudActionBar.get()

        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, map) = iterator.next()
            val player = Bukkit.getPlayer(uuid)
            if (player == null || !player.isOnline) {
                iterator.remove()
                hudShown.remove(uuid)
                continue
            }

            var best: Entry? = null
            val ready = ArrayList<Entry>()
            val it2 = map.entries.iterator()
            while (it2.hasNext()) {
                val e = it2.next().value
                val remaining = e.remainingMs(now)
                if (remaining <= 0) {
                    it2.remove()
                    ready.add(e)
                } else if (e.hud && (best == null || remaining > best.remainingMs(now))) {
                    best = e
                }
            }
            if (map.isEmpty()) iterator.remove()

            for (e in ready) {
                try {
                    e.onReady?.invoke()
                } catch (t: Throwable) {
                    plugin.logger.warning("Cooldown ready-callback failed: ${t.message}")
                }
            }

            if (best != null && updateHud) {
                val remaining = best.remainingMs(now)
                if (useXp) {
                    player.exp = (remaining.toFloat() / best.durationMs).coerceIn(0f, 1f)
                    player.level = Math.ceil(remaining / 1000.0).toInt()
                    hudShown.add(uuid)
                }
                if (useBar && tickCount % 5 == 0L) {
                    val text = plugin.configs.general.hudActionBarFormat.get()
                        .replace("{name}", best.label)
                        .replace("{seconds}", Math.ceil(remaining / 1000.0).toInt().toString())
                    plugin.actionBar(player, Text.color(text))
                }
            } else if (best == null) {
                resetHud(player)
            }
        }
        // Players whose last cooldown was removed outside tick (e.g. clear(uuid,key)) still need their XP bar reset.
        if (updateHud && hudShown.isNotEmpty()) {
            val stale = hudShown.filter { entries[it]?.values?.none { e -> e.hud } != false }
            for (uuid in stale) Bukkit.getPlayer(uuid)?.let { resetHud(it) } ?: hudShown.remove(uuid)
        }
    }
}
