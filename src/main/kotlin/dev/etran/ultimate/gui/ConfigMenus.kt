package dev.etran.ultimate.gui

import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.config.ConfigFile
import dev.bw1058.ultimate.config.Opt
import dev.bw1058.ultimate.config.OptType
import dev.bw1058.ultimate.config.OptValues
import dev.bw1058.ultimate.config.YamlWriter
import dev.bw1058.ultimate.core.ItemFactory
import dev.bw1058.ultimate.core.Text
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.math.RoundingMode

private const val UI_CLICK = "CLICK"

/** Runs [block] one tick later; opening a new inventory from inside a click event is unreliable. */
private fun UltimatePlugin.later(block: () -> Unit) {
    server.scheduler.runTask(this, Runnable { block() })
}

/** Top level of the in-game config editor: pick a file. */
class ConfigRootMenu(plugin: UltimatePlugin, player: Player) : Menu(plugin, player) {

    override fun title(): String = Text.color("&8Ultimate config")
    override fun rows(): Int = 4

    override fun render() {
        val filler = ItemFactory.create("GRAY_STAINED_GLASS_PANE", name = " ")
        for (slot in 0 until size()) set(slot, filler)

        val general = plugin.configs.generalFile
        set(
            10,
            ItemFactory.create("COMMAND_BLOCK", name = "&6General settings", lore = listOf("&7Arenas, selection, fall damage,", "&7HUD, menu, shop button, ...", "", "&8config.yml", "&eClick to edit"))
        ) { plugin.later { ConfigBrowserMenu(plugin, player, general).open() } }

        val messages = plugin.configs.messagesFile
        set(
            11,
            ItemFactory.create("WRITABLE_BOOK", name = "&6Messages", lore = listOf("&7Chat messages sent by the plugin.", "", "&8messages.yml", "&eClick to edit"))
        ) { plugin.later { ConfigBrowserMenu(plugin, player, messages).open() } }

        var slot = 19
        for (u in plugin.manager.all()) {
            val file = u.configFile
            val state = if (u.enabled.get()) "&aEnabled" else "&cDisabled"
            set(
                slot++,
                ItemFactory.create(
                    u.icon.material?.get() ?: "STONE", name = "&6${Text.strip(u.displayName.get())}",
                    lore = listOf("&7State: $state", "&7Options: &f${file.options.size}", "", "&8ultimates/${u.id}.yml", "&eClick to edit"),
                    glow = u.enabled.get()
                )
            ) { plugin.later { ConfigBrowserMenu(plugin, player, file).open() } }
        }

        set(28, ItemFactory.create("EMERALD", name = "&aReload from disk", lore = listOf("&7Same as &e/bwu reload&7.", "&7Re-reads every file."))) {
            val result = plugin.reloadAll()
            plugin.sendReloadSummary(player, result)
            refresh()
        }
        set(29, ItemFactory.create("HOPPER", name = "&aAdd missing options", lore = listOf("&7Writes any option that is not in", "&7your files yet (with its comments)."))) {
            var added = 0
            for (f in plugin.configs.allFiles()) added += f.addMissing()
            plugin.reply(player, plugin.configs.messages.configAddedMissing.get(), "count" to added.toString())
            refresh()
        }
        set(30, ItemFactory.create("PAPER", name = "&aApply to players", lore = listOf("&7Re-gives ultimate items (new names,", "&7lore, materials) in running games.", "&cThis resets ability cooldowns."))) {
            val count = plugin.manager.refreshAll()
            plugin.reply(player, "&aRefreshed &f$count &aplayer(s).")
        }
        set(31, ItemFactory.create("BOOK", name = "&aValidate", lore = listOf("&7Checks every value and prints", "&7problems to the chat."))) {
            val problems = plugin.configs.allFiles().flatMap { it.diagnostics() }
            if (problems.isEmpty()) plugin.reply(player, "&aNo problems found.")
            else for (p in problems) plugin.tell(player, plugin.configs.messages.reloadLine.get(), "text" to p)
        }
        set(35, ItemFactory.create("BARRIER", name = "&cClose")) { player.closeInventory() }
    }
}

