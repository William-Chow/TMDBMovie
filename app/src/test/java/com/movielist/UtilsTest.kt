package com.movielist

import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.network.model.Video
import com.movielist.tmdb.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun today_isAnIsoDateAsTmdbExpects() {
        val today = Utils.today()
        assertTrue(today, Regex("\\d{4}-\\d{2}-\\d{2}").matches(today))
        assertEquals(LocalDate.now().toString(), today)
    }

    @Test
    fun getGenres_joinsNamesAndSkipsMissingOnes() {
        val action = Genre().apply { id = 28; name = "Action" }
        val unnamed = Genre().apply { id = 99 }
        val drama = Genre().apply { id = 18; name = "Drama" }

        assertEquals("Action, Drama", Utils.getGenres(listOf(action, unnamed, drama)))
        assertEquals("", Utils.getGenres(null))
    }

    @Test
    fun videoLanguages_addsTheOriginalLanguageWhenItIsNotEnglish() {
        assertEquals("en,null,ko", Utils.videoLanguages("ko"))
        assertEquals("en,null,ja", Utils.videoLanguages(" JA "))
        assertEquals("en,null", Utils.videoLanguages("en"))
        assertEquals("en,null", Utils.videoLanguages(null))
        assertEquals("en,null", Utils.videoLanguages(""))
    }

    private fun video(key: String?, type: String, language: String?, official: Boolean = false, site: String = "YouTube") =
        Video().apply {
            this.key = key
            this.type = type
            this.site = site
            iso_639_1 = language
            this.official = official
        }

    @Test
    fun pickTrailerKey_prefersAnOfficialTrailer_thenAnyTrailer_thenATeaser() {
        assertEquals(
            "official",
            Utils.pickTrailerKey(
                listOf(
                    video("teaser", "Teaser", "en"),
                    video("unofficial", "Trailer", "en"),
                    video("official", "Trailer", "en", official = true)
                )
            )
        )
        assertEquals("teaser", Utils.pickTrailerKey(listOf(video("clip", "Clip", "en"), video("teaser", "Teaser", "en"))))
        assertEquals("clip", Utils.pickTrailerKey(listOf(video("clip", "Featurette", "en"))))
    }

    @Test
    fun pickTrailerKey_prefersEnglish_butFallsBackToTheOriginalLanguage() {
        val korean = video("ko-trailer", "Trailer", "ko", official = true)
        val english = video("en-trailer", "Trailer", "en", official = true)

        assertEquals("en-trailer", Utils.pickTrailerKey(listOf(korean, english)))
        assertEquals("ko-trailer", Utils.pickTrailerKey(listOf(korean)))
    }

    @Test
    fun pickTrailerKey_ignoresOtherSitesAndMissingKeys() {
        assertNull(Utils.pickTrailerKey(listOf(video("v", "Trailer", "en", site = "Vimeo"), video(null, "Trailer", "en"))))
        assertNull(Utils.pickTrailerKey(null))
    }

    @Test
    fun imageURL_joinsBaseSizeAndPath() {
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", Utils.imageURL("/abc.jpg", Utils.posterMedium))
        assertEquals("https://image.tmdb.org/t/p/w185/p.png", Utils.imageURL(" /p.png ", Utils.profileSmall))
    }

    @Test
    fun imageURL_isNullWhenThereIsNoImage_insteadOfEndingInNull() {
        assertNull(Utils.imageURL(null, Utils.posterLarge))
        assertNull(Utils.imageURL("", Utils.posterLarge))
        assertNull(Utils.imageURL("   ", Utils.posterSmall))
    }
}
