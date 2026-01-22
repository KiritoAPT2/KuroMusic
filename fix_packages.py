import os

# Replacements configuration
replacements = {
    "com.arturo254.kuromusic": "com.grp2.kuromusic",
    "com.arturo254.opentune": "com.grp2.kuromusic",
    "import com.arturo254.": "import com.grp2.kuromusic.", # Catch-all for imports if needed, but be careful
}

root_dir = os.getcwd()
extensions = ['.kt', '.xml', '.gradle', '.kts']

def replace_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # General fix for fully qualified names and package/imports
        content = content.replace("com.arturo254.kuromusic", "com.grp2.kuromusic")
        content = content.replace("com.arturo254.opentune", "com.grp2.kuromusic")
        
        # Ensure we didn't break innertube imports (just in case they were affected by opentune replace earlier, but unlikely)
        # But we must be careful. 
        # "com.arturo254.opentune" -> "com.grp2.kuromusic" is safe.
        # "com.arturo254.kuromusic" -> "com.grp2.kuromusic" is safe.

        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Fixed: {file_path}")
            
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

for root, dirs, files in os.walk(root_dir):
    if ".git" in root or "build" in root or ".gradle" in root:
        continue
    
    for file in files:
        if any(file.endswith(ext) for ext in extensions):
            replace_in_file(os.path.join(root, file))
