package com.remindr.app.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class FormatUtilsTest {
    @Test
    fun parseUsDate_acceptsSlashFormat() {
        assertEquals(
            expected = LocalDate(2026, 2, 19),
            actual = parseUsOrIsoLocalDateOrNull("02/19/2026"),
        )
    }

    @Test
    fun parseUsDate_acceptsIsoFormatForBackCompat() {
        assertEquals(
            expected = LocalDate(2026, 2, 19),
            actual = parseUsOrIsoLocalDateOrNull("2026-02-19"),
        )
    }

    @Test
    fun parseUsDate_rejectsInvalidText() {
        assertNull(parseUsOrIsoLocalDateOrNull("not-a-date"))
    }
}
