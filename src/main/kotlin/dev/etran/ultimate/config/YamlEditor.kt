package dev.etran.ultimate.config

/**
 * Surgical, comment-preserving YAML editing.
 *
 * Bukkit's `YamlConfiguration#save` discards every comment on servers older than 1.18.1, which would wipe the
 * documentation out of the config files each time something is changed in-game. Instead, edits are applied
 * directly to the file's lines: the one key being changed is replaced (or inserted under the right parent) and
 * everything else is left byte-for-byte as the admin wrote it.
 *
 * Supported shape: block-style maps with space indentation, scalar values, and lists of scalars (either
 * indented under the key or at the key's own indent, which is what some Bukkit versions emit).
 */
object YamlEditor {

    private class KeyLine(val index: Int, val indent: Int, val path: List<String>)

    private val KEY = Regex("^( *)(?:\"([^\"]+)\"|'([^']+)'|([A-Za-z0-9_\\-]+)) *:(?: .*|)$")

    private fun indentOf(line: String): Int {
        var i = 0
        while (i < line.length && line[i] == ' ') i++
        return i
    }

    private fun isBlankOrComment(line: String): Boolean = line.isBlank() || line.trimStart().startsWith("#")

    private fun isListItem(line: String): Boolean {
        val t = line.trimStart()
        return t == "-" || t.startsWith("- ")
    }

    private fun scan(lines: List<String>): List<KeyLine> {
        val result = ArrayList<KeyLine>()
        val stack = ArrayList<Pair<Int, String>>()
        for ((i, line) in lines.withIndex()) {
            if (isBlankOrComment(line) || isListItem(line)) continue
            val m = KEY.matchEntire(line) ?: continue
            val indent = m.groupValues[1].length
            val key = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            while (stack.isNotEmpty() && stack.last().first >= indent) stack.removeAt(stack.size - 1)
            stack.add(indent to key)
            result.add(KeyLine(i, indent, stack.map { it.second }))
        }
        return result
    }

    /** Index just past the last line that belongs to [key]'s value (blank/comment lines after it are excluded). */
    private fun blockEnd(lines: List<String>, key: KeyLine): Int {
        var end = key.index + 1
        var i = key.index + 1
        while (i < lines.size) {
            val line = lines[i]
            if (isBlankOrComment(line)) {
                i++
                continue
            }
            val belongs = indentOf(line) > key.indent || (indentOf(line) == key.indent && isListItem(line))
            if (!belongs) break
            i++
            end = i
        }
        return end
    }

    private fun childIndent(lines: List<String>, parent: KeyLine, keys: List<KeyLine>): Int {
        val child = keys.firstOrNull {
            it.path.size == parent.path.size + 1 && it.path.subList(0, parent.path.size) == parent.path && it.index > parent.index
        }
        return child?.indent ?: (parent.indent + 2)
    }

    /**
     * Replaces the entry at [path] with [newLines] (written relative to indent 0; may start with comment lines),
     * or inserts it - creating any missing parent sections - if it doesn't exist yet.
     */
    fun replace(lines: MutableList<String>, path: List<String>, newLines: List<String>) {
        val keys = scan(lines)

        val exact = keys.firstOrNull { it.path == path }
        if (exact != null) {
            val end = blockEnd(lines, exact)
            val indented = newLines.map { if (it.isEmpty()) it else " ".repeat(exact.indent) + it }
            repeat(end - exact.index) { lines.removeAt(exact.index) }
            lines.addAll(exact.index, indented)
            return
        }

        var ancestor: KeyLine? = null
        var depth = 0
        for (d in path.size - 1 downTo 1) {
            val candidate = keys.firstOrNull { it.path == path.subList(0, d) }
            if (candidate != null) {
                ancestor = candidate
                depth = d
                break
            }
        }

        val baseIndent: Int
        val insertAt: Int
        if (ancestor != null) {
            baseIndent = childIndent(lines, ancestor, keys)
            insertAt = blockEnd(lines, ancestor)
        } else {
            baseIndent = 0
            insertAt = lines.size
        }

        val out = ArrayList<String>()
        if (ancestor == null && lines.isNotEmpty() && lines.last().isNotBlank()) out.add("")
        var indent = baseIndent
        for (section in path.subList(depth, path.size - 1)) {
            out.add(" ".repeat(indent) + section + ":")
            indent += 2
        }
        for (l in newLines) out.add(if (l.isEmpty()) l else " ".repeat(indent) + l)
        lines.addAll(insertAt, out)
    }
}
