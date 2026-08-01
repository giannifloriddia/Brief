import gradio as gr
from pathlib import Path
from datetime import datetime
from src.models import (
    get_subjects, 
    transcribe_audio, 
    generate_notes, 
    get_downloaded_models, 
    delete_model,
    WHISPER_MLX_MODELS, WHISPER_CROSS_MODELS, 
    LLM_MLX_MODELS, LLM_GGUF_MODELS, fetch_ollama_models
)

def bind_events(app, c):
    """
    Binds the controller logic (event handlers) to the Gradio view components.
    'c' is the components dictionary created by views.py.
    """
    
    # Navigation Events
    def show_studio():
        return [gr.update(visible=True), gr.update(visible=False), gr.update(variant="primary"), gr.update(variant="secondary")]
        
    def show_models():
        return [gr.update(visible=False), gr.update(visible=True), gr.update(variant="secondary"), gr.update(variant="primary")]
        
    c["nav_studio_btn"].click(show_studio, outputs=[c["page_studio"], c["page_models"], c["nav_studio_btn"], c["nav_models_btn"]])
    c["nav_models_btn"].click(show_models, outputs=[c["page_studio"], c["page_models"], c["nav_studio_btn"], c["nav_models_btn"]])

    # Vault / Subjects
    def update_subjects_ui(export_dir):
        folders = get_subjects(export_dir)
        return gr.update(choices=folders, value="(Root)")
        
    app.load(fn=update_subjects_ui, inputs=c["export_dir_input"], outputs=c["subject_dropdown"])
    c["export_dir_input"].change(fn=update_subjects_ui, inputs=c["export_dir_input"], outputs=c["subject_dropdown"])
    c["refresh_subjects_btn"].click(fn=update_subjects_ui, inputs=c["export_dir_input"], outputs=c["subject_dropdown"])

    # Backend Toggles
    def update_whisper_dropdown(backend):
        choices = WHISPER_MLX_MODELS if backend == "Mac Native (MLX)" else WHISPER_CROSS_MODELS
        return gr.update(choices=choices, value=choices[0] if choices else None)

    def update_llm_dropdown(backend):
        if backend == "Mac Native (MLX)":
            choices = LLM_MLX_MODELS
        elif backend == "Windows/Linux (GGUF)":
            choices = LLM_GGUF_MODELS
        else: 
            choices = fetch_ollama_models()
        return gr.update(choices=choices, value=choices[0] if choices else "")
        
    c["whisper_backend_toggle"].change(fn=update_whisper_dropdown, inputs=c["whisper_backend_toggle"], outputs=c["whisper_model_input"])
    c["llm_backend_toggle"].change(fn=update_llm_dropdown, inputs=c["llm_backend_toggle"], outputs=c["llm_model_input"])

    # Processing Pipeline (Generator)
    def process_lecture(audio_path, language, output_lang, subject, whisper_backend, whisper_model, llm_backend, llm_model, prompt_type, export_dir, progress=gr.Progress()):
        if not audio_path:
            yield "⚠️ Please upload an audio file first.", "⚠️ Please upload an audio file first."
            return
            
        progress(0.1, desc="🎙️ Initializing Whisper Model...")
        yield "🎙️ Processing audio (transcribing)...", "⏳ Waiting for transcript to finish before generating notes..."
        
        # 1. Transcribe (Stream)
        transcript = ""
        try:
            for partial_transcript in transcribe_audio(audio_path, language, whisper_backend, whisper_model):
                transcript = partial_transcript
                progress(0.3, desc="🎙️ Transcribing Audio...")
                yield transcript, "⏳ Waiting for transcript to finish before generating notes..."
        except Exception as e:
            yield f"Transcription Failed: {e}", "❌ Cancelled."
            return
            
        progress(0.6, desc="🧠 Initializing LLM...")
        yield transcript, f"🧠 Transcript ready! Generating {output_lang} notes and AI filename using {llm_model}..."
        
        # 2. Generate Notes & AI Filename (Stream)
        ai_filename = "lecture-notes"
        notes = ""
        try:
            for partial_notes in generate_notes(transcript, prompt_type, output_lang, llm_backend, llm_model):
                if isinstance(partial_notes, dict):
                    ai_filename = partial_notes["filename"]
                    notes = partial_notes["final_notes"]
                else:
                    progress(0.8, desc="🧠 Generating Study Notes...")
                    yield transcript, partial_notes
        except Exception as e:
            yield transcript, f"Generation Failed: {e}"
            return
            
        progress(0.9, desc="💾 Saving Files...")
        # 3. File Operations
        export_path = Path(export_dir).expanduser()
        if subject and subject != "(Root)":
            export_path = export_path / subject
        export_path.mkdir(parents=True, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        base_name = f"{ai_filename}_{timestamp}"
        
        transcript_filename = f"transcript_{base_name}.md"
        notes_filename = f"notes_{base_name}.md"
        
        with open(export_path / transcript_filename, "w", encoding="utf-8") as f:
            f.write(f"# Transcript: {ai_filename}\n\n{transcript}")
            
        obsidian_link = f"\n\n---\n## 📎 Source Material\n- **Transcript**: [[{transcript_filename.replace('.md', '')}]]\n- **File**: [{transcript_filename}](./{transcript_filename})"
        final_notes = notes + obsidian_link
        
        with open(export_path / notes_filename, "w", encoding="utf-8") as f:
            f.write(final_notes)
            
        progress(1.0, desc="✅ Success!")
        gr.Info(f"✅ Success! Saved to {export_path.name}/{notes_filename}")
        yield transcript, final_notes

    c["process_btn"].click(
        fn=process_lecture, 
        inputs=[
            c["audio_input"], c["lang_input"], c["output_lang_input"], c["subject_dropdown"], 
            c["whisper_backend_toggle"], c["whisper_model_input"], c["llm_backend_toggle"], 
            c["llm_model_input"], c["prompt_type"], c["export_dir_input"]
        ], 
        outputs=[c["transcript_out"], c["summary_out"]]
    )

    # Model Manager
    def refresh_models_ui():
        choices, text = get_downloaded_models()
        return gr.update(choices=choices, value=choices[0] if choices else None), text

    def delete_model_ui(repo_id):
        if delete_model(repo_id):
            gr.Info(f"Deleted {repo_id}")
            return refresh_models_ui()
        return gr.update(), "Select a model first or model not found."

    app.load(fn=refresh_models_ui, outputs=[c["model_to_delete"], c["models_display"]])
    c["refresh_btn"].click(fn=refresh_models_ui, outputs=[c["model_to_delete"], c["models_display"]])
    c["delete_btn"].click(fn=delete_model_ui, inputs=c["model_to_delete"], outputs=[c["model_to_delete"], c["models_display"]])
