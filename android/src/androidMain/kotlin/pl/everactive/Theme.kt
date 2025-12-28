package pl.everactive

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Definicja kolorów
val SoftGreen = Color(0xFF81C784)           // Zielony (Primary) - aktywna obwódka
val DeepGreenContainer = Color(0xFF1B5E20)  // Ciemna zieleń (tła elementów)
val DarkCharcoal = Color(0xFF121212)        // Ciemny grafit (Tło główne)
val SurfaceGrey = Color(0xFF1E1E1E)         // Szary (Karty/Pola)
val OffWhite = Color(0xFFE0E0E0)            // Złamana biel (tekst)
val DarkOutline = Color(0xFF444444)         // <--- NOWY KOLOR: Ciemnoszary dla nieaktywnej ramki

// Schemat kolorów
private val MutedGreenScheme = darkColorScheme(
    primary = SoftGreen,                    // To będzie kolor ramki podczas wpisywania (focus)
    onPrimary = Color(0xFF003300),
    secondary = SoftGreen,
    onSecondary = Color.Black,

    background = DarkCharcoal,
    onBackground = OffWhite,

    surface = SurfaceGrey,
    onSurface = OffWhite,

    primaryContainer = DeepGreenContainer,
    onPrimaryContainer = Color(0xFFA5D6A7),

    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5),

    outline = DarkOutline                   // <--- ZMIANA: Ciemna ramka, gdy pole jest nieaktywne
)

@Composable
fun EverActiveTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MutedGreenScheme,
        content = content
    )
}
