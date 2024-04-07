package one.allme.godot.admob.banner

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

interface RewardedVideoListener {
    fun onRewardedVideoLoaded()
    fun onRewardedVideoFailedToLoad(errorCode: Int)
    fun onRewardedVideoFailedToShow(errorCode: Int)
    fun onRewardedVideoOpened()
    fun onRewardedVideoClosed()
    fun onRewarded(type: String?, amount: Int)
    fun onRewardedClicked()
    fun onRewardedAdImpression()
}

class RewardedVideo(private val activity: Activity, private val listener: RewardedVideoListener) {
    private var rewardedAd: RewardedAd? = null

    init {
        MobileAds.initialize(activity)
    }

    fun load(id: String?, adRequest: AdRequest?) {
        RewardedAd.load(activity, id!!, adRequest!!, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                super.onAdLoaded(rewardedAd)
                setAd(rewardedAd)
                Log.i(LOG_TAG, "rewarded video ad loaded")
                listener.onRewardedVideoLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                // safety
                setAd(null)
                Log.e(LOG_TAG, "rewarded video ad failed to load. errorCode: " + loadAdError.code)
                listener.onRewardedVideoFailedToLoad(loadAdError.code)
            }
        })
    }

    fun show() {
        if (rewardedAd != null) {
            rewardedAd!!.show(activity) { rewardItem: RewardItem ->
                Log.i(
                    LOG_TAG,
                    String.format(
                        "rewarded video ad reward received! currency: %s amount: %d",
                        rewardItem.type,
                        rewardItem.amount
                    )
                )
                listener.onRewarded(rewardItem.type, rewardItem.amount)
            }
        }
    }

    private fun setAd(rewardedAd: RewardedAd?) {
        // Avoid memory leaks.
        if (this.rewardedAd != null) this.rewardedAd!!.fullScreenContentCallback = null
        if (rewardedAd != null) {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.i(LOG_TAG, "rewarded video ad clicked")
                    listener.onRewardedClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    Log.w(LOG_TAG, "rewarded video ad dismissed full screen content")
                    listener.onRewardedVideoClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    Log.e(LOG_TAG, "rewarded video ad failed to show full screen content")
                    listener.onRewardedVideoFailedToShow(adError.code)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.i(LOG_TAG, "rewarded video ad impression")
                    listener.onRewardedAdImpression()
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    Log.i(LOG_TAG, "rewarded video ad showed full screen content")
                    listener.onRewardedVideoOpened()
                }
            }
        }
        this.rewardedAd = rewardedAd
    }

    companion object {
        private val CLASS_NAME = RewardedVideo::class.java.getSimpleName()
        private val LOG_TAG = "godot::" + GodotAndroidAdmobPlugin.CLASS_NAME + "::" + CLASS_NAME
    }
}