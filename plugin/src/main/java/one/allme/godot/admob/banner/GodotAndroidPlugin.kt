package one.allme.godot.admob.banner

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.collection.ArraySet
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays

class GodotAndroidAdmobPlugin(godot: Godot?) : GodotPlugin(godot) {
    private var activity: Activity? = null

    /**
     * Whether app is being tested (isReal=false) or app is in production (isReal=true)
     */
    private var isReal = false
    private var isForChildDirectedTreatment = false

    /**
     * Ads are personalized by default, GDPR compliance within the European Economic Area may require disabling of personalization.
     */
    private var isPersonalized = true
    private var maxAdContentRating: String? = ""
    private var extras: Bundle? = null
    private var layout: FrameLayout? = null
    private var rewardedVideo: RewardedVideo? = null
    private var rewardedInterstitial: RewardedInterstitial? = null
    private var interstitial: Interstitial? = null
    private var banner: Banner? = null
    override fun getPluginName(): String {
        return CLASS_NAME
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = ArraySet()
        signals.add(SignalInfo(SIGNAL_NAME_BANNER_LOADED))
        signals.add(
            SignalInfo(
                SIGNAL_NAME_BANNER_FAILED_TO_LOAD,
                Int::class.java
            )
        )
        signals.add(SignalInfo(SIGNAL_NAME_INTERSTITIAL_LOADED))
        signals.add(SignalInfo(SIGNAL_NAME_INTERSTITIAL_OPENED))
        signals.add(SignalInfo(SIGNAL_NAME_INTERSTITIAL_CLOSED))
        signals.add(SignalInfo(SIGNAL_NAME_INTERSTITIAL_CLICKED))
        signals.add(SignalInfo(SIGNAL_NAME_INTERSTITIAL_IMPRESSION))
        signals.add(
            SignalInfo(
                SIGNAL_NAME_INTERSTITIAL_FAILED_TO_LOAD,
                Int::class.java
            )
        )
        signals.add(
            SignalInfo(
                SIGNAL_NAME_INTERSTITIAL_FAILED_TO_SHOW,
                Int::class.java
            )
        )
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_VIDEO_OPENED))
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_VIDEO_LOADED))
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_VIDEO_CLOSED))
        signals.add(
            SignalInfo(
                SIGNAL_NAME_REWARDED_VIDEO_FAILED_TO_LOAD,
                Int::class.java
            )
        )
        signals.add(
            SignalInfo(
                SIGNAL_NAME_REWARDED_VIDEO_FAILED_TO_SHOW,
                Int::class.java
            )
        )
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_INTERSTITIAL_OPENED))
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_INTERSTITIAL_LOADED))
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_INTERSTITIAL_CLOSED))
        signals.add(
            SignalInfo(
                SIGNAL_NAME_REWARDED_INTERSTITIAL_FAILED_TO_LOAD,
                Int::class.java
            )
        )
        signals.add(
            SignalInfo(
                SIGNAL_NAME_REWARDED_INTERSTITIAL_FAILED_TO_SHOW,
                Int::class.java
            )
        )
        signals.add(
            SignalInfo(
                SIGNAL_NAME_REWARDED,
                String::class.java,
                Int::class.java
            )
        )
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_CLICKED))
        signals.add(SignalInfo(SIGNAL_NAME_REWARDED_IMPRESSION))
        return signals
    }

    override fun onMainCreate(activity: Activity): View? {
        this.activity = activity
        layout = FrameLayout(activity) // create and add a new layout to Godot
        return layout
    }

    @UsedByGodot
    fun init(isReal: Boolean) {
        initWithContentRating(
            isReal,
            isForChildDirectedTreatment,
            isPersonalized,
            maxAdContentRating
        )
    }

    /**
     * Initialize with additional content rating options
     *
     * @param isReal                      Tell if the environment is for real or test
     * @param isForChildDirectedTreatment Target audience is children.
     * @param isPersonalized              If ads should be personalized or not.
     * GDPR compliance within the European Economic Area requires that you
     * disable ad personalization if the user does not wish to opt into
     * ad personalization.
     * @param maxAdContentRating          must be "G", "PG", "T" or "MA"
     * @see [EU consent info](https://developers.google.com/admob/android/eu-consent.forward_consent_to_the_google_mobile_ads_sdk)
     */
    @UsedByGodot
    fun initWithContentRating(
        isReal: Boolean,
        isForChildDirectedTreatment: Boolean,
        isPersonalized: Boolean,
        maxAdContentRating: String?
    ) {
        this.isReal = isReal
        this.isForChildDirectedTreatment = isForChildDirectedTreatment
        this.isPersonalized = isPersonalized
        this.maxAdContentRating = maxAdContentRating
        setRequestConfigurations()
        if (!isPersonalized) {
            if (extras == null) {
                extras = Bundle()
            }
            extras!!.putString("npa", "1")
        }
        Log.d(LOG_TAG, "init AdMob with content rating options")
    }

    private fun setRequestConfigurations() {
        if (!isReal) {
            @SuppressLint("VisibleForTests") val testDeviceIds = Arrays.asList(
                AdRequest.DEVICE_ID_EMULATOR,
                adMobDeviceId
            )
            val requestConfiguration = MobileAds.getRequestConfiguration()
                .toBuilder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }
        if (isForChildDirectedTreatment) {
            val requestConfiguration = MobileAds.getRequestConfiguration()
                .toBuilder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }
        if (maxAdContentRating != null && !maxAdContentRating!!.isEmpty()) {
            val requestConfiguration = MobileAds.getRequestConfiguration()
                .toBuilder()
                .setMaxAdContentRating(maxAdContentRating)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }
    }

    private val adRequest: AdRequest
        /**
         * Returns AdRequest object constructed considering the extras.
         *
         * @return AdRequest object
         */
        private get() {
            val adBuilder = AdRequest.Builder()
            val adRequest: AdRequest
            if (!isForChildDirectedTreatment && extras != null) {
                adBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras!!)
            }
            adRequest = adBuilder.build()
            return adRequest
        }

    /**
     * Load an AdMob rewarded video ad
     *
     * @param id AdMob Rewarded video ID
     */
    @UsedByGodot
    fun loadRewardedVideo(id: String?) {
        activity!!.runOnUiThread {
            rewardedVideo = RewardedVideo(activity!!, object : RewardedVideoListener {
                override fun onRewardedVideoLoaded() {
                    emitSignal(SIGNAL_NAME_REWARDED_VIDEO_LOADED)
                }

                override fun onRewardedVideoFailedToLoad(errorCode: Int) {
                    emitSignal(
                        SIGNAL_NAME_REWARDED_VIDEO_FAILED_TO_LOAD,
                        errorCode
                    )
                }

                override fun onRewardedVideoFailedToShow(errorCode: Int) {
                    emitSignal(
                        SIGNAL_NAME_REWARDED_VIDEO_FAILED_TO_SHOW,
                        errorCode
                    )
                }

                override fun onRewardedVideoOpened() {
                    emitSignal(SIGNAL_NAME_REWARDED_VIDEO_OPENED)
                }

                override fun onRewardedVideoClosed() {
                    emitSignal(SIGNAL_NAME_REWARDED_VIDEO_CLOSED)
                }

                override fun onRewarded(type: String?, amount: Int) {
                    emitSignal(SIGNAL_NAME_REWARDED, type, amount)
                }

                override fun onRewardedClicked() {
                    emitSignal(SIGNAL_NAME_REWARDED_CLICKED)
                }

                override fun onRewardedAdImpression() {
                    emitSignal(SIGNAL_NAME_REWARDED_IMPRESSION)
                }
            })
            rewardedVideo!!.load(id, adRequest)
        }
    }

    @UsedByGodot
    fun showRewardedVideo() {
        activity!!.runOnUiThread {
            if (rewardedVideo == null) {
                return@runOnUiThread
            }
            rewardedVideo!!.show()
        }
    }

    /**
     * Load an AdMob rewarded interstitial
     *
     * @param id app's AdMob ID for rewarded interstitial ads
     */
    @UsedByGodot
    fun loadRewardedInterstitial(id: String?) {
        activity!!.runOnUiThread {
            rewardedInterstitial =
                RewardedInterstitial(activity!!, object : RewardedInterstitialListener {
                    override fun onRewardedInterstitialLoaded() {
                        emitSignal(SIGNAL_NAME_REWARDED_INTERSTITIAL_LOADED)
                    }

                    override fun onRewardedInterstitialOpened() {
                        emitSignal(SIGNAL_NAME_REWARDED_INTERSTITIAL_OPENED)
                    }

                    override fun onRewardedInterstitialClosed() {
                        emitSignal(SIGNAL_NAME_REWARDED_INTERSTITIAL_CLOSED)
                    }

                    override fun onRewardedInterstitialFailedToLoad(errorCode: Int) {
                        emitSignal(
                            SIGNAL_NAME_REWARDED_INTERSTITIAL_FAILED_TO_LOAD,
                            errorCode
                        )
                    }

                    override fun onRewardedInterstitialFailedToShow(errorCode: Int) {
                        emitSignal(
                            SIGNAL_NAME_REWARDED_INTERSTITIAL_FAILED_TO_SHOW,
                            errorCode
                        )
                    }

                    override fun onRewarded(type: String?, amount: Int) {
                        emitSignal(
                            SIGNAL_NAME_REWARDED,
                            type,
                            amount
                        )
                    }

                    override fun onRewardedClicked() {
                        emitSignal(SIGNAL_NAME_REWARDED_CLICKED)
                    }

                    override fun onRewardedAdImpression() {
                        emitSignal(SIGNAL_NAME_REWARDED_IMPRESSION)
                    }
                })
            rewardedInterstitial!!.load(id, adRequest)
        }
    }

    @UsedByGodot
    fun showRewardedInterstitial() {
        activity!!.runOnUiThread {
            if (rewardedInterstitial == null) {
                return@runOnUiThread
            }
            rewardedInterstitial!!.show()
        }
    }

    /**
     * Load an AdMob banner ad
     *
     * @param id      AdMod Banner ID
     * @param isOnTop To made the banner top or bottom
     */
    @UsedByGodot
    fun loadBanner(id: String?, isOnTop: Boolean, bannerSize: String?) {
        activity!!.runOnUiThread {
            banner?.remove()
            banner = bannerSize?.let {
                id?.let { it1 ->
                    Banner(it1, adRequest, activity!!, object : BannerListener {
                        override fun onBannerLoaded() {
                            emitSignal(SIGNAL_NAME_BANNER_LOADED)
                        }

                        override fun onBannerFailedToLoad(errorCode: Int) {
                            emitSignal(
                                SIGNAL_NAME_BANNER_FAILED_TO_LOAD,
                                errorCode
                            )
                        }
                    }, isOnTop, layout, it)
                }
            }
        }
    }

    @UsedByGodot
    fun showBanner() {
        activity!!.runOnUiThread {
            banner?.show()
        }
    }

    /**
     * Change banner's layer level
     * @param isOnTop whether the banner is on top of all layers
     */
    @UsedByGodot
    fun moveBanner(isOnTop: Boolean) {
        activity!!.runOnUiThread {
            banner?.move(isOnTop)
        }
    }

    @UsedByGodot
    fun resizeBanner() {
        activity!!.runOnUiThread {
            banner?.resize()
        }
    }

    @UsedByGodot
    fun hideBanner() {
        activity!!.runOnUiThread {
            banner?.hide()
        }
    }

    @get:UsedByGodot
    val bannerWidth: Int
        get() = banner?.getWidth() ?: 0

    @get:UsedByGodot
    val bannerHeight: Int
        get() = banner?.getHeight() ?: 0

    /**
     * Load an interstitial ad
     *
     * @param id AdMob interstitial ad ID
     */
    @UsedByGodot
    fun loadInterstitial(id: String?) {
        activity!!.runOnUiThread {
            interstitial = id?.let {
                Interstitial(
                    it,
                    adRequest, activity!!, object : InterstitialListener {
                        override fun onInterstitialLoaded() {
                            emitSignal(SIGNAL_NAME_INTERSTITIAL_LOADED)
                        }

                        override fun onInterstitialFailedToLoad(errorCode: Int) {
                            emitSignal(
                                SIGNAL_NAME_INTERSTITIAL_FAILED_TO_LOAD,
                                errorCode
                            )
                        }

                        override fun onInterstitialFailedToShow(errorCode: Int) {
                            emitSignal(
                                SIGNAL_NAME_INTERSTITIAL_FAILED_TO_SHOW,
                                errorCode
                            )
                        }

                        override fun onInterstitialOpened() {
                            // Not Implemented
                            emitSignal(SIGNAL_NAME_INTERSTITIAL_OPENED)
                        }

                        override fun onInterstitialClosed() {
                            emitSignal(SIGNAL_NAME_INTERSTITIAL_CLOSED)
                        }

                        override fun onInterstitialClicked() {
                            emitSignal(SIGNAL_NAME_INTERSTITIAL_CLICKED)
                        }

                        override fun onInterstitialImpression() {
                            emitSignal(SIGNAL_NAME_INTERSTITIAL_IMPRESSION)
                        }
                    })
            }
        }
    }

    @UsedByGodot
    fun showInterstitial() {
        activity!!.runOnUiThread {
            interstitial?.show()
        }
    }

    private val adMobDeviceId: String
        /**
         * Get the Device ID for AdMob
         *
         * @return String Device ID
         */
        private get() {
            @SuppressLint("HardwareIds") val androidId = Settings.Secure.getString(
                activity!!.contentResolver, Settings.Secure.ANDROID_ID
            )
            return md5(androidId).uppercase()
        }

    companion object {
        val CLASS_NAME = GodotAndroidAdmobPlugin::class.java.getSimpleName()
        private val LOG_TAG = "godot::" + CLASS_NAME
        private const val SIGNAL_NAME_BANNER_LOADED = "banner_loaded"
        private const val SIGNAL_NAME_BANNER_FAILED_TO_LOAD = "banner_failed_to_load"
        private const val SIGNAL_NAME_INTERSTITIAL_LOADED = "interstitial_loaded"
        private const val SIGNAL_NAME_INTERSTITIAL_OPENED = "interstitial_opened"
        private const val SIGNAL_NAME_INTERSTITIAL_CLOSED = "interstitial_closed"
        private const val SIGNAL_NAME_INTERSTITIAL_CLICKED = "interstitial_clicked"
        private const val SIGNAL_NAME_INTERSTITIAL_IMPRESSION = "interstitial_impression"
        private const val SIGNAL_NAME_INTERSTITIAL_FAILED_TO_LOAD = "interstitial_failed_to_load"
        private const val SIGNAL_NAME_INTERSTITIAL_FAILED_TO_SHOW = "interstitial_failed_to_show"
        private const val SIGNAL_NAME_REWARDED_VIDEO_OPENED = "rewarded_video_opened"
        private const val SIGNAL_NAME_REWARDED_VIDEO_LOADED = "rewarded_video_loaded"
        private const val SIGNAL_NAME_REWARDED_VIDEO_CLOSED = "rewarded_video_closed"
        private const val SIGNAL_NAME_REWARDED_VIDEO_FAILED_TO_LOAD =
            "rewarded_video_failed_to_load"
        private const val SIGNAL_NAME_REWARDED_VIDEO_FAILED_TO_SHOW =
            "rewarded_video_failed_to_show"
        private const val SIGNAL_NAME_REWARDED_INTERSTITIAL_OPENED = "rewarded_interstitial_opened"
        private const val SIGNAL_NAME_REWARDED_INTERSTITIAL_LOADED = "rewarded_interstitial_loaded"
        private const val SIGNAL_NAME_REWARDED_INTERSTITIAL_CLOSED = "rewarded_interstitial_closed"
        private const val SIGNAL_NAME_REWARDED_INTERSTITIAL_FAILED_TO_LOAD =
            "rewarded_interstitial_failed_to_load"
        private const val SIGNAL_NAME_REWARDED_INTERSTITIAL_FAILED_TO_SHOW =
            "rewarded_interstitial_failed_to_show"
        private const val SIGNAL_NAME_REWARDED = "rewarded"
        private const val SIGNAL_NAME_REWARDED_CLICKED = "rewarded_clicked"
        private const val SIGNAL_NAME_REWARDED_IMPRESSION = "rewarded_impression"

        /**
         * Generate MD5 for the deviceID
         *
         * @param s The string for which to generate the MD5
         * @return String The generated MD5
         */
        private fun md5(s: String): String {
            try {
                // Create MD5 Hash
                val digest = MessageDigest.getInstance("MD5")
                digest.update(s.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = StringBuilder()
                for (b in messageDigest) {
                    val h = StringBuilder(Integer.toHexString(0xFF and b.toInt()))
                    while (h.length < 2) h.insert(0, "0")
                    hexString.append(h)
                }
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                Log.e(LOG_TAG, "md5() - no such algorithm")
            }
            return ""
        }
    }
}