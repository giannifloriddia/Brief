import gradio as gr
from src.models import WHISPER_MLX_MODELS, LLM_MLX_MODELS

nocturnal_theme = gr.themes.Base(
    primary_hue=gr.themes.colors.indigo,
    secondary_hue=gr.themes.colors.slate,
    neutral_hue=gr.themes.colors.slate,
    font=[gr.themes.GoogleFont("Geist"), "ui-sans-serif", "system-ui", "sans-serif"],
    font_mono=[gr.themes.GoogleFont("JetBrains Mono"), "ui-monospace", "monospace"],
    radius_size=gr.themes.sizes.radius_sm,
    text_size=gr.themes.sizes.text_md,
).set(
    body_background_fill="#0b1326",
    body_background_fill_dark="#0b1326",
    body_text_color="#dae2fd",
    body_text_color_dark="#dae2fd",
    
    background_fill_primary="#0b1326",
    background_fill_primary_dark="#0b1326",
    background_fill_secondary="#131b2e",
    background_fill_secondary_dark="#131b2e",
    
    border_color_primary="#454653",
    border_color_primary_dark="#454653",
    border_color_accent="#818cf8",
    border_color_accent_dark="#818cf8",
    
    block_background_fill="#171f33",
    block_background_fill_dark="#171f33",
    block_border_color="#454653",
    block_border_width="1px",
    
    button_primary_background_fill="#818cf8",
    button_primary_background_fill_dark="#818cf8",
    button_primary_text_color="#ffffff",
    button_primary_text_color_dark="#ffffff",
    
    button_secondary_background_fill="#1e293b",
    button_secondary_background_fill_dark="#1e293b",
    button_secondary_text_color="#dae2fd",
    button_secondary_text_color_dark="#dae2fd",
    button_secondary_border_color="#475569",
    
    input_background_fill="#1e293b",
    input_background_fill_dark="#1e293b",
    input_border_color="#475569",
    input_border_color_dark="#475569",
    input_border_color_focus="#818cf8",
    input_border_color_focus_dark="#818cf8",
    
    block_label_background_fill="#131b2e",
    block_label_text_color="#dae2fd",
    block_title_text_color="#dae2fd",
    
    panel_background_fill="#171f33",
    panel_background_fill_dark="#171f33",
)

