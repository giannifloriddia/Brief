package brief.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.HttpURLConnection
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.URI

data class NotesResult(val filename: String, val finalNotes: String)

object Engine {

    private fun getBaseDir(): File {
        val composeResourcesDir = System.getProperty("compose.application.resources.dir")
        if (composeResourcesDir != null) {
            val resDir = File(composeResourcesDir)
            if (File(resDir, "python_engine/bridge.py").exists()) {
                return resDir
            }
        }
        return File(System.getProperty("user.dir"))
    }

    private fun getPythonExecutable(): String {
        val baseDir = getBaseDir()
        val rootVenvPython = File(baseDir, "venv/bin/python")
        val rootVenvPython3 = File(baseDir, "venv/bin/python3")
        val legacyVenvPython = File(baseDir, "python_engine/venv/bin/python")
        return when {
            rootVenvPython.exists() -> rootVenvPython.absolutePath
            rootVenvPython3.exists() -> rootVenvPython3.absolutePath
            legacyVenvPython.exists() -> legacyVenvPython.absolutePath
            else -> "python3" // Fallback
        }
    }

    private var currentProcess: Process? = null

    fun cancel() {
        currentProcess?.destroy()
        currentProcess = null
    }

    fun transcribeAudio(audioPath: String, language: String, backend: String, modelId: String): Flow<String> = flow {
        val baseDir = getBaseDir()
        val pythonExe = getPythonExecutable()
        val scriptPath = File(baseDir, "python_engine/bridge.py").absolutePath

        val processBuilder = ProcessBuilder(pythonExe, scriptPath, "transcribe", audioPath, language, backend, modelId)
        processBuilder.redirectErrorStream(true) // Merge stderr into stdout so we don't block

        val process = withContext(Dispatchers.IO) { processBuilder.start() }
        currentProcess = process

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (withContext(Dispatchers.IO) { reader.readLine().also { line = it } } != null) {
            val text = line!!
            if (text.startsWith("ERROR: ")) {
                throw Exception(text.substring(7))
            }
            emit(text)
        }

        val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
        currentProcess = null
        if (exitCode != 0) {
            throw Exception("Transcription process exited with code $exitCode")
        }
    }

    fun generateNotes(
        transcript: String,
        promptType: String,
        outputLang: String,
        backend: String,
        modelId: String
    ): Flow<Any> =
        flow {
            if (backend == "Ollama (Local API)") {
                // Ollama is better natively handled via HTTP to avoid python cold starts
                val langInstruction = " Write the notes strictly in $outputLang."
                val basePrompt = when (promptType) {
                    "Short Summary" -> "Provide a brief summary of this lecture."
                    "Detailed Notes" -> "Create detailed study notes with bullet points."
                    else -> "Generate 5 exam revision questions and answers."
                }

                val prompt = """$basePrompt$langInstruction

IMPORTANT RULES:
1. You must start your response with a suggested filename on the very first line, formatted exactly as 'FILENAME: short-hyphenated-name'.
2. Put 'NOTES:' on the next line.
3. DO NOT include any conversational filler, introduction, or preamble. Start the actual study material immediately after 'NOTES:'.

Transcript:
$transcript"""
                try {
                    val url = URI("http://localhost:11434/api/generate").toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    val jsonPayload = JsonObject().apply {
                        addProperty("model", modelId)
                        addProperty("prompt", prompt)
                        addProperty("stream", true)
                    }

                    withContext(Dispatchers.IO) {
                        connection.outputStream.write(jsonPayload.toString().toByteArray(Charsets.UTF_8))
                    }

                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    var line: String?
                    var rawOutput = ""
                    while (withContext(Dispatchers.IO) { reader.readLine().also { line = it } } != null) {
                        if (line!!.isNotEmpty()) {
                            val responseJson = JsonParser.parseString(line).asJsonObject
                            if (responseJson.has("response")) {
                                rawOutput += responseJson.get("response").asString
                                emit(rawOutput)
                            }
                        }
                    }

                    val lines = rawOutput.trim().split("\n")
                    var filename = "lecture-notes"
                    var notesContent = rawOutput

                    if (lines.isNotEmpty() && lines[0].startsWith("FILENAME:")) {
                        val rawFname = lines[0].replace("FILENAME:", "").trim()
                        filename = rawFname.filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == ' ' }
                            .replace(" ", "-").lowercase()

                        var startIdx = 1
                        for (i in 0 until minOf(5, lines.size)) {
                            if (lines[i].startsWith("NOTES:")) {
                                startIdx = i + 1
                                break
                            }
                        }
                        notesContent = lines.drop(startIdx).joinToString("\n").trim()
                    }

                    if (filename.isEmpty()) filename = "lecture-notes"

                    emit(NotesResult(filename, notesContent))
                    return@flow
                } catch (e: Exception) {
                    throw Exception("Ollama API Error: ${e.message}")
                }
            }

            // --- Mac Native (MLX) & Windows/Linux (GGUF) ---
            // Write transcript to a temp file because it's too large for sys.argv
            val tempTranscript = withContext(Dispatchers.IO) { File.createTempFile("transcript", ".txt") }
            withContext(Dispatchers.IO) { tempTranscript.writeText(transcript) }

            val baseDir = getBaseDir()
            val pythonExe = getPythonExecutable()
            val scriptPath = File(baseDir, "python_engine/bridge.py").absolutePath

            val processBuilder = ProcessBuilder(
                pythonExe,
                scriptPath,
                "generate_notes",
                tempTranscript.absolutePath,
                promptType,
                outputLang,
                backend,
                modelId
            )
            processBuilder.redirectErrorStream(true)

            val process = withContext(Dispatchers.IO) { processBuilder.start() }
            currentProcess = process

            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (withContext(Dispatchers.IO) { reader.readLine().also { line = it } } != null) {
                val text = line!!
                if (text.startsWith("ERROR: ")) {
                    withContext(Dispatchers.IO) { tempTranscript.delete() }
                    throw Exception(text.substring(7))
                } else if (text.startsWith("JSON_RESULT:")) {
                    val jsonString = text.substring(12)
                    val jsonObj = JsonParser.parseString(jsonString).asJsonObject
                    emit(NotesResult(jsonObj.get("filename").asString, jsonObj.get("final_notes").asString))
                } else if (text.startsWith("CHUNK:")) {
                    val rawText = text.substring(6).replace("\\n", "\n")
                    emit(rawText)
                } else {
                    emit(text)
                }
            }

            val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
            currentProcess = null
            withContext(Dispatchers.IO) { tempTranscript.delete() }
            if (exitCode != 0) {
                throw Exception("Generation process exited with code $exitCode")
            }
        }
            .flowOn(Dispatchers.IO)
}
