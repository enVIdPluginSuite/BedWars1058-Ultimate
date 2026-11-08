package dev.etran.ultimate

import dev.bw1058.ultimate.config.GeneralSettings
import dev.bw1058.ultimate.config.MessageSettings
import dev.bw1058.ultimate.config.Specs
import dev.bw1058.ultimate.ultimate.Builder
import dev.bw1058.ultimate.ultimate.Demolition
import dev.bw1058.ultimate.ultimate.Frozo
import dev.bw1058.ultimate.ultimate.Gatherer
import dev.bw1058.ultimate.ultimate.Healer
import dev.bw1058.ultimate.ultimate.Kangaroo
import dev.bw1058.ultimate.ultimate.Swordsman
import dev.bw1058.ultimate.ultimate.Ultimate

object TestSupport {

    init {
        // The unit-test classpath is the Spigot 1.8.8 API, and there is no server to ask.
        dev.bw1058.ultimate.compat.McVersion.testOverride = dev.bw1058.ultimate.compat.McVersion(1, 8, 8)
    }

    /**
     * Ultimates only touch the plugin once they are running; constructing them just declares options.
     * So an uninitialised plugin object (no server needed) is enough to instantiate every ultimate.
     */
    private fun fakePlugin(): UltimatePlugin {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null)
        return unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, UltimatePlugin::class.java) as UltimatePlugin
    }

    fun ultimates(): List<Ultimate> {
        val p = fakePlugin()
        return listOf(Kangaroo(p), Swordsman(p), Healer(p), Frozo(p), Builder(p), Demolition(p), Gatherer(p))
    }

    /** id -> declared options, for every config file the plugin creates. */
    fun allSpecs(): Map<String, Specs> {
        val map = LinkedHashMap<String, Specs>()
        map["general"] = GeneralSettings().specs
        map["messages"] = MessageSettings().specs
        for (u in ultimates()) map[u.id] = u.specs
        return map
    }
}
