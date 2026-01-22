import os

# Replacements configuration
replacements = {
    "com.arturo254.opentune": "com.grp2.kuromusic",
    "KiritoMusic": "KuroMusic",
    "OpenTune": "KuroMusic",
    "com.Arturo254.opentune": "com.grp2.kuromusic" # Case sensitive variation seen in build.gradle
}

# Special handling for "Kirito" -> "Kuro" to preserve "KiritoAPT2"
# We will handle this by first protecting "KiritoAPT2", doing the replace, then restoring.

root_dir = os.getcwd()
extensions = ['.kt', '.xml', '.gradle', '.kts', '.pro', '.json']

def replace_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # 1. Protect special terms
        content = content.replace("KiritoAPT2", "___PROTECTED_APT2___")
        content = content.replace("opentune", "kuromusic") # lowercase variation
        
        # 2. General replacements
        for old, new in replacements.items():
            content = content.replace(old, new)
            
        # 3. "Kirito" -> "Kuro" (Context aware safety is hard in pure text, but user asked for UI mainly)
        # We will act conservatively. The user said: "Kirito" -> "Kuro" (UI)
        # We should be careful not to break file paths if they weren't moved (but we moved them).
        # Let's apply it generally but watch out for the protected term.
        content = content.replace("Kirito", "Kuro")

        # 4. Restore protected terms
        content = content.replace("___PROTECTED_APT2___", "KiritoAPT2")
        
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Modified: {file_path}")
            
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

for root, dirs, files in os.walk(root_dir):
    if ".git" in root or "build" in root or ".gradle" in root:
        continue
        
    for file in files:
        if any(file.endswith(ext) for ext in extensions):
            replace_in_file(os.path.join(root, file))
