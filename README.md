# UniWhisper

An open-source, fully local lecture transcription app for university students, professors, and researchers.

## 🌟 Product Vision

UniWhisper is a cross-platform desktop and mobile app focused on turning long university lectures into clean transcripts and useful study summaries, without sending data to the cloud. 

The app should be simple: upload audio, transcribe locally with Whisper, then summarize locally with Gemma or another selected model, and export everything cleanly.

## ✨ Core Principles

- **Local first:** Audio, transcripts, and summaries stay on the device.
- **Lecture focused:** Optimized for classes, seminars, office hours, and research talks.
- **One app only:** No web dependency, no cloud lock-in, no separate companion services.
- **Open source:** Transparent architecture, model choices, and privacy behavior.
- **Cross-platform:** One codebase that runs on macOS, Windows, Linux, Android, and iOS where technically possible.
- **European language first:** Optimized for European languages and variants (e.g., European Portuguese) before global coverage.
- **Flexible inference:** Support MLX and other local backends, with clear separation between transcription and summarization steps.

## 🚀 Features

### Audio Input
- Upload audio files in common formats (MP3, WAV, M4A, AAC, FLAC, OGG).
- Drag and drop support.
- Background processing for uninterrupted device usage.

### Transcription (Whisper-first)
- Fully local transcription using downloadable Whisper models.
- Auto language detection optimized for European languages first.
- Strong support for European language variants (pt-PT, es-ES, en-GB, etc.).
- Punctuation, paragraph restoration, and timestamps.
- Optional speaker diarization for lecture Q&A.

### Summarization & Study Support (Gemma)
- One-click summary generation after transcription using local LLMs like Gemma 4.
- Multiple summary formats: short summary, detailed class notes, bullet points, exam revision sheets.
- Generate flashcards, quizzes, and term glossaries.
- Local Q&A about the lecture content.

### Privacy & Control
- No forced accounts, no cloud upload, no telemetry by default.
- Offline mode by default.
- User can delete all local data instantly.

## 🛠️ Recommended Architecture & Stack

- **App Language:** Kotlin Multiplatform (not yet implemented).
- **UI:** Shared logic with platform-native UI where needed.
- **Speech Model:** Whisper or Whisper-compatible models, with MLX support on Apple Silicon.
- **Summary Model:** Gemma 4 or similar compact LLM, with MLX support where available/Ollama models access.
- **Storage:** Local encrypted database plus file-based exports.

## 💻 Building Locally

*(Instructions will be expanded as the implementation progresses)*

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/UniWhisper.git
   cd UniWhisper
   ```
2. Setup environment (example for Python backend components):
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   python3 app.py
   ```

## 🤝 Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to get started.

## 📄 License

This project is licensed under the **GNU AGPLv3 License** - see the [LICENSE](LICENSE) file for details.
