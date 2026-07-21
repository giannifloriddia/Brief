import gradio as gr
import platform
import os
import shutil
import requests
from pathlib import Path
from datetime import datetime
from huggingface_hub import hf_hub_download, snapshot_download

# ponytail check: Wrap MLX imports so non-Mac users don't crash on startup.
HAS_MLX = False
if platform.system() == "Darwin" and platform.machine() == "arm64":
    try:
        import mlx_whisper
        from mlx_lm import load, generate
        HAS_MLX = True
    except ImportError:
        pass

# --- MODEL LISTS ---
WHISPER_MLX_MODELS = [
    "mlx-community/whisper-large-v3-turbo",
    "mlx-community/whisper-large-v3-turbo-8bit",
    "mlx-community/whisper-large-v3-mlx",
]
WHISPER_CROSS_MODELS = [
    "large-v3-turbo", 
    "small",
    "base"
]

LLM_MLX_MODELS = [
    "mlx-community/gemma-4-12b-it-4bit",
    "mlx-community/gemma-4-31b-it-4bit",
    "mlx-community/Meta-Llama-3.1-8B-Instruct-4bit"
]
LLM_GGUF_MODELS = [
    "bartowski/gemma-2-9b-it-GGUF/gemma-2-9b-it-Q4_K_M.gguf",
    "bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf"
]

def fetch_ollama_models():
    try:
        r = requests.get("http://localhost:11434/api/tags", timeout=1)
        if r.status_code == 200:
            return [m["name"] for m in r.json().get("models", [])]
    except:
        pass
    return ["llama3.1", "gemma2", "phi3", "mistral"]

def get_subjects(export_dir):
    try:
        path = Path(export_dir).expanduser()
        if not path.exists():
            return gr.update(choices=["(Root)"], value="(Root)")
        # Get only directories, ignore hidden folders
        folders = [d.name for d in path.iterdir() if d.is_dir() and not d.name.startswith('.')]
        folders.insert(0, "(Root)")
        return gr.update(choices=folders, value="(Root)")
    except Exception:
        return gr.update(choices=["(Root)"], value="(Root)")

def update_whisper_dropdown(backend):
    choices = WHISPER_MLX_MODELS if backend == "Mac Native (MLX)" else WHISPER_CROSS_MODELS
    return gr.update(choices=choices, value=choices[0])

def update_llm_dropdown(backend):
    if backend == "Mac Native (MLX)":
        return gr.update(choices=LLM_MLX_MODELS, value=LLM_MLX_MODELS[0])
    elif backend == "Windows/Linux (GGUF)":
        return gr.update(choices=LLM_GGUF_MODELS, value=LLM_GGUF_MODELS[0])
    else: 
        choices = fetch_ollama_models()
        return gr.update(choices=choices, value=choices[0] if choices else "")

# --- CORE LOGIC ---
def transcribe_audio(audio_path, language, backend, model_id):
    if backend == "Mac Native (MLX)":
        if not HAS_MLX: raise Exception("MLX is not installed or you are not on an M-series Mac.")
        result = mlx_whisper.transcribe(
            audio_path, path_or_hf_repo=model_id, language=language, 
            condition_on_previous_text=False, no_speech_threshold=0.6, logprob_threshold=-1.0
        )
        return result["text"]
    else:
        from faster_whisper import WhisperModel
        model = WhisperModel(model_id, device="auto", compute_type="default")
        segments, _ = model.transcribe(audio_path, language=language, condition_on_previous_text=False)
        return " ".join([s.text for s in segments])

def generate_notes(transcript, prompt_type, output_lang, backend, model_id):
    # ponytail logic: Do the AI filename generation in the same prompt to avoid loading the model twice (huge speedup)
    lang_instruction = f" Write the notes strictly in {output_lang}."
    
    if prompt_type == "Short Summary": base_prompt = "Provide a brief summary of this lecture."
    elif prompt_type == "Detailed Notes": base_prompt = "Create detailed study notes with bullet points."
    else: base_prompt = "Generate 5 exam revision questions and answers."
    
    prompt = (
        f"{base_prompt}{lang_instruction}\n\n"
        "IMPORTANT RULES:\n"
        "1. You must start your response with a suggested filename on the very first line, formatted exactly as 'FILENAME: short-hyphenated-name'.\n"
        "2. Put 'NOTES:' on the next line.\n"
        "3. DO NOT include any conversational filler, introduction, or preamble (e.g., never say 'Here are the notes' or 'Based on the transcript'). Start the actual study material immediately after 'NOTES:'.\n\n"
        f"Transcript:\n{transcript}"
    )
        
    raw_output = ""
    if backend == "Mac Native (MLX)":
        if not HAS_MLX: raise Exception("MLX is not installed or you are not on an M-series Mac.")
        model, tokenizer = load(model_id)
        raw_output = generate(model, tokenizer, prompt=prompt, max_tokens=1024, verbose=True)
    elif backend == "Windows/Linux (GGUF)":
        from llama_cpp import Llama
        repo_id, filename = "/".join(model_id.split("/")[:2]), "/".join(model_id.split("/")[2:])
        model_path = hf_hub_download(repo_id=repo_id, filename=filename)
        llm = Llama(model_path=model_path, n_ctx=8192, verbose=False)
        res = llm.create_chat_completion(messages=[{"role": "user", "content": prompt}], max_tokens=1024)
        raw_output = res["choices"][0]["message"]["content"]
    else: # Ollama
        r = requests.post("http://localhost:11434/api/generate", json={"model": model_id, "prompt": prompt, "stream": False})
        if r.status_code == 200:
            raw_output = r.json().get("response", "")
        else:
            raise Exception(f"Ollama API Error: {r.text}")

    # Parse combined output
    lines = raw_output.strip().split('\n')
    filename = "lecture-notes"
    notes_content = raw_output
    
    if lines and lines[0].startswith("FILENAME:"):
        raw_fname = lines[0].replace("FILENAME:", "").strip()
        filename = "".join(c for c in raw_fname if c.isalnum() or c in "-_ ").replace(" ", "-").lower()
        
        start_idx = 1
        for i, line in enumerate(lines[:5]):
            if line.startswith("NOTES:"):
                start_idx = i + 1
                break
        notes_content = "\n".join(lines[start_idx:]).strip()

    if not filename: filename = "lecture-notes"
    return filename, notes_content

