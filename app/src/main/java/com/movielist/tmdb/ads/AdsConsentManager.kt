package com.movielist.tmdb.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.movielist.tmdb.R

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
     * Whether this user has to be offered a way to review or withdraw their
     * consent (UMP says so for regions like the EEA and the UK). Observable,
     * because it is only known once the consent info update has come back.
     */
    var isPrivacyOptionsRequired by mutableStateOf(false)
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

    /**
     * Opens UMP's privacy options form, where the user can review, change or
     * withdraw the consent they gave. Offered wherever
     * [isPrivacyOptionsRequired] is true.
     */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Toast.makeText(activity, R.string.privacy_settings_unavailable, Toast.LENGTH_SHORT).show()
            }
            // A changed choice can change whether ads may be requested at all.
            syncCanRequestAds(activity, UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun syncCanRequestAds(context: Context, consentInformation: ConsentInformation) {
        isPrivacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        // Follows the SDK both ways, so ads stop if consent has to be asked again.
        canRequestAds = consentInformation.canRequestAds()
        if (!canRequestAds) return
        if (!mobileAdsInitialized) {
            mobileAdsInitialized = true
            MobileAds.initialize(context.applicationContext) { }
        }
    }
}
