package id.lesin.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LesinLightColors = lightColorScheme(
    primary = LesinColors.Ink,
    onPrimary = LesinColors.OnInk,
    background = LesinColors.Canvas,
    onBackground = LesinColors.Ink,
    surface = LesinColors.Canvas,
    onSurface = LesinColors.Ink,
    surfaceVariant = LesinColors.Surface,
    onSurfaceVariant = LesinColors.Charcoal,
    outline = LesinColors.Hairline,
    error = LesinColors.Error,
)

@Composable
fun LesinTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LesinLightColors,
        typography = Typography(),
        content = content,
    )
}
