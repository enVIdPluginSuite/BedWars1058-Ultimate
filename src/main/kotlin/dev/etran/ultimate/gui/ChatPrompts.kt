package dev.etran.ultimate.gui

import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.config.ConfigFile
import dev.bw1058.ultimate.config.Opt
import dev.bw1058.ultimate.config.OptValues
import dev.bw1058.ultimate.core.Text
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.UUID

/** Lets an admin type a new value for a config option in chat (used by the in-game editor). */
class ChatPrompts(private val plugin: UltimatePlugin) : Listener {

    private class Session(val file: ConfigFile, val opt: Opt<*>, val done: () -> Unit, val list: MutableList<String>?)

    private val sessions = HashMap<UUID, Session>()

    fun isPrompting(uuid: UUID): Boolean = sessions.containsKey(uuid)

    fun forget(uuid: UUID) {
        sessions.remove(uuid)
    }

    fun promptValue(player: Player, file: ConfigFile, opt: Opt<*>, done: () -> Unit) {
        player.closeInventory()
        val current = file.raw(opt) ?: opt.get()
        sessions[player.uniqueId] = Session(file, opt, done, null)
        val ph = arrayOf("path" to opt.path, "value" to OptValues.display(current))
        plugin.reply(player, plugin.configs.messages.promptValue.get(), *ph)
        if (opt.type == dev.bw1058.ultimate.config.OptType.CHOICE) {
            plugin.reply(player, "&7Allowed: &f" + opt.choices.joinToString(", "))
        }
    }

    fun promptList(player: Player, file: ConfigFile, opt: Opt<*>, done: () -> Unit) {
        player.closeInventory()
        @Suppress("UNCHECKED_CAST")
        val current = ((file.raw(opt) as? List<*>)?.map { it.toString() } ?: (opt.get() as List<String>)).toMutableList()
        sessions[player.uniqueId] = Session(file, opt, done, current)
        plugin.reply(player, plugin.configs.messages.promptList.get(), "path" to opt.path)
        preview(player, current)
    }

    private fun preview(player: Player, list: List<String>) {
        if (list.isEmpty()) {
            plugin.reply(player, "&7(the list is empty)")
            return
        }
        for ((i, line) in list.withIndex()) {
            plugin.tell(player, plugin.configs.messages.promptListLine.get(), "index" to (i + 1).toString(), "text" to Text.color(line))
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncPlayerChatEvent) {
        val uuid = event.player.uniqueId
        if (!sessions.containsKey(uuid)) return
        event.isCancelled = true
        event.recipients.clear()
        val message = event.message
        Bukkit.getScheduler().runTask(plugin, Runnable { handle(event.player, message) })
    }

    private fun handle(player: Player, message: String) {
        val session = sessions[player.uniqueId] ?: return
        if (message.trim().equals("cancel", ignoreCase = true)) {
            finish(player, session, plugin.configs.messages.promptCancelled.get())
            return
        }
        if (session.list != null) handleList(player, session, message) else handleValue(player, session, message)
    }

    private fun handleValue(player: Player, session: Session, message: String) {
        val msgs = plugin.configs.messages
        when (val result = OptValues.parse(session.opt, message)) {
            is OptValues.Result.Err -> plugin.reply(player, msgs.configInvalid.get(), "path" to session.opt.path, "reason" to result.message)
            is OptValues.Result.Ok -> {
                val error = session.file.write(session.opt, result.value)
                val text = if (error == null) {
                    Text.apply(msgs.configSet.get(), mapOf("file" to session.file.id, "path" to session.opt.path, "value" to OptValues.display(result.value)))
                } else Text.apply(msgs.configWriteFailed.get(), mapOf("reason" to error))
                finish(player, session, text)
            }
        }
    }

    private fun handleList(player: Player, session: Session, message: String) {
        val list = session.list!!
        val msgs = plugin.configs.messages
        val parts = message.trim().split(Regex("\\s+"), limit = 3)
        val command = parts[0].lowercase()
        fun text(from: Int): String = message.trim().split(Regex("\\s+"), limit = from + 1).getOrNull(from) ?: ""
        fun index(): Int? = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..list.size }

        when (command) {
            "add" -> list.add(text(1))
            "set" -> {
                val i = index()
                if (i == null) plugin.reply(player, "&cUsage: set <line number> <text>") else list[i - 1] = parts.getOrNull(2) ?: ""
            }
            "insert" -> {
                val i = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..(list.size + 1) }
                if (i == null) plugin.reply(player, "&cUsage: insert <line number> <text>") else list.add(i - 1, parts.getOrNull(2) ?: "")
            }
            "remove" -> {
                val i = index()
                if (i == null) plugin.reply(player, "&cUsage: remove <line number>") else list.removeAt(i - 1)
            }
            "clear" -> list.clear()
            "done" -> {
                val error = session.file.write(session.opt, list.toList())
                val out = if (error == null) {
                    Text.apply(msgs.configSet.get(), mapOf("file" to session.file.id, "path" to session.opt.path, "value" to "${list.size} line(s)"))
                } else Text.apply(msgs.configWriteFailed.get(), mapOf("reason" to error))
                finish(player, session, out)
                return
            }
            else -> {
                plugin.reply(player, plugin.configs.messages.promptList.get(), "path" to session.opt.path)
                return
            }
        }
        preview(player, list)
    }

    private fun finish(player: Player, session: Session, message: String) {
        sessions.remove(player.uniqueId)
        plugin.reply(player, message)
        session.done()
    }
}
