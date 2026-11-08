package dev.etran.ultimate

import dev.bw1058.ultimate.compat.AliasGroups
import dev.bw1058.ultimate.compat.Effects
import dev.bw1058.ultimate.compat.McVersion
import dev.bw1058.ultimate.compat.Materials
import dev.bw1058.ultimate.compat.Particles
import dev.bw1058.ultimate.compat.Sounds
import dev.bw1058.ultimate.config.OptType
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.TreeMap
import java.util.TreeSet
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the plugin against the real Bukkit API of many Minecraft versions without needing a server:
 *
 *  1. Binary check: every `org.bukkit` method / field / class our compiled bytecode references must exist on each
 *     version, and be the same kind (class vs interface - this is what crashed the original plugin on 1.21+).
 *  2. Name check: every default sound, material, particle and potion effect must resolve on each version.
 *
 * Run with `gradle compatTest`; jar locations are supplied by the build.
 */
class ApiCompatTest {

    private val jars: Map<String, File> = (System.getProperty("compat.jars") ?: "")
        .split(File.pathSeparator).filter { it.contains('=') }
        .associate { it.substringBefore('=') to File(it.substringAfter('=')) }

    private val classDirs: List<File> = (System.getProperty("compat.classes") ?: "")
        .split(File.pathSeparator).filter { it.isNotBlank() }.map { File(it) }

    private val typeInDescriptor = Regex("L(org/bukkit/[^;]+);")

    private fun ours() = classDirs.flatMap { ClassFiles.parseDirectory(it) }.filter { it.model.name.startsWith("dev/bw1058/ultimate/") }

    private fun modern(version: String): Boolean = McVersion.parse(version)!!.atLeast(13)

    @Test fun bukkitReferencesResolveOnEveryVersion() {
        assumeTrue(jars.isNotEmpty() && classDirs.isNotEmpty(), "run through `gradle compatTest`")
        val ours = ours()
        assertTrue(ours.size > 40, "expected to find the plugin's classes, found ${ours.size}")

        val report = TreeMap<String, Set<String>>()
        for ((version, jar) in jars) {
            val api = ApiIndex(ClassFiles.indexJar(jar))
            assertTrue(api.classes.size > 500, "$version: API jar looks empty (${api.classes.size} classes)")
            report[version] = checkBinary(ours, api)
        }

        val refs = ours.flatMap { it.refs }.count { it.owner.startsWith("org/bukkit/") }
        println("Checked $refs Bukkit member references from ${ours.size} classes against ${jars.size} API versions: ${jars.keys}")
        val failures = report.filterValues { it.isNotEmpty() }
        for ((version, problems) in failures) {
            println("== $version: ${problems.size} problem(s)")
            problems.forEach { println("   $it") }
        }
        assertTrue(failures.isEmpty(), "Binary incompatibilities found on: ${failures.keys}")
    }

    private fun checkBinary(ours: List<ParsedClass>, api: ApiIndex): Set<String> {
        val out = TreeSet<String>()
        for (c in ours) {
            val where = c.model.name.removePrefix("dev/bw1058/ultimate/")
            for (ref in c.refs) {
                if (!ref.owner.startsWith("org/bukkit/")) continue
                val owner = api.classes[ref.owner]
                if (owner == null) {
                    out.add("$where: class ${ref.owner} does not exist")
                    continue
                }
                when (ref.kind) {
                    MemberRef.Kind.METHOD -> {
                        if (owner.isInterface) out.add("$where: ${ref.owner} is an INTERFACE here but is called like a class (IncompatibleClassChangeError): $ref")
                        else if (api.findMethod(ref.owner, ref.name, ref.desc) == ApiIndex.Found.NO) out.add("$where: missing method $ref")
                    }
                    MemberRef.Kind.INTERFACE_METHOD -> {
                        if (!owner.isInterface) out.add("$where: ${ref.owner} is a CLASS here but is called like an interface (IncompatibleClassChangeError): $ref")
                        else if (api.findMethod(ref.owner, ref.name, ref.desc) == ApiIndex.Found.NO) out.add("$where: missing method $ref")
                    }
                    MemberRef.Kind.FIELD ->
                        if (api.findField(ref.owner, ref.name) == ApiIndex.Found.NO) out.add("$where: missing field $ref")
                }
            }
            for (cls in c.classRefs) if (cls.startsWith("org/bukkit/") && cls !in api.classes) out.add("$where: class $cls does not exist")
            for (d in c.descriptors) for (m in typeInDescriptor.findAll(d)) {
                val t = m.groupValues[1]
                if (t !in api.classes) out.add("$where: type $t (in a signature) does not exist")
            }
        }
        return out
    }

