package dev.tidytv.app.data

import dev.tidytv.core.ops.CytProfile
import dev.tidytv.core.ops.CytProfiles
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** A profile picked on the phone: what's in it, and the bundle's apps copied to the phone's cache. */
class ImportedProfile(val profile: CytProfile, val apps: List<File>, val skippedApps: List<String>)

/**
 * Reads a CleanYourTV profile: a .cytv (profile.json + theme/), or a bundle .zip (profile.cytv + apps/ + bundle.json)
 * from CleanYourTV for Windows. Apps are .apk files; split .apks sets are listed as skipped (the phone installs
 * single APKs only). Theme files are at most a few MB, so they are read into memory; apps are streamed to [cache].
 */
object ProfileImport {
    private const val MAX_SMALL = 40L * 1024 * 1024
    private val APP = Regex("^apps/([A-Za-z0-9_.()+-]+\\.apks?)$")

    fun read(open: () -> InputStream, cache: File): ImportedProfile {
        val dir = File(cache, "profile-apps").apply { deleteRecursively(); mkdirs() }
        val top = mutableMapOf<String, ByteArray>()
        val apps = mutableListOf<File>()
        val skipped = mutableListOf<String>()
        ZipInputStream(open().buffered()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (e.isDirectory || e.name.contains("..")) continue
                val app = APP.matchEntire(e.name)?.groupValues?.get(1)
                when {
                    app != null && app.endsWith(".apk", ignoreCase = true) ->
                        File(dir, app).also { f -> f.outputStream().use { out -> z.copyTo(out) }; apps += f }
                    app != null -> skipped += app
                    else -> top[e.name] = readLimited(z, e.name)
                }
            }
        }
        val entries = top["profile.cytv"]?.let(::unzip) ?: top
        return ImportedProfile(CytProfiles.parse(entries), apps, skipped)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (!e.isDirectory && !e.name.contains("..")) out[e.name] = readLimited(z, e.name)
            }
        }
        return out
    }

    private fun readLimited(z: InputStream, name: String): ByteArray {
        val buf = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = z.read(chunk)
            if (n < 0) break
            total += n
            if (total > MAX_SMALL) throw IllegalArgumentException("$name is too big for a profile")
            buf.write(chunk, 0, n)
        }
        return buf.toByteArray()
    }
}
