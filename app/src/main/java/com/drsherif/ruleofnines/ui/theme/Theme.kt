package com.drsherif.ruleofnines.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClinicColors = lightColorScheme(
    primary = Color(0xFF006B70), onPrimary = Color.White,
    primaryContainer = Color(0xFFC5EEEB), onPrimaryContainer = Color(0xFF00383B),
    secondary = Color(0xFF48666B),
    background = Color(0xFFF3F8F7), onBackground = Color(0xFF162D35),
    surface = Color.White, onSurface = Color(0xFF162D35),
    surfaceVariant = Color(0xFFE4EEEC), onSurfaceVariant = Color(0xFF405A60),
    outline = Color(0xFF637C80), error = Color(0xFFB3261E),
)

@Composable
fun RuleOfNinesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClinicColors, content = content)
}
