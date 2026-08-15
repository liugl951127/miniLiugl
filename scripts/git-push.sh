#!/usr/bin/env bash
# Git push 脚本 (Day 30)
# 用法: GITHUB_PAT=xxx bash scripts/git-push.sh "commit message" "2026-07-27"

set -e

BASE="/workspace/minimax-platform"
cd "$BASE"

GITHUB_PAT="${GITHUB_PAT}"
if [ -z "$GITHUB_PAT" ]; then
    echo "❌ GITHUB_PAT 环境变量未设置"
    exit 1
fi

COMMIT_MSG="${1:-feat(day-30): 智能化提升 V2}"
DATE="${2:-$(date +%Y-%m-%d)}"

echo "=== Git Push ==="
echo "Message: $COMMIT_MSG"
echo "Date: $DATE"

# 配置 git
git config user.email "bot@minimax.ai" 2>/dev/null || true
git config user.name "MiniMax Bot" 2>/dev/null || true

# 添加 token 到 remote（临时）
ORIGIN_URL=$(git remote get-url origin)
if [[ "$ORIGIN_URL" == https://github.com/* ]]; then
    NEW_URL="https://${GITHUB_PAT}@github.com/${ORIGIN_URL#https://github.com/}"
    git remote set-url origin "$NEW_URL"
fi

# add / commit
git add -A
git commit -m "$COMMIT_MSG" -m "Date: $DATE"

# pull --rebase
git pull --rebase origin main 2>&1 || {
    echo "⚠️  pull rebase 有冲突，手动处理中..."
    git status --short
}

# push
git push origin main

# 还原 origin（去掉 token）
if [[ "$ORIGIN_URL" == https://github.com/* ]]; then
    git remote set-url origin "$ORIGIN_URL"
fi

echo "✅ Push 完成！"
