# Brief

An open-source, fully local lecture transcription app for university students, professors, and researchers.

<p align="center">
  <img src="Screenshot%202026-08-08%20at%2015.34.45.png" alt="Main Screen" width="2784">
</p>

## 🌟 Product Vision

Brief is a cross-platform desktop and mobile app focused on turning long university lectures into clean transcripts and useful study summaries, without sending data to the cloud. 

The app should be simple: upload audio, transcribe locally with Whisper, then summarize locally with Gemma or another selected model, and export everything cleanly.

## ✨ Core Principles

- **Local first:** Audio, transcripts, and summaries stay on the device.
- **Lecture focused:** Optimized for classes, seminars, office hours, and research talks.
- **One app only:** No web dependency, no cloud lock-in, no separate companion services.
- **Open source:** Transparent architecture, model choices, and privacy behavior.
- **Cross-platform:** One codebase that runs on macOS, Windows, Linux.
- **European language first:** Optimized for European languages and variants (e.g., European Portuguese).
- **Flexible inference:** Support MLX and other local backends.

---

## 🛠️ Prerequisites for Building Locally

To build Brief from scratch, you will need the following installed on your machine:

1. **Java Development Kit (JDK) 21**
   - **Mac:** `brew install openjdk@21`
   - **Windows:** Download from [Adoptium](https://adoptium.net/) or use `winget install Microsoft.OpenJDK.21`
   - **Linux:** `sudo apt install openjdk-21-jdk`
2. **Python 3.10+**
   - **Mac:** `brew install python`
   - **Windows:** Download from [Python.org](https://python.org)
   - **Linux:** `sudo apt install python3 python3-pip python3-venv`
3. **Git**
   - Needed to clone the repository.

---

## 💻 Step-by-Step Build Guide

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/Brief.git
cd Brief
```

### 2. Setup the Python ML Engine (Required)
Brief relies on a Python bridge for local MLX and Whisper inference. You **must** create a Python Virtual Environment at the root of the project before running.

**Mac / Linux:**
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r python_engine/requirements.txt
```

**Windows:**
```powershell
python -m venv venv
.\venv\Scripts\activate
pip install -r python_engine/requirements.txt
```

### 3. Run the App Locally (Development)
You can run the app instantly via Gradle:
```bash
# On Mac/Linux
./gradlew run

# On Windows
gradlew.bat run
```

### 4. Build Standalone Installers (.app, .dmg, .exe, .msi, .deb)
To package Brief into a native installer for your current operating system, run:
```bash
# On Mac/Linux
./gradlew packageDistributionForCurrentOS

# On Windows
gradlew.bat packageDistributionForCurrentOS
```
Once the build completes, your standalone installer (e.g., `Brief.dmg` on Mac or `Brief.msi` on Windows) will be located inside the `build/compose/binaries/main/` folder!

**Good News:** The build system is now fully automated to bundle your localized Python environment directly into the generated installer. Your `.dmg` or `.exe` will be a true standalone application that you can distribute anywhere without requiring users to install Python!

---

## 🤝 Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to get started.

## 📄 License

This project is licensed under the **GNU AGPLv3 License** - see the [LICENSE](LICENSE) file for details.
