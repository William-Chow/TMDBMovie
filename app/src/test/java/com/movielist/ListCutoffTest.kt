package com.movielist

import com.movielist.tmdb.ui.ListCutoff
import org.junit.Assert.assertEquals
import org.junit.Test

class ListCutoffTest {

    private var today = "2026-10-15"
    private val cutoff = ListCutoff { today }

    @Test
    fun aListScrolledPastMidnight_keepsTheCutoffOfItsFirstPage() {
        assertEquals("2026-10-15", cutoff.forPage(1))

        today = "2026-10-16"

        assertEquals("2026-10-15", cutoff.forPage(2))
        assertEquals("2026-10-15", cutoff.forPage(3))
    }

    @Test
    fun fetchingPageOneAgain_takesTodaysDate() {
        cutoff.forPage(1)
        cutoff.forPage(2)

        today = "2026-10-16"

        assertEquals("a refresh starts from today", "2026-10-16", cutoff.forPage(1))
        assertEquals("2026-10-16", cutoff.forPage(2))
    }
}
