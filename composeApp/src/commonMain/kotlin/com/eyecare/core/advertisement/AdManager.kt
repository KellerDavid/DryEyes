package com.eyecare.core.advertisement

/** Where a banner may appear. Exercise, success, reminder and first-launch surfaces never show ads (US-18). */
enum class AdPlacement(val allowsAds: Boolean) {
    PANEL(true),
    ONBOARDING(false),
    EXERCISE(false),
    SUCCESS(false),
    REMINDER(false),
}

/** A bundled, static banner (no ad network in the MVP). Colors are ARGB for the image tile gradient. */
enum class AdBanner(val brand: String, val title: String, val subtitle: String, val from: Long, val to: Long) {
    AQUALID("AquaLid", "AquaLid hidratáló szemcsepp", "Statikus minta banner", 0xFF7CC6E8, 0xFF3A88C9),
    BLUESHIELD("BlueShield", "Kékfény-szűrős szemüveg", "Statikus minta banner", 0xFF9A7BFF, 0xFF5B3FD1),
    ERGOSTAND("ErgoStand", "Állítható monitorállvány", "Statikus minta banner", 0xFF8BD3A8, 0xFF3C9A6B),
}

class AdManager(private val banners: List<AdBanner> = AdBanner.entries) {
    private var cursor = 0

    /** Next banner in rotation for [placement], or null where ads are not allowed. */
    fun bannerFor(placement: AdPlacement): AdBanner? {
        if (!placement.allowsAds || banners.isEmpty()) return null
        return banners[cursor++ % banners.size]
    }
}
