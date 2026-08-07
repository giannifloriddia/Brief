package brief.models

import java.io.File
import java.net.HttpURLConnection
import com.google.gson.JsonParser

object Models {
    val WHISPER_MLX_MODELS = listOf(
        "mlx-community/whisper-large-v3-turbo",
        "mlx-community/whisper-large-v3-mlx"
    )

    val WHISPER_CROSS_MODELS = listOf(
        "Systran/faster-whisper-large-v3",
        "Systran/faster-whisper-small"
    )

    val LLM_MLX_MODELS = listOf(
        "mlx-community/gemma-4-12b-it-4bit",
        "mlx-community/gemma-4-31b-it-4bit"
    )

    val LLM_GGUF_MODELS = listOf(
        "bartowski/gemma-2-9b-it-GGUF"
    )

    fun fetchOllamaModels(): List<String> {
        try {
            val url = java.net.URI("http://localhost:11434/api/tags").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = JsonParser.parseString(response).asJsonObject
                val modelsArray = jsonObject.getAsJsonArray("models")
                val models = mutableListOf<String>()
                for (i in 0 until modelsArray.size()) {
                    models.add(modelsArray[i].asJsonObject.get("name").asString)
                }
                if (models.isNotEmpty()) return models
            }
        } catch (e: Exception) {
            // Fallback
        }
        return listOf("llama3.1", "gemma2", "phi3", "mistral")
    }

    fun getSubjects(exportDir: String): List<String> {
        val path = File(exportDir.replace("~", System.getProperty("user.home")))
        if (!path.exists()) return listOf("(Root)")
        
        val folders = mutableListOf("(Root)")
        path.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach {
            folders.add(it.name)
        }
        return folders
    }

    fun getDownloadedModels(): Pair<List<String>, String> {
        val cacheDir = File(System.getProperty("user.home"), ".cache/huggingface/hub")
        if (!cacheDir.exists()) return Pair(emptyList(), "No models downloaded yet.")

        val models = mutableListOf<String>()
        val textOut = mutableListOf<String>()

        cacheDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("models--") }?.forEach { p ->
            val repoId = p.name.replace("models--", "").replace("--", "/")
            val sizeMb = p.walkTopDown().filter { it.isFile }.map { it.length() }.sum() / (1024.0 * 1024.0)
            models.add(repoId)
            textOut.add(String.format("%s (%.1f MB)", repoId, sizeMb))
        }

        return if (models.isNotEmpty()) Pair(models, textOut.joinToString("\n"))
        else Pair(emptyList(), "No models downloaded yet.")
    }

    fun deleteModel(repoId: String?): Boolean {
        if (repoId == null) return false
        val cacheDir = File(System.getProperty("user.home"), ".cache/huggingface/hub")
        val path = File(cacheDir, "models--" + repoId.replace("/", "--"))
        if (path.exists()) {
            return path.deleteRecursively()
        }
        return false
    }
}
