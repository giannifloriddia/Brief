package brief.views

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ThemeColors {
    val Background = Color(0xFF09090E)
    val Surface = Color(0xFF13131A)
    val SurfaceVariant = Color(0xFF1A1A24)
    val Primary = Color(0xFF6366F1)
    val PrimaryVariant = Color(0xFF4F46E5)
    val Accent = Color(0xFF8B5CF6)
    val TextMain = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val Border = Color(0xFF2E2E3A)

    val GradientPrimary = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
    val GradientSurface = Brush.linearGradient(listOf(Color(0xFF13131A), Color(0xFF1A1A24)))
}
