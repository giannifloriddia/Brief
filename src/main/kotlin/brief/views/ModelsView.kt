package brief.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brief.controllers.AppController

@Composable
fun ModelsView(controller: AppController) {
    var modelsText by remember { mutableStateOf("") }
    var modelsList by remember { mutableStateOf(listOf<String>()) }
    var selectedModel by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val (list, text) = controller.getDownloadedModels()
        modelsList = list
        modelsText = text
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text("Model Manager", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, letterSpacing = (-1).sp)
        Text("Manage locally downloaded Whisper and LLM weights (HuggingFace Cache).", color = ThemeColors.TextMuted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 32.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("Downloaded Models Cache", color = ThemeColors.TextMain, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                val (list, text) = controller.getDownloadedModels()
                                modelsList = list
                                modelsText = text
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = ThemeColors.SurfaceVariant, contentColor = ThemeColors.TextMain),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔄 Refresh")
                        }
                    }

                    Text("These models are stored on your disk and used by Apple Silicon (MLX) or Universal (GGUF) engines. Ollama models are managed separately via the Ollama App.", color = ThemeColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))

                    OutlinedTextField(
                        value = modelsText,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        readOnly = true,
                        colors = modernTextFieldColors(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("Free up space", fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, modifier = Modifier.padding(bottom = 16.dp))
                        ModernDropdown(
                            label = "Select Model to Delete",
                            options = modelsList,
                            selectedOption = selectedModel,
                            onOptionSelected = { selectedModel = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                controller.deleteModel(selectedModel)
                                val (list, text) = controller.getDownloadedModels()
                                modelsList = list
                                modelsText = text
                                selectedModel = ""
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF4444), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("🗑️ Delete Cache", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("How to Download Models", fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, modifier = Modifier.padding(bottom = 16.dp))
                        Text("Models are downloaded automatically when you start processing a lecture in the Studio tab. Just select the model you want from the dropdown, and if it's not here, Brief will download it for you seamlessly.", color = ThemeColors.TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
