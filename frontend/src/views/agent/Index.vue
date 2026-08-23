<!--
  @file agent/Index.vue - Agent 编排 V7.6 (router shell)
  路由: /agent (默认 → /agent/tasks)

  V7.6 改造 (告别 1214 行 5 tab 摘要):
  - /agent/tasks    - 任务编排 (完整列表)
  - /agent/canvas   - Agent 画布 (已存在)
  - /agent/multi    - 多智能体协作 (已存在, GroupDesigner)
  - /agent/training - 训练可视化 (重定向 /training)
  - /agent/approval - Skill 审批 (完整列表)
-->
<template>
  <PageStandard
    title="🤖 Agent 编排"
    subtitle="任务 · 画布 · 群协作 · 训练 · 审批"
  >
    <div class="sub-nav">
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="sub-nav-item"
        :class="{ active: isActive(tab.path) }"
      >
        <span class="icon">{{ tab.icon }}</span>
        <span class="label">{{ tab.label }}</span>
      </router-link>
    </div>

    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </PageStandard>
</template>

<script setup>
import { useRoute } from 'vue-router'
import PageStandard from '@/components/PageStandard.vue'

const route = useRoute()

const tabs = [
  { path: '/agent/tasks',    label: '任务编排', icon: '🗂' },
  { path: '/agent/canvas',   label: '画布',     icon: '🎨' },
  { path: '/agent/multi',    label: '多智能体', icon: '🔀' },
  { path: '/agent/training', label: '训练',     icon: '📈' },
  { path: '/agent/approval', label: '审批',     icon: '✅' }
]

function isActive(path) {
  return route.path === path || (path === '/agent/tasks' && route.path === '/agent')
}
</script>

<style scoped>
.sub-nav {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 16px;
  width: fit-content;
  flex-wrap: wrap;
}
.sub-nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  color: #64748b;
  text-decoration: none;
  font-size: 0.9em;
  transition: all 0.2s;
}
.sub-nav-item:hover { background: rgba(255, 255, 255, 0.6); color: #1e293b; }
.sub-nav-item.active {
  background: white;
  color: #1e293b;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.sub-nav-item .icon { font-size: 1.1em; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
