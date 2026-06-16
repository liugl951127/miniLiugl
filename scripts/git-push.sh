#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - GitHub 推送脚本
# 把工作目录的代码 add/commit/push 到远程 main 分支
# =============================================================
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

REMOTE_URL="https://github.com/liugl951127/miniLiugl.git"
BRANCH="main"
COMMIT_MSG="${1:-chore: auto update by Mavis}"
DAY="${2:-$(date '+%Y-%m-%d')}"

# 1. 拿 PAT
if [ -z "${GITHUB_PAT:-}" ]; then
    echo "[FATAL] GITHUB_PAT 未设置" >&2
    exit 1
fi

# 2. 配 remote（用 token 走 https）
AUTH_URL="https://x-access-token:${GITHUB_PAT}@github.com/liugl951127/miniLiugl.git"

if ! git remote get-url origin >/dev/null 2>&1; then
    git remote add origin "$AUTH_URL"
    echo "[INFO] 添加 origin"
else
    git remote set-url origin "$AUTH_URL"
    echo "[INFO] 更新 origin URL"
fi

# 3. 确保是 main 分支
git branch -M "$BRANCH" 2>/dev/null || true

# 4. 检查 .gitignore 健在
if [ ! -f .gitignore ]; then
    cat > .gitignore <<'EOF'
# 构建产物
node_modules/
target/
dist/
build/
out/
*.log
logs/*.log

# IDE
.idea/
.vscode/
*.iml
*.ipr
*.iws

# 系统
.DS_Store
Thumbs.db

# OS 临时
*.swp
*.bak
*~
EOF
fi

# 5. 先 add
git add -A

# 6. 如果有暂存，才 commit
if git diff --cached --quiet; then
    echo "[INFO] 无变更，跳过 commit"
else
    git commit -m "$COMMIT_MSG" -m "Day: $DAY | Co-authored-by: Mavis <liugl951127@gmail.com>" || true
    echo "[OK] commit 完成"
fi

# 7. pull --rebase 防止远端有别人提交
git pull --rebase --autostash origin "$BRANCH" 2>&1 || {
    echo "[WARN] pull --rebase 失败，尝试普通 pull"
    git pull origin "$BRANCH" 2>&1 || true
}

# 8. push
git push -u origin "$BRANCH" 2>&1
echo "[OK] push 完成 → $REMOTE_URL"
