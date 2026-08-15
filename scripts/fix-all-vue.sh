#!/bin/bash
cd /workspace/miniLiugl
files=(
  "frontend/src/views/About.vue"
  "frontend/src/views/admin/Governance.vue"
  "frontend/src/views/admin/Leaderboard.vue"
  "frontend/src/views/admin/WechatBindings.vue"
  "frontend/src/views/admin/WechatUnionidAdmin.vue"
  "frontend/src/views/agent/Multi.vue"
  "frontend/src/views/agent/Stream.vue"
  "frontend/src/views/ai/AiChat.vue"
  "frontend/src/views/ai/AutoAgentGroup.vue"
  "frontend/src/views/ai/ImageGen.vue"
  "frontend/src/views/ai/ModelMarket.vue"
  "frontend/src/views/ai/TensorBoard.vue"
  "frontend/src/views/ai/TensorBoardStats.vue"
  "frontend/src/views/ai/WebhookManager.vue"
  "frontend/src/views/analytics/Reports.vue"
  "frontend/src/views/apikey/Stats.vue"
  "frontend/src/views/auth/WechatScanPage.vue"
  "frontend/src/views/collab/Index.vue"
  "frontend/src/views/memory/Index.vue"
  "frontend/src/views/plugins/Index.vue"
  "frontend/src/views/prompts/Index.vue"
  "frontend/src/views/showcase/AudioShowcase.vue"
  "frontend/src/views/showcase/DagShowcase.vue"
  "frontend/src/views/showcase/ImageGenShowcase.vue"
  "frontend/src/views/showcase/LeaderboardShowcase.vue"
  "frontend/src/views/showcase/Liugl-AIShowcase.vue"
  "frontend/src/views/showcase/PluginShowcase.vue"
  "frontend/src/views/showcase/SingleChatPlayground.vue"
  "frontend/src/views/showcase/StreamShowcase.vue"
  "frontend/src/views/showcase/VideoGenShowcase.vue"
)

fixed=0
for f in "${files[@]}"; do
  # 找干净 commit
  for commit in $(git log --all --oneline | awk '{print $1}' | head -300); do
    content=$(git show $commit:$f 2>/dev/null)
    if [ -n "$content" ]; then
      if echo "$content" | grep -q "<template>"; then
        if ! echo "$content" | grep -qE 'class="[a-zA-Z][^"]*[a-zA-Z]>$'; then
          git checkout $commit -- $f
          echo "✓ $f ← $commit"
          fixed=$((fixed+1))
          break
        fi
      fi
    fi
  done
done
echo "Fixed: $fixed / ${#files[@]}"
