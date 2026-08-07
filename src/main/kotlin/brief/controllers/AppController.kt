package brief.controllers

import brief.models.Engine
import brief.models.Models
import brief.models.NotesResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import kotlinx.coroutines.Job

class AppController(private val coroutineScope: CoroutineScope) {

    private var activeJob: Job? = null

    fun cancelProcessing(onStatusUpdate: (String) -> Unit) {
        activeJob?.cancel()
        Engine.cancel()
        onStatusUpdate("❌ Cancelled by User.")
    }

    fun processLecture(
        audioPath: String,
        language: String,
        outputLang: String,
        subject: String,
        whisperBackend: String,
        whisperModel: String,
        llmBackend: String,
        llmModel: String,
        promptType: String,
        exportDir: String,
        onTranscriptUpdate: (String) -> Unit,
        onNotesUpdate: (String) -> Unit,
        onStatusUpdate: (String) -> Unit,
        onFinished: () -> Unit
    ) {
        if (audioPath.isBlank()) {
            onTranscriptUpdate("⚠️ Please upload an audio file first.")
            onFinished()
            return
        }

        activeJob = coroutineScope.launch {
            try {
                onStatusUpdate("🎙️ Initializing Whisper Model...")
                onTranscriptUpdate("🎙️ Processing audio (transcribing)...")
                onNotesUpdate("⏳ Waiting for transcript to finish before generating notes...")

                var fullTranscript = ""
                
                Engine.transcribeAudio(audioPath, language, whisperBackend, whisperModel).collect { partial ->
                    fullTranscript = partial
                    onTranscriptUpdate(fullTranscript)
                }
                
                onStatusUpdate("🧠 Initializing LLM...")
                onNotesUpdate("🧠 Transcript ready! Generating $outputLang notes and AI filename using $llmModel...")

                var aiFilename = "lecture-notes"
                var finalNotes = ""

                Engine.generateNotes(fullTranscript, promptType, outputLang, llmBackend, llmModel).collect { update ->
                    when (update) {
                        is String -> {
                            onNotesUpdate(update)
                        }
                        is NotesResult -> {
                            aiFilename = update.filename
                            finalNotes = update.finalNotes
                        }
                    }
                }

                onStatusUpdate("💾 Saving Files...")
                
                val userHome = System.getProperty("user.home")
                var exportPath = File(exportDir.replace("~", userHome))
                if (subject.isNotBlank() && subject != "(Root)") {
                    exportPath = File(exportPath, subject)
                }
                exportPath.mkdirs()

                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val baseName = "${aiFilename}_${timestamp}"
                
                val transcriptFilename = "transcript_$baseName.md"
                val notesFilename = "notes_$baseName.md"

                File(exportPath, transcriptFilename).writeText("# Transcript: $aiFilename\n\n$fullTranscript")
                
                val obsidianLink = "\n\n---\n## 📎 Source Material\n- **Transcript**: [[${transcriptFilename.replace(".md", "")}]]"
                val combinedNotes = finalNotes + obsidianLink

                File(exportPath, notesFilename).writeText(combinedNotes)

                onStatusUpdate("✅ Success!")
                onNotesUpdate(combinedNotes)

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    onStatusUpdate("❌ Cancelled by User.")
                } else {
                    onTranscriptUpdate("Error: ${e.message}")
                    onNotesUpdate("❌ Cancelled.")
                    onStatusUpdate("❌ Failed.")
                }
            } finally {
                onFinished()
            }
        }
    }

    fun getSubjects(exportDir: String): List<String> = Models.getSubjects(exportDir)
    
    fun getDownloadedModels(): Pair<List<String>, String> = Models.getDownloadedModels()
    
    fun deleteModel(repoId: String?): Boolean = Models.deleteModel(repoId)
}
