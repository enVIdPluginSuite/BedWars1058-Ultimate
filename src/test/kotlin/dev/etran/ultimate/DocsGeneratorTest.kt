package dev.etran.ultimate

import dev.bw1058.ultimate.config.OptType
import dev.bw1058.ultimate.config.OptValues
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Generates docs/OPTIONS.md from the option declarations, so the reference can never drift from the code. */
class DocsGeneratorTest {

    private val titles = mapOf(
        "general" to "config.yml", "messages" to "messages.yml"
    )

    private fun typeName(t: OptType) = when (t) {
        OptType.BOOLEAN -> "true/false"
        OptType.INT -> "whole number"
        OptType.DOUBLE -> "number"
        OptType.STRING -> "text"
        OptType.TEXT -> "text (&-colours)"
        OptType.SOUND -> "sound"
        OptType.MATERIAL -> "material"
        OptType.PARTICLE -> "particle"
        OptType.EFFECT -> "potion effect"
        OptType.CHOICE -> "choice"
        OptType.TEXT_LIST -> "list of text"
        OptType.STRING_LIST -> "list"
        OptType.MATERIAL_LIST -> "list of materials"
    }

    private fun cell(s: String) = s.replace("|", "\\|").replace("\n", " ")

    @Test fun generateOptionReference() {
        val sb = StringBuilder()
        sb.append("# Option reference\n\n")
        sb.append("Generated from the plugin's option declarations - every option below exists in the YAML files, in `/bwu config` and in the in-game editor. ")
        sb.append("Ranges are enforced: values outside them are clamped when read from a file and rejected when typed in-game.\n\n")
        var total = 0
        val specs = TestSupport.allSpecs()
        val ultimates = TestSupport.ultimates().associateBy { it.id }
        for ((id, s) in specs) {
            val file = titles[id] ?: "ultimates/$id.yml"
            sb.append("## `$file`\n\n")
            if (ultimates[id] != null) sb.append("Placeholders usable in this file's icon lore: `{status}` and `${ultimates.getValue(id).placeholders().keys.joinToString("`, `") { "{$it}" }}` (read live from the options below).\n\n")
            var currentSection = ""
            for (opt in s.all) {
                val section = opt.path.substringBefore('.', "")
                if (section != currentSection) {
                    currentSection = section
                    val banner = s.sections[section]
                    sb.append("\n### ${if (section.isEmpty()) "Other" else section}\n")
                    if (banner != null) sb.append("_${banner.lowercase().replaceFirstChar { it.uppercase() }}_\n")
                    sb.append("\n| Option | Type | Default | Description |\n|---|---|---|---|\n")
                }
                val default = opt.default.let {
                    when {
                        it is List<*> -> if (it.isEmpty()) "_empty_" else "${it.size} line(s)"
                        it is String && it.isEmpty() -> "_blank_"
                        else -> "`${cell(OptValues.display(it))}`"
                    }
                }
                val extra = buildList {
                    if (opt.type.isNumber && (opt.min != null || opt.max != null)) add("Range ${opt.min?.let { OptValues.display(if (opt.type == OptType.INT) it.toLong() else it) } ?: "-"} to ${opt.max?.let { OptValues.display(if (opt.type == OptType.INT) it.toLong() else it) } ?: "-"}.")
                    if (opt.type == OptType.CHOICE) add("One of: ${opt.choices.joinToString(", ")}.")
                }.joinToString(" ")
                sb.append("| `${opt.path}` | ${typeName(opt.type)} | $default | ${cell(opt.description)} $extra |\n")
                total++
            }
            sb.append("\n")
        }
        val out = File(System.getProperty("docs.out") ?: "build/docs/OPTIONS.md")
        out.parentFile.mkdirs()
        out.writeText(sb.toString())
        println("wrote ${out.path} ($total options)")
        assertTrue(total > 250)
    }
}
