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

val preparePythonResources = tasks.register<Copy>("preparePythonResources") {
    description = "Python venv resources"
    from("python_engine") {
        into("python_engine")
    }
    from("venv") {
        into("venv")
    }
    into(layout.buildDirectory.dir("appResources"))
}

compose.desktop {
    application {
        mainClass = "brief.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "Brief"
            macOS {
                bundleID = "com.brief.app"
                iconFile.set(project.file("src/main/resources/logo.icns"))
            }
            appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))
            
            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}

tasks.matching { it.name == "prepareAppResources" || it.name.startsWith("package") }.configureEach {
    dependsOn(preparePythonResources)
}
