package one.allme.godot.admob.banner

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

interface RewardedInterstitialListener {
    fun onRewardedInterstitialLoaded()
    fun onRewardedInterstitialOpened()
    fun onRewardedInterstitialClosed()
    fun onRewardedInterstitialFailedToLoad(errorCode: Int)
    fun onRewardedInterstitialFailedToShow(errorCode: Int)
    fun onRewarded(type: String?, amount: Int)
    fun onRewardedClicked()
    fun onRewardedAdImpression()
}

class RewardedInterstitial(
    private val activity: Activity,
    private val listener: RewardedInterstitialListener
) {
    private var rewardedAd: RewardedInterstitialAd? = null

    init {
        MobileAds.initialize(activity)
    }

    fun load(id: String?, adRequest: AdRequest?) {
        RewardedInterstitialAd.load(
            activity,
            id!!,
            adRequest!!,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    super.onAdLoaded(rewardedAd)
                    setAd(rewardedAd)
                    Log.i(LOG_TAG, "rewarded interstitial ad loaded")
                    listener.onRewardedInterstitialLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    // safety
                    setAd(null)
                    Log.e(
                        LOG_TAG,
                        "rewarded interstitial ad failed to load. errorCode: " + loadAdError.code
                    )
                    listener.onRewardedInterstitialFailedToLoad(loadAdError.code)
                }
            })
    }

    fun show() {
        if (rewardedAd != null) {
            rewardedAd!!.show(activity) { rewardItem: RewardItem ->
                Log.i(
                    LOG_TAG,
                    String.format(
                        "rewarded interstitial ad rewarded! currency: %s amount: %d",
                        rewardItem.type,
                        rewardItem.amount
                    )
                )
                listener.onRewarded(rewardItem.type, rewardItem.amount)
            }
        }
    }

    private fun setAd(rewardedAd: RewardedInterstitialAd?) {
        // Avoid memory leaks.
        if (this.rewardedAd != null) this.rewardedAd!!.fullScreenContentCallback = null
        if (rewardedAd != null) {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.i(LOG_TAG, "rewarded interstitial ad clicked")
                    listener.onRewardedClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    Log.w(LOG_TAG, "rewarded interstitial ad dismissed full screen content")
                    listener.onRewardedInterstitialClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    Log.e(LOG_TAG, "rewarded interstitial ad failed to show full screen content")
                    listener.onRewardedInterstitialFailedToShow(adError.code)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.i(LOG_TAG, "rewarded interstitial ad impression")
                    listener.onRewardedAdImpression()
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    Log.i(LOG_TAG, "rewarded interstitial ad showed full screen content")
                    listener.onRewardedInterstitialOpened()
                }
            }
        }
        this.rewardedAd = rewardedAd
    }

    companion object {
        private val CLASS_NAME = RewardedInterstitial::class.java.getSimpleName()
        private val LOG_TAG = "godot::" + GodotAndroidAdmobPlugin.CLASS_NAME + "::" + CLASS_NAME
    }
}