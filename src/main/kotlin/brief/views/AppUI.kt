package brief.views

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brief.controllers.AppController
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.skia.Image

@Composable
fun AppUI(controller: AppController, coroutineScope: CoroutineScope) {
    MaterialTheme(
        colors = darkColors(
            background = ThemeColors.Background,
            surface = ThemeColors.Surface,
            primary = ThemeColors.Primary,
            onPrimary = Color.White,
            onBackground = ThemeColors.TextMain,
            onSurface = ThemeColors.TextMain
        )
    ) {
        var currentTab by remember { mutableStateOf("Studio") }

        var isProcessing by remember { mutableStateOf(false) }
        var transcriptOut by remember { mutableStateOf("") }
        var summaryOut by remember { mutableStateOf("") }
        var statusText by remember { mutableStateOf("Ready to transcribe") }

        Row(modifier = Modifier.fillMaxSize().background(ThemeColors.Background)) {
            Sidebar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ThemeColors.GradientSurface)
                    .padding(horizontal = 40.dp, vertical = 32.dp)
            ) {
                Crossfade(targetState = currentTab) { tab ->
                    when (tab) {
                        "Studio" -> StudioView(
                            controller,
                            coroutineScope,
                            isProcessing, { isProcessing = it },
                            transcriptOut, { transcriptOut = it },
                            summaryOut, { summaryOut = it },
                            statusText, { statusText = it }
                        )
                        "Models" -> ModelsView(controller)
                        "Files" -> FilesView(coroutineScope)
                    }
                }
            }
        }
    }
}

@Composable
fun Sidebar(currentTab: String, onTabSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(ThemeColors.Surface)
            .border(1.dp, ThemeColors.Border)
            .padding(28.dp)
    ) {
        val logoPainter = remember {
            val logoBytes = Thread.currentThread().contextClassLoader.getResourceAsStream("logo.png")?.readAllBytes()
            BitmapPainter(Image.makeFromEncoded(logoBytes!!).toComposeImageBitmap())
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = ThemeColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = logoPainter,
                    contentDescription = "Brief Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Brief", color = ThemeColors.TextMain, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Text("Local-First AI", color = ThemeColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Text("MENU", color = ThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))

        NavButton("🏠", "Studio", currentTab == "Studio") { onTabSelected("Studio") }
        Spacer(modifier = Modifier.height(8.dp))
        NavButton("📁", "Files", currentTab == "Files") { onTabSelected("Files") }
        Spacer(modifier = Modifier.height(8.dp))
        NavButton("🧠", "Models", currentTab == "Models") { onTabSelected("Models") }
    }
}

