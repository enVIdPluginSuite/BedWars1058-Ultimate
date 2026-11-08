package dev.etran.ultimate.config

import dev.etran.ultimate.compat.Effects
import dev.etran.ultimate.compat.Materials
import dev.etran.ultimate.compat.Particles
import dev.etran.ultimate.compat.Sounds
import java.math.BigDecimal

/** Parsing, validation and display of option values. Shared by the file loader, the command and the GUI editor. */
object OptValues {

    sealed class Result {
        class Ok(val value: Any) : Result()
        class Err(val message: String) : Result()
    }

    /** Separator used when a list is typed on a single line (command argument). */
    const val LIST_SEPARATOR = "|"

    fun parse(opt: Opt<*>, input: String): Result {
        val text = input.trim()
        return when (opt.type) {
            OptType.BOOLEAN -> when (text.lowercase()) {
                "true", "yes", "on", "1", "enable", "enabled" -> Result.Ok(true)
                "false", "no", "off", "0", "disable", "disabled" -> Result.Ok(false)
                else -> Result.Err("Expected true or false.")
            }
            OptType.INT -> {
                val n = text.toIntOrNull() ?: return Result.Err("Expected a whole number.")
                checkRange(opt, n.toDouble())?.let { return Result.Err(it) }
                Result.Ok(n)
            }
            OptType.DOUBLE -> {
                val n = text.replace(',', '.').toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
                    ?: return Result.Err("Expected a number.")
                checkRange(opt, n)?.let { return Result.Err(it) }
                Result.Ok(n)
            }
            OptType.STRING, OptType.TEXT -> Result.Ok(input.trim('\n', '\r'))
            OptType.CHOICE -> {
                val match = opt.choices.firstOrNull { it.equals(text, ignoreCase = true) }
                    ?: return Result.Err("Must be one of: ${opt.choices.joinToString(", ")}.")
                Result.Ok(match)
            }
            OptType.SOUND, OptType.MATERIAL, OptType.PARTICLE, OptType.EFFECT -> {
                validateScalar(opt.type, text)?.let { return Result.Err(it) }
                Result.Ok(text)
            }
            OptType.TEXT_LIST, OptType.STRING_LIST, OptType.MATERIAL_LIST -> {
                val items = if (text.isEmpty()) emptyList() else text.split(LIST_SEPARATOR).map { it.trim() }
                for (item in items) validateListItem(opt.type, item)?.let { return Result.Err(it) }
                Result.Ok(items)
            }
        }
    }

    private fun checkRange(opt: Opt<*>, n: Double): String? {
        if (opt.min != null && n < opt.min) return "Must be at least ${display(opt.min)}."
        if (opt.max != null && n > opt.max) return "Must be at most ${display(opt.max)}."
        return null
    }

    /** Returns a problem description for a stored value, or null when it is fine on this server. */
    fun validate(opt: Opt<*>, raw: Any?): String? {
        if (raw == null) return null
        return when (opt.type) {
            OptType.BOOLEAN ->
                if (raw is Boolean || raw.toString().lowercase() in setOf("true", "false")) null else "expected true/false"
            OptType.INT, OptType.DOUBLE -> {
                val n = (raw as? Number)?.toDouble() ?: raw.toString().toDoubleOrNull()
                    ?: return "expected a number"
                if (opt.type == OptType.INT && n != Math.floor(n)) return "expected a whole number"
                when {
                    opt.min != null && n < opt.min -> "below minimum ${display(opt.min)}"
                    opt.max != null && n > opt.max -> "above maximum ${display(opt.max)}"
                    else -> null
                }
            }
            OptType.STRING, OptType.TEXT -> null
            OptType.CHOICE ->
                if (opt.choices.any { it.equals(raw.toString(), ignoreCase = true) }) null
                else "must be one of ${opt.choices.joinToString(", ")}"
            OptType.SOUND, OptType.MATERIAL, OptType.PARTICLE, OptType.EFFECT -> validateScalar(opt.type, raw.toString())
            OptType.TEXT_LIST, OptType.STRING_LIST, OptType.MATERIAL_LIST -> {
                if (raw !is List<*>) return "expected a list"
                raw.firstNotNullOfOrNull { validateListItem(opt.type, it.toString()) }
            }
        }
    }

    private fun validateListItem(type: OptType, item: String): String? =
        if (type == OptType.MATERIAL_LIST) validateScalar(OptType.MATERIAL, item) else null

    private fun validateScalar(type: OptType, value: String): String? = when (type) {
        OptType.SOUND -> if (Sounds.isValid(value)) null else "unknown sound '$value' on this server version"
        OptType.MATERIAL -> if (Materials.isValid(value)) null else "unknown material '$value' on this server version"
        OptType.PARTICLE -> if (Particles.isValid(value)) null else "unknown particle '$value' on this server version"
        OptType.EFFECT -> if (Effects.isValid(value)) null else "unknown potion effect '$value' on this server version"
        else -> null
    }

    /** Converts a raw YAML value into the option's canonical Kotlin type, or null if it can't be (default is used then). */
    fun coerce(opt: Opt<*>, raw: Any?): Any? {
        if (raw == null) return null
        return when (opt.type) {
            OptType.BOOLEAN -> when (raw) {
                is Boolean -> raw
                else -> when (raw.toString().lowercase()) {
                    "true", "yes", "on" -> true
                    "false", "no", "off" -> false
                    else -> null
                }
            }
            OptType.INT -> {
                val n = (raw as? Number)?.toDouble() ?: raw.toString().toDoubleOrNull() ?: return null
                clamp(opt, n).toInt()
            }
            OptType.DOUBLE -> {
                val n = (raw as? Number)?.toDouble() ?: raw.toString().toDoubleOrNull() ?: return null
                if (n.isNaN() || n.isInfinite()) null else clamp(opt, n)
            }
            OptType.STRING, OptType.TEXT, OptType.SOUND, OptType.MATERIAL, OptType.PARTICLE, OptType.EFFECT -> raw.toString()
            OptType.CHOICE -> opt.choices.firstOrNull { it.equals(raw.toString(), ignoreCase = true) }
            OptType.TEXT_LIST, OptType.STRING_LIST, OptType.MATERIAL_LIST -> when (raw) {
                is List<*> -> raw.filterNotNull().map { it.toString() }
                is String -> listOf(raw)
                else -> null
            }
        }
    }

    private fun clamp(opt: Opt<*>, n: Double): Double {
        var v = n
        if (opt.min != null && v < opt.min) v = opt.min
        if (opt.max != null && v > opt.max) v = opt.max
        return v
    }

    /** Human readable value for lore, command output and logs. */
    fun display(value: Any?): String = when (value) {
        null -> "(unset)"
        is Double -> formatDouble(value)
        is List<*> -> if (value.isEmpty()) "(empty list)" else value.joinToString(" | ")
        else -> value.toString()
    }

    fun formatDouble(d: Double): String {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return "${d.toLong()}.0"
        val plain = BigDecimal(d.toString()).stripTrailingZeros().toPlainString()
        return if (plain.contains('.')) plain else "$plain.0"
    }
}
