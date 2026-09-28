"""Stop: chequeos baratos solo si hubo cambios desde el último chequeo."""
import hashlib, json, os, shutil, subprocess, sys

data = json.load(sys.stdin)
if data.get("stop_hook_active"):
    sys.exit(0)

root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
env = {**os.environ, "GIT_OPTIONAL_LOCKS": "0"}
def git(*a):
    return subprocess.run(["git", *a], cwd=root, env=env, capture_output=True, text=True).stdout

changed = sorted(set(filter(None, (git("diff", "--name-only", "HEAD") +
                                   git("ls-files", "--others", "--exclude-standard")).splitlines())))
if not changed:
    sys.exit(0)

sig = hashlib.sha256("".join(
    f"{p}:{os.path.getmtime(os.path.join(root, p)) if os.path.exists(os.path.join(root, p)) else 0}"
    for p in changed).encode()).hexdigest()
cache = os.path.join(root, ".claude", ".cache", "last_stop_check")
os.makedirs(os.path.dirname(cache), exist_ok=True)
if os.path.exists(cache) and open(cache).read() == sig:
    sys.exit(0)

errors = []
if any(p.startswith("backend/") and p.endswith(".py") for p in changed) and shutil.which("ruff"):
    r = subprocess.run(["ruff", "check", "--quiet", "app", "tests", "alembic"],
                       cwd=os.path.join(root, "backend"), capture_output=True, text=True)
    if r.returncode:
        errors.append("ruff (backend):\n" + r.stdout[-2000:])

fe = [p[len("frontend/"):] for p in changed
      if p.startswith("frontend/") and p.endswith((".ts", ".tsx"))
      and os.path.exists(os.path.join(root, p))]
npx = shutil.which("npx")
if fe and npx:
    r = subprocess.run([npx, "eslint", "--max-warnings=0", *fe],
                       cwd=os.path.join(root, "frontend"), capture_output=True, text=True)
    if r.returncode:
        errors.append("eslint (archivos cambiados):\n" + r.stdout[-2500:])

if errors:
    print("\n\n".join(errors) + "\n\nCorrígelo antes de terminar.", file=sys.stderr)
    sys.exit(2)

open(cache, "w").write(sig)
if any(p.startswith("mobile/") for p in changed):
    print(json.dumps({"systemMessage":
        "Hubo cambios en mobile/: antes de darlos por buenos, verificar con `verifier` (compileDebugKotlin + lintDebug)."}))
sys.exit(0)
