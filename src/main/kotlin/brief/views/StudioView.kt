package brief.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brief.controllers.AppController
import brief.models.Models
import brief.models.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StudioView(
    controller: AppController,
    coroutineScope: CoroutineScope,
    isProcessing: Boolean, setIsProcessing: (Boolean) -> Unit,
    transcriptOut: String, setTranscriptOut: (String) -> Unit,
    summaryOut: String, setSummaryOut: (String) -> Unit,
    statusText: String, setStatusText: (String) -> Unit
) {
    var audioPath by remember { mutableStateOf(Preferences.get("audioPath", "")) }
    var language by remember { mutableStateOf(Preferences.get("language", "pt")) }
    var outputLang by remember { mutableStateOf(Preferences.get("outputLang", "pt")) }
    var exportDir by remember { mutableStateOf(Preferences.get("exportDir", "~/Desktop/Brief_Notes")) }
    var selectedSubject by remember { mutableStateOf("(Root)") }

    val whisperBackends = listOf("Mac Native (MLX)", "Windows/Linux (Cross)")
    val llmBackends = listOf("Mac Native (MLX)", "Windows/Linux (GGUF)", "Ollama (Local API)")
    val promptTypes = listOf("Short Summary", "Detailed Notes", "Exam Q&A")

    var whisperBackend by remember { mutableStateOf(Preferences.get("whisperBackend", whisperBackends[0])) }
    var llmBackend by remember { mutableStateOf(Preferences.get("llmBackend", llmBackends[0])) }
    var ollamaModels by remember { mutableStateOf(listOf<String>()) }
    var downloadedModels by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        ollamaModels = withContext(Dispatchers.IO) { Models.fetchOllamaModels() }
        downloadedModels = withContext(Dispatchers.IO) { controller.getDownloadedModels().first }
    }

    val whisperModels = if (whisperBackend == "Mac Native (MLX)") Models.WHISPER_MLX_MODELS else Models.WHISPER_CROSS_MODELS
    val llmModels = when (llmBackend) {
        "Mac Native (MLX)" -> Models.LLM_MLX_MODELS
        "Windows/Linux (GGUF)" -> Models.LLM_GGUF_MODELS
        else -> ollamaModels.ifEmpty { listOf("llama3.1", "gemma2") }
    }

    var whisperModel by remember { mutableStateOf(Preferences.get("whisperModel", whisperModels.firstOrNull() ?: "")) }
    var llmModel by remember { mutableStateOf(Preferences.get("llmModel", llmModels.firstOrNull() ?: "")) }
    var promptType by remember { mutableStateOf(Preferences.get("promptType", promptTypes[1])) }

    LaunchedEffect(audioPath) { Preferences.set("audioPath", audioPath) }
    LaunchedEffect(language) { Preferences.set("language", language) }
    LaunchedEffect(outputLang) { Preferences.set("outputLang", outputLang) }
    LaunchedEffect(exportDir) { Preferences.set("exportDir", exportDir) }
    LaunchedEffect(promptType) { Preferences.set("promptType", promptType) }
    LaunchedEffect(whisperBackend) { Preferences.set("whisperBackend", whisperBackend) }
    LaunchedEffect(llmBackend) { Preferences.set("llmBackend", llmBackend) }
    LaunchedEffect(whisperModel) { if (whisperModel.isNotEmpty()) Preferences.set("whisperModel", whisperModel) }
    LaunchedEffect(llmModel) { if (llmModel.isNotEmpty()) Preferences.set("llmModel", llmModel) }

    LaunchedEffect(whisperBackend) {
        if (!whisperModels.contains(whisperModel)) whisperModel = whisperModels.firstOrNull() ?: ""
    }
    LaunchedEffect(llmBackend, ollamaModels) {
        if (!llmModels.contains(llmModel)) llmModel = llmModels.firstOrNull() ?: ""
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text("Studio", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, letterSpacing = (-1).sp)
        Text("Transform your lectures into actionable study material securely.", color = ThemeColors.TextMuted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ThemeColors.SurfaceVariant)
                .border(1.dp, ThemeColors.Border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (statusText.contains("Ready") || statusText.contains("Success")) Color(0xFF10B981) else if (statusText.contains("Error") || statusText.contains("Fail")) ThemeColors.Accent else Color(0xFFF59E0B)))
            Spacer(modifier = Modifier.width(12.dp))
            Text(statusText, color = ThemeColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("1. Input & Options", fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, modifier = Modifier.padding(bottom = 16.dp))
                    FilePickerInput(
                        label = "Audio File Path (MP3, M4A, WAV)",
                        value = audioPath,
                        onValueChange = { audioPath = it },
                        isDirectory = false,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        enabled = !isProcessing
                    )
                    FilePickerInput(
                        label = "Export Directory",
                        value = exportDir,
                        onValueChange = { exportDir = it },
                        isDirectory = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        enabled = !isProcessing
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        val commonLangs = listOf("en", "pt", "es", "fr", "de", "it", "nl")
                        ModernDropdown(
                            label = "Audio Language",
                            options = commonLangs,
                            selectedOption = language,
                            onOptionSelected = { language = it },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        )
                        ModernDropdown(
                            label = "Notes Language",
                            options = commonLangs,
                            selectedOption = outputLang,
                            onOptionSelected = { outputLang = it },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        )
                    }
                    ModernDropdown(
                        label = "Notes Style",
                        options = promptTypes,
                        selectedOption = promptType,
                        onOptionSelected = { promptType = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("2. AI Engines (Local)", fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, modifier = Modifier.padding(bottom = 16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        ModernDropdown(
                            label = "Transcription Engine",
                            options = whisperBackends,
                            selectedOption = whisperBackend,
                            onOptionSelected = { whisperBackend = it },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        )
                        ModernDropdown(
                            label = "Transcription Model",
                            options = whisperModels,
                            selectedOption = whisperModel,
                            onOptionSelected = { whisperModel = it },
                            modifier = Modifier.weight(1.5f),
                            enabled = !isProcessing,
                            optionLabel = { if (downloadedModels.contains(it)) "💾 $it" else "☁️ $it" }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ModernDropdown(
                            label = "Summary Engine",
                            options = llmBackends,
                            selectedOption = llmBackend,
                            onOptionSelected = { llmBackend = it },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        )
                        ModernDropdown(
                            label = "Summary Model",
                            options = llmModels,
                            selectedOption = llmModel,
                            onOptionSelected = { llmModel = it },
                            modifier = Modifier.weight(1.5f),
                            enabled = !isProcessing,
                            optionLabel = { if (llmBackend.contains("Ollama")) "🦙 $it" else if (downloadedModels.contains(it)) "💾 $it" else "☁️ $it" }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isProcessing) {
            Button(
                onClick = { controller.cancelProcessing(setStatusText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🛑 Cancel Processing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Button(
                onClick = {
                    setIsProcessing(true)
                    controller.processLecture(
                        audioPath, language, outputLang, selectedSubject, whisperBackend, whisperModel, llmBackend, llmModel, promptType, exportDir,
                        onTranscriptUpdate = { setTranscriptOut(it) },
                        onNotesUpdate = { setSummaryOut(it) },
                        onStatusUpdate = { setStatusText(it) },
                        onFinished = { setIsProcessing(false) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = ThemeColors.Primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ThemeColors.GradientPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚀 Start Processing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().height(400.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Transcript", color = ThemeColors.TextMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = transcriptOut,
                    onValueChange = { setTranscriptOut(it) },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Study Notes", color = ThemeColors.TextMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = summaryOut,
                    onValueChange = { setSummaryOut(it) },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
