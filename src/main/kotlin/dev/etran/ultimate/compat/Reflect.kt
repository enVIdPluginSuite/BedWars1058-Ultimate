package dev.etran.ultimate.compat

import java.lang.reflect.Method

/**
 * Tiny reflection helpers. Everything here fails soft (returns null) because the whole point is to
 * probe APIs that may not exist on the running server version.
 */
internal object Reflect {

    fun classOrNull(name: String): Class<*>? = try {
        Class.forName(name)
    } catch (ignored: Throwable) {
        null
    }

    /** Works for enum constants and for interface/class constants alike (Sound, Particle, ... changed kind over time). */
    fun staticField(cls: Class<*>?, name: String): Any? = try {
        cls?.getField(name)?.get(null)
    } catch (ignored: Throwable) {
        null
    }

    fun method(cls: Class<*>?, name: String, vararg params: Class<*>): Method? = try {
        cls?.getMethod(name, *params)
    } catch (ignored: Throwable) {
        null
    }

    fun methodByName(cls: Class<*>?, name: String, paramCount: Int): Method? = try {
        cls?.methods?.firstOrNull { it.name == name && it.parameterTypes.size == paramCount }
    } catch (ignored: Throwable) {
        null
    }

    fun invoke(method: Method?, target: Any?, vararg args: Any?): Any? = try {
        method?.invoke(target, *args)
    } catch (ignored: Throwable) {
        null
    }
}

/**
 * Groups of names that refer to the same thing across Minecraft versions
 * (e.g. `NOTE_PLING` on 1.8 and `BLOCK_NOTE_BLOCK_PLING` on 1.13+).
 * Resolving a name tries the name itself first, then every other name in any group containing it.
 */
internal class AliasGroups(groups: List<List<String>>) {

    private val byName: Map<String, List<String>>

    init {
        val map = HashMap<String, MutableList<String>>()
        for (group in groups) {
            for (name in group) {
                val list = map.getOrPut(name) { ArrayList() }
                for (other in group) if (other != name && other !in list) list.add(other)
            }
        }
        byName = map
    }

    fun candidates(rawName: String): List<String> {
        val name = normalize(rawName)
        val result = LinkedHashSet<String>()
        result.add(name)
        byName[name]?.let { result.addAll(it) }
        return result.toList()
    }

    companion object {
        fun normalize(raw: String): String =
            raw.trim().uppercase().removePrefix("MINECRAFT:").replace('.', '_').replace('-', '_').replace(' ', '_')

        fun of(vararg groups: String): AliasGroups =
            AliasGroups(groups.map { line -> line.split(',').map { it.trim() } })
    }
}
