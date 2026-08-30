package com.movielist

import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilsTest {

    @Test
    fun formatRuntime_rendersHoursAndMinutes() {
        assertEquals("1h 47m", Utils.formatRuntime(107))
        assertEquals("2h 0m", Utils.formatRuntime(120))
        assertEquals("45m", Utils.formatRuntime(45))
    }

    @Test
    fun formatRuntime_isNullWhenTheApiReportedNothing() {
        assertNull(Utils.formatRuntime(null))
        assertNull(Utils.formatRuntime(0))
    }

    @Test
    fun getYear_readsTheYearFromAReleaseDate() {
        assertEquals(1999, Utils.getYear("1999-03-31"))
        assertEquals(0, Utils.getYear(""))
    }

    @Test
    fun getGenres_joinsNamesAndSkipsMissingOnes() {
        val action = Genre().apply { id = 28; name = "Action" }
        val unnamed = Genre().apply { id = 99 }
        val drama = Genre().apply { id = 18; name = "Drama" }

        assertEquals("Action, Drama", Utils.getGenres(listOf(action, unnamed, drama)))
        assertEquals("", Utils.getGenres(null))
    }
}
