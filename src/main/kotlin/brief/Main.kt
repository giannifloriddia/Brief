package brief

import androidx.compose.ui.window.Window
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
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Brief"
    ) {
        window.minimumSize = Dimension(1000, 800)
        AppUI(controller, coroutineScope)
    }
}
