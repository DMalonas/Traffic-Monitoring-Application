import os

# File extensions to include
extensions = ['.java']

# Output file
output_file = 'merged_code.txt'

with open(output_file, 'w', encoding='utf-8') as out:
    for root, _, files in os.walk('.'):
        for file in files:
            if any(file.endswith(ext) for ext in extensions):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        out.write(f"\n\n==== {file_path} ====\n\n")
                        out.write(f.read())
                except Exception as e:
                    print(f"Could not read {file_path}: {e}")

print(f"\n✅ All code merged into {output_file}")
