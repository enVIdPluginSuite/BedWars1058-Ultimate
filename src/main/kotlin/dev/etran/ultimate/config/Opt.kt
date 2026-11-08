package dev.etran.ultimate.config

enum class OptType {
    BOOLEAN, INT, DOUBLE,

    /** Free text, no colour codes expected (ids, permission nodes, modes). */
    STRING,

    /** Free text that is shown to players; supports `&` colour codes. */
    TEXT,
    SOUND, MATERIAL, PARTICLE, EFFECT, CHOICE,
    TEXT_LIST, STRING_LIST, MATERIAL_LIST;

    val isList: Boolean get() = this == TEXT_LIST || this == STRING_LIST || this == MATERIAL_LIST
    val isNumber: Boolean get() = this == INT || this == DOUBLE
}

/**
 * One configurable option. Options are declared once in code; that single declaration drives
 * the generated YAML (with comments), value validation, `/bwu config`, and the in-game editor.
 *
 * [get] returns the live value from the owning [ConfigFile] (cached, so it is cheap enough for per-tick use)
 * and falls back to [default] when the key is missing or invalid, so a half-edited file never breaks gameplay.
 */
class Opt<T : Any>(
    val path: String,
    val type: OptType,
    val default: T,
    val description: String,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val choices: List<String> = emptyList()
) {
    @Volatile
    internal var owner: ConfigFile? = null

    @Suppress("UNCHECKED_CAST")
    fun get(): T = (owner?.cached(path) as T?) ?: default

    /** Last path segment, e.g. `cooldown-seconds`. */
    val key: String get() = path.substringAfterLast('.')

    override fun toString(): String = "Opt($path)"
}

/** Collects the options of one config file in declaration order. */
class Specs {
    val all: MutableList<Opt<*>> = ArrayList()
    val sections: MutableMap<String, String> = LinkedHashMap()

    private fun <T : Any> add(opt: Opt<T>): Opt<T> {
        require(all.none { it.path == opt.path }) { "Duplicate option path ${opt.path}" }
        all.add(opt)
        return opt
    }

    /** Adds a banner comment above the section with the given path. */
    fun section(path: String, comment: String) {
        sections[path] = comment
    }

    fun bool(path: String, default: Boolean, description: String): Opt<Boolean> =
        add(Opt(path, OptType.BOOLEAN, default, description))

    fun int(
        path: String, default: Int, description: String,
        min: Int? = null, max: Int? = null, step: Int? = null
    ): Opt<Int> = add(Opt(path, OptType.INT, default, description, min?.toDouble(), max?.toDouble(), step?.toDouble()))

    fun double(
        path: String, default: Double, description: String,
        min: Double? = null, max: Double? = null, step: Double? = null
    ): Opt<Double> = add(Opt(path, OptType.DOUBLE, default, description, min, max, step))

    fun string(path: String, default: String, description: String): Opt<String> =
        add(Opt(path, OptType.STRING, default, description))

    fun text(path: String, default: String, description: String): Opt<String> =
        add(Opt(path, OptType.TEXT, default, description))

    fun sound(path: String, default: String, description: String): Opt<String> =
        add(Opt(path, OptType.SOUND, default, "$description Blank disables it. Any era's name works (NOTE_PLING, BLOCK_NOTE_BLOCK_PLING, ...). Format: NAME or NAME:volume:pitch."))

    fun material(path: String, default: String, description: String): Opt<String> =
        add(Opt(path, OptType.MATERIAL, default, description))

    fun particle(path: String, default: String, description: String): Opt<String> =
        add(Opt(path, OptType.PARTICLE, default, "$description Blank disables it."))

    fun effect(path: String, default: String, description: String): Opt<String> =
        add(Opt(path, OptType.EFFECT, default, description))

    fun choice(path: String, default: String, choices: List<String>, description: String): Opt<String> =
        add(Opt(path, OptType.CHOICE, default, description, choices = choices))

    fun textList(path: String, default: List<String>, description: String): Opt<List<String>> =
        add(Opt(path, OptType.TEXT_LIST, default, description))

    fun stringList(path: String, default: List<String>, description: String): Opt<List<String>> =
        add(Opt(path, OptType.STRING_LIST, default, description))

    fun materialList(path: String, default: List<String>, description: String): Opt<List<String>> =
        add(Opt(path, OptType.MATERIAL_LIST, default, description))

    fun find(path: String): Opt<*>? = all.firstOrNull { it.path == path }
}
