package dev.tidytv.core.ops

import dev.tidytv.core.lists.HomeApp
import dev.tidytv.core.lists.Launchers
import dev.tidytv.core.lists.PackageName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * CleanYourTV's Live TV extras for the Apple TV style home screen (the launcher is built with CleanYourTV's
 * patch, tools/patch_launcher.py): the TV's own Live TV app as a blue "Live TV" tile, opened by the remote's TV
 * button, and the tuner's channels as a row on Home. They reach the launcher as config.json keys, so one build
 * works on every TV; a theme that sets a key itself keeps its own value.
 */
object LiveTv {
    const val PERMISSION = "android.permission.READ_TV_LISTINGS"

    /** Tuner apps by brand, tried when nothing answers for a channel (same list as CleanYourTV for Windows). */
    val KNOWN = listOf(
        "com.sony.dtv.tvx", "com.sony.dtv.tvlin", "org.droidtv.playtv", "com.mediatek.wwtv.tvcenter", "com.tcl.tv",
        "com.mitv.livetv", "com.xiaomi.mitv.tvplayer", "com.heytap.tv.livetv", "com.jamdeo.tv.livetv",
        "com.skyworth.tv", "com.zeasn.livetv", "com.smartdevice.livetv", "com.apps.livetv", "com.vt.tv.tvcenter",
        "com.cltv.hybrid", "com.realtek.tv", "com.sdmc.tv", "com.droidlogic.droidlivetv", "com.amazon.tv.livetv",
        "net.oqee.androidtv", "com.hisense.tv.tvplayer", "com.android.tv", "com.google.android.tv",
    )

    const val RESOLVE_CHANNEL = "cmd package resolve-activity --brief -a android.intent.action.VIEW " +
        "-d content://android.media.tv/channel/1 -t vnd.android.cursor.item/channel"

    fun grant(pkg: String) = "pm grant ${PackageName.require(pkg)} $PERMISSION 2>/dev/null; echo granted"

    /** The app that tunes a channel, as the stock launcher's channel tiles do; else a known tuner app; null without one. */
    suspend fun detect(tv: TvShell): String? = runCatching {
        val installed = Parse.packages(tv.run(Commands.LIST_PACKAGES))
        val last = Parse.lines(tv.run(RESOLVE_CHANNEL)).lastOrNull().orEmpty()
        val pkg = last.substringBefore('/', "").takeIf { PackageName.isValid(it) }
        val notLive = setOf("android", HomeApp.STYLED.pkg, HomeApp.SIMPLE.pkg) + Launchers.KNOWN_STOCK
        if (pkg != null && pkg in installed && pkg !in notLive && !pkg.contains("launcher")) pkg
        else KNOWN.firstOrNull { it in installed }
    }.getOrNull()

    /** The config.json keys for [pkg]. */
    fun keys(pkg: String): Map<String, JsonElement> {
        PackageName.require(pkg)
        return mapOf(
            "live_tv_package" to JsonPrimitive(pkg),
            "app_labels" to JsonArray(listOf(JsonPrimitive("$pkg=Live TV"))),
            "app_banners" to JsonArray(listOf(JsonPrimitive("$pkg=banner_tv_blue"))),
            "app_keys" to JsonArray(listOf(JsonPrimitive("KEYCODE_TV:$pkg"))),
        )
    }
}
