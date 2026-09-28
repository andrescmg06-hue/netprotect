"""PostToolUse: ruff sobre el .py de backend recién editado. Nada más."""
import json, os, shutil, subprocess, sys

data = json.load(sys.stdin)
path = data.get("tool_input", {}).get("file_path", "")
root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
rel = os.path.relpath(os.path.abspath(path), root).replace("\\", "/") if path else ""
ruff = shutil.which("ruff")
if not (rel.startswith("backend/") and rel.endswith(".py") and ruff):
    sys.exit(0)

backend = os.path.join(root, "backend")
r = subprocess.run([ruff, "check", "--quiet", os.path.relpath(os.path.abspath(path), backend)],
                   cwd=backend, capture_output=True, text=True)
if r.returncode != 0:
    print(f"ruff en {rel}:\n{(r.stdout or r.stderr)[-2500:]}", file=sys.stderr)
    sys.exit(2)
sys.exit(0)
