package dev.etran.ultimate.config

/** Renders default config files (with comments) from option declarations. */
object YamlWriter {

    private const val WRAP = 76

    private class Node(val name: String, val path: String) {
        val children = LinkedHashMap<String, Node>()
        var opt: Opt<*>? = null
    }

    fun render(header: List<String>, specs: Specs): String {
        val root = Node("", "")
        for (opt in specs.all) {
            var node = root
            val parts = opt.path.split('.')
            for ((i, part) in parts.withIndex()) {
                val path = parts.subList(0, i + 1).joinToString(".")
                node = node.children.getOrPut(part) { Node(part, path) }
                require(!(node.opt != null && i < parts.lastIndex)) { "${node.path} is both an option and a section" }
            }
            require(node.children.isEmpty()) { "${opt.path} is both an option and a section" }
            node.opt = opt
        }

        val out = StringBuilder()
        for (line in header) out.append(line).append('\n')
        for (child in root.children.values) emit(child, 0, specs, out)
        return out.toString()
    }

    private fun emit(node: Node, indent: Int, specs: Specs, out: StringBuilder) {
        val pad = " ".repeat(indent)
        val opt = node.opt
        if (opt != null) {
            out.append('\n')
            for (line in commentLines(opt)) out.append(pad).append(line).append('\n')
            for (line in leafLines(opt.key, opt.type, opt.default)) out.append(pad).append(line).append('\n')
            return
        }

        out.append('\n')
        val banner = specs.sections[node.path]
        if (indent == 0) {
            out.append("# ").append("-".repeat(WRAP)).append('\n')
            out.append("# ").append(banner ?: node.name.replace('-', ' ').uppercase()).append('\n')
            out.append("# ").append("-".repeat(WRAP)).append('\n')
        } else if (banner != null) {
            for (line in wrap(banner, WRAP - indent)) out.append(pad).append("# == ").append(line).append(" ==\n")
        }
        out.append(pad).append(node.name).append(":\n")
        for (child in node.children.values) emit(child, indent + 2, specs, out)
    }

    /** Comment block placed above an option (also used when appending missing options to an existing file). */
    fun commentLines(opt: Opt<*>): List<String> {
        val lines = ArrayList<String>()
        for (l in wrap(opt.description, WRAP)) lines.add("# $l")
        val facts = ArrayList<String>()
        if (opt.type.isNumber && (opt.min != null || opt.max != null)) {
            facts.add("Range: ${opt.min?.let { OptValues.display(fmt(opt, it)) } ?: "-inf"} to ${opt.max?.let { OptValues.display(fmt(opt, it)) } ?: "inf"}")
        }
        if (opt.type == OptType.CHOICE) facts.add("Allowed: ${opt.choices.joinToString(", ")}")
        if (opt.type.isList) facts.add("List option")
        facts.add("Default: ${defaultText(opt)}")
        lines.add("# " + facts.joinToString(" | "))
        return lines
    }

    private fun fmt(opt: Opt<*>, n: Double): Any = if (opt.type == OptType.INT) n.toLong() else n

    private fun defaultText(opt: Opt<*>): String {
        val d = opt.default
        return when {
            d is List<*> -> if (d.isEmpty()) "(empty)" else "${d.size} line(s)"
            d is String && d.isEmpty() -> "(blank)"
            else -> OptValues.display(d)
        }
    }

    /** Lines (indent 0) that define `key` with [value]. Block style for lists. */
    fun leafLines(key: String, type: OptType, value: Any): List<String> {
        if (type.isList) {
            val items = (value as? List<*>)?.map { it.toString() } ?: listOf(value.toString())
            if (items.isEmpty()) return listOf("$key: []")
            return listOf("$key:") + items.map { "  - ${quote(it)}" }
        }
        return listOf("$key: ${scalar(type, value)}")
    }

    private fun scalar(type: OptType, value: Any): String = when (type) {
        OptType.BOOLEAN -> (value as? Boolean ?: value.toString().toBoolean()).toString()
        OptType.INT -> ((value as? Number)?.toLong() ?: value.toString().toLong()).toString()
        OptType.DOUBLE -> OptValues.formatDouble((value as? Number)?.toDouble() ?: value.toString().toDouble())
        else -> quote(value.toString())
    }

    fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    fun wrap(text: String, width: Int): List<String> {
        val result = ArrayList<String>()
        for (paragraph in text.split('\n')) {
            var line = StringBuilder()
            for (word in paragraph.split(' ')) {
                if (line.isNotEmpty() && line.length + 1 + word.length > width) {
                    result.add(line.toString())
                    line = StringBuilder()
                }
                if (line.isNotEmpty()) line.append(' ')
                line.append(word)
            }
            result.add(line.toString())
        }
        return result
    }
}
