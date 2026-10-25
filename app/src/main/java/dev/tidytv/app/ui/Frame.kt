package dev.tidytv.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight

/**
 * Every screen: a white rounded sheet on the pale-blue backdrop (concept C), a scrolling body and an
 * optional pinned bottom action so the main button is always in reach.
 */
@Composable
fun ScreenFrame(
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Tidy.Backdrop).safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(28.dp)).background(Tidy.Card),
        ) {
            if (top != null) Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp)) { top() }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = body,
            )
            if (bottom != null) Column(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 16.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bottom,
            )
        }
    }
}

/**
 * The bar on every screen inside My TV: a back link on the left and the TV's name on the right,
 * so it's always clear which TV the buttons act on.
 */
@Composable
fun TopBar(back: String, onBack: () -> Unit, tvName: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextLink(back, onBack, icon = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack)
        Spacer(Modifier.weight(1f))
        if (tvName != null) Text(
            tvName, style = MaterialTheme.typography.labelMedium, color = Tidy.Ink, maxLines = 1,
            modifier = Modifier.background(Tidy.BlueSoft, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) =
    Text(text, style = MaterialTheme.typography.headlineMedium, color = Tidy.Ink, textAlign = align,
        modifier = modifier.fillMaxWidth().semantics { heading() })

@Composable
fun Body(text: String, color: Color = Tidy.Muted, align: TextAlign = TextAlign.Start) =
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color, textAlign = align, modifier = Modifier.fillMaxWidth())

/** Speech bubble the mascot "says", as in concept C. */
@Composable
fun Bubble(text: String, color: Color = Tidy.BlueSoft, textColor: Color = Tidy.Ink, leading: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(color).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
        Text(text, style = MaterialTheme.typography.headlineMedium, color = textColor, modifier = Modifier.semantics { heading() })
    }
}

/** The mascot's moods, each a Codex illustration in res/drawable-nodpi. */
enum class Mood(val res: Int, val description: String) {
    STAND(dev.tidytv.app.R.drawable.mascot_stand, "The TidyTV mascot, a smiling blue TV"),
    SEARCH(dev.tidytv.app.R.drawable.mascot_search, "The mascot looking through a magnifying glass"),
    HAPPY(dev.tidytv.app.R.drawable.mascot_happy, "The mascot smiling with its eyes closed"),
    CHEER(dev.tidytv.app.R.drawable.mascot_cheer, "The mascot cheering with both arms up"),
    WORRIED(dev.tidytv.app.R.drawable.mascot_worried, "The mascot looking a little worried"),
}

/** The mascot at the top of a screen, gently bobbing. */
@Composable
fun HeroMascot(worried: Boolean = false, mood: Mood = if (worried) Mood.WORRIED else Mood.STAND, height: androidx.compose.ui.unit.Dp = 150.dp) {
    // Previews and screenshot tests must go idle, so the bob only exists outside inspection mode.
    val still = androidx.compose.ui.platform.LocalInspectionMode.current
    val bob = if (still) 0f else androidx.compose.animation.core.rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f, targetValue = -8f, label = "bob",
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
    ).value
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(mood.res), mood.description,
            Modifier.height(height).graphicsLayer { translationY = bob.dp.toPx() },
        )
    }
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit, label: String) {
    Switch(
        checked = on, onCheckedChange = onChange,
        modifier = Modifier.semantics { contentDescription = label },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = Tidy.Green, checkedBorderColor = Tidy.Green,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = Tidy.Grey, uncheckedBorderColor = Tidy.Line,
        ),
    )
}

/** Numbered instruction, used on Get ready and the help states. */
@Composable
fun NumberedStep(n: Int, title: String, detail: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Tidy.Blue), contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Tidy.Ink)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = Tidy.Muted)
        }
    }
}

/** A little TV with a colourful screen, standing in for the TV's own picture. */
@Composable
fun TvPicture(width: androidx.compose.ui.unit.Dp = 92.dp) {
    Canvas(Modifier.size(width, width * 0.66f)) {
        val w = size.width; val h = size.height
        drawRoundRect(Tidy.Ink, Offset(0f, 0f), Size(w, h * 0.86f), CornerRadius(w * 0.04f))
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF2D5BFF), Color(0xFFB040E0), Color(0xFFFF5A3C), Color(0xFFFFB020)), Offset(0f, 0f), Offset(w, h)),
            Offset(w * 0.035f, h * 0.05f), Size(w * 0.93f, h * 0.74f), CornerRadius(w * 0.02f),
        )
        drawLine(Tidy.Ink, Offset(w * 0.2f, h * 0.86f), Offset(w * 0.14f, h), strokeWidth = w * 0.03f)
        drawLine(Tidy.Ink, Offset(w * 0.8f, h * 0.86f), Offset(w * 0.86f, h), strokeWidth = w * 0.03f)
    }
}

/** Card for one TV in the scan list and on My TVs. */
@Composable
fun TvCard(title: String, subtitle: String, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Tidy.Card)
            .border(2.dp, Tidy.Line, RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 72.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvPicture(84.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Tidy.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Tidy.Muted)
        }
        if (trailing != null) trailing() else if (onClick != null) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.ChevronRight, null, Modifier.size(28.dp), tint = Tidy.Muted)
        }
    }
}

/** Round coloured badge holding a glyph, for group headers. */
@Composable
fun Badge(color: Color, glyph: String) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(glyph, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
fun Outline(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    CardBox(modifier, border = Tidy.Line, content = content)
