package com.movielist.tmdb.ads

/**
 * Decides when leaving a movie's detail screen may show an interstitial: at
 * most once every [visitsPerAd] visits, and never within [minIntervalMs] of
 * the last one shown. Counts live in memory for the life of the process,
 * which is what keeps ads from arriving back to back.
 *
 * Times are monotonic milliseconds (SystemClock.elapsedRealtime()) passed in
 * by the caller, so the rules stay testable.
 */
class InterstitialPacer(
    private val visitsPerAd: Int = 3,
    private val minIntervalMs: Long = 2 * 60 * 1000L
) {
    private var visitsSinceLastAd = 0
    private var lastShownAt: Long? = null

    /** Counts one visit to a detail screen; a rotation is not a new visit. */
    fun onDetailVisited() {
        visitsSinceLastAd++
    }

    /** Whether the visit being left at [now] may end with an interstitial. */
    fun isAdDue(now: Long): Boolean {
        if (visitsSinceLastAd < visitsPerAd) return false
        val last = lastShownAt ?: return true
        return now - last >= minIntervalMs
    }

    fun onAdShown(now: Long) {
        visitsSinceLastAd = 0
        lastShownAt = now
    }

    companion object {
        /** The app-wide pacing used by the detail screen. */
        val shared = InterstitialPacer()
    }
}
