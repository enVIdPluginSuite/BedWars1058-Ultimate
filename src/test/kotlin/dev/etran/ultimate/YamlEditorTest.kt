package dev.etran.ultimate

import dev.bw1058.ultimate.config.YamlEditor
import dev.bw1058.ultimate.config.YamlWriter
import dev.bw1058.ultimate.config.OptType
import org.bukkit.configuration.file.YamlConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlEditorTest {

    private fun parse(lines: List<String>): YamlConfiguration {
        val y = YamlConfiguration()
        y.loadFromString(lines.joinToString("\n"))
        return y
    }

    private val sample = """
        # Top comment
        enabled: true

        # Section comment
        double-jump:
          # Cooldown
          cooldown-seconds: 10
          sound: "ENTITY_BAT_TAKEOFF"
          lore:
            - "line one"
            - "line two: with colon"

        icon:
          material: "RABBIT_FOOT"   # inline
          slot: 19
        trailing: 1
    """.trimIndent().lines().toMutableList()

    @Test fun replacesScalarAndKeepsComments() {
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("double-jump", "cooldown-seconds"), YamlWriter.leafLines("cooldown-seconds", OptType.INT, 25))
        val y = parse(lines)
        assertEquals(25, y.getInt("double-jump.cooldown-seconds"))
        assertEquals("ENTITY_BAT_TAKEOFF", y.getString("double-jump.sound"))
        assertTrue(lines.contains("# Top comment"))
        assertTrue(lines.contains("  # Cooldown"))
        assertTrue(lines.contains("# Section comment"))
        assertEquals(1, y.getInt("trailing"))
    }

    @Test fun replacesListWithDifferentLength() {
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("double-jump", "lore"), YamlWriter.leafLines("lore", OptType.TEXT_LIST, listOf("a", "b", "c \"quoted\"")))
        val y = parse(lines)
        assertEquals(listOf("a", "b", "c \"quoted\""), y.getStringList("double-jump.lore"))
        assertEquals(19, y.getInt("icon.slot"))
        assertEquals("RABBIT_FOOT", y.getString("icon.material"))
    }

    @Test fun replacesListWithEmptyAndBack() {
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("double-jump", "lore"), YamlWriter.leafLines("lore", OptType.TEXT_LIST, emptyList<String>()))
        assertEquals(emptyList<String>(), parse(lines).getStringList("double-jump.lore"))
        YamlEditor.replace(lines, listOf("double-jump", "lore"), YamlWriter.leafLines("lore", OptType.TEXT_LIST, listOf("x")))
        assertEquals(listOf("x"), parse(lines).getStringList("double-jump.lore"))
        assertEquals(10, parse(lines).getInt("double-jump.cooldown-seconds"))
    }

    @Test fun handlesListAtSameIndentAsKey() {
        val lines = """
            a:
              lore:
              - one
              - two
              after: 5
        """.trimIndent().lines().toMutableList()
        YamlEditor.replace(lines, listOf("a", "lore"), YamlWriter.leafLines("lore", OptType.TEXT_LIST, listOf("z")))
        val y = parse(lines)
        assertEquals(listOf("z"), y.getStringList("a.lore"))
        assertEquals(5, y.getInt("a.after"))
    }

    @Test fun insertsMissingKeyIntoExistingSection() {
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("double-jump", "power"), YamlWriter.leafLines("power", OptType.DOUBLE, 0.5))
        val y = parse(lines)
        assertEquals(0.5, y.getDouble("double-jump.power"))
        assertEquals(10, y.getInt("double-jump.cooldown-seconds"))
        assertEquals(19, y.getInt("icon.slot"))
    }

    @Test fun insertsMissingNestedSectionsAtEnd() {
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("brand", "new", "deep"), YamlWriter.leafLines("deep", OptType.BOOLEAN, true))
        val y = parse(lines)
        assertEquals(true, y.getBoolean("brand.new.deep"))
        assertEquals(1, y.getInt("trailing"))
    }

    @Test fun insertsUnderPartiallyExistingChain() {
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("icon", "extra", "flag"), YamlWriter.leafLines("flag", OptType.BOOLEAN, false))
        val y = parse(lines)
        assertEquals(false, y.getBoolean("icon.extra.flag", true))
        assertEquals(19, y.getInt("icon.slot"))
        assertEquals(1, y.getInt("trailing"))
    }

    @Test fun stringsWithSpecialCharactersRoundTrip() {
        val tricky = listOf("&a▬▬ \"hi\" \\ back: #notcomment", "  leading spaces", "§cSection", "tab\there")
        val lines = sample.toMutableList()
        YamlEditor.replace(lines, listOf("double-jump", "lore"), YamlWriter.leafLines("lore", OptType.TEXT_LIST, tricky))
        assertEquals(tricky, parse(lines).getStringList("double-jump.lore"))
        YamlEditor.replace(lines, listOf("double-jump", "sound"), YamlWriter.leafLines("sound", OptType.SOUND, "a: b # c"))
        assertEquals("a: b # c", parse(lines).getString("double-jump.sound"))
    }
}
