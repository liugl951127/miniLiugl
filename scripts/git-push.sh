#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - GitHub 推送脚本（稳健版）
# - 远端 main 不存在（首次 push）→ 跳过 pull
# - 远端 main 存在但本地有落后 → pull --ff-only（安全快进）
# - 远端 main 存在但本地分叉 → 警告，**不**自动 rebase（避免冲突掩盖）
# =============================================================
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

REMOTE_URL="https://github.com/liugl951127/miniLiugl.git"
BRANCH="main"
COMMIT_MSG="${1:-chore: auto update by Mavis}"
DAY="${2:-$(date '+%Y-%m-%d')}"

# 1. 凭证检查
if [ -z "${GITHUB_PAT:-}" ]; then
    echo "[FATAL] GITHUB_PAT 未设置" >&2
    exit 1
fi
AUTH_URL="https://x-access-token:${GITHUB_PAT}@github.com/liugl951127/miniLiugl.git"

# 2. remote
if ! git remote get-url origin >/dev/null 2>&1; then
    git remote add origin "$AUTH_URL"
    echo "[INFO] 添加 origin"
else
    git remote set-url origin "$AUTH_URL"
fi

# 3. 主分支
git branch -M "$BRANCH" 2>/dev/null || true

# 4. 远端是否存在 main？
REMOTE_HEAD=$(git ls-remote --heads origin "$BRANCH" 2>/dev/null | awk '{print $1}')
if [ -z "$REMOTE_HEAD" ]; then
    echo "[INFO] 远端 $BRANCH 不存在（首次 push），跳过 pull"
    NEED_PULL=0
else
    # 5. 远端 main 存在：检查是否需要 pull
    git fetch origin "$BRANCH" 2>&1 | tail -2 || true
    LOCAL_HEAD=$(git rev-parse HEAD 2>/dev/null || echo "")
    if [ "$LOCAL_HEAD" = "$REMOTE_HEAD" ]; then
        echo "[INFO] 本地与远端一致，无需 pull"
        NEED_PULL=0
    else
        # 检查是否分叉
        MERGE_BASE=$(git merge-base HEAD "origin/$BRANCH" 2>/dev/null || echo "")
        if [ "$MERGE_BASE" = "$REMOTE_HEAD" ]; then
            # 远端在本地之前 → 落后，可快进
            echo "[INFO] 本地落后远端，执行 fast-forward pull"
            git pull --ff-only origin "$BRANCH" 2>&1 | tail -2
            NEED_PULL=0
        elif [ "$MERGE_BASE" = "$LOCAL_HEAD" ]; then
            # 本地在远端之前 → 领先，无需 pull
            echo "[INFO] 本地领先远端，无需 pull"
            NEED_PULL=0
        else
            # 分叉了 → 不自动合并，停止
            echo "[FATAL] 本地与远端分叉！" >&2
            echo "        本地: $LOCAL_HEAD" >&2
            echo "        远端: $REMOTE_HEAD" >&2
            echo "        请人工处理冲突后再 push" >&2
            exit 2
        fi
    fi
fi

# 6. add + commit
git add -A
if git diff --cached --quiet; then
    echo "[INFO] 无暂存变更，跳过 commit"
else
    git commit -m "$COMMIT_MSG" -m "Day: $DAY | Co-authored-by: Mavis <liugl951127@gmail.com>" || true
    echo "[OK] commit 完成"
fi

# 7. push
git push -u origin "$BRANCH" 2>&1
PUSH_EXIT=$?
if [ $PUSH_EXIT -eq 0 ]; then
    NEW_HEAD=$(git rev-parse HEAD)
    echo "[OK] push 成功 · $NEW_HEAD → $REMOTE_URL"
else
    echo "[FATAL] push 失败，退出码 $PUSH_EXIT" >&2
    exit $PUSH_EXIT
fi
