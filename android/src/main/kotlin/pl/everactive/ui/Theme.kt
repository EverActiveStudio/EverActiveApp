package pl.everactive

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SoftGreen = Color(0xFF81C784)
val DeepGreenContainer = Color(0xFF1B5E20)
val DarkCharcoal = Color(0xFF121212)
val SurfaceGrey = Color(0xFF1E1E1E)
val OffWhite = Color(0xFFE0E0E0)
val DarkOutline = Color(0xFF444444)

private val MutedGreenScheme = darkColorScheme(
    primary = SoftGreen,
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

    outline = DarkOutline
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
