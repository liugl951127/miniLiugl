#!/usr/bin/env bash
# ============================================================
# Git Push 脚本 (V5.33 Day 19)
# 用法: GITHUB_PAT="xxx" bash scripts/git-push.sh "msg" "date"
# ============================================================

set -e
cd "$(dirname "$0")/.."

MSG="${1:-feat: update}"
DATE="${2:-$(date +%Y-%m-%d)}"

git remote set-url origin "https://x-access-token:${GITHUB_PAT}@github.com/liugl951127/miniLiugl.git" 2>/dev/null || true

echo ">>> git status"
git status --short

echo ">>> git add ."
git add -A

echo ">>> git commit"
git commit -m "[${DATE}] ${MSG}" || { echo "Nothing to commit"; exit 0; }

echo ">>> git pull --rebase"
git pull --rebase origin master || git pull --rebase origin main || true

echo ">>> git push"
git push origin HEAD:master || git push origin HEAD:main

echo ">>> commit hash: $(git rev-parse HEAD)"
