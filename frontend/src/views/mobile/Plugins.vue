<template>
  <div class="m-plugins">
    <van-nav-bar title="🧩 插件市场" fixed :border="false" />

    <div class="content">
      <van-pull-refresh v-model="refreshing" @refresh="loadList">
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
              <div class="plugin-status">
                <van-tag v-if="installedSet.has(p.id)" type="success" size="small">已安装</van-tag>
                <van-tag v-else plain type="primary" size="small">未安装</van-tag>
              </div>
            </div>
          </van-grid-item>
        </van-grid>
        <van-loading v-if="loading" size="24px" style="text-align:center;padding:16px" />
        <van-empty v-if="!plugins.length && !loading" description="暂无插件" />
      </van-pull-refresh>
    </div>

    <van-popup v-model:show="showDetailPopup" position="bottom" round :style="{ height: '65%' }">
      <div v-if="detail" class="detail">
        <div class="detail-header">
          <div class="detail-icon">{{ iconOf(detail.category) }}</div>
          <div class="detail-info">
            <h2>{{ detail.displayName }}</h2>
            <div class="detail-tags">
              <van-tag type="primary">{{ detail.category }}</van-tag>
              <van-tag type="success">v{{ detail.version }}</van-tag>
              <van-tag type="warning" v-if="installedSet.has(detail.id)">✅ 已安装</van-tag>
            </div>
          </div>
        </div>

        <van-cell-group inset>
          <van-cell title="作者" :value="detail.author || '匿名'" />
          <van-cell title="下载量" :value="String(detail.downloads || 0)" />
          <van-cell title="评分" :value="`⭐ ${detail.rating || 0}`" />
        </van-cell-group>

        <div class="detail-desc" v-if="detail.description">
          <h4>简介</h4>
          <p>{{ detail.description }}</p>
        </div>

        <div class="detail-actions">
          <van-button
            v-if="!installedSet.has(detail.id)"
            type="primary"
            block
            :loading="actionLoading"
            @click="installPlugin(detail)"
          >
            <template #icon>
              <van-icon name="plus" />
            </template>
            安装插件
          </van-button>
          <van-button
            v-else
            plain
            type="danger"
            block
            :loading="actionLoading"
            @click="uninstallPlugin(detail)"
          >
            <template #icon>
              <van-icon name="delete-o" />
            </template>
            卸载插件
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import axios from 'axios'
import { useUserStore } from '@/store/user'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()

const plugins = ref<any[]>([])
const detail = ref<any>(null)
const showDetailPopup = ref(false)
const loading = ref(false)
const refreshing = ref(false)
const actionLoading = ref(false)
const installedSet = reactive(new Set<number>())

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
  loading.value = true
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/plugins`, auth())
    plugins.value = data.data || []
    // 模拟已安装列表
    installedSet.clear()
    if (plugins.value.length > 0) {
      installedSet.add(plugins.value[0]?.id)
    }
  } catch (e: any) {
    showToast('加载失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function installPlugin(p: any) {
  actionLoading.value = true
  try {
    await axios.post(`${API}/api/v1/agent/plugins/${p.id}/install`, {}, auth())
    installedSet.add(p.id)
    showToast({ message: '安装成功', position: 'bottom' })
  } catch (e: any) {
    showToast('安装失败')
  } finally {
    actionLoading.value = false
  }
}

async function uninstallPlugin(p: any) {
  try {
    await showConfirmDialog({ title: '提示', message: `确认卸载 ${p.displayName}?` })
    actionLoading.value = true
    await axios.post(`${API}/api/v1/agent/plugins/${p.id}/uninstall`, {}, auth())
    installedSet.delete(p.id)
    showToast({ message: '已卸载', position: 'bottom' })
  } catch (e: any) {
    if (e !== 'cancel') showToast('卸载失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.m-plugins { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 60px; min-height: calc(100vh - 110px); }
.plugin-card {
  background: white;
  border-radius: 12px;
  padding: 16px 8px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.plugin-icon { font-size: 36px; margin-bottom: 4px; }
.plugin-name { font-size: 13px; font-weight: 600; color: #303133; line-height: 1.3; }
.plugin-version { font-size: 11px; color: #909399; }
.plugin-stats { font-size: 11px; color: #606266; }
.plugin-status { margin-top: 4px; }
.detail { padding: 16px; height: 100%; display: flex; flex-direction: column; gap: 12px; }
.detail-header { display: flex; align-items: center; gap: 12px; }
.detail-icon { font-size: 48px; }
.detail-info h2 { margin: 0 0 6px; font-size: 18px; }
.detail-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.detail-desc h4 { margin: 0 0 6px; font-size: 14px; color: #303133; }
.detail-desc p { margin: 0; font-size: 13px; color: #606266; line-height: 1.6; }
.detail-actions { margin-top: auto; padding-top: 8px; }
</style>
