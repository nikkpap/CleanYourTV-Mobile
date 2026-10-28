package dev.tidytv.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.tidytv.app.PickedFile
import dev.tidytv.app.ProfileState
import dev.tidytv.app.ui.BigButton
import dev.tidytv.app.ui.Body
import dev.tidytv.app.ui.CardBox
import dev.tidytv.app.ui.InfoChip
import dev.tidytv.app.ui.MoreInfo
import dev.tidytv.app.ui.Note
import dev.tidytv.app.ui.ScreenFrame
import dev.tidytv.app.ui.Spinner
import dev.tidytv.app.ui.Tidy
import dev.tidytv.app.ui.Title
import dev.tidytv.app.ui.Toggle
import dev.tidytv.app.ui.TopBar

/**
 * "My profile" (CleanYourTV): pick a profile saved with CleanYourTV for Windows (Backup → Profiles: a .cytv, or a
 * .zip with the apps too), choose its parts, and put it on this TV.
 */
@Composable
fun ProfileScreen(
    st: ProfileState,
    tvName: String?,
    onPick: (PickedFile) -> Unit,
    onToggle: (String) -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
) {
    val resolver = LocalContext.current.contentResolver
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            var name = "profile"
            var size = 0L
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val z = c.getColumnIndex(OpenableColumns.SIZE)
                        if (n >= 0 && !c.isNull(n)) name = c.getString(n) ?: "profile"
                        if (z >= 0 && !c.isNull(z)) size = c.getLong(z)
                    }
                }
            }
            onPick(PickedFile(name, size) { resolver.openInputStream(uri) ?: error("can't open") })
        }
    }
    val busy = st.running || st.loading
    ScreenFrame(
        top = { TopBar(back = "My TV", onBack = onClose, tvName = tvName) },
        bottom = {
            if (st.name != null && st.message?.ok != true) BigButton(
                if (st.running) "Setting up…" else "Apply to this TV", onApply, enabled = !busy && st.parts.isNotEmpty(),
            )
            BigButton(if (st.name == null) "Pick a profile" else "Pick another", {
                picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            }, color = Tidy.Card, textColor = Tidy.Ink, enabled = !busy)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title("My profile", Modifier.weight(1f))
            MoreInfo("Profiles") {
                Body("A profile is saved with CleanYourTV for Windows: Backup → Profiles → Save profile (a .cytv), or Save profile + apps (a .zip with the apps too).", color = Tidy.Ink)
                Body("Applying it on a TV of the same kind turns off the same apps, installs the apps, puts the same look on the home screen and sets the same Private DNS, animation speed and screensaver.", color = Tidy.Ink)
                Body("Every app still goes through the same safety checks as Tidy up, a backup is saved first, and Restore puts it all back.", color = Tidy.Ink)
            }
        }
        if (st.name == null && !st.loading) {
            Body("Copy your .cytv or .zip to this phone (from your PC, e-mail or Drive), then pick it here.")
        }
        if (st.loading) Row(verticalAlignment = Alignment.CenterVertically) { Spinner(); Text("  Reading ${st.fileName ?: "the file"}…") }
        st.message?.let { if (it.ok) InfoChip(Icons.Rounded.CheckCircle, it.message) else Note(it.message) }
        if (st.name != null) {
            CardBox(Modifier.fillMaxWidth()) {
                Text(st.name, style = MaterialTheme.typography.titleMedium, color = Tidy.Ink)
                if (!st.from.isNullOrEmpty()) Body("Saved from ${st.from}")
            }
            if (st.apps > 0) Part("Install ${st.apps} app${if (st.apps == 1) "" else "s"}", "apps", st, onToggle)
            if (st.skippedApps.isNotEmpty()) Body("Not installed from the phone (split .apks): ${st.skippedApps.joinToString()}. Use CleanYourTV for Windows for these.")
            if (st.packages > 0) Part("Turn off ${st.packages} apps", "packages", st, onToggle)
            else Body("The apps this profile turns off are already off here, or not on this TV.")
            if (st.hasTheme) Part("The look (wallpaper, dock, colours)", "theme", st, onToggle)
            if (st.hasTheme && !st.styledHome) Note("The look goes on the Apple TV style home screen. Put it on first from Home screen, then come back here.")
            for (g in st.settingGroups) Part(when (g) {
                "dns" -> "Private DNS (ad blocking)"
                "anim" -> "Animation speed"
                "dream" -> "Screensaver"
                else -> g
            }, g, st, onToggle)
        }
        for (l in st.lines) Text(l, style = MaterialTheme.typography.bodyMedium, color = Tidy.Ink)
        if (st.running) Spinner()
    }
}

@Composable
private fun Part(label: String, key: String, st: ProfileState, onToggle: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Tidy.Ink)
        Toggle(key in st.parts, { if (!st.running) onToggle(key) }, label)
    }
}
