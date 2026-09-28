package com.movielist

import com.movielist.tmdb.ads.InterstitialPacer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialPacerTest {

    private val minute = 60_000L
    private val pacer = InterstitialPacer(visitsPerAd = 3, minIntervalMs = 2 * minute)

    private fun visit(times: Int) = repeat(times) { pacer.onDetailVisited() }

    @Test
    fun noAd_beforeTheThirdVisit() {
        visit(2)
        assertFalse(pacer.isAdDue(now = 10 * minute))

        visit(1)
        assertTrue(pacer.isAdDue(now = 10 * minute))
    }

    @Test
    fun afterAnAd_itTakesThreeMoreVisits() {
        visit(3)
        pacer.onAdShown(now = 10 * minute)

        visit(2)
        assertFalse(pacer.isAdDue(now = 30 * minute))

        visit(1)
        assertTrue(pacer.isAdDue(now = 30 * minute))
    }

    @Test
    fun afterAnAd_twoMinutesHaveToPass_howeverManyVisits() {
        visit(3)
        pacer.onAdShown(now = 10 * minute)
        visit(10)

        assertFalse(pacer.isAdDue(now = 10 * minute + 2 * minute - 1))
        assertTrue(pacer.isAdDue(now = 10 * minute + 2 * minute))
    }

    @Test
    fun anAdThatWasNeverShown_doesNotResetTheCount() {
        visit(3)
        // Not shown (failed to load, no consent): the next exit may try again.
        visit(1)
        assertTrue(pacer.isAdDue(now = minute))
    }
}
