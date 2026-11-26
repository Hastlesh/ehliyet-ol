package com.celikgames.ehliyetol

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Web sürümündeki stil.css renkleri (açık / koyu) */
@Immutable
data class Renkler(
    val dersler: List<Color>,
    val dogru: Color, val dogruZemin: Color,
    val yanlis: Color, val yanlisZemin: Color,
    val secili: Color, val uyari: Color, val koyu: Boolean,
)

private val ACIK_R = Renkler(
    dersler = listOf(Color(0xFFD6334A), Color(0xFF1D5BD6), Color(0xFF6D4BD1), Color(0xFF0F7A6E)),
    dogru = Color(0xFF0F7A4A), dogruZemin = Color(0xFFDFF3E8),
    yanlis = Color(0xFFC8283E), yanlisZemin = Color(0xFFFBE3E4),
    secili = Color(0xFFE6EEFC), uyari = Color(0xFFE0A100), koyu = false,
)
private val KOYU_R = Renkler(
    dersler = listOf(Color(0xFFFF7086), Color(0xFF6F9CFF), Color(0xFFA88DFF), Color(0xFF4FD1C0)),
    dogru = Color(0xFF5BD69A), dogruZemin = Color(0xFF123326),
    yanlis = Color(0xFFFF7A86), yanlisZemin = Color(0xFF3A1619),
    secili = Color(0xFF1C2740), uyari = Color(0xFFFFC93D), koyu = true,
)

val LocalRenk = staticCompositionLocalOf { ACIK_R }

private val ACIK = lightColorScheme(
    primary = Color(0xFF1D5BD6), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEFC), onPrimaryContainer = Color(0xFF0D2F73),
    secondary = Color(0xFF0F7A4A), onSecondary = Color.White,
    background = Color(0xFFF3F5F9), onBackground = Color(0xFF151B26),
    surface = Color.White, onSurface = Color(0xFF151B26),
    surfaceVariant = Color(0xFFE9EDF4), onSurfaceVariant = Color(0xFF5B6577),
    outline = Color(0xFFDFE4EC), error = Color(0xFFC8283E),
)
private val KOYU = darkColorScheme(
    primary = Color(0xFF6F9CFF), onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF1C2740), onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF5BD69A), onSecondary = Color(0xFF07210F),
    background = Color(0xFF0E121A), onBackground = Color(0xFFE8ECF4),
    surface = Color(0xFF171C27), onSurface = Color(0xFFE8ECF4),
    surfaceVariant = Color(0xFF222938), onSurfaceVariant = Color(0xFF9AA4B8),
    outline = Color(0xFF2A3142), error = Color(0xFFFF7A86),
)

/** tema: "" (telefona uy) | "acik" | "koyu" */
@Composable
fun temaKoyu(tema: String): Boolean = when (tema) { "acik" -> false; "koyu" -> true; else -> isSystemInDarkTheme() }

@Composable
fun EhliyetTema(tema: String, icerik: @Composable () -> Unit) {
    val koyu = temaKoyu(tema)
    CompositionLocalProvider(LocalRenk provides if (koyu) KOYU_R else ACIK_R) {
        MaterialTheme(colorScheme = if (koyu) KOYU else ACIK, content = icerik)
    }
}
