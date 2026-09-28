"""PreToolUse: bloquea ediciones a archivos que nunca debe tocar un agente,
y a archivos delegados a otra tarea (p. ej. DeepSeek/OpenCode) en curso."""
import json, os, subprocess, sys

data = json.load(sys.stdin)
path = data.get("tool_input", {}).get("file_path", "")
if not path:
    sys.exit(0)
root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
rel = os.path.relpath(os.path.abspath(path), root).replace("\\", "/")

THIRD_PARTY = ("impeccable", "animate", "animation-vocabulary", "apple-design", "emil-design-eng",
               "find-animation-opportunities", "improve-animations", "pick-ui-library",
               "review-animations")

def tracked(p):
    env = {**os.environ, "GIT_OPTIONAL_LOCKS": "0"}
    r = subprocess.run(["git", "ls-files", "--error-unmatch", p], cwd=root, env=env,
                       capture_output=True)
    return r.returncode == 0

def delegated_reason(rel_path):
    lock_path = os.path.join(root, ".claude", "delegate-lock.json")
    if not os.path.exists(lock_path):
        return None
    try:
        lock = json.load(open(lock_path, encoding="utf-8"))
    except Exception:
        return None
    for tarea in lock.get("tareas", []):
        if tarea.get("estado") != "en-progreso":
            continue
        if rel_path not in tarea.get("rutas", []):
            continue
        wt = tarea.get("worktree", "")
        abs_wt = os.path.normpath(os.path.join(root, wt)) if wt else None
        if abs_wt != os.path.normpath(root):
            return (f"Delegado a DeepSeek en la tarea '{tarea.get('id')}' "
                     f"(worktree {wt}). Espera a que cierre, o libérala editando "
                     f".claude/delegate-lock.json.")
    return None

reason = delegated_reason(rel)
if reason is None:
    if rel == ".env" or (rel.startswith(".env.") and not rel.endswith(".example")):
        reason = "El .env real no se edita desde Claude. Cambia los .env.*.example."
    elif rel.startswith("secrets/") and rel not in ("secrets/README.md", "secrets/generate-dev-secrets.sh"):
        reason = "secrets/ contiene valores reales de despliegue."
    elif rel.startswith("backend/alembic/versions/") and tracked(rel):
        reason = "Migración ya versionada: no se edita. Crea una nueva con `make revision m=...`."
    elif rel == "frontend/AGENTS.md":
        reason = "Lo regenera `next dev`; editarlo solo produce diffs que vuelven."
    elif rel.endswith(("local.properties", "google-services.json")):
        reason = "Configuración local/credenciales de Android."
    elif rel.startswith(".claude/agents/impeccable-") or any(
            rel.startswith(f".claude/skills/{s}/") for s in THIRD_PARTY):
        reason = "Skill/agente de terceros: se reinstala y pisaría el cambio."

if reason:
    print(f"Bloqueado: {rel}. {reason}", file=sys.stderr)
    sys.exit(2)
sys.exit(0)
