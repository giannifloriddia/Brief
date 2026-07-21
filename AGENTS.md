# UniWhisper

An open-source, fully local lecture transcription app for university students, professors, and researchers.

## Product vision

UniWhisper is a cross-platform desktop and mobile app focused on turning long university lectures into clean transcripts and useful study summaries, without sending data to the cloud.

The app should be simple: upload audio, transcribe locally with Whisper, then summarize locally with Gemma or another selected model, and export everything cleanly.

## Core principles

- Local first: audio, transcripts, and summaries stay on the device.
- Lecture focused: optimized for classes, seminars, office hours, and research talks.
- One app only: no web dependency, no cloud lock-in, no separate companion services.
- Open source: transparent architecture, model choices, and privacy behavior.
- Cross-platform: one codebase that runs on macOS, Windows, Linux, Android, and iOS where technically possible.
- European language first: optimized for European languages and variants such as European Portuguese before global coverage.
- Flexible inference: support MLX and other local backends, with clear separation between transcription and summarization steps.

## Main use case

1. A student records or uploads a lecture audio file.
2. The app transcribes the full lecture locally using a Whisper-based model.
3. The app automatically cleans the transcript with punctuation, paragraphs, and speaker labels if available.
4. The app sends the transcript to a second local model, such as Gemma 4, to generate a study summary.
5. The user exports transcript, summary, and notes in one click.

## Must-have features

### Audio input

- Upload audio files in common formats such as MP3, WAV, M4A, AAC, FLAC, and OGG.
- Support long recordings, including multi-hour lectures.
- Drag and drop support.
- Batch upload for multiple classes.
- Optional live recording inside the app.
- Background processing so users can keep using the device while transcription runs.

### Transcription (Whisper-first)

- Default transcription engine based on Whisper or Whisper-compatible models.
- Fully local transcription using downloadable Whisper models.
- Auto language detection optimized for European languages first.
- Manual language selection for better accuracy.
- Strong support for European language variants:
  - European Portuguese (pt-PT) prioritized over Brazilian Portuguese (pt-BR).
  - European Spanish (es-ES) and Latin American variants.
  - UK English (en-GB) and other European English accents.
  - German, French, Italian, Dutch, Polish, Swedish, and other major EU languages.
- Punctuation and paragraph restoration.
- Timestamps for every segment.
- Optional speaker diarization for lecture Q&A or group discussions.
- Custom vocabulary for course names, professor names, acronyms, and technical terms.
- Transcript editing with search and replace.

### Summarization and study support (Gemma or other selected model)

- One-click summary generation after transcription.
- Use a second local LLM, such as Gemma 4, for summaries.
- Configurable model selection for summarization:
  - Gemma 4 as the default.
  - Option to choose other compatible local LLMs.
- Offer several summary formats:
  - Short summary.
  - Detailed class notes.
  - Bullet-point recap.
  - Key concepts and definitions.
  - Exam revision sheet.
  - Questions and answers.
- Generate flashcards from the transcript.
- Generate quizzes from the transcript.
- Generate a glossary of terms from the lecture.
- Let the user ask questions about the lecture content locally using the same LLM.
- Summary language can be:
  - Same as lecture language.
  - User’s preferred language, with local translation support.

### Export and sharing

- Export transcript as TXT, MD, PDF, DOCX, and SRT/VTT.
- Export summary separately or together with the transcript.
- Copy formatted notes to clipboard.
- Share to other apps through the OS share sheet where available.
- Preserve timestamps and speaker labels in exports.
- Optional markdown template for note-taking apps like Obsidian and Notion.

### Privacy and control

- No forced accounts.
- No cloud upload.
- No telemetry by default.
- Offline mode by default.
- Clear model storage controls.
- User can delete all local data instantly.
- Transparency page showing which models are running and where files are stored.

## Multilingual and localization strategy

- Default language packs focused on European languages.
- Priority order for model support:
  1. European Portuguese (pt-PT).
  2. English (en-GB, en-IE, en-US).
  3. Spanish (es-ES), French (fr-FR), German (de-DE).
  4. Italian (it-IT), Dutch (nl-NL), Polish (pl-PL), Swedish (sv-SE).
  5. Additional EU languages over time.
- Separate acoustic models for European Portuguese vs Brazilian Portuguese.
- Language-specific system prompts for summarization:
  - Prompts tuned for academic language in each target language.
  - Prompts adapted to local university terminology.
- UI localization starting with:
  - English.
  - European Portuguese.
  - Spanish, French, German.
- Manual language override for edge cases or mixed-language lectures.

## Model management

- Built-in button to automatically download required models.
- One-click setup wizard for first-time users.
- Show model size, speed, RAM usage, and expected accuracy.
- Separate model packs for:
  - Whisper-based speech-to-text (with European language variants).
  - Summarization LLMs (Gemma 4 and other compatible models).
  - Speaker diarization.
  - Optional translation.
- Allow users to choose between smaller fast models and larger more accurate models.
- Support automatic updates for local models.
- Warn users before downloading large files on mobile data.

## Inference backends and MLX support

- Support multiple local inference backends:
  - MLX for Apple Silicon (M1/M2/M3/M4) as a first-class backend.
  - Other CPU/GPU backends for Windows, Linux, and non-Apple devices.