/** Browses and edits the options of one config file, one section at a time. */
class ConfigBrowserMenu(
    plugin: UltimatePlugin, player: Player,
    private val file: ConfigFile,
    private val prefix: String = "",
    private var page: Int = 0
) : Menu(plugin, player) {

    private class Entry(val name: String, val opt: Opt<*>?, val sectionPath: String?)

    private val perPage = 45

    override fun title(): String {
        val where = if (prefix.isEmpty()) file.relativePath else "${file.id}: $prefix"
        return Text.color("&8$where")
    }

    override fun rows(): Int = 6

    private fun entries(): List<Entry> {
        val base = if (prefix.isEmpty()) "" else "$prefix."
        val map = LinkedHashMap<String, Entry>()
        for (o in file.options) {
            if (!o.path.startsWith(base)) continue
            val rest = o.path.substring(base.length)
            val seg = rest.substringBefore('.')
            if (rest == seg) map[seg] = Entry(seg, o, null) else map.getOrPut(seg) { Entry(seg, null, base + seg) }
        }
        return map.values.toList()
    }

    override fun render() {
        val all = entries()
        val pages = Math.max(1, (all.size + perPage - 1) / perPage)
        page = page.coerceIn(0, pages - 1)

        for ((i, entry) in all.drop(page * perPage).take(perPage).withIndex()) {
            if (entry.opt != null) {
                set(i, optionItem(entry.opt)) { onOptionClick(entry.opt, it) }
            } else {
                set(i, sectionItem(entry)) {
                    val target = entry.sectionPath!!
                    plugin.later { ConfigBrowserMenu(plugin, player, file, target).open() }
                }
            }
        }

        val nav = ItemFactory.create("BLACK_STAINED_GLASS_PANE", name = " ")
        for (slot in 45..53) set(slot, nav)

        set(45, ItemFactory.create("ARROW", name = "&eBack", lore = listOf(if (prefix.isEmpty()) "&7To the file list" else "&7Up one level"))) {
            plugin.later {
                if (prefix.isEmpty()) ConfigRootMenu(plugin, player).open()
                else ConfigBrowserMenu(plugin, player, file, prefix.substringBeforeLast('.', "")).open()
            }
        }
        if (page > 0) set(48, ItemFactory.create("PAPER", name = "&ePrevious page")) {
            page--
            refresh()
        }
        set(49, ItemFactory.create("BOOK", name = "&7Page ${page + 1}/$pages", lore = listOf("&8${file.relativePath}${if (prefix.isEmpty()) "" else " > $prefix"}")))
        if (page < pages - 1) set(50, ItemFactory.create("PAPER", name = "&eNext page")) {
            page++
            refresh()
        }
        set(53, ItemFactory.create("BARRIER", name = "&cClose")) { player.closeInventory() }
    }

    private fun sectionItem(entry: Entry): ItemStack {
        val path = entry.sectionPath!!
        val count = file.options.count { it.path.startsWith("$path.") }
        val banner = file.specs.sections[path]
        val lore = ArrayList<String>()
        if (banner != null) lore.addAll(YamlWriter.wrap(banner.lowercase().replaceFirstChar { it.uppercase() }, 36).map { "&7$it" })
        lore.add("")
        lore.add("&7Options inside: &f$count")
        lore.add("&eClick to open")
        return ItemFactory.create("CHEST", name = "&6${entry.name}", lore = lore)
    }

    private fun shown(opt: Opt<*>, value: Any?): String {
        val text = OptValues.display(value)
        // Keep colour codes visible instead of letting the lore renderer colour them.
        return if (opt.type == OptType.TEXT || opt.type == OptType.TEXT_LIST) text.replace("&", "&​") else text
    }

    private fun optionItem(opt: Opt<*>): ItemStack {
        val value: Any = opt.get()
        val lore = ArrayList<String>()
        lore.addAll(YamlWriter.wrap(opt.description, 38).map { "&7$it" })
        lore.add("")
        if (value is List<*>) {
            lore.add("&7Value: &f${value.size} line(s)")
            for (line in value.take(8)) lore.add("&8 - &f${shown(opt, line)}")
            if (value.size > 8) lore.add("&8 ... and ${value.size - 8} more")
        } else {
            lore.add("&7Value: &f${shown(opt, value)}")
        }
        lore.add("&8Default: ${shown(opt, opt.default).take(40)}")
        if (opt.type.isNumber && (opt.min != null || opt.max != null)) {
            lore.add("&8Range: ${opt.min?.let { OptValues.display(it) } ?: "-"} to ${opt.max?.let { OptValues.display(it) } ?: "-"}")
        }
        lore.add("")
        lore.addAll(hints(opt))
        lore.add("&6Shift + right-click&7: reset to default")

        val material = when (opt.type) {
            OptType.BOOLEAN -> if (value == true) "LIME_WOOL" else "RED_WOOL"
            OptType.INT, OptType.DOUBLE -> "CLOCK"
            OptType.STRING -> "NAME_TAG"
            OptType.TEXT -> "SIGN"
            OptType.SOUND -> "NOTE_BLOCK"
            OptType.MATERIAL -> if (dev.bw1058.ultimate.compat.Materials.isValid(value.toString())) value.toString() else "BARRIER"
            OptType.PARTICLE -> "BLAZE_POWDER"
            OptType.EFFECT -> "BREWING_STAND"
            OptType.CHOICE -> "COMPARATOR"
            OptType.TEXT_LIST, OptType.STRING_LIST, OptType.MATERIAL_LIST -> "BOOK"
        }
        return ItemFactory.create(material, name = "&e${opt.key}", lore = lore)
    }

    private fun hints(opt: Opt<*>): List<String> = when (opt.type) {
        OptType.BOOLEAN -> listOf("&6Click&7: toggle")
        OptType.INT, OptType.DOUBLE -> listOf("&6Left / right click&7: +/- ${stepOf(opt)}", "&6Shift + click&7: x10", "&6Middle click or Q&7: type a value")
        OptType.CHOICE -> listOf("&6Left / right click&7: next / previous")
        OptType.MATERIAL -> listOf("&6Click with an item on your cursor&7: use that item", "&6Click&7: type a name in chat")
        OptType.TEXT_LIST, OptType.STRING_LIST, OptType.MATERIAL_LIST -> listOf("&6Click&7: edit the list in chat")
        else -> listOf("&6Click&7: type a new value in chat")
    }

    private fun stepOf(opt: Opt<*>): String =
        if (opt.type == OptType.INT) (opt.step ?: 1.0).toInt().coerceAtLeast(1).toString()
        else OptValues.formatDouble(opt.step ?: 0.1)

    private fun write(opt: Opt<*>, value: Any) {
        val error = file.write(opt, value)
        if (error != null) {
            plugin.reply(player, plugin.configs.messages.configWriteFailed.get(), "reason" to error)
            return
        }
        for (u in plugin.manager.all()) u.onReload()
        Sounds.play(player, UI_CLICK)
        refresh()
    }

    private fun tryParseAndWrite(opt: Opt<*>, text: String) {
        when (val result = OptValues.parse(opt, text)) {
            is OptValues.Result.Ok -> write(opt, result.value)
            is OptValues.Result.Err -> plugin.reply(player, plugin.configs.messages.configInvalid.get(), "path" to opt.path, "reason" to result.message)
        }
    }

    private fun onOptionClick(opt: Opt<*>, event: InventoryClickEvent) {
        val click = event.click
        if (click == ClickType.SHIFT_RIGHT) {
            file.reset(opt)?.let { plugin.reply(player, plugin.configs.messages.configWriteFailed.get(), "reason" to it) }
            refresh()
            return
        }
        val reopen = { plugin.later { ConfigBrowserMenu(plugin, player, file, prefix, page).open() } }

        when (opt.type) {
            OptType.BOOLEAN -> write(opt, opt.get() != true)
            OptType.INT, OptType.DOUBLE -> {
                if (click == ClickType.MIDDLE || click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
                    plugin.later { plugin.prompts.promptValue(player, file, opt, reopen) }
                    return
                }
                val multiplier = if (click.isShiftClick) 10.0 else 1.0
                val direction = if (click.isLeftClick) 1.0 else if (click.isRightClick) -1.0 else return
                val step = (opt.step ?: if (opt.type == OptType.INT) 1.0 else 0.1) * multiplier
                val current = (opt.get() as Number).toDouble()
                val next = BigDecimal(current + direction * step).setScale(4, RoundingMode.HALF_UP).toDouble()
                val text = if (opt.type == OptType.INT) Math.round(next).toString() else OptValues.formatDouble(next)
                tryParseAndWrite(opt, text)
            }
            OptType.CHOICE -> {
                val choices = opt.choices
                val index = choices.indexOfFirst { it.equals(opt.get().toString(), ignoreCase = true) }.coerceAtLeast(0)
                val next = if (click.isRightClick) (index - 1 + choices.size) % choices.size else (index + 1) % choices.size
                write(opt, choices[next])
            }
            OptType.MATERIAL -> {
                val cursor = event.cursor
                if (cursor != null && cursor.type != Material.AIR) {
                    write(opt, cursor.type.name)
                } else {
                    plugin.later { plugin.prompts.promptValue(player, file, opt, reopen) }
                }
            }
            OptType.TEXT_LIST, OptType.STRING_LIST, OptType.MATERIAL_LIST ->
                plugin.later { plugin.prompts.promptList(player, file, opt, reopen) }
            else -> plugin.later { plugin.prompts.promptValue(player, file, opt, reopen) }
        }
    }
}
