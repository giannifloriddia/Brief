package brief.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brief.models.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FilesView(coroutineScope: CoroutineScope) {
    val exportDirPath = Preferences.get("exportDir", "~/Desktop/Brief_Notes").replace("~", System.getProperty("user.home"))
    val rootDir = File(exportDirPath)

    var files by remember { mutableStateOf(listOf<File>()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileContent by remember { mutableStateOf("") }

    fun loadFiles() {
        if (rootDir.exists()) {
            files = rootDir.walkTopDown().filter { it.isFile && it.extension == "md" }.toList().sortedByDescending { it.lastModified() }
        }
    }

    LaunchedEffect(Unit) { loadFiles() }

    Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        GlassCard(modifier = Modifier.width(340.dp).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("My Notes", fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, fontSize = 20.sp)
                    Button(
                        onClick = {
                            try {
                                val os = System.getProperty("os.name").lowercase()
                                val path = rootDir.absolutePath
                                if (os.contains("mac")) {
                                    Runtime.getRuntime().exec(arrayOf("open", "obsidian://open?path=$path"))
                                } else if (os.contains("win")) {
                                    Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "obsidian://open?path=$path"))
                                } else {
                                    Runtime.getRuntime().exec(arrayOf("xdg-open", "obsidian://open?path=$path"))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = ThemeColors.Primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Open in Obsidian", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No markdown files found in export directory.", color = ThemeColors.TextMuted)
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        files.forEach { file ->
                            val isSelected = selectedFile == file
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ThemeColors.Primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        selectedFile = file
                                        fileContent = file.readText()
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(if (file.name.startsWith("notes_")) "📝" else "🎙️", modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(file.name, color = ThemeColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(file.parentFile.name, color = ThemeColors.TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.weight(2f).fillMaxHeight()) {
            if (selectedFile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a file to edit", color = ThemeColors.TextMuted)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedFile!!.name, fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, fontSize = 18.sp)
                    }
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = { newText ->
                            fileContent = newText
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    selectedFile?.writeText(newText)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
