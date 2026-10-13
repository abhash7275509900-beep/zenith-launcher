package com.zenith.launcher.ui

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.Prefs

/** font: 0 sans, 1 serif, 2 mono. corner: base corner radius in dp. glow: thin accent edge on panels. */
data class NovaTheme(
    val id: String,
    val name: String,
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val primary: Color,
    val secondary: Color,
    val text: Color,
    val textSecondary: Color,
    val corner: Int = 24,
    val font: Int = 0,
    val glow: Boolean = false
)

private fun c(v: Long) = Color(v)

val NovaThemes = listOf(
    NovaTheme("obsidian", "Obsidian Ultra", true, c(0xFF05070B), c(0xFF101722), c(0xFF172235), c(0xFF38BDF8), c(0xFF8B5CF6), c(0xFFF4F7FB), c(0xFFA6B2C4), glow = true),
    NovaTheme("cyberpunk", "Cyberpunk X", true, c(0xFF0B0614), c(0xFF1A0F2E), c(0xFF251543), c(0xFF22D3EE), c(0xFFE879F9), c(0xFFF5F0FF), c(0xFFB7A8D6), corner = 10, glow = true),
    NovaTheme("titanium", "Titanium", true, c(0xFF0A0B0D), c(0xFF16181C), c(0xFF212429), c(0xFFC7CDD6), c(0xFF8A94A3), c(0xFFF1F3F5), c(0xFF9AA3AF), corner = 18),
    NovaTheme("arctic", "Arctic Glass", false, c(0xFFF2F7FB), c(0xFFFFFFFF), c(0xFFE3EEF7), c(0xFF0369A1), c(0xFF4F46E5), c(0xFF0B1220), c(0xFF51607A), corner = 28),
    NovaTheme("matrix", "Matrix", true, c(0xFF020A06), c(0xFF0B1A12), c(0xFF12281B), c(0xFF22C55E), c(0xFF86EFAC), c(0xFFE6FFF0), c(0xFF8FB9A0), corner = 6, font = 2, glow = true),
    NovaTheme("deepspace", "Deep Space", true, c(0xFF050B1E), c(0xFF0E1A3A), c(0xFF16264F), c(0xFF7DD3FC), c(0xFFA5B4FC), c(0xFFEAF2FF), c(0xFFA9B8D9), corner = 26),
    NovaTheme("oldmoney", "Old Money", true, c(0xFF1C1A17), c(0xFF2A2723), c(0xFF36322D), c(0xFFC9A961), c(0xFFE8DCC0), c(0xFFF3EBDD), c(0xFFB8AE9B), corner = 8, font = 1),
    NovaTheme("chrome", "Liquid Chrome", true, c(0xFF0C0D10), c(0xFF1A1C22), c(0xFF262932), c(0xFFA5B4FC), c(0xFFF0ABFC), c(0xFFF5F6FA), c(0xFFA9AFC0), corner = 30),
    NovaTheme("synthwave", "Synthwave", true, c(0xFF120726), c(0xFF220F45), c(0xFF2F175F), c(0xFFFF4FD8), c(0xFF22D3EE), c(0xFFFFF1FB), c(0xFFD2B5E8), corner = 16, glow = true),
    NovaTheme("crimson", "Crimson", true, c(0xFF0A0506), c(0xFF1A0C0F), c(0xFF2A1217), c(0xFFFF4D5E), c(0xFFFB923C), c(0xFFFFF1F2), c(0xFFD6A9AE), corner = 6),
    NovaTheme("aurora", "Aurora", false, c(0xFFF6F1FB), c(0xFFFFFFFF), c(0xFFEDE4F7), c(0xFF7C3AED), c(0xFF0D9488), c(0xFF1E1630), c(0xFF5B4F73), corner = 32),
    NovaTheme("hologram", "Hologram OS", true, c(0xFF02080C), c(0xFF071820), c(0xFF0C2631), c(0xFF22E5FF), c(0xFF67E8F9), c(0xFFE0FBFF), c(0xFF8FC4D0), corner = 4, glow = true)
)

fun themeById(id: String): NovaTheme = NovaThemes.firstOrNull { it.id == id } ?: NovaThemes[0]

val LocalNova = staticCompositionLocalOf { NovaThemes[0] }

val Accents = listOf(
    Color(0xFF38BDF8), Color(0xFF8B5CF6), Color(0xFF34D399),
    Color(0xFFFBBF24), Color(0xFFFB7185), Color(0xFFE879F9)
)

/** Thin accent edge for themes that use glow. Used sparingly: dock, search, cards. */
fun Modifier.novaEdge(t: NovaTheme, shape: Shape): Modifier =
    if (t.glow) this.border(1.dp, t.primary.copy(alpha = 0.55f), shape) else this

/** Translucent panels get more opaque in high-contrast mode. */
fun glassAlpha(prefs: Prefs, normal: Float): Float = if (prefs.highContrast) 0.90f else normal

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun LauncherTheme(prefs: Prefs, previewId: String?, content: @Composable () -> Unit) {
    val t = themeById(previewId ?: prefs.themeId)
    val ctx = LocalContext.current
    val accent = when {
        prefs.accentMode == 1 && Build.VERSION.SDK_INT >= 31 ->
            (if (t.dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)).primary
        prefs.accentMode == 2 -> Accents[prefs.accentIndex.coerceIn(Accents.indices)]
        else -> t.primary
    }
    val onAccent = if (t.dark) Color.Black else Color.White
    val base = if (t.dark) {
        darkColorScheme(primary = accent, onPrimary = onAccent, secondary = t.secondary,
            background = t.background, onBackground = t.text, surface = t.surface, onSurface = t.text,
            surfaceVariant = t.elevated, onSurfaceVariant = t.textSecondary)
    } else {
        lightColorScheme(primary = accent, onPrimary = onAccent, secondary = t.secondary,
            background = t.background, onBackground = t.text, surface = t.surface, onSurface = t.text,
            surfaceVariant = t.elevated, onSurfaceVariant = t.textSecondary)
    }
    val family = when (t.font) { 1 -> FontFamily.Serif; 2 -> FontFamily.Monospace; else -> FontFamily.Default }
    val r = t.corner
    CompositionLocalProvider(LocalNova provides t) {
        MaterialTheme(
            colorScheme = base.copy(surfaceContainer = t.elevated, surfaceContainerHigh = t.elevated, surfaceContainerHighest = t.elevated),
            typography = Typography().withFont(family),
            shapes = Shapes(
                extraSmall = RoundedCornerShape((r / 4).dp), small = RoundedCornerShape((r / 2).dp),
                medium = RoundedCornerShape(r.dp), large = RoundedCornerShape((r * 3 / 2).dp),
                extraLarge = RoundedCornerShape((r * 2).dp)
            ),
            content = content
        )
    }
}
