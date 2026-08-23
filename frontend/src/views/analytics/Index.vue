<!--
  @file analytics/Index.vue - 数据分析 V8.0 (router shell)
  拆分: 1097 行 / 3 tab → 3 子路由
  - /analytics/overview - 总览 (调用量/趋势/分布/Top用户)
  - /analytics/nlsql    - NL2SQL 自然语言查询
  - /analytics/vote     - 多模型投票
-->
<template>
  <PageStandard
    title="📊 数据分析"
    subtitle="调用统计 · NL2SQL · 多模型投票"
  >
    <div class="sub-nav">
      <router-link
        v-for="tab in tabs" :key="tab.path" :to="tab.path"
        class="sub-nav-item" :class="{ active: isActive(tab.path) }"
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
  { path: '/analytics/overview', label: '总览',   icon: '📈' },
  { path: '/analytics/nlsql',    label: 'NL2SQL', icon: '💬' },
  { path: '/analytics/vote',     label: '多模型投票', icon: '🗳️' }
]

function isActive(path) {
  return route.path === path
}
</script>

<style scoped>
.sub-nav {
  display: flex; gap: 4px; background: #f1f5f9;
  border-radius: 10px; padding: 4px; margin-bottom: 16px; width: fit-content;
}
.sub-nav-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; border-radius: 8px;
  color: #64748b; text-decoration: none; font-size: 0.9em; transition: all 0.2s;
}
.sub-nav-item:hover { background: rgba(255, 255, 255, 0.6); color: #1e293b; }
.sub-nav-item.active {
  background: white; color: #1e293b; font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.sub-nav-item .icon { font-size: 1.1em; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
