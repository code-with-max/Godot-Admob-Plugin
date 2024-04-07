package one.allme.godot.admob.banner

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError


interface BannerListener {
    fun onBannerLoaded()
    fun onBannerFailedToLoad(errorCode: Int)
}


class Banner constructor(
    id: String,
    private val adRequest: AdRequest,
    private val activity: Activity,
    listener: BannerListener,
    isOnTop: Boolean,
    private val layout: FrameLayout?,
    private val bannerSize: String
) {
    private var adView: AdView? = null // Banner view
    private var adParams: FrameLayout.LayoutParams? = null

    init {
        addBanner(
            id,
            if (isOnTop) Gravity.TOP else Gravity.BOTTOM,
            getAdSize(bannerSize),
            object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(LOG_TAG, "banner ad loaded")
                    listener.onBannerLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(LOG_TAG, "banner ad failed to load. errorCode: " + adError.code)
                    listener.onBannerFailedToLoad(adError.code)
                }
            })
    }

    fun show() {
        if (adView == null) {
            Log.w(LOG_TAG, "show ad - banner not loaded")
            return
        }
        if (adView!!.visibility == View.VISIBLE) {
            return
        }
        adView!!.visibility = View.VISIBLE
        adView!!.resume()
        Log.d(LOG_TAG, "show banner ad")
    }

    fun move(isOnTop: Boolean) {
        if (layout == null || adView == null || adParams == null) {
            return
        }
        layout.removeView(adView) // Remove the old view
        val adListener = adView!!.adListener
        val id = adView!!.adUnitId
        addBanner(id, if (isOnTop) Gravity.TOP else Gravity.BOTTOM, adView!!.adSize, adListener)
        Log.d(LOG_TAG, "banner ad moved")
    }

    fun resize() {
        if (layout == null || adView == null || adParams == null) {
            return
        }
        layout.removeView(adView) // Remove the old view
        val adListener = adView!!.adListener
        val id = adView!!.adUnitId
        addBanner(id, adParams!!.gravity, getAdSize(bannerSize), adListener)
        Log.d(LOG_TAG, "banner ad resized")
    }

    private fun addBanner(id: String, gravity: Int, size: AdSize?, listener: AdListener) {
        adParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        adParams!!.gravity = gravity

        // Create new view & set old params
        adView = AdView(activity)
        adView!!.adUnitId = id
        adView!!.setBackgroundColor(Color.TRANSPARENT)
        adView!!.setAdSize(size!!)
        adView!!.adListener = listener

        // Add to layout and load ad
        layout!!.addView(adView, adParams)

        // Request
        adView!!.loadAd(adRequest)
    }

    fun remove() {
        if (adView != null) {
            layout!!.removeView(adView) // Remove the old view
        }
    }

    fun hide() {
        if (adView!!.visibility == View.GONE) return
        adView!!.visibility = View.GONE
        adView!!.pause()
        Log.d(LOG_TAG, "hide banner ad")
    }

    private val adaptiveAdSize: AdSize
        private get() {
            // Determine the screen width (less decorations) to use for the ad width.
            val display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            val widthPixels: Int
            val density: Float
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                widthPixels = activity.windowManager.currentWindowMetrics.bounds.width()
                density = activity.resources.configuration.densityDpi.toFloat()
            } else {
                display.getMetrics(outMetrics)
                widthPixels = outMetrics.widthPixels
                density = outMetrics.density
            }
            var adWidth = 50
            if (density == 0f) {
                Log.e(LOG_TAG, "Cannot detect display density.")
            } else {
                adWidth = (widthPixels / density).toInt()
            }

            // Get adaptive ad size and return for setting on the ad view.
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }

    private fun getAdSize(bannerSize: String): AdSize {
        return when (bannerSize) {
            "BANNER" -> AdSize.BANNER
            "LARGE_BANNER" -> AdSize.LARGE_BANNER
            "MEDIUM_RECTANGLE" -> AdSize.MEDIUM_RECTANGLE
            "FULL_BANNER" -> AdSize.FULL_BANNER
            "LEADERBOARD" -> AdSize.LEADERBOARD
            else -> adaptiveAdSize
        }
    }

    fun getWidth(): Int {
        return getAdSize(bannerSize).getWidthInPixels(activity)
    }

    fun getHeight(): Int {
        return getAdSize(bannerSize).getHeightInPixels(activity)
    }

    companion object {
        // private val CLASS_NAME = Banner::class.java.getSimpleName()
        const val LOG_TAG: String = BuildConfig.GODOT_PLUGIN_NAME
    }
}