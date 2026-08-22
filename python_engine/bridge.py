import sys
import json
import os

try:
    import imageio_ffmpeg
    ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
    os.environ["PATH"] = os.path.dirname(ffmpeg_exe) + os.pathsep + os.environ.get("PATH", "")
except ImportError:
    os.environ["PATH"] = os.environ.get("PATH", "") + os.pathsep + "/opt/homebrew/bin" + os.pathsep + "/usr/local/bin"

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
