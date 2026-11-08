package dev.etran.ultimate.config

import dev.bw1058.ultimate.UltimatePlugin
import dev.bw1058.ultimate.ultimate.Ultimate

class ConfigReport(val errors: List<String>, val warnings: List<String>, val files: Int) {
    val ok: Boolean get() = errors.isEmpty()
}

/** Owns every config file of the plugin and (re)loads them. */
class ConfigManager(private val plugin: UltimatePlugin) {

    val general = GeneralSettings()
    val messages = MessageSettings()

    val generalFile = ConfigFile(plugin.dataFolder, "general", "config.yml", "BedWars1058-Ultimate - general settings", general.specs)
    val messagesFile = ConfigFile(plugin.dataFolder, "messages", "messages.yml", "BedWars1058-Ultimate - messages", messages.specs)

    private val ultimateFiles = LinkedHashMap<String, ConfigFile>()

    fun registerUltimate(ultimate: Ultimate) {
        val file = ConfigFile(
            plugin.dataFolder, ultimate.id, "ultimates/${ultimate.id}.yml",
            "BedWars1058-Ultimate - ${ultimate.defaultName} ultimate", ultimate.specs
        )
        ultimate.configFile = file
        ultimateFiles[ultimate.id] = file
    }

    fun ultimateFile(id: String): ConfigFile? = ultimateFiles[id.lowercase()]

    fun allFiles(): List<ConfigFile> = listOf(generalFile, messagesFile) + ultimateFiles.values

    fun file(id: String): ConfigFile? = allFiles().firstOrNull { it.id.equals(id, ignoreCase = true) }

    /** Loads (creating defaults when missing) every file and validates it. */
    fun loadAll(): ConfigReport {
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()
        for (file in allFiles()) {
            file.load()?.let { errors.add(it) }
            warnings.addAll(file.diagnostics())
        }
        warnings.addAll(crossChecks())
        return ConfigReport(errors, warnings, allFiles().size)
    }

    /** Problems that span options, e.g. two icons sharing one menu slot. */
    private fun crossChecks(): List<String> {
        val result = ArrayList<String>()
        val rows = general.menuRows.get()
        val used = HashMap<Int, String>()
        for (u in plugin.manager.all()) {
            if (!u.enabled.get()) continue
            val slot = u.icon.slot!!.get()
            if (slot >= rows * 9) result.add("ultimates/${u.id}.yml: icon.slot $slot doesn't fit in a $rows-row menu (menu.rows in config.yml)")
            used[slot]?.let { other -> result.add("ultimates/${u.id}.yml: icon.slot $slot is also used by '$other'") }
            used[slot] = u.id
        }
        if (general.menuBackEnabled.get()) {
            val backSlot = general.menuBack.slot!!.get()
            if (backSlot >= rows * 9) result.add("config.yml: menu.back-button.slot $backSlot doesn't fit in a $rows-row menu")
            used[backSlot]?.let { other -> result.add("config.yml: menu.back-button.slot $backSlot collides with the icon of '$other'") }
        }
        return result
    }
}
