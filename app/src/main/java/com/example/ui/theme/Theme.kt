package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = TealPrimary,
  onPrimary = Color.White,
  primaryContainer = TealContainer,
  onPrimaryContainer = TealDark,
  secondary = TealLight,
  onSecondary = Color.White,
  background = BgLight,
  onBackground = TextDark,
  surface = CardWhite,
  onSurface = TextDark,
  surfaceVariant = Color(0xFFE8EFF1),
  onSurfaceVariant = TextMuted,
  error = StatusRed,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = TealLight,
  onPrimary = Color.White,
  primaryContainer = TealDark,
  onPrimaryContainer = Color.White,
  secondary = TealPrimary,
  onSecondary = Color.White,
  background = Color(0xFF10191C),
  onBackground = Color(0xFFF1F5F9),
  surface = Color(0xFF182428),
  onSurface = Color(0xFFF1F5F9),
  error = StatusRed,
  onError = Color.White
)

/**
 * Konfigurasi warna seragam untuk semua Input Teks (OutlinedTextField).
 * Memastikan tulisan saat mengetik SELALU terlihat jelas (warna TextDark di atas latar putih),
 * tidak akan pernah berubah menjadi teks putih di atas latar putih pada tema/perangkat apapun.
 */
@Composable
fun absensiTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedTextColor = TextDark,
  unfocusedTextColor = TextDark,
  focusedContainerColor = CardWhite,
  unfocusedContainerColor = CardWhite,
  disabledContainerColor = CardWhite,
  errorContainerColor = CardWhite,
  focusedBorderColor = TealPrimary,
  unfocusedBorderColor = BorderMuted,
  focusedLabelColor = TealPrimary,
  unfocusedLabelColor = TextMuted,
  cursorColor = TealPrimary,
  focusedLeadingIconColor = TealPrimary,
  unfocusedLeadingIconColor = TextMuted,
  focusedTrailingIconColor = TealPrimary,
  unfocusedTrailingIconColor = TextMuted,
  focusedPlaceholderColor = TextMuted.copy(alpha = 0.7f),
  unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.7f)
)

@Composable
fun AbsensiKantorTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  // Desain Absensi Kantor menggunakan palet bersih (Clean Light Design)
  // sebagai tampilan utama identitas brand kantor.
  val colorScheme = LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
