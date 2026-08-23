<!--
  @file settings/Index.vue - 系统管理 V8.0 (router shell)
  拆分: 754 行 / 7 tab → 4 子路由
  - /settings/users   - 用户 + 租户管理 (合并: 普通用户 + 管理员租户)
  - /settings/apikey  - API Key
  - /settings/audit   - 审计日志 + 运维统计 (合并: 运维 + 审计)
  - /settings/system  - 系统设置 + 系统监控 (admin only, 合并: 配置 + 监控)
-->
<template>
  <PageStandard
    title="⚙️ 系统管理"
    subtitle="用户 · 租户 · 密钥 · 审计 · 系统"
  >
    <div class="sub-nav">
      <router-link
        v-for="tab in tabs" :key="tab.path" :to="tab.path"
        class="sub-nav-item" :class="{ active: isActive(tab.path) }"
      >
        <span class="icon">{{ tab.icon }}</span>
        <span class="label">{{ tab.label }}</span>
        <el-tag v-if="tab.badge" :type="tab.badgeType || 'primary'" size="small" effect="plain" style="margin-left:4px">
          {{ tab.badge }}
        </el-tag>
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
import { useUserStore } from '@/store/user'
import { computed } from 'vue'
import PageStandard from '@/components/PageStandard.vue'

const route = useRoute()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isSuperAdmin)

const tabs = computed(() => {
  const all = [
    { path: '/settings/users',   label: '用户租户',  icon: '👥' },
    { path: '/settings/apikey',  label: 'API Key',    icon: '🔑' },
    { path: '/settings/audit',   label: '审计',      icon: '📋' }
  ]
  if (isAdmin.value) all.push({ path: '/settings/system', label: '系统', icon: '⚡' })
  return all
})

function isActive(path) {
  return route.path.startsWith(path)
}
</script>

<style scoped>
.sub-nav {
  display: flex; gap: 4px; background: #f1f5f9;
  border-radius: 10px; padding: 4px; margin-bottom: 16px; width: fit-content;
  flex-wrap: wrap;
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
