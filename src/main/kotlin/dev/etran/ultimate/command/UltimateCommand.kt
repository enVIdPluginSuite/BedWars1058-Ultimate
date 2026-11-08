package dev.etran.ultimate.command

import dev.etran.ultimate.UltimatePlugin
import dev.etran.ultimate.compat.McVersion
import dev.etran.ultimate.config.ConfigFile
import dev.etran.ultimate.config.Opt
import dev.etran.ultimate.config.OptType
import dev.etran.ultimate.config.OptValues
import dev.etran.ultimate.core.Text
import dev.etran.ultimate.gui.ConfigBrowserMenu
import dev.etran.ultimate.gui.ConfigRootMenu
import dev.etran.ultimate.gui.UltimateMenu
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/** `/bwu` - selection, reload, and the whole config editor. */
class UltimateCommand(private val plugin: UltimatePlugin) : CommandExecutor, TabCompleter {

    private val msgs get() = plugin.configs.messages

    private companion object {
        const val ADMIN = "etranultimate.admin"
        const val USE = "etranultimate.use"
        const val RELOAD = "etranultimate.reload"
        const val CONFIG = "etranultimate.config"
        const val GIVE = "etranultimate.give"
        val SUBCOMMANDS = listOf("help", "list", "info", "select", "menu", "reload", "config", "enable", "disable", "give", "clear", "refresh", "status")
    }

    private fun allowed(sender: CommandSender, vararg nodes: String): Boolean =
        sender.hasPermission(ADMIN) || nodes.any { sender.hasPermission(it) }

    private fun deny(sender: CommandSender): Boolean {
        plugin.reply(sender, msgs.noPermission.get())
        return true
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val sub = args.firstOrNull()?.lowercase() ?: "help"
        val rest = args.drop(1)
        when (sub) {
            "help", "?" -> help(sender, label)
            "list" -> list(sender)
            "info" -> info(sender, rest)
            "select", "pick" -> if (!allowed(sender, USE)) return deny(sender) else select(sender, rest)
            "menu" -> if (!allowed(sender, USE)) return deny(sender) else openMenu(sender)
            "reload" -> if (!allowed(sender, RELOAD)) return deny(sender) else reload(sender)
            "config", "cfg" -> if (!allowed(sender, CONFIG)) return deny(sender) else config(sender, rest, label)
            "enable", "disable" -> if (!allowed(sender, CONFIG)) return deny(sender) else toggle(sender, rest, sub == "enable")
            "give" -> if (!allowed(sender, GIVE)) return deny(sender) else give(sender, rest)
            "clear" -> if (!allowed(sender, GIVE)) return deny(sender) else clear(sender, rest)
            "refresh" -> if (!allowed(sender, RELOAD)) return deny(sender) else refresh(sender)
            "status", "debug" -> if (!allowed(sender, ADMIN)) return deny(sender) else status(sender)
            else -> plugin.reply(sender, msgs.unknownCommand.get())
        }
        return true
    }

    // ---- simple subcommands -------------------------------------------------------------------------------------

    private fun help(sender: CommandSender, label: String) {
        val lines = ArrayList<String>()
        lines.add("&6&lBedWars1058-Ultimate &7v${plugin.description.version}")
        lines.add("&e/$label list &7- list the ultimates")
        lines.add("&e/$label info <ultimate> &7- describe one")
        if (allowed(sender, USE)) {
            lines.add("&e/$label menu &7- open the selection menu")
            lines.add("&e/$label select <ultimate> &7- pick your ultimate")
        }
        if (allowed(sender, CONFIG)) {
            lines.add("&e/$label config &7- open the in-game config editor")
            lines.add("&e/$label config get|set|reset|list <file> <option> [value] &7- edit from chat")
            lines.add("&e/$label config validate | update &7- check files / add missing options")
            lines.add("&e/$label enable|disable <ultimate> &7- switch an ultimate on or off")
        }
        if (allowed(sender, RELOAD)) {
            lines.add("&e/$label reload &7- reload every config file")
            lines.add("&e/$label refresh &7- re-give ultimate items to players")
        }
        if (allowed(sender, GIVE)) {
            lines.add("&e/$label give <player> <ultimate> &7- force an ultimate")
            lines.add("&e/$label clear <player> &7- remove a player's ultimate")
        }
        if (allowed(sender, ADMIN)) lines.add("&e/$label status &7- diagnostics")
        for (l in lines) Text.send(sender, l)
    }