custom_css = """
/* Hide generic gradio padding to allow full bleed layouts */
.gradio-container {
    max-width: 100% !important;
    padding: 0 !important;
    margin: 0 !important;
}

/* Minimalist, Flat UI with Tonal Depth */
* {
    box-shadow: none !important;
}

/* Layout */
.main-layout {
    display: flex;
    flex-direction: row;
    min-height: 100vh;
    flex-wrap: nowrap !important;
}
.sidebar-col {
    width: 260px !important;
    min-width: 260px !important;
    max-width: 260px !important;
    flex-grow: 0 !important;
    background-color: #171f33 !important; /* surface-container */
    border-right: 1px solid #454653 !important;
    padding: 24px !important;
    display: flex;
    flex-direction: column;
}
.main-canvas {
    flex-grow: 1 !important;
    padding: 32px !important;
    max-width: 1280px !important;
    margin: 0 auto;
}

/* Sidebar Brand Header */
.brand-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 32px;
}
.brand-header .icon {
    width: 40px; height: 40px;
    background-color: #818cf8; /* primary-container */
    color: #101b8a;
    border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    font-size: 24px;
}
.brand-header h2 {
    margin: 0; color: #bdc2ff; font-size: 24px; font-weight: 600;
}
.brand-header p {
    margin: 0; color: #c6c5d5; font-size: 12px;
}

/* Navigation Buttons */
.nav-btn {
    text-align: left !important;
    justify-content: flex-start !important;
    padding-left: 16px !important;
    background-color: transparent !important;
    border: none !important;
    box-shadow: none !important;
    color: #c6c5d5 !important;
    font-size: 16px !important;
    margin-bottom: 8px !important;
}
.nav-btn.primary {
    background-color: #39485a !important; /* secondary-container */
    color: #bdc2ff !important;
    border-left: 4px solid #bdc2ff !important;
    border-radius: 0 8px 8px 0 !important;
}
.nav-btn:hover {
    background-color: #222a3d !important;
}

/* Top App Bar */
.top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    border-bottom: 1px solid #454653;
    margin-bottom: 32px;
}
.status-indicators {
    display: flex;
    gap: 16px;
}
.status-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 12px;
    background-color: #060e20;
    border: 1px solid #454653;
    border-radius: 9999px;
    font-size: 12px;
    color: #b9c8de;
}
.status-badge .dot {
    width: 8px; height: 8px;
    border-radius: 50%;
    background-color: #34d399; /* emerald-400 */
}

/* Typography Refinements */
h1.page-title {
    font-size: 48px !important;
    font-weight: 700 !important;
    letter-spacing: -0.02em !important;
    color: #dae2fd !important;
    margin-bottom: 0.5rem !important;
}
.page-subtitle {
    font-size: 16px;
    color: #c6c5d5;
    margin-bottom: 32px;
}

/* Audio Dropzone inspired by HTML */
.audio-dropzone {
    border: 2px dashed #454653 !important;
    border-radius: 12px !important;
    background-color: #131b2e !important;
    padding: 16px !important;
    transition: all 0.3s ease;
}
.audio-dropzone:hover {
    border-color: #818cf8 !important;
    background-color: rgba(129, 140, 248, 0.05) !important;
}

/* Tonal Accordion */
.accordion {
    background-color: #171f33 !important;
    border: 1px solid #454653 !important;
    border-radius: 8px !important;
}
"""

