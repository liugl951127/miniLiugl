<!--
  @file knowledge/Index.vue - 知识中心 V7.6 (router shell)
  路由: /knowledge (默认 → /knowledge/list)
-->
<template>
  <PageStandard
    title="📚 知识中心"
    subtitle="知识库 · 检索 · 图谱 · 记忆"
  >
    <template #actions>
      <el-tag :type="kbs.length ? 'success' : 'info'" size="small">
        {{ kbs.length }} 个知识库
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
        <component :is="Component" :kbs="kbs" @kb-changed="loadKbs" />
      </transition>
    </router-view>
  </PageStandard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import PageStandard from '@/components/PageStandard.vue'
import { listMyKbs } from '@/api/rag'

const route = useRoute()

const tabs = [
  { path: '/knowledge/list',     label: '知识库',   icon: '📚' },
  { path: '/knowledge/kg',       label: '知识图谱', icon: '🕸️' },
  { path: '/knowledge/memory',   label: '记忆中心', icon: '🧠' }
]

const kbs = ref([])

function isActive(path) {
  return route.path === path || (path === '/knowledge/list' && route.path === '/knowledge')
}

async function loadKbs() {
  try {
    const res = await listMyKbs(0, { params: { page: 1, size: 100 } })
    if (res.code === 0) {
      kbs.value = res.data?.list || res.data || []
    }
  } catch (e) {
    console.error('loadKbs', e)
  }
}

onMounted(loadKbs)
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
