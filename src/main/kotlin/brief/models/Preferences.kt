package brief.models

import java.io.File
import java.util.Properties

object Preferences {
    private val prefsFile = File(System.getProperty("user.home"), ".brief/preferences.properties")
    private val props = Properties()

    init {
        if (prefsFile.exists()) {
            prefsFile.inputStream().use { props.load(it) }
        } else {
            prefsFile.parentFile.mkdirs()
            prefsFile.createNewFile()
        }
    }

    fun get(key: String, default: String): String = props.getProperty(key, default)

    fun set(key: String, value: String) {
        props.setProperty(key, value)
        try {
            prefsFile.outputStream().use { props.store(it, "Brief App Preferences") }
        } catch (e: Exception) {
            // Ignore write errors
        }
    }
}
