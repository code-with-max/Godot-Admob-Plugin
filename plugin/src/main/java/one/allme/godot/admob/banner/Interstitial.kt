package one.allme.godot.admob.banner

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

interface InterstitialListener {
    fun onInterstitialLoaded()
    fun onInterstitialFailedToLoad(errorCode: Int)
    fun onInterstitialFailedToShow(errorCode: Int)
    fun onInterstitialOpened()
    fun onInterstitialClosed()
    fun onInterstitialClicked()
    fun onInterstitialImpression()
}

class Interstitial(
    private val id: String,
    private val adRequest: AdRequest,
    private val activity: Activity,
    private val listener: InterstitialListener
) {
    private var interstitialAd: InterstitialAd? = null

    init {
        load()
    }

    fun show() {
        if (interstitialAd != null) {
            interstitialAd!!.show(activity)
        } else {
            Log.w(LOG_TAG, "show ad - interstitial not loaded")
        }
    }

    private fun setAd(interstitialAd: InterstitialAd?) {
        if (interstitialAd === this.interstitialAd) return

        // Avoid memory leaks
        if (this.interstitialAd != null) {
            this.interstitialAd!!.fullScreenContentCallback = null
            this.interstitialAd!!.onPaidEventListener = null
        }
        if (interstitialAd != null) {
            interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.i(LOG_TAG, "interstitial ad clicked")
                    listener.onInterstitialClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    setAd(null)
                    Log.w(LOG_TAG, "interstitial ad dismissed full screen consent")
                    listener.onInterstitialClosed()
                    load()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    Log.e(LOG_TAG, "interstitial ad failed to show full screen content")
                    listener.onInterstitialFailedToShow(adError.code)
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    Log.i(LOG_TAG, "interstitial ad showed full screen content")
                    listener.onInterstitialOpened()
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.i(LOG_TAG, "interstitial ad impression")
                    listener.onInterstitialImpression()
                }
            }
        }
        this.interstitialAd = interstitialAd
    }

    private fun load() {
        InterstitialAd.load(activity, id, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                setAd(interstitialAd)
                Log.i(LOG_TAG, "interstitial ad loaded")
                listener.onInterstitialLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                // safety
                setAd(null)
                Log.e(LOG_TAG, "interstitial ad failed to load - error code: " + loadAdError.code)
                listener.onInterstitialFailedToLoad(loadAdError.code)
            }
        })
    }

    companion object {
        private val CLASS_NAME = Interstitial::class.java.getSimpleName()
        private val LOG_TAG = "godot::" + GodotAndroidAdmobPlugin.CLASS_NAME + "::" + CLASS_NAME
    }
}