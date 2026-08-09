from src.views import create_ui
from src.controllers import bind_events

def main():
    # 1. Initialize Views
    app, components = create_ui()
    
    # 2. Bind Controllers
    with app:
        bind_events(app, components)
    
    # 3. Launch App
    app.queue().launch()

if __name__ == "__main__":
    main()
