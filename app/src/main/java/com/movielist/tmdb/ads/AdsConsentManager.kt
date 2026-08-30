package com.movielist.tmdb.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Owns the GDPR/UMP consent handshake and the one-time Mobile Ads start-up.
 *
 * Nothing is requested from AdMob until the SDK reports that this user may be
 * served ads, which is what regions covered by the EU consent rules require.
 */
object AdsConsentManager {

    private var mobileAdsInitialized = false

    /** Observable so ad slots appear as soon as consent allows them. */
    var canRequestAds by mutableStateOf(false)
        private set

    /**
     * Refreshes what is known about this user's consent and shows the form if
     * their region requires one. Consent is per-app, so only the launcher
     * activity calls this.
     */
    fun gatherConsent(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // The form error is not worth surfacing: canRequestAds()
                    // below is the authority on whether ads may load.
                    syncCanRequestAds(activity, consentInformation)
                }
            },
            {
                // Consent lookup failed; fall back to whatever is already stored.
                syncCanRequestAds(activity, consentInformation)
            }
        )

        // Consent already on file from an earlier run — no need to wait for the
        // network round trip before showing ads.
        syncCanRequestAds(activity, consentInformation)
    }

    /** For screens other than the launcher, which never gather consent themselves. */
    fun refresh(context: Context) {
        syncCanRequestAds(context, UserMessagingPlatform.getConsentInformation(context))
    }

    private fun syncCanRequestAds(context: Context, consentInformation: ConsentInformation) {
        if (!consentInformation.canRequestAds()) return
        canRequestAds = true
        if (!mobileAdsInitialized) {
            mobileAdsInitialized = true
            MobileAds.initialize(context.applicationContext) { }
        }
    }
}