# Using a generator for sequential UI loading states
def process_lecture(audio_path, language, output_lang, subject, whisper_backend, whisper_model, llm_backend, llm_model, prompt_type, export_dir):
    if not audio_path:
        yield "⚠️ Please upload an audio file first.", "⚠️ Please upload an audio file first."
        return
        
    yield "🎙️ Processing audio (transcribing)...", "⏳ Waiting for transcript to finish before generating notes..."
    
    # 1. Transcribe
    try:
        transcript = transcribe_audio(audio_path, language, whisper_backend, whisper_model)
    except Exception as e:
        yield f"Transcription Failed: {e}", "❌ Cancelled."
        return
        
    yield transcript, f"🧠 Transcript ready! Generating {output_lang} notes and AI filename using {llm_model}..."
    
    # 2. Generate Notes & AI Filename
    try:
        ai_filename, notes = generate_notes(transcript, prompt_type, output_lang, llm_backend, llm_model)
    except Exception as e:
        yield transcript, f"Generation Failed: {e}"
        return
        
    # 3. File Operations
    export_path = Path(export_dir).expanduser()
    if subject and subject != "(Root)":
        export_path = export_path / subject
    export_path.mkdir(parents=True, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    base_name = f"{ai_filename}_{timestamp}"
    
    transcript_filename = f"transcript_{base_name}.md"
    notes_filename = f"notes_{base_name}.md"
    
    # Save Transcript
    with open(export_path / transcript_filename, "w", encoding="utf-8") as f:
        f.write(f"# Transcript: {ai_filename}\n\n{transcript}")
        
    # Save Notes with Obsidian Link
    obsidian_link = f"\n\n---\n## 📎 Source Material\n- **Transcript**: [[{transcript_filename.replace('.md', '')}]]\n- **File**: [{transcript_filename}](./{transcript_filename})"
    final_notes = notes + obsidian_link
    
    with open(export_path / notes_filename, "w", encoding="utf-8") as f:
        f.write(final_notes)
        
    gr.Info(f"✅ Success! Saved to {export_path.name}/{notes_filename}")
    yield transcript, final_notes

# --- CACHE MANAGEMENT ---
CACHE_DIR = Path.home() / ".cache" / "huggingface" / "hub"
def get_downloaded_models():
    if not CACHE_DIR.exists(): return [], "No models downloaded yet."
    models, text_out = [], []
    for p in CACHE_DIR.iterdir():
        if p.is_dir() and p.name.startswith("models--"):
            repo_id = p.name.replace("models--", "").replace("--", "/")
            size_mb = sum(f.stat().st_size for f in p.rglob('*') if f.is_file()) / (1024 * 1024)
            models.append(repo_id)
            text_out.append(f"{repo_id} ({size_mb:.1f} MB)")
    return (models, "\n".join(text_out)) if models else ([], "No models downloaded yet.")

def refresh_models():
    choices, text = get_downloaded_models()
    return gr.update(choices=choices, value=choices[0] if choices else None), text

def delete_model(repo_id):
    if not repo_id: return gr.update(), "Select a model first."
    path = CACHE_DIR / ("models--" + repo_id.replace("/", "--"))
    if path.exists(): shutil.rmtree(path)
    gr.Info(f"Deleted {repo_id}")
    return refresh_models()[0], f"Deleted {repo_id}\n\nCurrent:\n{refresh_models()[1]}"

# --- UI DEFINITION ---
custom_css = """
body { background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%); color: #f8fafc; font-family: 'Inter', sans-serif; }
.gradio-container { background: rgba(255, 255, 255, 0.03) !important; backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border-radius: 24px !important; border: 1px solid rgba(255, 255, 255, 0.1) !important; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5) !important; }
.primary { background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%) !important; border: none !important; color: white !important; transition: transform 0.2s ease, box-shadow 0.2s ease !important; }
.primary:hover { transform: translateY(-2px); box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.4) !important; }
textarea { background: rgba(15, 23, 42, 0.6) !important; border: 1px solid rgba(255, 255, 255, 0.1) !important; }
"""

with gr.Blocks(title="UniWhisper", css=custom_css, theme=gr.themes.Base()) as app:
    gr.Markdown("# 🎙️ UniWhisper")
    gr.Markdown("Local-first lecture transcription and summarization. 100% on-device.")
    
    with gr.Accordion("⚙️ Global Settings & Vault Config", open=True):
        with gr.Row():
            export_dir_input = gr.Textbox(label="Export Directory (Obsidian Vault)", value="~/Desktop/UniWhisper_Exports")
            subject_dropdown = gr.Dropdown(label="Subject (Vault Folder)", choices=["(Root)"], value="(Root)")
            refresh_subjects_btn = gr.Button("🔄 Refresh Folders")
            
        with gr.Row():
            whisper_backend_toggle = gr.Radio(choices=["Mac Native (MLX)", "Cross-Platform"], value="Mac Native (MLX)", label="Whisper Hardware Backend")
            llm_backend_toggle = gr.Radio(choices=["Mac Native (MLX)", "Windows/Linux (GGUF)", "Ollama (Local API)"], value="Mac Native (MLX)", label="LLM Hardware Backend")
    
    with gr.Tabs():
        with gr.Tab("🎙️ Auto-Studio"):
            with gr.Row():
                audio_input = gr.Audio(type="filepath", label="Upload Lecture Audio")
                
                with gr.Column():
                    with gr.Row():
                        lang_input = gr.Dropdown(choices=["pt", "en", "es", "fr", "de"], value="pt", label="Audio Input Language")
                        output_lang_input = gr.Dropdown(choices=["pt", "en", "es", "fr", "de", "it", "nl"], value="pt", label="Notes Output Language")
                    
                    with gr.Row():
                        whisper_model_input = gr.Dropdown(choices=WHISPER_MLX_MODELS, value=WHISPER_MLX_MODELS[0], label="Whisper Model")
                        llm_model_input = gr.Dropdown(choices=LLM_MLX_MODELS, value=LLM_MLX_MODELS[0], label="LLM Model")
                        
                    prompt_type = gr.Dropdown(choices=["Short Summary", "Detailed Notes", "Exam Q&A"], value="Detailed Notes", label="Study Tool")
            
            with gr.Row():
                process_btn = gr.Button("🚀 Process Lecture", variant="primary", scale=2)
                
            with gr.Row():
                with gr.Column():
                    gr.Markdown("### Transcript")
                    transcript_out = gr.Textbox(show_label=False, lines=12, interactive=True, placeholder="Transcription will appear here...")
                with gr.Column():
                    gr.Markdown("### Study Notes")
                    summary_out = gr.Textbox(show_label=False, lines=12, interactive=True, placeholder="Waiting for transcript...")
            
        with gr.Tab("💾 Model Manager"):
            gr.Markdown("### Manage Local Models")
            
            with gr.Row():
                refresh_btn = gr.Button("Refresh Status")
            
            with gr.Row():
                models_display = gr.Textbox(label="Storage Used (MB)", lines=8, interactive=False)
            
            with gr.Row():
                model_to_delete = gr.Dropdown(label="Cached Models", choices=[])
                delete_btn = gr.Button("Delete Selected", variant="stop")
            
            # Events
            app.load(fn=get_subjects, inputs=export_dir_input, outputs=subject_dropdown)
            export_dir_input.change(fn=get_subjects, inputs=export_dir_input, outputs=subject_dropdown)
            refresh_subjects_btn.click(fn=get_subjects, inputs=export_dir_input, outputs=subject_dropdown)
            
            whisper_backend_toggle.change(fn=update_whisper_dropdown, inputs=whisper_backend_toggle, outputs=whisper_model_input)
            llm_backend_toggle.change(fn=update_llm_dropdown, inputs=llm_backend_toggle, outputs=llm_model_input)
            
            process_btn.click(
                fn=process_lecture, 
                inputs=[audio_input, lang_input, output_lang_input, subject_dropdown, whisper_backend_toggle, whisper_model_input, llm_backend_toggle, llm_model_input, prompt_type, export_dir_input], 
                outputs=[transcript_out, summary_out]
            )
            
            app.load(fn=refresh_models, outputs=[model_to_delete, models_display])
            refresh_btn.click(fn=refresh_models, outputs=[model_to_delete, models_display])
            delete_btn.click(fn=delete_model, inputs=model_to_delete, outputs=[model_to_delete, models_display])

if __name__ == "__main__":
    app.launch()