    private fun resolve(sender: CommandSender, id: String?): dev.etran.ultimate.ultimate.Ultimate? {
        val u = id?.let { plugin.manager.get(it) }
        if (u == null) {
            plugin.reply(
                sender, msgs.unknownUltimate.get(),
                "id" to (id ?: ""), "list" to plugin.manager.all().joinToString(", ") { it.id }
            )
        }
        return u
    }

    private fun list(sender: CommandSender) {
        plugin.reply(sender, "&6Ultimates:")
        for (u in plugin.manager.all()) {
            val state = if (u.enabled.get()) "&aenabled" else "&cdisabled"
            Text.send(sender, "&7 - &e${u.id} &8(${Text.color(u.displayName.get())}&8) $state")
        }
    }

    private fun info(sender: CommandSender, args: List<String>) {
        val u = resolve(sender, args.firstOrNull()) ?: return
        val ph = HashMap(u.placeholders())
        ph["status"] = ""
        ph["name"] = u.displayName.get()
        Text.send(sender, "&6&l${Text.strip(u.displayName.get())} &7(${u.id}) " + if (u.enabled.get()) "&aenabled" else "&cdisabled")
        for (line in u.icon.lore.get()) {
            val text = Text.apply(line, ph)
            if (text.isNotBlank()) Text.send(sender, "  " + text)
        }
    }

    private fun select(sender: CommandSender, args: List<String>) {
        val player = sender as? Player ?: return plugin.reply(sender, msgs.playersOnly.get())
        val u = resolve(sender, args.firstOrNull()) ?: return
        plugin.manager.select(player, u)
    }

    private fun openMenu(sender: CommandSender) {
        val player = sender as? Player ?: return plugin.reply(sender, msgs.playersOnly.get())
        UltimateMenu(plugin, player).open()
    }

    private fun reload(sender: CommandSender) {
        plugin.sendReloadSummary(sender, plugin.reloadAll())
    }

    private fun refresh(sender: CommandSender) {
        plugin.reply(sender, "&aRefreshed &f${plugin.manager.refreshAll()} &aplayer(s).")
    }

    private fun toggle(sender: CommandSender, args: List<String>, enable: Boolean) {
        val u = resolve(sender, args.firstOrNull()) ?: return
        val error = u.configFile.write(u.enabled, enable)
        if (error != null) return plugin.reply(sender, msgs.configWriteFailed.get(), "reason" to error)
        plugin.reply(sender, msgs.configSet.get(), "file" to u.id, "path" to "enabled", "value" to enable.toString())
        if (!enable) plugin.manager.refreshAll()
    }

    private fun give(sender: CommandSender, args: List<String>) {
        val target = args.getOrNull(0)?.let { Bukkit.getPlayerExact(it) }
        if (target == null) return plugin.reply(sender, msgs.unknownPlayer.get(), "name" to (args.getOrNull(0) ?: ""))
        val u = resolve(sender, args.getOrNull(1)) ?: return
        plugin.manager.select(target, u, force = true)
        plugin.reply(sender, "&aGave &f${target.name} &athe &6${Text.strip(u.displayName.get())} &aultimate.")
    }