    @Test fun checkerCatchesTheOriginalPluginsCrashPatterns() {
        assumeTrue(jars.isNotEmpty(), "run through `gradle compatTest`")
        val fixtureDirs = (System.getProperty("compat.testClasses") ?: "").split(File.pathSeparator).filter { it.isNotBlank() }.map { File(it) }
        val fixture = fixtureDirs.flatMap { ClassFiles.parseDirectory(it) }.filter { it.model.name.startsWith("dev/bw1058/ultimate/fixture/") }
        assertTrue(fixture.isNotEmpty(), "negative-control fixture not found")

        val onOld = checkBinary(fixture, ApiIndex(ClassFiles.indexJar(jars.getValue("1.8.8"))))
        assertTrue(onOld.isEmpty(), "the fixture is valid on 1.8.8, got $onOld")

        val onNew = checkBinary(fixture, ApiIndex(ClassFiles.indexJar(jars.getValue("26.3"))))
        println("Negative control on 26.3 (these are SUPPOSED to be reported):")
        onNew.forEach { println("   $it") }
        assertTrue(onNew.any { "InventoryView" in it && "INTERFACE" in it }, "InventoryView class->interface change not detected: $onNew")
        assertTrue(onNew.any { "Sound" in it && "valueOf" in it }, "Sound.valueOf removal not detected: $onNew")
    }

    @Test fun defaultNamesResolveOnEveryVersion() {
        assumeTrue(jars.isNotEmpty(), "run through `gradle compatTest`")
        val specs = TestSupport.allSpecs()
        val problems = TreeSet<String>()
        val perVersion = TreeMap<String, Int>()

        for ((version, jar) in jars) {
            val api = ApiIndex(ClassFiles.indexJar(jar))
            val isModern = modern(version)
            val materials = api.staticFieldNames("org/bukkit/Material")
            val sounds = api.staticFieldNames("org/bukkit/Sound")
            val particles = api.staticFieldNames("org/bukkit/Particle") + api.staticFieldNames("org/bukkit/Effect")
            val effects = api.staticFieldNames("org/bukkit/potion/PotionEffectType")
            val enchants = api.staticFieldNames("org/bukkit/enchantments/Enchantment")
            assertTrue(materials.size > 100 && sounds.size > 100 && effects.size > 5, "$version: API jar incomplete")
            var checked = 0

            fun material(name: String) = Materials.resolveName(name, { it in materials }, isModern) != null

            for ((file, s) in specs) for (opt in s.all) {
                val values: List<String> = when (opt.type) {
                    OptType.SOUND, OptType.MATERIAL, OptType.PARTICLE, OptType.EFFECT -> listOf(opt.default.toString())
                    OptType.MATERIAL_LIST -> (opt.default as List<*>).map { it.toString() }
                    else -> continue
                }
                for (v in values) {
                    if (v.isBlank()) continue
                    checked++
                    val ok = when (opt.type) {
                        OptType.SOUND -> v.contains('.') || Sounds.aliases.candidates(v).any { it in sounds }
                        OptType.MATERIAL, OptType.MATERIAL_LIST -> material(v)
                        OptType.PARTICLE -> Particles.aliases.candidates(v).any { it in particles }
                        OptType.EFFECT -> Effects.aliases.candidates(v).any { it in effects }
                        else -> true
                    }
                    if (!ok) problems.add("$version: $file:${opt.path} default '$v' does not resolve")
                }
            }

            // Names the code looks up directly.
            for (name in listOf("MILK_BUCKET", "BARRIER", "AIR", "STONE", "TNT", "ENDER_CHEST", "WHITE_WOOL", "FLINT_AND_STEEL")) {
                checked++
                if (!material(name)) problems.add("$version: code looks up material $name which does not resolve")
            }
            checked++
            if (!material("SPLASH_POTION") && !("POTION" in materials)) problems.add("$version: no splash potion material")
            // Fire is looked up by exact name (no alias needed): it has been FIRE in every version.
            if ("FIRE" !in materials) problems.add("$version: Material.FIRE missing")
            checked++
            if (listOf("DURABILITY", "UNBREAKING", "LUCK", "LUCK_OF_THE_SEA").none { it in enchants }) problems.add("$version: no enchantment usable for the glow effect")
            perVersion[version] = checked
        }
        println("Resolved defaults per version (count): $perVersion")
        problems.forEach { println(it) }
        assertTrue(problems.isEmpty(), "Some defaults don't resolve on some versions: $problems")
    }

    @Test fun aliasSanity() {
        // pure checks that also run without jars
        assertTrue("BLOCK_NOTE_BLOCK_PLING" in Sounds.aliases.candidates("NOTE_PLING"))
        assertTrue("FIRE_CHARGE" in Materials.aliases.candidates("FIREBALL"))
        assertTrue(AliasGroups.normalize("minecraft:a.b") == "A_B")
    }
}
