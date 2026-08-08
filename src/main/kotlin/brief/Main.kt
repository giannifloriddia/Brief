package brief

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import brief.controllers.AppController
import brief.views.AppUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.Dimension

fun main() = application {
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val controller = AppController(coroutineScope)
    
    val logoBytes = Thread.currentThread().contextClassLoader.getResourceAsStream("logo.png")?.readAllBytes()
    val logoPainter = BitmapPainter(Image.makeFromEncoded(logoBytes!!).toComposeImageBitmap())
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Brief",
        icon = logoPainter,
        state = WindowState(width = 1280.dp, height = 900.dp)
    ) {
        window.minimumSize = Dimension(1000, 800)
        AppUI(controller, coroutineScope)
    }
}