def create_ui():
    components = {}
    with gr.Blocks(title="UniWhisper", css=custom_css, theme=nocturnal_theme) as app:
        
        with gr.Row(elem_classes="main-layout"):
            
            # --- SIDEBAR ---
            with gr.Column(elem_classes="sidebar-col"):
                gr.HTML("""
                <div class="brand-header">
                    <div class="icon">🧠</div>
                    <div>
                        <h2>UniWhisper</h2>
                        <p>Local-first Research</p>
                    </div>
                </div>
                """)
                
                # Main CTA
                gr.Button("➕ New Transcription", variant="primary", elem_classes="nav-btn", scale=0)
                
                gr.HTML("<div style='margin-top: 24px;'></div>")
                
                # Navigation
                components["nav_studio_btn"] = gr.Button("🏠 Home", variant="primary", elem_classes="nav-btn")
                gr.Button("🎙️ Lectures", variant="secondary", elem_classes="nav-btn")
                gr.Button("🎓 Courses", variant="secondary", elem_classes="nav-btn")
                components["nav_models_btn"] = gr.Button("🧠 Models", variant="secondary", elem_classes="nav-btn")
                gr.Button("⚙️ Settings", variant="secondary", elem_classes="nav-btn")
                
            # --- MAIN CANVAS ---
            with gr.Column(elem_classes="main-canvas"):
                
                # Top App Bar
                gr.HTML("""
                <div class="top-bar">
                    <div class="status-indicators">
                        <div class="status-badge"><div class="dot"></div> Whisper: Ready</div>
                        <div class="status-badge"><div class="dot"></div> Gemma: Ready</div>
                    </div>
                    <div style="display: flex; gap: 16px; align-items: center;">
                        <span style="color: #c6c5d5;">Local Mode Only</span>
                        <div style="width: 32px; height: 32px; border-radius: 50%; background: #818cf8; color: #101b8a; display: flex; align-items: center; justify-content: center; font-weight: bold;">U</div>
                    </div>
                </div>
                """)
                
                # Studio Page
                with gr.Group(visible=True) as page_studio:
                    components["page_studio"] = page_studio
                    
                    gr.HTML("""
                    <h1 class="page-title">Início</h1>
                    <p class="page-subtitle">Welcome back. Drop a lecture recording to begin processing.</p>
                    """)
                    
                    with gr.Accordion("⚙️ Global Settings & Vault Config", open=False, elem_classes="accordion"):
                        with gr.Row():
                            components["export_dir_input"] = gr.Textbox(label="Export Directory (Obsidian Vault)", value="~/Desktop/Eu/UniWhisper_Notes", scale=3)
                            components["subject_dropdown"] = gr.Dropdown(label="Subject (Vault Folder)", choices=["(Root)"], value="(Root)", scale=2)
                            components["refresh_subjects_btn"] = gr.Button("🔄 Refresh", scale=1)
                            
                        with gr.Row():
                            components["whisper_backend_toggle"] = gr.Radio(choices=["Mac Native (MLX)", "Cross-Platform"], value="Mac Native (MLX)", label="Whisper Hardware Backend")
                            components["llm_backend_toggle"] = gr.Radio(choices=["Mac Native (MLX)", "Windows/Linux (GGUF)", "Ollama (Local API)"], value="Mac Native (MLX)", label="LLM Hardware Backend")
                            
                    with gr.Row(equal_height=True):
                        with gr.Column(scale=1):
                            gr.Markdown("### 1. Upload Media")
                            components["audio_input"] = gr.Audio(type="filepath", label="Upload Lecture Audio", elem_classes="audio-dropzone")
                        
                        with gr.Column(scale=1):
                            gr.Markdown("### 2. Configuration")
                            with gr.Group():
                                with gr.Row():
                                    components["lang_input"] = gr.Dropdown(choices=["pt", "en", "es", "fr", "de"], value="pt", label="Audio Input Language")
                                    components["output_lang_input"] = gr.Dropdown(choices=["pt", "en", "es", "fr", "de", "it", "nl"], value="pt", label="Notes Output Language")
                                with gr.Row():
                                    components["whisper_model_input"] = gr.Dropdown(choices=WHISPER_MLX_MODELS, value=WHISPER_MLX_MODELS[0] if WHISPER_MLX_MODELS else None, label="Whisper Model")
                                    components["llm_model_input"] = gr.Dropdown(choices=LLM_MLX_MODELS, value=LLM_MLX_MODELS[0] if LLM_MLX_MODELS else None, label="LLM Model")
                                components["prompt_type"] = gr.Dropdown(choices=["Short Summary", "Detailed Notes", "Exam Q&A"], value="Detailed Notes", label="Study Tool")
                    
                    with gr.Row():
                        components["process_btn"] = gr.Button("🚀 Process Lecture", variant="primary", size="lg")
                        
                    with gr.Row():
                        with gr.Column():
                            gr.Markdown("### Transcript")
                            components["transcript_out"] = gr.Textbox(show_label=False, lines=16, interactive=True, placeholder="Transcription will appear here...")
                        with gr.Column():
                            gr.Markdown("### Study Notes")
                            components["summary_out"] = gr.Textbox(show_label=False, lines=16, interactive=True, placeholder="Waiting for transcript...")
                            
                # Models Page
                with gr.Group(visible=False) as page_models:
                    components["page_models"] = page_models
                    gr.HTML("""
                    <h1 class="page-title">Models</h1>
                    <p class="page-subtitle">Manage your local AI models and cache.</p>
                    """)
                    
                    with gr.Row():
                        components["refresh_btn"] = gr.Button("Refresh Status")
                    
                    with gr.Row():
                        components["models_display"] = gr.Textbox(label="Storage Used (MB)", lines=8, interactive=False)
                    
                    with gr.Row():
                        components["model_to_delete"] = gr.Dropdown(label="Cached Models", choices=[])
                        components["delete_btn"] = gr.Button("Delete Selected", variant="stop")
                        
    return app, components
