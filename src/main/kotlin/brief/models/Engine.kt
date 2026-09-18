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

    private fun getPythonExecutable(): List<String> {
        val os = System.getProperty("os.name").lowercase()
        val executableName = if (os.contains("win")) "bridge.exe" else "bridge"

        // 1. Local development: use the venv + bridge.py directly
        //    The venv only exists in the project directory, never inside the installed app,
        //    so this check is safe and gives the most reliable dev experience.
        val localVenv = File(System.getProperty("user.dir"), "venv/bin/python")
        if (localVenv.exists()) {
            val bridgePath = File(System.getProperty("user.dir"), "python_engine/bridge.py")
            return listOf(localVenv.absolutePath, bridgePath.absolutePath)
        }

        // 2. Compose Desktop app resources (used in native distributions)
        //    compose.application.resources.dir points to the resources/ folder inside the app bundle
        val appResourcesDir = System.getProperty("compose.application.resources.dir")
        if (appResourcesDir != null) {
            val bundledExecutable = File(appResourcesDir, "python_engine/$executableName")
            if (bundledExecutable.exists()) {
                bundledExecutable.setExecutable(true)
                return listOf(bundledExecutable.absolutePath)
            }
        }

        // 3. Last resort: try the PyInstaller dist output directly (dev without venv)
        val distBridge = File(System.getProperty("user.dir"), "python_engine/dist/bridge/$executableName")
        if (distBridge.exists()) {
            distBridge.setExecutable(true)
            return listOf(distBridge.absolutePath)
        }

        throw IllegalStateException(
            "Could not find the Python engine binary. " +
            "For development, ensure the venv is set up. " +
            "For distribution, run: cd python_engine && pyinstaller bridge.spec && then rebuild the app."
        )
    }

    private var currentProcess: Process? = null

    fun cancel() {
        currentProcess?.destroy()
        currentProcess = null
    }

    fun transcribeAudio(audioPath: String, language: String, backend: String, modelId: String): Flow<String> = flow {
        // Use the unified executable extractor!
        val pythonExe = getPythonExecutable()

        val command = mutableListOf<String>().apply {
            addAll(pythonExe)
            add("transcribe")
            add(audioPath)
            add(language)
            add(backend)
            add(modelId)
        }
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)

        val process = withContext(Dispatchers.IO) { processBuilder.start() }
        currentProcess = process

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        val lastLines = ArrayDeque<String>(10)
        while (withContext(Dispatchers.IO) { reader.readLine().also { line = it } } != null) {
            val text = line!!
            lastLines.addLast(text)
            if (lastLines.size > 10) lastLines.removeFirst()
            if (text.startsWith("ERROR: ")) {
                throw Exception(text.substring(7))
            }
            emit(text)
        }

        val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
        currentProcess = null
        if (exitCode != 0) {
            val output = lastLines.joinToString("\n")
            throw Exception("Transcription process exited with code $exitCode.\nOutput:\n$output")
        }
    }.flowOn(Dispatchers.IO)

    fun generateNotes(
        transcript: String,
        promptType: String,
        outputLang: String,
        backend: String,
        modelId: String
    ): Flow<Any> = flow {
        if (backend == "Ollama (Local API)") {
            // [Ollama Logic remains completely untouched]
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

        val tempTranscript = withContext(Dispatchers.IO) { File.createTempFile("transcript", ".txt") }
        withContext(Dispatchers.IO) { tempTranscript.writeText(transcript) }

        val pythonExe = getPythonExecutable()

        val command = mutableListOf<String>().apply {
            addAll(pythonExe)
            add("generate_notes")
            add(tempTranscript.absolutePath)
            add(promptType)
            add(outputLang)
            add(backend)
            add(modelId)
        }
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)

        val process = withContext(Dispatchers.IO) { processBuilder.start() }
        currentProcess = process

        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        val lastLines = ArrayDeque<String>(10)
        while (withContext(Dispatchers.IO) { reader.readLine().also { line = it } } != null) {
            val text = line!!
            lastLines.addLast(text)
            if (lastLines.size > 10) lastLines.removeFirst()
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
            val output = lastLines.joinToString("\n")
            throw Exception("Generation process exited with code $exitCode.\nOutput:\n$output")
        }
    }.flowOn(Dispatchers.IO)
}