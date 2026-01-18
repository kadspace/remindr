
import os
from PIL import Image

# Paths
source_logo = r"c:\Users\wave\Documents\_code\remindr\remindr logo.png"
res_dir = r"c:\Users\wave\Documents\_code\remindr\Calendar\compose-multiplatform\sample\src\androidMain\res"

# Dimensions for each density
# mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

def update_icons():
    if not os.path.exists(source_logo):
        print(f"Error: Source logo not found at {source_logo}")
        return

    try:
        img = Image.open(source_logo)
        print("Loaded source logo.")
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    for folder, size in densities.items():
        target_dir = os.path.join(res_dir, folder)
        if not os.path.exists(target_dir):
            print(f"Warning: Directory {target_dir} does not exist. Skipping.")
            continue
        
        # Resize
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save ic_launcher.png
        target_path = os.path.join(target_dir, "ic_launcher.png")
        resized_img.save(target_path, "PNG")
        print(f"Saved {target_path}")
        
        # Save ic_launcher_round.png used by API 26+ usually, or we can just overwrite if it exists
        target_round_path = os.path.join(target_dir, "ic_launcher_round.png")
        # For simplicity, using same square-ish icon, usually round icon should be circular masked
        # But if the source logo is already suitable, we just save it.
        # Android adapts it.
        resized_img.save(target_round_path, "PNG")
        print(f"Saved {target_round_path}")

if __name__ == "__main__":
    update_icons()
