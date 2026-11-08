package dev.etran.ultimate.core

import dev.etran.ultimate.compat.McVersion
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

object Text {

    private val HEX = Regex("&#([0-9a-fA-F]{6})")

    /** Translates `&` colour codes, plus `&#RRGGBB` hex colours on 1.16+ (older clients/servers just see the nearest legacy output). */
    fun color(text: String?): String {
        if (text == null) return ""
        var s = text
        if (McVersion.supports(16)) {
            s = HEX.replace(s) { m ->
                val sb = StringBuilder("§x")
                for (c in m.groupValues[1]) sb.append('§').append(c)
                sb.toString()
            }
        } else {
            s = HEX.replace(s, "")
        }
        return ChatColor.translateAlternateColorCodes('&', s)
    }

    fun strip(text: String?): String = ChatColor.stripColor(color(text)) ?: ""

    /** Replaces `{key}` placeholders. */
    fun apply(text: String, placeholders: Map<String, String>): String {
        if (placeholders.isEmpty() || !text.contains('{')) return text
        var s = text
        for ((k, v) in placeholders) s = s.replace("{$k}", v)
        return s
    }

    fun colorAll(lines: List<String>, placeholders: Map<String, String> = emptyMap()): List<String> =
        lines.map { color(apply(it, placeholders)) }

    fun send(to: CommandSender, text: String) {
        if (text.isEmpty()) return
        for (line in text.split("\\n")) to.sendMessage(color(line))
    }

    fun seconds(value: Double): String =
        if (value == Math.rint(value)) value.toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", value)
}
