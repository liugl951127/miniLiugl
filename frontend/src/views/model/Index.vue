<!--
  @file model/Index.vue - 模型管理 V8.0 (router shell)
  拆分: 1100 行 / 3 tab → 3 子路由
  - /model/trained - 训练模型 (admin)
  - /model/local   - 本地推理服务器
  - /model/cloud   - 第三方模型配置
-->
<template>
  <div class="model-page">
    <PageStandard
      title="🤖 模型管理"
      subtitle="训练模型 / 本地推理 / 第三方服务商"
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
  </div>
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
    { path: '/model/local',   label: '本地模型',  icon: '🏠' },
    { path: '/model/cloud',   label: '第三方模型', icon: '☁️' }
  ]
  if (isAdmin.value) all.unshift({ path: '/model/trained', label: '训练模型', icon: '🧬' })
  return all
})

function isActive(path) {
  return route.path.startsWith(path)
}
</script>

<style scoped>
.model-page { background: white; border-radius: 12px; }
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
