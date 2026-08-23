<!--
  @file builder/Index.vue - Agent Forge V1.0 (router shell)
  5 步流水线: 需求 → 解析 → 设计 → 部署 → 监控
-->
<template>
  <div class="builder-page">
    <!-- 顶部品牌 + 5 步进度 -->
    <div class="forge-header">
      <div class="forge-brand">
        <div class="forge-logo">🔥</div>
        <div>
          <h1 class="forge-title">Agent Forge</h1>
          <p class="forge-subtitle">从客户需求到生产部署 · 一站式智能体群流水线</p>
        </div>
      </div>
      <div class="step-pills">
        <router-link
          v-for="(s, i) in steps" :key="s.path" :to="s.path"
          class="step-pill" :class="{ active: isActive(s.path), done: isDone(i) }"
        >
          <span class="step-num">{{ doneIdx > i ? '✓' : i + 1 }}</span>
          <span class="step-label">{{ s.label }}</span>
        </router-link>
      </div>
    </div>

    <router-view v-slot="{ Component }">
      <transition name="forge-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const steps = [
  { path: '/builder/requirements', label: '需求' },
  { path: '/builder/analysis',    label: 'AI 解析' },
  { path: '/builder/designer',    label: '团队设计' },
  { path: '/builder/deploy',      label: '远程部署' },
  { path: '/builder/monitor',     label: '实时监控' }
]

const currentIdx = computed(() => {
  const idx = steps.findIndex(s => route.path.startsWith(s.path))
  return idx === -1 ? 0 : idx
})
const doneIdx = computed(() => currentIdx.value)

function isActive(path) { return route.path.startsWith(path) }
function isDone(i) { return i < doneIdx.value }

import { computed } from 'vue'
</script>

<style scoped>
.builder-page {
  min-height: calc(100vh - 88px);
  background:
    radial-gradient(at 0% 0%, rgba(99, 102, 241, 0.08) 0px, transparent 40%),
    radial-gradient(at 100% 100%, rgba(236, 72, 153, 0.06) 0px, transparent 40%),
    linear-gradient(180deg, #fafbfc 0%, #f1f5f9 100%);
  padding: 24px 32px;
}

.forge-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px; flex-wrap: wrap; gap: 16px;
}
.forge-brand { display: flex; align-items: center; gap: 14px; }
.forge-logo {
  width: 56px; height: 56px; border-radius: 14px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
}
.forge-title {
  font-size: 26px; font-weight: 800; margin: 0; letter-spacing: -0.5px;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.forge-subtitle { margin: 2px 0 0; font-size: 13px; color: #64748b; }

.step-pills {
  display: flex; gap: 4px; background: white;
  border-radius: 14px; padding: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
}
.step-pill {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; border-radius: 10px;
  color: #64748b; text-decoration: none; font-size: 13px;
  font-weight: 500; transition: all 0.2s;
  position: relative;
}
.step-pill:hover { background: #f8fafc; color: #1e293b; }
.step-pill.active {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white; font-weight: 600;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}
.step-pill.done .step-num {
  background: #10b981; color: white;
}
.step-num {
  width: 20px; height: 20px; border-radius: 50%;
  background: #e2e8f0; color: #64748b;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
}
.step-pill.active .step-num { background: rgba(255,255,255,0.25); color: white; }

.forge-fade-enter-active, .forge-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.forge-fade-enter-from { opacity: 0; transform: translateY(8px); }
.forge-fade-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
