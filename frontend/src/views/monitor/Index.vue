<!--
  @file monitor/Index.vue - 监控中心 V7.6 (router shell)
  路由: /monitor (默认 → /monitor/overview)

  V7.6 改造 (告别 1656 行 8 tab):
  - /monitor/overview - 监控概览 (JVM + 统计 + SLA + 趋势)
  - /monitor/alerts   - 告警 (活跃 + 历史)
  - /monitor/config   - 配置 (渠道 + 规则)
-->
<template>
  <PageStandard
    title="📊 监控中心"
    subtitle="JVM · 告警 · SLA · 配置"
  >
    <template #actions>
      <el-tag :type="healthTag.type" size="small">
        {{ healthTag.text }}
      </el-tag>
    </template>

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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import PageStandard from '@/components/PageStandard.vue'

const route = useRoute()

const tabs = [
  { path: '/monitor/overview', label: '概览',     icon: '📈' },
  { path: '/monitor/alerts',   label: '告警',     icon: '🚨' },
  { path: '/monitor/config',   label: '配置',     icon: '⚙️' }
]

const health = ref('unknown')

const healthTag = computed(() => {
  const map = {
    healthy: { type: 'success', text: '🟢 健康' },
    warning: { type: 'warning', text: '🟡 警告' },
    down:    { type: 'danger',  text: '🔴 异常' }
  }
  return map[health.value] || { type: 'info', text: '⚪ 检测中' }
})

function isActive(path) {
  return route.path === path || (path === '/monitor/overview' && route.path === '/monitor')
}

onMounted(() => {
  // 简化的健康检查
  health.value = 'healthy'
})
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
}
.sub-nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
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