- Users can select which backend to use:
  - Auto-detect recommended backend.
  - Manual override for advanced users.
- Whisper transcription:
  - Available via MLX-Whisper on macOS where supported.
  - Available via alternative Whisper runtimes on other platforms.
- Summarization LLM:
  - Gemma 4 via MLX when running on Apple Silicon.
  - Equivalent local LLM backends on other platforms.
- Clear separation of steps:
  1. Audio → Whisper (transcription).
  2. Transcript → Gemma or selected LLM (summarization / Q&A / flashcards).
- Allow users to swap models without re-transcribing:
  - Re-run summarization with a different LLM on an existing transcript.

## Suggested architecture

### Frontend

- Kotlin Multiplatform UI for shared logic across platforms.
- Platform-native UI wrappers if needed for best performance.
- Shared state management and file handling.

### Core engine

- Local inference engine abstraction for speech and LLM models.
- Pluggable model backends so the app can support different local runtimes, including MLX.
- Separate modules for:
  - Transcription (Whisper).
  - Summarization (Gemma or selected LLM).
  - Indexing and search.
- Local search index for transcript retrieval.

### Storage

- Encrypted local storage for transcripts and metadata.
- Per-course folders and session database.
- Optional export cache for generated summaries.
- Indexed full-text search.

### Performance goals

- Transcribe long lectures without freezing the UI.
- Support resumable jobs if the app closes.
- Use GPU acceleration when available (including MLX on Apple Silicon).
- Fall back to CPU gracefully.
- Show progress, ETA, and resource usage.

## UX requirements

- Simple home screen with one primary action: upload or record.
- Clear progress state during transcription.
- Results screen with tabs for transcript, summary, highlights, and export.
- Minimal settings by default.
- Advanced settings hidden but available.
- Dark mode and light mode.
- Keyboard shortcuts for desktop users.
- Mobile-friendly controls for quick review on the go.

## Nice-to-have features

- Automatic lecture segmentation by topic.
- Slide-aware notes if the user uploads lecture slides alongside audio.
- Multilingual lecture support.
- Translation of transcripts into the user’s preferred language.
- Sync between devices only through user-controlled local methods, not mandatory cloud sync.
- Integration with calendar course schedules.
- Import from voice memos, Zoom recordings, and screen recordings.
- Local AI assistant for asking “What was the professor saying about X?”

## Non-goals

- No cloud-first workflow.
- No user tracking.
- No social network features.
- No device-specific lock-in.
- No dependence on proprietary APIs for core functionality.
- No complicated enterprise administration in the first version.

## MVP scope

### Version 1

- Upload audio.
- Local transcription optimized for European languages, starting with European Portuguese, using Whisper.
- Basic transcript editor.
- Local summary generation with Gemma 4.
- Export transcript and summary.
- Automatic model download.
- MLX support on Apple Silicon for Whisper and Gemma.
- Cross-platform desktop support first.

### Version 2

- Speaker diarization.
- Flashcards and quizzes.
- Course organization.
- Search across transcripts.
- Mobile support expansion.

### Version 3

- Live lecture recording.
- On-device Q&A over lecture history.
- Slide-aware note generation.
- Advanced study workflows.

## Success metrics

- Time to first transcription.
- Transcript accuracy on lecture audio in European languages.
- Summary usefulness for exam prep.
- Model download success rate.
- Offline completion rate.
- Export usage rate.
- User retention across a semester.

## Open source goals

- Easy local build instructions.
- Clear contribution guide.
- Reproducible model setup.
- Modular codebase with separate transcription and summarization layers.
- Community-friendly licensing for app code and model integration layers.

## Example user flow

1. Student in Lisbon opens UniWhisper on a Mac with M-series chip.
2. Clicks “Download models” and selects European Portuguese and English.
3. App recommends MLX backend for Whisper and Gemma.
4. User uploads a 90-minute lecture recording in European Portuguese.
5. App transcribes locally using a Whisper model via MLX.
6. App sends the transcript to Gemma 4 (also via MLX) to generate a lecture summary.
7. Student exports a Markdown note to Obsidian and a PDF transcript for revision.

## Positioning

UniWhisper should feel like the **MacWhisper** experience, but built around university learning: longer lectures, study summaries, exam preparation, and local privacy by design. MacWhisper is known for local transcription on Mac, while Plaud-style workflows emphasize transcripts plus structured AI summaries, which matches your desired direction for lecture notes and study support [web:6][web:3][web:10].

## Recommended stack

- App language: Kotlin Multiplatform.
- UI: shared logic with platform-native UI where needed.
- Speech model: Whisper or Whisper-compatible models, with MLX support on Apple Silicon.
- Summary model: Gemma 4 or similar compact LLM, with MLX support where available.
- Storage: local encrypted database plus file-based exports.
- Search: full-text index for transcripts and summaries.

## Final product promise

One app. One local workflow. Upload lecture audio, transcribe with Whisper, summarize with Gemma or your chosen model, study faster, and keep everything on your device, with best-in-class support for European languages such as European Portuguese and first-class MLX support on Apple Silicon.