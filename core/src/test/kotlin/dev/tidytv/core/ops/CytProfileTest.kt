package dev.tidytv.core.ops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CytProfileTest {
    private val profileJson = """
        {"format": "cleanyourtv-profile", "version": 1, "name": "SkyworthDigital COSMOTE-TV TV Box (HY44G)",
         "created": "2026-10-24 17:05", "tv": {"model": "TV Box", "android": "14"},
         "disabled_packages": ["com.google.android.katniss", "not a package", "com.google.android.katniss", "com.sdt.retailapp"],
         "settings": {"global/private_dns_mode": "hostname", "global/private_dns_specifier": "dns.adguard-dns.com",
                      "global/window_animation_scale": "0.5", "secure/evil": "x", "system/screen_off_timeout": "600000; reboot"},
         "theme": {"from": "full-black", "files": ["config.json"]}}
    """.trimIndent()

    @Test fun readsProfile() {
        val p = CytProfiles.parse(mapOf(
            "profile.json" to profileJson.toByteArray(),
            "theme/config.json" to """{"background_color": "#000000"}""".toByteArray(),
            "theme/wallpapers/1-a.jpg" to byteArrayOf(1),
            "theme/wallpapers/../x.jpg" to byteArrayOf(2),
        ))
        assertEquals("SkyworthDigital COSMOTE-TV TV Box (HY44G)", p.name)
        assertEquals(listOf("com.google.android.katniss", "com.sdt.retailapp"), p.disabled)
        assertEquals(setOf("global/private_dns_mode", "global/private_dns_specifier", "global/window_animation_scale"), p.settings.keys)
        assertNotNull(p.theme)
        assertEquals(listOf("1-a.jpg"), p.theme!!.wallpapers.map { it.first })
        assertNull(p.theme!!.wallpaper)
    }

    @Test fun refusesOtherFiles() {
        val e = runCatching { CytProfiles.parse(mapOf("profile.json" to """{"format": "other"}""".toByteArray())) }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
        assertTrue(runCatching { CytProfiles.parse(emptyMap()) }.isFailure)
    }

    @Test fun noThemeWhenConfigIsBroken() {
        val p = CytProfiles.parse(mapOf("profile.json" to profileJson.toByteArray(), "theme/config.json" to "{oops".toByteArray()))
        assertNull(p.theme)
    }
}
