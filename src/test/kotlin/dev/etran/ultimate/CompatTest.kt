package dev.etran.ultimate

import dev.etran.ultimate.compat.AliasGroups
import dev.etran.ultimate.compat.McVersion
import dev.etran.ultimate.compat.Sounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompatTest {

    @Test fun parsesClassicVersions() {
        assertEquals(McVersion(1, 8, 8), McVersion.parse("1.8.8-R0.1-SNAPSHOT"))
        assertEquals(McVersion(1, 21, 4), McVersion.parse("1.21.4-R0.1-SNAPSHOT"))
        assertEquals(McVersion(1, 12, 0), McVersion.parse("git-Spigot-abc123-def456 (MC: 1.12)"))
    }

    @Test fun parsesYearBasedVersions() {
        assertEquals(McVersion(26, 1, 2), McVersion.parse("26.1.2-R0.1-SNAPSHOT"))
        assertEquals(McVersion(26, 3, 0), McVersion.parse("26.3-pre-2.build.0-alpha"))
    }

    @Test fun yearBasedAlwaysCountsAsNewerThanAnyClassic() {
        val v = McVersion(26, 1, 0)
        assertTrue(v.atLeast(13))
        assertTrue(v.atLeast(21))
        assertTrue(McVersion(1, 21, 0).atLeast(13))
        assertFalse(McVersion(1, 8, 8).atLeast(9))
        assertTrue(McVersion(26, 1, 0) > McVersion(1, 21, 11))
        assertTrue(McVersion(26, 3, 0) > McVersion(26, 1, 2))
    }

    @Test fun aliasesCoverEveryEra() {
        val c = Sounds.aliases.candidates("NOTE_PLING")
        assertTrue(c.containsAll(listOf("NOTE_PLING", "BLOCK_NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING")))
        val modern = Sounds.aliases.candidates("block.note_block.pling")
        assertTrue("NOTE_PLING" in modern, "modern spelling must reach the 1.8 name: $modern")
        assertEquals(listOf("SOMETHING_ELSE"), Sounds.aliases.candidates("something else"))
    }

    @Test fun soundSpecParsing() {
        assertNull(Sounds.parse(""))
        assertNull(Sounds.parse("none"))
        assertEquals(Sounds.Spec("NOTE_PLING", 1f, 1f), Sounds.parse("NOTE_PLING"))
        assertEquals(Sounds.Spec("NOTE_PLING", 0.5f, 2f), Sounds.parse("NOTE_PLING:0.5:2"))
        assertEquals(Sounds.Spec("minecraft:entity.player.levelup", 1f, 1.5f), Sounds.parse("minecraft:entity.player.levelup:1:1.5"))
        assertNotNull(Sounds.parse("entity.player.levelup:2"))
        assertTrue(Sounds.isValid(""))
        assertTrue(Sounds.isValid("entity.custom.pack_sound"))
    }

    @Test fun normalizeHandlesSeparators() {
        assertEquals("ENTITY_PLAYER_LEVELUP", AliasGroups.normalize("minecraft:entity.player.levelup"))
        assertEquals("BLOCK_NOTE_BLOCK_PLING", AliasGroups.normalize(" block-note block.pling "))
    }
}
