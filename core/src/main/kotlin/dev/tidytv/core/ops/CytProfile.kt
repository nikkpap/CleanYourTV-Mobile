package dev.tidytv.core.ops

import dev.tidytv.core.lists.PackageName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A profile saved by CleanYourTV for Windows (Backup → Profiles): a .cytv zip with profile.json and the theme's
 * files under theme/, or a bundle .zip with that .cytv as profile.cytv plus the apps under apps/. Applying it sets
 * a TV up the way the saved one was: the same apps off, the same look, the same few settings.
 */
data class CytProfile(
    val name: String,
    val created: String,
    val model: String?,
    val android: String?,
    /** Packages that were off on the saved TV. They still go through every check before one is touched. */
    val disabled: List<String>,
    /** "namespace/key" -> value, only the keys in [CytProfiles.SETTINGS] and only plain values. */
    val settings: Map<String, String>,
    /** The look, for the Apple TV style home screen; null when the profile has none. */
    val theme: LookFiles?,
)

object CytProfiles {
    /** The settings a profile carries, by group (as CleanYourTV for Windows offers them). */
    val SETTINGS: Map<String, List<String>> = linkedMapOf(
        "dns" to listOf("global/private_dns_mode", "global/private_dns_specifier"),
        "anim" to listOf("global/window_animation_scale", "global/transition_animation_scale", "global/animator_duration_scale"),
        "dream" to listOf("secure/screensaver_components", "secure/screensaver_enabled", "secure/screensaver_activate_on_sleep", "system/screen_off_timeout"),
    )
    private val ALL_KEYS = SETTINGS.values.flatten().toSet()
    private val VALUE = Regex("^[A-Za-z0-9_.,:/-]*$")
    private val WALL = Regex("^theme/wallpapers/([A-Za-z0-9_][A-Za-z0-9_.-]{0,63})$")
    private val json = Json { ignoreUnknownKeys = true }

    /** Reads a .cytv from its zip entries (name -> bytes). Throws IllegalArgumentException when it isn't one. */
    fun parse(entries: Map<String, ByteArray>): CytProfile {
        val text = entries["profile.json"]?.decodeToString() ?: throw IllegalArgumentException("no profile.json: not a CleanYourTV profile")
        val o = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: throw IllegalArgumentException("profile.json is not valid")
        if (o.str("format") != "cleanyourtv-profile") throw IllegalArgumentException("not a CleanYourTV profile")
        val tv = o["tv"] as? JsonObject
        val disabled = runCatching { o["disabled_packages"]!!.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull } }
            .getOrDefault(emptyList()).filter(PackageName::isValid).distinct()
        val settings = (o["settings"] as? JsonObject).orEmpty().mapNotNull { (k, v) ->
            val value = runCatching { v.jsonPrimitive.contentOrNull }.getOrNull()?.trim() ?: return@mapNotNull null
            if (k in ALL_KEYS && VALUE.matches(value)) k to value else null
        }.toMap()
        val config = entries["theme/config.json"]?.decodeToString()
        val theme = config?.takeIf { runCatching { json.parseToJsonElement(it).jsonObject }.isSuccess }?.let {
            val walls = entries.keys.mapNotNull { n -> WALL.matchEntire(n)?.groupValues?.get(1) }
                .filter { n -> n.matches(Regex("(?i).+\\.(jpe?g|png|webp)")) }.sorted()
            LookFiles(
                id = "profile",
                config = it,
                wallpaper = entries["theme/wallpaper.jpg"],
                wallpapers = walls.map { n -> n to entries.getValue("theme/wallpapers/$n") },
                captions = entries["theme/wallpapers/captions.txt"],
            )
        }
        return CytProfile(
            name = o.str("name") ?: "Profile",
            created = o.str("created").orEmpty(),
            model = tv?.str("model"),
            android = tv?.str("android"),
            disabled = disabled,
            settings = settings,
            theme = theme,
        )
    }

    private fun JsonObject.str(k: String): String? = runCatching { this[k]?.jsonPrimitive?.contentOrNull }.getOrNull()
}

/** The parts of a profile that aren't packages or the look: settings and the bundle's apps. */
class ProfileApplier(
    private val tv: TvShell,
    private val log: ActionLog,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /**
     * Puts the profile's settings for [groups] on the TV. Each one's value from before goes in the log first, so
     * Restore puts the TV's own value back. A screensaver that needs the CleanYourTV home screen is skipped when
     * that isn't installed. Returns how many were set.
     */
    suspend fun settings(p: CytProfile, groups: Set<String>, styledInstalled: Boolean): Int {
        var n = 0
        for ((group, keys) in CytProfiles.SETTINGS) {
            if (group !in groups) continue
            for (key in keys) {
                val value = p.settings[key] ?: continue
                if (key == "secure/screensaver_components" && value.contains(dev.tidytv.core.lists.HomeApp.STYLED.pkg) && !styledInstalled) continue
                val (ns, name) = key.split('/', limit = 2)
                val before = tv.run(Settings.get(ns, name)).trim().ifEmpty { "null" }
                if (before == value) continue
                log.append(ActionRecord(clock(), Op.SETTING, key, Phase.INTENT, before = before))
                tv.run(if (value.isEmpty() || value == "null") Settings.delete(ns, name) else Settings.put(ns, name, value))
                log.append(ActionRecord(clock(), Op.SETTING, key, Phase.DONE, before = before))
                n++
            }
        }
        return n
    }

    /** Installs one app from a bundle (an .apk); "" when it worked, else the TV's answer. */
    suspend fun installApp(source: java.io.InputStream, size: Long, onProgress: (Long) -> Unit = {}): String {
        tv.pushStream(source, size, APP_TMP, onProgress)
        val out = tv.run("pm install -r -g $APP_TMP 2>&1 || pm install -r $APP_TMP 2>&1")
        tv.run("rm -f $APP_TMP")
        return if (out.contains("Success")) "" else Parse.lines(out).lastOrNull() ?: "no answer"
    }

    companion object {
        const val APP_TMP = "/data/local/tmp/cyt-profile-app.apk"
    }
}
