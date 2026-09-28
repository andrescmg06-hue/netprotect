"""SessionStart: contexto mínimo tras abrir, reanudar o compactar."""
import os, re, subprocess

root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
env = {**os.environ, "GIT_OPTIONAL_LOCKS": "0"}
run = lambda *a: subprocess.run(a, cwd=root, env=env, capture_output=True, text=True).stdout.strip()

branch = run("git", "branch", "--show-current") or "?"
dirty = len(run("git", "status", "--short").splitlines())
print(f"[NetProtect] rama `{branch}` · {dirty} archivo(s) sin commit")

p = os.path.join(root, "docs", "progress.md")
if os.path.exists(p):
    text = open(p, encoding="utf-8").read()
    for title in ("En curso", "Siguiente paso"):
        m = re.search(rf"^## {title}\n(.*?)(?=^## |\Z)", text, re.S | re.M)
        if m:
            lines = m.group(1).strip().splitlines()[:15]
            print(f"\n## {title}\n" + "\n".join(lines))