    private fun clear(sender: CommandSender, args: List<String>) {
        val target = args.getOrNull(0)?.let { Bukkit.getPlayerExact(it) }
        if (target == null) return plugin.reply(sender, msgs.unknownPlayer.get(), "name" to (args.getOrNull(0) ?: ""))
        plugin.manager.selected(target)?.relinquish(target, switching = true)
        plugin.manager.clearSelection(target)
        plugin.reply(sender, "&aRemoved the ultimate of &f${target.name}&a.")
    }

    private fun status(sender: CommandSender) {
        Text.send(sender, "&6&lStatus")
        Text.send(sender, "&7Minecraft: &f${McVersion.current} &8(${plugin.server.bukkitVersion})")
        Text.send(sender, "&7Java: &f${System.getProperty("java.version")}")
        Text.send(sender, "&7BedWars1058: &f${plugin.server.pluginManager.getPlugin("BedWars1058")?.description?.version ?: "?"}")
        Text.send(sender, "&7Active ultimate arenas: &f${plugin.arenas.activeCount()}")
        val picks = plugin.manager.selectionSnapshot().values.groupingBy { it }.eachCount()
        for (u in plugin.manager.all()) {
            Text.send(sender, "&7 - &e${u.id}&7: ${if (u.enabled.get()) "&aon" else "&coff"} &7holders: &f${picks[u.id] ?: 0}")
        }
    }

    // ---- config subcommands -------------------------------------------------------------------------------------

    private fun fileOf(sender: CommandSender, id: String?): ConfigFile? {
        val file = id?.let { plugin.configs.file(it) }
        if (file == null) {
            plugin.reply(sender, msgs.configUnknownFile.get(), "id" to (id ?: ""), "list" to plugin.configs.allFiles().joinToString(", ") { it.id })
        }
        return file
    }

    private fun optOf(sender: CommandSender, file: ConfigFile, path: String?): Opt<*>? {
        val opt = path?.let { file.specs.find(it) }
        if (opt == null) plugin.reply(sender, msgs.configUnknownOption.get(), "file" to file.id, "path" to (path ?: ""))
        return opt
    }

    private fun config(sender: CommandSender, args: List<String>, label: String) {
        val action = args.firstOrNull()?.lowercase() ?: "gui"
        when (action) {
            "gui", "menu" -> {
                val player = sender as? Player ?: return plugin.reply(sender, msgs.playersOnly.get())
                val fileArg = args.getOrNull(1)
                if (fileArg != null) {
                    val file = fileOf(sender, fileArg) ?: return
                    ConfigBrowserMenu(plugin, player, file).open()
                } else ConfigRootMenu(plugin, player).open()
            }
            "get" -> {
                val file = fileOf(sender, args.getOrNull(1)) ?: return
                val opt = optOf(sender, file, args.getOrNull(2)) ?: return
                plugin.reply(sender, msgs.configGet.get(), "path" to opt.path, "value" to OptValues.display(opt.get()), "default" to OptValues.display(opt.default))
            }
            "set" -> {
                val file = fileOf(sender, args.getOrNull(1)) ?: return
                val opt = optOf(sender, file, args.getOrNull(2)) ?: return
                val text = args.drop(3).joinToString(" ")
                if (text.isEmpty() && !opt.type.isList && opt.type != OptType.TEXT && opt.type != OptType.STRING && opt.type != OptType.SOUND && opt.type != OptType.PARTICLE) {
                    return plugin.reply(sender, "&cUsage: /$label config set ${file.id} ${opt.path} <value>")
                }
                when (val result = OptValues.parse(opt, text)) {
                    is OptValues.Result.Err -> plugin.reply(sender, msgs.configInvalid.get(), "path" to opt.path, "reason" to result.message)
                    is OptValues.Result.Ok -> {
                        val error = file.write(opt, result.value)
                        if (error != null) plugin.reply(sender, msgs.configWriteFailed.get(), "reason" to error)
                        else plugin.reply(sender, msgs.configSet.get(), "file" to file.id, "path" to opt.path, "value" to OptValues.display(result.value))
                    }
                }
            }
            "reset" -> {
                val file = fileOf(sender, args.getOrNull(1)) ?: return
                val opt = optOf(sender, file, args.getOrNull(2)) ?: return
                val error = file.reset(opt)
                if (error != null) plugin.reply(sender, msgs.configWriteFailed.get(), "reason" to error)
                else plugin.reply(sender, msgs.configReset.get(), "file" to file.id, "path" to opt.path, "value" to OptValues.display(opt.default))
            }
            "list" -> {
                val file = fileOf(sender, args.getOrNull(1)) ?: return
                val prefix = args.getOrNull(2) ?: ""
                val matches = file.options.filter { it.path.startsWith(prefix) }
                if (matches.isEmpty()) return plugin.reply(sender, msgs.configUnknownOption.get(), "file" to file.id, "path" to prefix)
                for (opt in matches.take(40)) Text.send(sender, "&e${opt.path} &7= &f${OptValues.display(opt.get()).take(60)}")
                if (matches.size > 40) Text.send(sender, "&7... ${matches.size - 40} more. Narrow it down with &e/$label config list ${file.id} <prefix>")
            }
            "validate" -> {
                val problems = plugin.configs.allFiles().flatMap { it.diagnostics() }
                if (problems.isEmpty()) plugin.reply(sender, "&aNo problems found.")
                else for (p in problems) plugin.tell(sender, msgs.reloadLine.get(), "text" to p)
            }
            "update" -> {
                var added = 0
                for (f in plugin.configs.allFiles()) added += f.addMissing()
                plugin.reply(sender, msgs.configAddedMissing.get(), "count" to added.toString())
            }
            else -> plugin.reply(sender, "&cUsage: /$label config [gui|get|set|reset|list|validate|update]")
        }
    }

