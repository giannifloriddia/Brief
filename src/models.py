import platform
import os
import shutil
import requests
from pathlib import Path
from huggingface_hub import hf_hub_download, HfApi

# --- ENVIRONMENT CONFIG ---
HAS_MLX = False
if platform.system() == "Darwin" and platform.machine() == "arm64":
    try:
        import mlx_whisper
        from mlx_lm import load, generate
        HAS_MLX = True
    except ImportError:
        pass

# --- MODEL LISTS ---
def fetch_hf_models(filter_tags=None, search_query=None, limit=5, fallback=None):
    try:
        api = HfApi()
        models = api.list_models(filter=filter_tags, search=search_query, sort="downloads", direction=-1, limit=limit)
        return [m.id for m in models]
    except Exception:
        return fallback or []

WHISPER_MLX_MODELS = fetch_hf_models(filter_tags=["mlx", "whisper"], fallback=[
    "mlx-community/whisper-large-v3-turbo",
    "mlx-community/whisper-large-v3-mlx",
])

WHISPER_CROSS_MODELS = fetch_hf_models(search_query="faster-whisper", fallback=[
    "Systran/faster-whisper-large-v3", 
    "Systran/faster-whisper-small"
])

LLM_MLX_MODELS = fetch_hf_models(filter_tags=["mlx"], search_query="gemma", fallback=[
    "mlx-community/gemma-4-12b-it-4bit",
    "mlx-community/gemma-4-31b-it-4bit"
])

LLM_GGUF_MODELS = fetch_hf_models(filter_tags=["gguf"], search_query="gemma", fallback=[
    "bartowski/gemma-2-9b-it-GGUF"
])

def fetch_ollama_models():
    try:
        r = requests.get("http://localhost:11434/api/tags", timeout=1)
        if r.status_code == 200:
            return [m["name"] for m in r.json().get("models", [])]
    except:
        pass
    return ["llama3.1", "gemma2", "phi3", "mistral"]

# --- STORAGE LOGIC ---
def get_subjects(export_dir):
    try:
        path = Path(export_dir).expanduser()
        if not path.exists():
            return ["(Root)"]
        folders = [d.name for d in path.iterdir() if d.is_dir() and not d.name.startswith('.')]
        folders.insert(0, "(Root)")
        return folders
    except Exception:
        return ["(Root)"]

# --- TRANSCRIPTION ---
def transcribe_audio(audio_path, language, backend, model_id):
    if backend == "Mac Native (MLX)":
        if not HAS_MLX: raise Exception("MLX is not installed or you are not on an M-series Mac.")
        yield "🎙️ Processing audio on MLX... (This may take a moment for the entire file to complete)"
        result = mlx_whisper.transcribe(
            audio_path, path_or_hf_repo=model_id, language=language, 
            condition_on_previous_text=False, no_speech_threshold=0.6, logprob_threshold=-1.0
        )
        yield result["text"]
    else:
        from faster_whisper import WhisperModel
        yield "⚙️ Loading faster-whisper model..."
        model = WhisperModel(model_id, device="auto", compute_type="default")
        segments, _ = model.transcribe(audio_path, language=language, condition_on_previous_text=False)
        transcript = ""
        for s in segments:
            transcript += s.text + " "
            yield transcript.strip()

# --- SUMMARIZATION ---
def generate_notes(transcript, prompt_type, output_lang, backend, model_id):
    lang_instruction = f" Write the notes strictly in {output_lang}."
    
    if prompt_type == "Short Summary": base_prompt = "Provide a brief summary of this lecture."
    elif prompt_type == "Detailed Notes": base_prompt = "Create detailed study notes with bullet points."
    else: base_prompt = "Generate 5 exam revision questions and answers."
    
    prompt = (
        f"{base_prompt}{lang_instruction}\n\n"
        "IMPORTANT RULES:\n"
        "1. You must start your response with a suggested filename on the very first line, formatted exactly as 'FILENAME: short-hyphenated-name'.\n"
        "2. Put 'NOTES:' on the next line.\n"
        "3. DO NOT include any conversational filler, introduction, or preamble. Start the actual study material immediately after 'NOTES:'.\n\n"
        f"Transcript:\n{transcript}"
    )
        
    raw_output = ""
    if backend == "Mac Native (MLX)":
        if not HAS_MLX: raise Exception("MLX is not installed or you are not on an M-series Mac.")
        model, tokenizer = load(model_id)
        # For simplicity, fallback to block generate for MLX, yield once
        yield "🧠 Generating notes with MLX (please wait for full response)..."
        raw_output = generate(model, tokenizer, prompt=prompt, max_tokens=1024, verbose=True)
        yield raw_output
    elif backend == "Windows/Linux (GGUF)":
        from llama_cpp import Llama
        
        if len(model_id.split("/")) > 2:
            repo_id, filename = "/".join(model_id.split("/")[:2]), "/".join(model_id.split("/")[2:])
        else:
            repo_id = model_id
            api = HfApi()
            files = api.list_repo_files(repo_id)
            gguf_files = [f for f in files if f.endswith(".gguf")]
            if not gguf_files: raise Exception(f"No .gguf files found in {repo_id}")
            q4_files = [f for f in gguf_files if "Q4_K_M" in f or "q4_k_m" in f]
            filename = q4_files[0] if q4_files else gguf_files[0]
            
        model_path = hf_hub_download(repo_id=repo_id, filename=filename)
        llm = Llama(model_path=model_path, n_ctx=8192, verbose=False)
        res = llm.create_chat_completion(messages=[{"role": "user", "content": prompt}], max_tokens=1024, stream=True)
        for chunk in res:
            if "content" in chunk["choices"][0]["delta"]:
                raw_output += chunk["choices"][0]["delta"]["content"]
                yield raw_output
    else: # Ollama
        import json
        r = requests.post("http://localhost:11434/api/generate", json={"model": model_id, "prompt": prompt, "stream": True}, stream=True)
        if r.status_code != 200:
            raise Exception(f"Ollama API Error: {r.text}")
        for line in r.iter_lines():
            if line:
                data = json.loads(line)
                raw_output += data.get("response", "")
                yield raw_output

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
    
    # We yield a dict at the very end to pass the parsed data back to the caller
    yield {"filename": filename, "final_notes": notes_content}

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

def delete_model(repo_id):
    if not repo_id: return False
    path = CACHE_DIR / ("models--" + repo_id.replace("/", "--"))
    if path.exists():
        shutil.rmtree(path)
        return True
    return False
