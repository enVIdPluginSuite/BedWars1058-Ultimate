package dev.etran.ultimate

import dev.etran.ultimate.config.ConfigFile
import dev.etran.ultimate.config.OptType
import dev.etran.ultimate.config.OptValues
import dev.etran.ultimate.config.Specs
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigFileTest {

    private fun tempDir(): File = Files.createTempDirectory("bwu-test").toFile().also { it.deleteOnExit() }

    private fun fileFor(dir: File, id: String, specs: Specs): ConfigFile = ConfigFile(dir, id, "$id.yml", "Test $id", specs)

    @Test fun everyGeneratedDefaultFileLoadsAndMatchesTheDeclaredDefaults() {
        val dir = tempDir()
        var total = 0
        for ((id, specs) in TestSupport.allSpecs()) {
            val file = fileFor(dir, id, specs)
            assertNull(file.load(), "$id failed to load")
            // Effect names can't be validated without a running server (PotionEffectType is registered by the server);
            // they are verified against the real API jars in ApiCompatTest instead.
            val problems = file.diagnostics().filterNot { "potion effect" in it }
            assertTrue(problems.isEmpty(), "$id: unexpected diagnostics: $problems")
            assertTrue(file.missingOptions().isEmpty(), "$id: generated file is missing options: ${file.missingOptions()}")
            for (opt in specs.all) {
                assertEquals(opt.default, opt.get(), "${opt.path} in $id does not round-trip through YAML")
                total++
            }
        }
        println("verified $total options round-trip through generated YAML")
        assertTrue(total > 250, "expected a rich option set, got $total")
    }

    @Test fun everyOptionHasAReasonableDescription() {
        for ((id, specs) in TestSupport.allSpecs()) for (opt in specs.all) {
            assertTrue(opt.description.length >= 10, "$id:${opt.path} needs a description")
            assertTrue(opt.path.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*(\\.[a-z0-9]+(-[a-z0-9]+)*)*")), "bad path ${opt.path}")
            if (opt.type == OptType.CHOICE) assertTrue(opt.default.toString() in opt.choices, "${opt.path} default not in choices")
        }
    }

    @Test fun editingWritesToDiskAndKeepsCommentsAndOtherValues() {
        val dir = tempDir()
        val specs = TestSupport.allSpecs().getValue("kangaroo")
        val file = fileFor(dir, "kangaroo", specs)
        assertNull(file.load())
        val cooldown = specs.find("double-jump.cooldown-seconds")!!
        val lore = specs.find("icon.lore")!!
        val before = file.file.readText()

        assertNull(file.write(cooldown, 42.5))
        assertNull(file.write(lore, listOf("&aone", "&bsecond \"quoted\": line")))
        assertEquals(42.5, cooldown.get())
        assertEquals(listOf("&aone", "&bsecond \"quoted\": line"), lore.get())

        // a fresh instance reading the same file sees the edits
        val reread = fileFor(dir, "kangaroo", TestSupport.allSpecs().getValue("kangaroo"))
        assertNull(reread.load())
        assertEquals(42.5, reread.specs.find("double-jump.cooldown-seconds")!!.get())

        val after = file.file.readText()
        assertTrue(after.contains("# Cooldown after the last jump"), "comments must survive edits")
        assertEquals(before.lines().count { it.trimStart().startsWith("#") }, after.lines().count { it.trimStart().startsWith("#") }, "comment lines changed")
        // everything else untouched
        for (opt in specs.all) if (opt !== cooldown && opt !== lore) assertEquals(opt.default, opt.get(), opt.path)
    }

    @Test fun resetRestoresTheDefault() {
        val dir = tempDir()
        val specs = TestSupport.allSpecs().getValue("healer")
        val file = fileFor(dir, "healer", specs)
        file.load()
        val radius = specs.find("potion.zone.radius")!!
        file.write(radius, 9.0)
        assertEquals(9.0, radius.get())
        assertNull(file.reset(radius))
        assertEquals(radius.default, radius.get())
    }

    @Test fun missingOptionsAreAppendedWithTheirComments() {
        val dir = tempDir()
        val specs = TestSupport.allSpecs().getValue("builder")
        val file = fileFor(dir, "builder", specs)
        file.load()
        val text = file.file.readLines(StandardCharsets.UTF_8)
        // simulate an older version of the plugin: drop two options and one whole section
        val trimmed = text.filterNot { it.contains("cooldown-seconds") || it.contains("cover-top") || it.contains("max-distance") }
        file.file.writeText(trimmed.joinToString("\n") + "\n")
        file.load()
        val missing = file.missingOptions()
        assertTrue(missing.size >= 3, "expected removed options to be reported missing, got $missing")
        // defaults still apply while missing
        for (opt in missing) assertEquals(opt.default, opt.get())

        val added = file.addMissing()
        assertEquals(missing.size, added)
        file.load()
        assertTrue(file.missingOptions().isEmpty())
        assertTrue(file.file.readText().contains("Also cover the top of the bed"), "appended options keep their documentation")
    }

    @Test fun invalidValuesFallBackToDefaultsAndAreReported() {
        val dir = tempDir()
        val specs = TestSupport.allSpecs().getValue("kangaroo")
        val file = fileFor(dir, "kangaroo", specs)
        file.load()
        file.file.writeText(
            """
            enabled: "maybe"
            double-jump:
              cooldown-seconds: "lots"
              max-jumps: 9999
              sound: "NOT_A_REAL_SOUND_XYZ"
              typo-option: 5
            """.trimIndent()
        )
        assertNull(file.load())
        assertEquals(true, specs.find("enabled")!!.get(), "garbage boolean falls back to default")
        assertEquals(10.0, specs.find("double-jump.cooldown-seconds")!!.get(), "garbage number falls back to default")
        assertEquals(10, specs.find("double-jump.max-jumps")!!.get(), "out-of-range numbers are clamped to the maximum")
        val diag = file.diagnostics().joinToString("\n")
        assertTrue("double-jump.sound" in diag, diag)
        assertTrue("typo-option" in diag, diag)
        assertTrue("cooldown-seconds" in diag, diag)
    }

    @Test fun brokenYamlKeepsThePreviousValuesAndReportsTheError() {
        val dir = tempDir()
        val specs = TestSupport.allSpecs().getValue("swordsman")
        val file = fileFor(dir, "swordsman", specs)
        file.load()
        val cooldown = specs.find("dash.cooldown-seconds")!!
        file.write(cooldown, 33.0)
        file.file.writeText("dash:\n  cooldown-seconds: [unclosed\n  : :\n")
        val error = file.load()
        assertNotNull(error)
        assertTrue("syntax error" in error!!, error)
        assertEquals(33.0, cooldown.get(), "a broken file must not wipe the running configuration")
    }

    @Test fun commandStyleValueParsing() {
        val specs = TestSupport.allSpecs().getValue("kangaroo")
        val cooldown = specs.find("double-jump.cooldown-seconds")!!
        assertTrue(OptValues.parse(cooldown, "12.5") is OptValues.Result.Ok)
        assertTrue(OptValues.parse(cooldown, "-3") is OptValues.Result.Err)
        assertTrue(OptValues.parse(cooldown, "abc") is OptValues.Result.Err)
        val enabled = specs.find("enabled")!!
        assertEquals(false, (OptValues.parse(enabled, "off") as OptValues.Result.Ok).value)
        val choice = TestSupport.allSpecs().getValue("demolition").find("kill-streak.mode")!!
        assertEquals("CYCLE", (OptValues.parse(choice, "cycle") as OptValues.Result.Ok).value)
        assertTrue(OptValues.parse(choice, "nope") is OptValues.Result.Err)
        val list = specs.find("icon.lore")!!
        assertEquals(listOf("a", "b"), (OptValues.parse(list, "a | b") as OptValues.Result.Ok).value)
    }
}
