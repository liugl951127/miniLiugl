<template>
  <div class="m-plugins">
    <van-nav-bar title="🧩 插件市场" fixed :border="false" />

    <div class="content">
      <van-grid :column-num="2" gutter="8" :border="false">
        <van-grid-item
          v-for="p in plugins"
          :key="p.id"
          @click="showDetail(p)"
        >
          <div class="plugin-card">
            <div class="plugin-icon">{{ iconOf(p.category) }}</div>
            <div class="plugin-name">{{ p.displayName }}</div>
            <div class="plugin-version">v{{ p.version }}</div>
            <div class="plugin-stats">⭐ {{ p.rating || 0 }} · 📥 {{ p.downloads || 0 }}</div>
          </div>
        </van-grid-item>
      </van-grid>
      <van-empty v-if="!plugins.length" description="加载中..." />
    </div>

    <van-popup v-model:show="showDetailPopup" position="bottom" round :style="{ height: '50%' }">
      <div v-if="detail" class="detail">
        <h2>{{ iconOf(detail.category) }} {{ detail.displayName }}</h2>
        <van-tag>{{ detail.category }}</van-tag>
        <van-tag type="success" style="margin-left:6px">v{{ detail.version }}</van-tag>
        <p v-if="detail.description">{{ detail.description }}</p>
        <p>作者: {{ detail.author || '匿名' }}</p>
        <p>下载量: {{ detail.downloads || 0 }}</p>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import axios from 'axios'
import { useUserStore } from '@/store/user'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()

const plugins = ref<any[]>([])
const detail = ref<any>(null)
const showDetailPopup = ref(false)

function auth() {
  return { headers: { Authorization: `Bearer ${userStore.accessToken}` } }
}

function iconOf(cat: string) {
  return ({ ui: '🎨', export: '📤', enhance: '✨', general: '🧩' } as any)[cat] || '🧩'
}

function showDetail(p: any) {
  detail.value = p
  showDetailPopup.value = true
}

async function loadList() {
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/plugins`, auth())
    plugins.value = data.data || []
  } catch (e: any) {
    showToast('加载失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.m-plugins { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 60px; }
.plugin-card {
  background: white;
  border-radius: 12px;
  padding: 16px 8px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.plugin-icon { font-size: 36px; margin-bottom: 6px; }
.plugin-name { font-size: 13px; font-weight: 600; color: #303133; }
.plugin-version { font-size: 11px; color: #909399; margin-top: 2px; }
.plugin-stats { font-size: 11px; color: #606266; margin-top: 4px; }
.detail { padding: 16px; }
.detail h2 { margin: 0 0 8px; }
</style>
