package dev.etran.ultimate.compat

import org.bukkit.Bukkit

/**
 * Minecraft server version, understanding both numbering schemes:
 * the classic `1.<minor>.<patch>` and the year-based `<year>.<drop>.<hotfix>` scheme (26.1, 26.3, ...).
 *
 * Year-based versions are always newer than any `1.x`, which is what [atLeast] relies on.
 */
data class McVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<McVersion> {

    val isClassicScheme: Boolean get() = major == 1

    /** True when the server is at least `1.<minor>`; year-based versions (26.x) always qualify. */
    fun atLeast(minorOf1x: Int): Boolean = major > 1 || minor >= minorOf1x

    override fun compareTo(other: McVersion): Int =
        compareValuesBy(this, other, McVersion::major, McVersion::minor, McVersion::patch)

    override fun toString(): String = if (patch > 0) "$major.$minor.$patch" else "$major.$minor"

    companion object {
        private val PATTERN = Regex("(\\d+)\\.(\\d+)(?:\\.(\\d+))?")

        fun parse(text: String): McVersion? {
            val match = PATTERN.find(text) ?: return null
            val (major, minor, patch) = match.destructured
            return McVersion(major.toInt(), minor.toInt(), patch.ifEmpty { "0" }.toInt())
        }

        /** Set by unit tests, which run without a server. Never set in production. */
        @Volatile
        internal var testOverride: McVersion? = null

        private val detected: McVersion by lazy {
            parse(Bukkit.getBukkitVersion()) ?: parse(Bukkit.getVersion()) ?: McVersion(1, 8, 8)
        }

        /** The running server's version. Falls back to 1.8.8 only if the version string is unparseable. */
        val current: McVersion get() = testOverride ?: detected

        /** Shorthand used throughout the compat layer: `1.9+` style checks. */
        fun supports(minorOf1x: Int): Boolean = current.atLeast(minorOf1x)
    }
}
