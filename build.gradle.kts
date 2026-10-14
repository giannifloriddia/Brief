import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.compose") version "2.3.21"
    id("org.jetbrains.compose") version "1.7.3"
}

group = "com.brief"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

// ---------------------------------------------------------------------------
// Python engine (PyInstaller) build automation
//
// The desktop app shells out to a standalone `bridge` binary produced by
// PyInstaller from python_engine/bridge.py. In development the venv + bridge.py
// are used directly, but packaged distributions must ship the binary inside
// the app resources. These tasks make that automatic and incremental:
//
//   setupPythonEngineVenv -> venv + python_engine dependencies (once)
//   buildPythonEngine     -> PyInstaller bundle (only when sources change)
//   copyPythonEngine      -> dist/bridge into the Compose Desktop app resources
//
// Any package*/createDistributable task therefore produces a DMG/MSI/Deb that
// includes the Python engine. Opt out with: -Pbrief.skipPythonEngine=true
//
// Note: MLX (Apple Silicon) is the only fully supported engine stack today, so
// the automatic build only runs on macOS. On other hosts the existing
// dist/bridge output is bundled if present.
// ---------------------------------------------------------------------------

val pythonEngineDir = project.file("python_engine")
val venvDirFile = project.file("venv")
val venvPythonFile = venvDirFile.resolve(if (org.gradle.internal.os.OperatingSystem.current().isWindows) "Scripts/python.exe" else "bin/python")

val hostIsMacOs = org.gradle.internal.os.OperatingSystem.current().isMacOsX
val skipPythonEngine = providers.gradleProperty("brief.skipPythonEngine").orNull?.toBoolean() ?: false
val pythonEngineEnabled = hostIsMacOs && !skipPythonEngine

// Compose Desktop copies <appResourcesRootDir>/<os>-<arch> into the packaged app.
val hostResourcesDirName = run {
    val os = org.gradle.internal.os.OperatingSystem.current()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
        os.isMacOsX -> "macos"
        os.isWindows -> "windows"
        os.isLinux -> "linux"
        else -> error("Unsupported host OS: ${os.name}")
    }
    "$osName-" + if (arch.contains("aarch64") || arch.contains("arm")) "arm64" else "x64"
}

abstract class SetupPythonEngineVenvTask @Inject constructor(
    private val execOps: ExecOperations
) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val requirementsFile: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    val venvDir: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    val venvPython: RegularFileProperty = project.objects.fileProperty()

    @get:OutputFile
    val stampFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun prepare() {
        if (!venvPython.get().asFile.exists()) {
            execOps.exec {
                commandLine("python3", "-m", "venv", venvDir.get().asFile.absolutePath)
            }
        }
        execOps.exec {
            commandLine(
                venvPython.get().asFile.absolutePath, "-m", "pip", "install",
                "-r", requirementsFile.get().asFile.absolutePath
            )
        }
        stampFile.get().asFile.writeText("venv prepared for Brief python engine")
    }
}

abstract class BuildPythonEngineTask @Inject constructor(
    private val execOps: ExecOperations
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sources: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val requirementsFile: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    val venvPython: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    val engineDir: RegularFileProperty = project.objects.fileProperty()

    @get:OutputDirectory
    val distDir: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun build() {
        val dir = engineDir.get().asFile
        execOps.exec {
            workingDir(dir)
            commandLine(
                venvPython.get().asFile.absolutePath, "-m", "PyInstaller",
                "bridge.spec",
                "--noconfirm",
                "--distpath", dir.resolve("dist").absolutePath,
                "--workpath", dir.resolve("build/pyinstaller").absolutePath
            )
        }
    }
}

val setupPythonEngineVenv = tasks.register<SetupPythonEngineVenvTask>("setupPythonEngineVenv") {
    group = "build"
    description = "Ensures the Python venv exists with the python_engine dependencies installed."
    onlyIf("the Python engine can only be built on macOS") { pythonEngineEnabled }
    requirementsFile.set(pythonEngineDir.resolve("requirements.txt"))
    venvDir.set(venvDirFile)
    venvPython.set(venvPythonFile)
    stampFile.set(venvDirFile.resolve(".brief-venv-setup.stamp"))
}

val buildPythonEngine = tasks.register<BuildPythonEngineTask>("buildPythonEngine") {
    group = "build"
    description = "Builds the standalone Python engine binary with PyInstaller."
    dependsOn(setupPythonEngineVenv)
    onlyIf("the Python engine can only be built on macOS") { pythonEngineEnabled }
    sources.from(fileTree(pythonEngineDir) { include("*.py", "*.spec") })
    requirementsFile.set(pythonEngineDir.resolve("requirements.txt"))
    venvPython.set(venvPythonFile)
    engineDir.set(pythonEngineDir)
    distDir.set(pythonEngineDir.resolve("dist/bridge"))
}

// --- Copy PyInstaller bridge binary into Compose Desktop app resources ---
val copyPythonEngine = tasks.register<Copy>("copyPythonEngine") {
    group = "build"
    description = "Copies the PyInstaller bridge binary into the Compose Desktop app resources."
    dependsOn(buildPythonEngine)
    onlyIf("the Python engine is disabled") { !skipPythonEngine }
    from(pythonEngineDir.resolve("dist/bridge"))
    into(layout.buildDirectory.dir("appResources/$hostResourcesDirName/python_engine"))
    // Gradle's Copy does not reliably preserve the PyInstaller binary's exec bit
    filePermissions { unix("rwxr-xr-x") } // 0755
}

// Ensure the copy runs before any packaging or distribution task.
// prepareAppResources is included because Compose consumes the appResources
// directory from it (required by Gradle's implicit-dependency validation);
// the python build chain is incremental, so `run` stays fast after the first build.
tasks.matching {
    it.name.startsWith("package") ||
    it.name.contains("Distributable") ||
    it.name == "prepareAppResources"
}.configureEach {
    dependsOn(copyPythonEngine)
}

compose.desktop {
    application {
        mainClass = "brief.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Brief"
            macOS {
                bundleID = "com.brief.app"
                iconFile.set(project.file("src/main/resources/logo.icns"))
            }
            appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))

            buildTypes.release.proguard {
                isEnabled.set(false)
            }

            jvmArgs("--enable-native-access=ALL-UNNAMED")
        }
    }
}
