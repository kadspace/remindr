
import subprocess
import re

adb_path = r"C:\Users\wave\Documents\_code\_Crap\platform-tools-latest-windows\platform-tools\adb.exe"

def get_packages():
    cmd = [adb_path, "shell", "pm", "list", "packages", "-3"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    packages = []
    for line in result.stdout.splitlines():
        if line.startswith("package:"):
            packages.append(line.split(":")[1])
    return packages

def check_version(package):
    cmd = [adb_path, "shell", "dumpsys", "package", package]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if "versionName=1.6.8" in result.stdout:
        return True
    return False

def main():
    print("Getting packages...")
    pkgs = get_packages()
    print(f"Found {len(pkgs)} packages. Scanning for version 1.6.8...")
    
    for pkg in pkgs:
        if check_version(pkg):
            print(f"MATCH FOUND: {pkg}")
            return
            
    print("No match found.")

if __name__ == "__main__":
    main()
