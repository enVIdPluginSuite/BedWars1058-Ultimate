package dev.etran.ultimate.config

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * One YAML file backed by a set of declared options ([Specs]).
 *
 * - Creates a fully commented default file on first run.
 * - Reads values into a typed cache, so [Opt.get] is cheap and never throws.
 * - A file that fails to parse keeps the previous values in effect and reports the error.
 * - Edits go through [YamlEditor], preserving the admin's comments and formatting.
 */
class ConfigFile(
    baseDir: File,
    val id: String,
    val relativePath: String,
    val title: String,
    val specs: Specs
) {
    val file: File = File(baseDir, relativePath)

    var yaml: YamlConfiguration = YamlConfiguration()
        private set

    @Volatile
    private var cache: Map<String, Any> = emptyMap()

    val options: List<Opt<*>> get() = specs.all

    init {
        for (opt in specs.all) opt.owner = this
    }

    internal fun cached(path: String): Any? = cache[path]

    private fun header(): List<String> = listOf(
        "# ==============================================================================",
        "# $title",
        "# Every option below is documented. You can edit this file and run /bwu reload,",
        "# or change values in-game with /bwu config. Missing options fall back to their",
        "# defaults, so deleting a line is always safe.",
        "# =============================================================================="
    )

    fun ensureExists() {
        if (file.exists()) return
        file.parentFile?.mkdirs()
        Files.write(file.toPath(), YamlWriter.render(header(), specs).toByteArray(StandardCharsets.UTF_8))
    }

    /** Loads the file from disk. Returns null on success or an error message (previous values are kept on failure). */
    fun load(): String? {
        try {
            ensureExists()
        } catch (e: IOException) {
            return "Could not create ${relativePath}: ${e.message}"
        }
        val fresh = YamlConfiguration()
        try {
            fresh.load(file)
        } catch (e: InvalidConfigurationException) {
            return "${relativePath} has a YAML syntax error and was NOT reloaded: ${e.message?.lineSequence()?.firstOrNull()}"
        } catch (e: IOException) {
            return "Could not read ${relativePath}: ${e.message}"
        } catch (e: RuntimeException) {
            return "Could not parse ${relativePath}: ${e.message}"
        }
        yaml = fresh
        rebuildCache()
        return null
    }

    private fun rebuildCache() {
        val map = HashMap<String, Any>()
        for (opt in specs.all) {
            OptValues.coerce(opt, yaml.get(opt.path))?.let { map[opt.path] = it }
        }
        cache = map
    }

    /** Raw stored value (null when the key is absent from the file). */
    fun raw(opt: Opt<*>): Any? = yaml.get(opt.path)

    fun isSet(opt: Opt<*>): Boolean = yaml.contains(opt.path)

    /** Problems found in the loaded file: invalid values and unknown keys. Empty when everything is fine. */
    fun diagnostics(): List<String> {
        val problems = ArrayList<String>()
        val known = specs.all.map { it.path }.toSet()
        for (opt in specs.all) {
            val raw = yaml.get(opt.path) ?: continue
            OptValues.validate(opt, raw)?.let { problems.add("$relativePath: ${opt.path}: $it (using default)") }
        }
        for (key in yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key)) continue
            if (key !in known) problems.add("$relativePath: unknown option '$key' (ignored - typo?)")
        }
        return problems
    }

    fun missingOptions(): List<Opt<*>> = specs.all.filter { !yaml.contains(it.path) }

    private fun readLines(): MutableList<String> =
        Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).toMutableList()

    private fun writeLines(lines: List<String>): String? {
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            return "Could not write ${relativePath}: ${e.message}"
        }
        return load()
    }

    /** Writes [value] (already validated/coerced) for [opt]. Returns null on success, else an error message. */
    fun write(opt: Opt<*>, value: Any): String? {
        return try {
            ensureExists()
            val lines = readLines()
            YamlEditor.replace(lines, opt.path.split('.'), YamlWriter.leafLines(opt.key, opt.type, value))
            writeLines(lines)
        } catch (e: IOException) {
            "Could not edit ${relativePath}: ${e.message}"
        }
    }

    fun reset(opt: Opt<*>): String? = write(opt, opt.default)

    /** Appends every option missing from the file, with its documentation comments. Returns how many were added. */
    fun addMissing(): Int {
        val missing = missingOptions()
        if (missing.isEmpty()) return 0
        return try {
            val lines = readLines()
            for (opt in missing) {
                val block = YamlWriter.commentLines(opt) + YamlWriter.leafLines(opt.key, opt.type, opt.default)
                YamlEditor.replace(lines, opt.path.split('.'), block)
            }
            if (writeLines(lines) == null) missing.size else 0
        } catch (e: IOException) {
            0
        }
    }
}
