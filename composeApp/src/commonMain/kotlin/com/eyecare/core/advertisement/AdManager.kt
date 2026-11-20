package com.eyecare.core.advertisement

/** Where a banner may appear. Exercise, success and reminder surfaces are deliberately absent (US-18). */
enum class AdPlacement(val allowsAds: Boolean) {
    HOME(true),
    SETTINGS(true),
    SCHEDULE(false),
    EXERCISE(false),
    SUCCESS(false),
    REMINDER(false),
}

/** A bundled, static banner image (no ad network in the MVP). */
enum class AdBanner(val description: String) {
    BANNER_1("Minta hirdetés: kékfény-szűrős szemüveg"),
    BANNER_2("Minta hirdetés: állítható monitorállvány"),
    BANNER_3("Minta hirdetés: hidratáló szemcsepp"),
}

class AdManager(private val banners: List<AdBanner> = AdBanner.entries) {
    private var cursor = 0

    /** Next banner in rotation for [placement], or null where ads are not allowed. */
    fun bannerFor(placement: AdPlacement): AdBanner? {
        if (!placement.allowsAds || banners.isEmpty()) return null
        return banners[cursor++ % banners.size]
    }
}
