package brief.views

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brief.controllers.AppController
import brief.models.Models
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

object ThemeColors {
    val Background = Color(0xFF09090E)
    val Surface = Color(0xFF13131A)
    val SurfaceVariant = Color(0xFF1A1A24)
    val Primary = Color(0xFF6366F1) // Indigo 500
    val PrimaryVariant = Color(0xFF4F46E5)
    val Accent = Color(0xFF8B5CF6) // Violet 500
    val TextMain = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val Border = Color(0xFF2E2E3A)
    
    val GradientPrimary = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
    val GradientSurface = Brush.linearGradient(listOf(Color(0xFF13131A), Color(0xFF1A1A24)))
}

@Composable
fun ModernDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, color = ThemeColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ThemeColors.Background)
                .border(1.dp, if (expanded) ThemeColors.Primary else ThemeColors.Border, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedOption.ifEmpty { "Select..." }, 
                    color = ThemeColors.TextMain, 
                    fontSize = 14.sp, 
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text("▼", color = ThemeColors.TextMuted, fontSize = 10.sp)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(ThemeColors.SurfaceVariant).border(1.dp, ThemeColors.Border, RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }) {
                        Text(option, color = ThemeColors.TextMain)
                    }
                }
            }
        }
    }
}

@Composable
fun FilePickerInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDirectory: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, color = ThemeColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ThemeColors.Background)
                .border(1.dp, ThemeColors.Border, RoundedCornerShape(10.dp))
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(color = ThemeColors.TextMain, fontSize = 14.sp),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                singleLine = true
            )
            Button(
                onClick = {
                    if (isDirectory) {
                        System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    }
                    val dialog = FileDialog(null as Frame?, if (isDirectory) "Select Directory" else "Select File", FileDialog.LOAD)
                    dialog.isVisible = true
                    if (isDirectory) {
                        System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    }
                    if (dialog.directory != null && dialog.file != null) {
                        onValueChange(File(dialog.directory, dialog.file).absolutePath)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ThemeColors.SurfaceVariant, contentColor = ThemeColors.TextMain),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(if (isDirectory) "📂" else "📄", fontSize = 14.sp)
            }
        }
    }
}

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

        Row(modifier = Modifier.fillMaxSize().background(ThemeColors.Background)) {
            // Sidebar
            Sidebar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ThemeColors.GradientSurface)
                    .padding(horizontal = 40.dp, vertical = 32.dp)
            ) {
                Crossfade(targetState = currentTab) { tab ->
                    when (tab) {
                        "Studio" -> StudioView(controller, coroutineScope)
                        "Models" -> ModelsView(controller)
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
        // Logo Area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ThemeColors.GradientPrimary, RoundedCornerShape(12.dp))
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = ThemeColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Brief", color = ThemeColors.TextMain, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Text("Local-First AI", color = ThemeColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Navigation
        Text("MENU", color = ThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
        
        NavButton("🏠", "Studio", currentTab == "Studio") { onTabSelected("Studio") }
        Spacer(modifier = Modifier.height(8.dp))
        NavButton("🧠", "Models", currentTab == "Models") { onTabSelected("Models") }
    }
}

@Composable
fun NavButton(icon: String, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val targetBgColor = when {
        isSelected -> ThemeColors.Primary.copy(alpha = 0.15f)
        isHovered -> ThemeColors.SurfaceVariant
        else -> Color.Transparent
    }
    
    val bgColor by animateColorAsState(targetValue = targetBgColor, tween(200))
    val textColor by animateColorAsState(targetValue = if (isSelected || isHovered) ThemeColors.TextMain else ThemeColors.TextMuted, tween(200))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StudioView(controller: AppController, coroutineScope: CoroutineScope) {
    var audioPath by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("pt") }
    var outputLang by remember { mutableStateOf("pt") }
    var exportDir by remember { mutableStateOf("~/Desktop/Brief_Notes") }
    
    var subjects by remember { mutableStateOf(controller.getSubjects(exportDir)) }
    var selectedSubject by remember { mutableStateOf("(Root)") }

    val whisperBackends = listOf("Mac Native (MLX)", "Windows/Linux (Cross)")
    val llmBackends = listOf("Mac Native (MLX)", "Windows/Linux (GGUF)", "Ollama (Local API)")
    val promptTypes = listOf("Short Summary", "Detailed Notes", "Exam Q&A")

    var whisperBackend by remember { mutableStateOf(whisperBackends[0]) }
    var llmBackend by remember { mutableStateOf(llmBackends[0]) }
    
    var ollamaModels by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) {
        ollamaModels = withContext(Dispatchers.IO) { Models.fetchOllamaModels() }
    }

    val whisperModels = if (whisperBackend == "Mac Native (MLX)") Models.WHISPER_MLX_MODELS else Models.WHISPER_CROSS_MODELS
    val llmModels = when (llmBackend) {
        "Mac Native (MLX)" -> Models.LLM_MLX_MODELS
        "Windows/Linux (GGUF)" -> Models.LLM_GGUF_MODELS
        else -> ollamaModels.ifEmpty { listOf("llama3.1", "gemma2") }
    }

    var whisperModel by remember { mutableStateOf(whisperModels.firstOrNull() ?: "") }
    var llmModel by remember { mutableStateOf(llmModels.firstOrNull() ?: "") }
    var promptType by remember { mutableStateOf(promptTypes[1]) }

    LaunchedEffect(whisperBackend) {
        if (!whisperModels.contains(whisperModel)) whisperModel = whisperModels.firstOrNull() ?: ""
    }
    LaunchedEffect(llmBackend, ollamaModels) {
        if (!llmModels.contains(llmModel)) llmModel = llmModels.firstOrNull() ?: ""
    }

    var transcriptOut by remember { mutableStateOf("") }
    var summaryOut by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Ready to transcribe") }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text("Studio", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, letterSpacing = (-1).sp)
        Text("Transform your lectures into actionable study material securely.", color = ThemeColors.TextMuted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 24.dp))
        
        // Status Bar
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

        // Settings Stacked Layout for better spacing
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("1. Input & Options", fontWeight = FontWeight.Bold, color = ThemeColors.TextMain, modifier = Modifier.padding(bottom = 16.dp))
                    FilePickerInput(
                        label = "Audio File Path (MP3, M4A, WAV)",
                        value = audioPath,
                        onValueChange = { audioPath = it },
                        isDirectory = false,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    FilePickerInput(
                        label = "Export Directory",
                        value = exportDir,
                        onValueChange = { exportDir = it },
                        isDirectory = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        val commonLangs = listOf("en", "pt", "es", "fr", "de", "it", "nl")
                        ModernDropdown(
                            label = "Audio Language",
                            options = commonLangs,
                            selectedOption = language,
                            onOptionSelected = { language = it },
                            modifier = Modifier.weight(1f)
                        )
                        ModernDropdown(
                            label = "Notes Language",
                            options = commonLangs,
                            selectedOption = outputLang,
                            onOptionSelected = { outputLang = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModernDropdown(
                        label = "Notes Style",
                        options = promptTypes,
                        selectedOption = promptType,
                        onOptionSelected = { promptType = it },
                        modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(1f)
                        )
                        ModernDropdown(
                            label = "Transcription Model",
                            options = whisperModels,
                            selectedOption = whisperModel,
                            onOptionSelected = { whisperModel = it },
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ModernDropdown(
                            label = "Summary Engine",
                            options = llmBackends,
                            selectedOption = llmBackend,
                            onOptionSelected = { llmBackend = it },
                            modifier = Modifier.weight(1f)
                        )
                        ModernDropdown(
                            label = "Summary Model",
                            options = llmModels,
                            selectedOption = llmModel,
                            onOptionSelected = { llmModel = it },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Process Button
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        
        Button(
            onClick = {
                controller.processLecture(
                    audioPath, language, outputLang, selectedSubject, whisperBackend, whisperModel, llmBackend, llmModel, promptType, exportDir,
                    onTranscriptUpdate = { transcriptOut = it },
                    onNotesUpdate = { summaryOut = it },
                    onStatusUpdate = { statusText = it }
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

        Spacer(modifier = Modifier.height(24.dp))

        // Outputs
        Row(modifier = Modifier.fillMaxWidth().height(400.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Transcript", color = ThemeColors.TextMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = transcriptOut,
                    onValueChange = { transcriptOut = it },
                    modifier = Modifier.fillMaxSize(),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Study Notes", color = ThemeColors.TextMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = summaryOut,
                    onValueChange = { summaryOut = it },
                    modifier = Modifier.fillMaxSize(),
                    colors = modernTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

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

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ThemeColors.Surface.copy(alpha = 0.6f))
            .border(1.dp, ThemeColors.Border, RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        content()
    }
}

@Composable
fun modernTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = ThemeColors.TextMain,
    backgroundColor = ThemeColors.Background,
    focusedBorderColor = ThemeColors.Primary,
    unfocusedBorderColor = ThemeColors.Border,
    cursorColor = ThemeColors.Primary,
    focusedLabelColor = ThemeColors.Primary,
    unfocusedLabelColor = ThemeColors.TextMuted
)
