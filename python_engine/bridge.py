import multiprocessing
import sys
import os

# --- PyInstaller frozen-app multiprocessing fix ---
# In a PyInstaller bundle, multiprocessing's resource_tracker subprocess can die
# and respawn with a noisy UserWarning that leaks into stdout, corrupting the
# transcript stream. Three things fix this:
#   1. freeze_support() MUST run before any library that spawns workers internally
#      (e.g. mlx_whisper, huggingface_hub's file downloads use multiprocessing).
#   2. Use 'spawn' instead of the default 'fork' start method — fork is unsafe
#      in frozen single-file executables and causes the resource_tracker crash.
#   3. Suppress the specific UserWarning from resource_tracker so even if it
#      fires, it never reaches stdout.
if __name__ == "__main__":
    multiprocessing.freeze_support()

import warnings
warnings.filterwarnings("ignore", message="resource_tracker:.*", category=UserWarning)

# Force 'spawn' start method when running as a frozen PyInstaller bundle.
# The default 'fork' method is broken in frozen executables because the child
# process re-executes the bootloader instead of forking the Python interpreter.
if getattr(sys, 'frozen', False):
    try:
        multiprocessing.set_start_method('spawn', force=True)
    except RuntimeError:
        pass  # Already set

import json

try:
    import imageio_ffmpeg
    ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
    os.environ["PATH"] = os.path.dirname(ffmpeg_exe) + os.pathsep + os.environ.get("PATH", "")
except Exception:
    os.environ["PATH"] = os.environ.get("PATH", "") + os.pathsep + "/opt/homebrew/bin" + os.pathsep + "/usr/local/bin"

# Suppress resource_tracker stderr noise at the OS level for frozen builds.
# Even with the warnings filter above, the resource_tracker subprocess may write
# directly to fd 2. Redirect stderr to devnull so it never reaches the Kotlin
# process reader (which merges stderr into stdout via redirectErrorStream).
if getattr(sys, 'frozen', False):
    try:
        _devnull = os.open(os.devnull, os.O_WRONLY)
        os.dup2(_devnull, 2)
        os.close(_devnull)
        # Re-open sys.stderr so Python-level prints still work (to devnull)
        sys.stderr = open(os.devnull, 'w')
    except Exception:
        pass

# Add the directory to the path so we can import models
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import models

def main():
    if len(sys.argv) < 2:
        print("Usage: bridge.py <action> [args...]")
        sys.exit(1)
        
    action = sys.argv[1]
    
    # Disable buffering so output streams back immediately
    sys.stdout.reconfigure(line_buffering=True)
    
    try:
        if action == "transcribe":
            # bridge.py transcribe <audio_path> <language> <backend> <model_id>
            audio_path = sys.argv[2]
            language = sys.argv[3]
            backend = sys.argv[4]
            model_id = sys.argv[5]
            
            for output in models.transcribe_audio(audio_path, language, backend, model_id):
                print(output)
                sys.stdout.flush()
                
        elif action == "generate_notes":
            # bridge.py generate_notes <transcript_file> <prompt_type> <output_lang> <backend> <model_id>
            transcript_file = sys.argv[2]
            prompt_type = sys.argv[3]
            output_lang = sys.argv[4]
            backend = sys.argv[5]
            model_id = sys.argv[6]
            
            with open(transcript_file, "r", encoding="utf-8") as f:
                transcript = f.read()
                
            for output in models.generate_notes(transcript, prompt_type, output_lang, backend, model_id):
                if isinstance(output, dict):
                    # Signal end with JSON
                    print(f"JSON_RESULT:{json.dumps(output)}")
                else:
                    # Multiline outputs from LLM need special handling to avoid confusing stream readers,
                    # but in this case, we can just replace newlines or print as-is.
                    # Since models.py yields raw_output incrementally, we will just print the latest chunk
                    # But the Kotlin side needs to replace the whole string. 
                    # Let's wrap raw text in a marker:
                    print(f"CHUNK:{output}")
                sys.stdout.flush()
    except Exception as e:
        print(f"ERROR: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
