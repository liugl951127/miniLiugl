<!--
  跨应用绑定组件 (V5.1)
  显示当前用户所有微信应用绑定 (unionid 关联)
  支持扫多个应用二维码, 自动识别为同一用户
-->
<template>
  <div class="cross-app-binding">
    <header class="header">
      <h3>🔗 跨应用账号绑定</h3>
      <p class="hint">同一微信开放平台下, 公众号 / 小程序 / App 自动识别为同一账号</p>
    </header>

    <div v-if="loading" class="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      加载中...
    </div>

    <div v-else-if="!data || data.length === 0" class="empty">
      <el-empty description="未绑定微信账号" />
    </div>

    <div v-else class="content">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>
          检测到 1 个 unionid, 已绑定 {{ totalBindings }} 个应用
        </template>
      </el-alert>

      <div v-for="user in data" :key="user.userId" class="user-card">
        <div class="user-header">
          <el-avatar :size="48" :src="user.avatar || ''">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="user-meta">
            <h4>{{ user.nickname || user.username }}</h4>
            <p class="user-id">@{{ user.username }} · ID: {{ user.userId }}</p>
            <p class="unionid">
              <code>unionid: {{ user.unionid }}</code>
            </p>
          </div>
        </div>

        <div class="bindings">
          <h5>📱 关联应用 ({{ user.bindings.length }})</h5>
          <el-row :gutter="12">
            <el-col v-for="b in user.bindings" :key="`${b.appType}-${b.openid}`" :span="8">
              <el-card shadow="hover" class="app-card">
                <div class="app-card-content">
                  <div class="app-icon">{{ getAppIcon(b.appType) }}</div>
                  <div class="app-info">
                    <div class="app-type">{{ getAppLabel(b.appType) }}</div>
                    <div class="app-nickname">{{ b.nickname || '未设置' }}</div>
                    <div class="app-time" v-if="b.lastLoginAt">
                      最近: {{ formatTime(b.lastLoginAt) }}
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>

      <el-divider />

      <div class="add-app">
        <h4>➕ 添加新应用绑定</h4>
        <p class="hint">扫码绑定同一微信的其他应用 (公众号/小程序/App)</p>
        <el-tabs v-model="addAppType">
          <el-tab-pane label="📱 小程序" name="mini">
            <p>用小程序前端调 <code>wx.login()</code> → POST /auth/wechat/mobile-login</p>
            <p>接口会自动识别 unionid, 复用当前账号</p>
          </el-tab-pane>
          <el-tab-pane label="📢 公众号" name="mp">
            <p>用微信内打开页面, 触发 OAuth 授权</p>
            <p>自动识别为同一 unionid</p>
          </el-tab-pane>
          <el-tab-pane label="🌐 网站应用" name="web">
            <el-button type="primary" @click="goScan">去扫码</el-button>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(true)
const data = ref([])

const totalBindings = computed(() => {
  if (!data.value || data.value.length === 0) return 0
  return data.value.reduce((sum, u) => sum + (u.bindings?.length || 0), 0)
})

const addAppType = ref('mini')

async function load() {
  loading.value = true
  try {
    const r = await http.get('/auth/wechat/unionid/me')
    data.value = r.data || []
  } catch (e) {
    console.warn('加载失败:', e.message)
  } finally {
    loading.value = false
  }
}

function getAppIcon(type) {
  return {
    mp: '📢', mini: '📱', open: '🌐', web: '💻'
  }[type] || '🔗'
}

function getAppLabel(type) {
  return {
    mp: '公众号', mini: '小程序', open: '开放平台', web: '网站应用'
  }[type] || type
}

function formatTime(t) { return dayjs(t).format('MM-DD HH:mm') }

function goScan() { router.push('/wechat') }

onMounted(load)
</script>

<style scoped>
.cross-app-binding { padding: 8px 0; }
.header h3 { margin: 0 0 4px; font-size: 18px; }
.hint { color: #64748b; font-size: 13px; margin: 0 0 16px; }
.loading, .empty { padding: 40px 0; text-align: center; color: #94a3b8; }

.user-card { background: #f8fafc; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.user-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.user-meta h4 { margin: 0 0 4px; }
.user-id { font-size: 12px; color: #94a3b8; margin: 0; }
.unionid code { font-size: 11px; background: #fff; padding: 2px 6px; border-radius: 3px; }

.bindings h5 { margin: 0 0 8px; font-size: 14px; }
.app-card { margin-bottom: 8px; }
.app-card-content { display: flex; align-items: center; gap: 12px; }
.app-icon { font-size: 24px; }
.app-type { font-weight: 600; font-size: 14px; }
.app-nickname { font-size: 12px; color: #64748b; margin-top: 2px; }
.app-time { font-size: 11px; color: #94a3b8; margin-top: 2px; }

.add-app h4 { margin: 16px 0 8px; }
.add-app .hint { font-size: 12px; }
</style>