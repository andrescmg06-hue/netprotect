package com.netprotect.app.feature.supervised

import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.feature.supervised.block.BLOCK_COVER_NOTE
import com.netprotect.app.feature.supervised.block.BLOCK_TITLE
import com.netprotect.app.feature.supervised.block.blockMessage
import com.netprotect.app.feature.supervised.block.blockPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPresentationTest {

    @Test
    fun everyReasonHasACompletePresentation() {
        // BlockReason.entries, not a hand-written list: a reason added later must be mapped or
        // this fails.
        BlockReason.entries.forEach { reason ->
            val p = blockPresentation(reason, categoryLabel = "Juegos")
            assertEquals(BLOCK_TITLE, p.title)
            assertTrue("$reason message", p.message.isNotBlank())
            assertTrue("$reason hint", p.hint.isNotBlank())
            assertNotEquals("$reason badge", 0, p.badgeIcon)
            assertNotEquals("$reason hint icon", 0, p.hintIcon)
        }
    }

    @Test
    fun messagesAreTheOnesTheOldScreenAlreadyUsed() {
        assertEquals("Tu tutor bloqueó esta app.", blockMessage(BlockReason.BLOCK))
        assertEquals("Ya usaste el tiempo diario permitido para esta app.", blockMessage(BlockReason.DAILY_LIMIT))
        assertEquals("Ya usaste el tiempo semanal permitido para esta app.", blockMessage(BlockReason.WEEKLY_LIMIT))
        assertEquals("Esta app está bloqueada en este horario.", blockMessage(BlockReason.SCHEDULE))
        assertEquals(
            "Tu tutor bloqueó la categoría a la que pertenece esta app.",
            blockMessage(BlockReason.CATEGORY),
        )
        assertEquals(
            "Es horario escolar y esta app no está aprobada para este momento.",
            blockMessage(BlockReason.SCHOOL_MODE),
        )
        assertEquals(
            "Este dispositivo sólo permite las apps que tu tutor aprobó, y ésta no está aprobada.",
            blockMessage(BlockReason.DEFAULT_POLICY),
        )
    }

    @Test
    fun onlyTheTwoLimitsPromiseAReset() {
        // The engine resets the daily count at local midnight and the weekly one on Monday
        // (AppInventoryCollector); nothing else resets, so nothing else may promise it.
        assertTrue(blockPresentation(BlockReason.DAILY_LIMIT, null).hint.contains("mañana"))
        assertTrue(blockPresentation(BlockReason.WEEKLY_LIMIT, null).hint.contains("lunes"))
        BlockReason.entries
            .filter { it != BlockReason.DAILY_LIMIT && it != BlockReason.WEEKLY_LIMIT }
            .forEach { reason ->
                val hint = blockPresentation(reason, "Juegos").hint
                assertFalse("$reason promises a reset", hint.contains("mañana") || hint.contains("lunes"))
            }
    }

    @Test
    fun noHintShowsHoursOrOffersToUnlock() {
        // D-11 b was not chosen (no schedule hours reach this screen) and there is no
        // "ask for more time" function.
        BlockReason.entries.forEach { reason ->
            val text = blockPresentation(reason, "Juegos").run { "$message $hint" }
            assertFalse("$reason shows an hour", Regex("""\d{1,2}:\d{2}""").containsMatchIn(text))
            listOf("desbloque", "pedir", "solicitar", "más tiempo").forEach {
                assertFalse("$reason offers '$it'", text.contains(it, ignoreCase = true))
            }
        }
    }

    @Test
    fun categoryHintUsesTheRealNameOrNoneAndNeverInventsOne() {
        assertTrue(blockPresentation(BlockReason.CATEGORY, "Streaming").hint.contains("«Streaming»"))
        val without = blockPresentation(BlockReason.CATEGORY, null).hint
        assertFalse(without.contains("«"))
        assertFalse(without.contains("null"))
        // The sentence says "limita", not "bloquea": a category rule can also be a limit or a schedule.
        assertTrue(without.contains("limita"))
    }

    @Test
    fun theCoverNoteIsHonestWithoutExplainingHowToGetAround() {
        assertTrue(BLOCK_COVER_NOTE.contains("segundo plano"))
        assertFalse(BLOCK_COVER_NOTE.contains("Ajustes"))
        assertFalse(BLOCK_COVER_NOTE.contains("revoc", ignoreCase = true))
    }
}