    // ---- tab completion -----------------------------------------------------------------------------------------

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val current = args.lastOrNull()?.lowercase() ?: ""
        val options: List<String> = when {
            args.size <= 1 -> SUBCOMMANDS.filter { visible(sender, it) }
            else -> when (args[0].lowercase()) {
                "info", "select", "pick", "enable", "disable" -> if (args.size == 2) plugin.manager.all().map { it.id } else emptyList()
                "give" -> when (args.size) {
                    2 -> Bukkit.getOnlinePlayers().map { it.name }
                    3 -> plugin.manager.all().map { it.id }
                    else -> emptyList()
                }
                "clear" -> if (args.size == 2) Bukkit.getOnlinePlayers().map { it.name } else emptyList()
                "config", "cfg" -> configTab(args)
                else -> emptyList()
            }
        }
        return options.filter { it.lowercase().startsWith(current) }.sorted()
    }

    private fun visible(sender: CommandSender, sub: String): Boolean = when (sub) {
        "select", "menu" -> allowed(sender, USE)
        "reload", "refresh" -> allowed(sender, RELOAD)
        "config", "enable", "disable" -> allowed(sender, CONFIG)
        "give", "clear" -> allowed(sender, GIVE)
        "status" -> allowed(sender, ADMIN)
        else -> true
    }

    private fun configTab(args: Array<out String>): List<String> {
        if (args.size == 2) return listOf("gui", "get", "set", "reset", "list", "validate", "update")
        val action = args[1].lowercase()
        if (action in setOf("gui", "menu", "get", "set", "reset", "list")) {
            if (args.size == 3) return plugin.configs.allFiles().map { it.id }
            val file = plugin.configs.file(args[2]) ?: return emptyList()
            if (args.size == 4 && action != "gui" && action != "menu") return file.options.map { it.path }
            if (args.size == 5 && action == "set") {
                val opt = file.specs.find(args[3]) ?: return emptyList()
                return when (opt.type) {
                    OptType.BOOLEAN -> listOf("true", "false")
                    OptType.CHOICE -> opt.choices
                    else -> listOf(OptValues.display(opt.get()))
                }
            }
        }
        return emptyList()
    }
}
