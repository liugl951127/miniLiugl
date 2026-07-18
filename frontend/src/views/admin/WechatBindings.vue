<!--
  微信绑定管理 (V5)
  adminLiugl 专属
  - 列出全部绑定
  - 按 openid 查找
  - 强制绑定/解绑
  - 用户侧: 我的绑定
-->
<!--
  @file views/admin/WechatBindings.vue (微信绑定管理)
  @version V3.5.12+ (前端注释补全)
  @description 微信绑定管理
-->
<template>
  <div class="wechat-bindings">
    <header class="header">
      <h1>📱 微信绑定管理</h1>
      <p class="subtitle">adminLiugl 专属: 管理用户与微信 openid 的绑定关系</p>
    </header>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 全部绑定 -->
      <el-tab-pane label="📋 全部绑定" name="all">
        <div class="filter-bar">
          <el-input v-model="filterKw" placeholder="搜索用户名/openid" clearable
                    style="width: 320px" @clear="loadAll" @keyup.enter="loadAll" />
          <el-button type="primary" @click="loadAll">刷新</el-button>
        </div>
        <el-table :data="bindings" stripe v-loading="loading">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="用户" min-width="200">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="user-name">{{ row.user_nickname || row.username }}</span>
                <span class="user-id">@{{ row.username }} (id={{ row.user_id }})</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="openid" label="OpenID" min-width="200">
            <template #default="{ row }">
              <code class="code">{{ row.openid }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="unionid" label="UnionID" min-width="180">
            <template #default="{ row }">
              <code v-if="row.unionid" class="code">{{ row.unionid }}</code>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="app_type" label="App" width="80">
            <template #default="{ row }">
              <el-tag size="small">{{ row.app_type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="绑定时间" width="170">
            <template #default="{ row }">
              {{ row.bound_at ? formatTime(row.bound_at) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain
                         @click="confirmUnbind(row)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 按 openid 查找 -->
      <el-tab-pane label="🔍 按 OpenID 查找" name="find">
        <div class="find-form">
          <el-input v-model="searchOpenid" placeholder="输入 openid (完整)" style="width: 480px"
                    @keyup.enter="doFind" />
          <el-button type="primary" :loading="finding" @click="doFind">查找</el-button>
        </div>
        <div v-if="foundResult" class="find-result">
          <el-alert v-if="!foundResult.found" type="warning" :closable="false" show-icon>
            <template #title>未找到该 openid 的绑定记录</template>
          </el-alert>
          <el-card v-else shadow="hover">
            <template #header>
              <span>找到绑定 ✓</span>
            </template>
            <div class="kv">
              <span class="k">用户 ID</span><span class="v">{{ foundResult.userId }}</span>
            </div>
            <div class="kv">
              <span class="k">用户名</span><span class="v">{{ foundResult.username }}</span>
            </div>
            <div class="kv">
              <span class="k">昵称</span><span class="v">{{ foundResult.nickname }}</span>
            </div>
            <div class="kv">
              <span class="k">绑定信息</span>
              <pre class="code">{{ JSON.stringify(foundResult.binding, null, 2) }}</pre>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 强制绑定 -->
      <el-tab-pane label="➕ 强制绑定" name="force">
        <el-form :model="bindForm" label-width="120px" style="max-width: 600px">
          <el-form-item label="目标用户 ID" required>
            <el-input v-model="bindForm.userId" placeholder="如 2" type="number" />
          </el-form-item>
          <el-form-item label="OpenID" required>
            <el-input v-model="bindForm.openid" placeholder="wx 用户的 openid" />
          </el-form-item>
          <el-form-item label="UnionID (可选)">
            <el-input v-model="bindForm.unionid" placeholder="跨应用唯一标识" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="bindForm.nickname" placeholder="默认 '微信用户'" />
          </el-form-item>
          <el-form-item label="头像 URL">
            <el-input v-model="bindForm.avatar" placeholder="https://..." />
          </el-form-item>
          <el-form-item label="App 类型">
            <el-select v-model="bindForm.appType" style="width: 100%">
              <el-option label="mp (公众号)" value="mp" />
              <el-option label="mini (小程序)" value="mini" />
              <el-option label="open (开放平台)" value="open" />
              <el-option label="web (网页应用)" value="web" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="binding" @click="doBind">强制绑定</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import dayjs from 'dayjs'

const activeTab = ref('all')
const loading = ref(false)
const bindings = ref([])
const filterKw = ref('')

const searchOpenid = ref('')
const finding = ref(false)
const foundResult = ref(null)

const bindForm = ref({
  userId: '', openid: '', unionid: '', nickname: '', avatar: '', appType: 'mp'
})
const binding = ref(false)

async function loadAll() {
  loading.value = true
  try {
    const r = await http.get('/auth/admin/wechat/bindings', { params: { limit: 100 } })
    let rows = r.data || []
    if (filterKw.value) {
      const kw = filterKw.value.toLowerCase()
      rows = rows.filter(x =>
        (x.username || '').toLowerCase().includes(kw) ||
        (x.openid || '').toLowerCase().includes(kw) ||
        (x.user_nickname || '').toLowerCase().includes(kw)
      )
    }
    bindings.value = rows
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function doFind() {
  if (!searchOpenid.value.trim()) {
    ElMessage.warning('请输入 openid')
    return
  }
  finding.value = true
  try {
    const r = await http.get('/auth/admin/wechat/find', { params: { openid: searchOpenid.value.trim() } })
    foundResult.value = r.data
  } catch (e) {
    ElMessage.error('查找失败: ' + e.message)
  } finally {
    finding.value = false
  }
}

async function doBind() {
  if (!bindForm.value.userId || !bindForm.value.openid) {
    ElMessage.warning('用户 ID 和 OpenID 必填')
    return
  }
  binding.value = true
  try {
    await http.post('/auth/admin/wechat/bind', bindForm.value)
    ElMessage.success('绑定成功')
    bindForm.value = { userId: '', openid: '', unionid: '', nickname: '', avatar: '', appType: 'mp' }
    loadAll()
  } catch (e) {
    ElMessage.error('绑定失败: ' + e.message)
  } finally {
    binding.value = false
  }
}

async function confirmUnbind(row) {
  try {
    await ElMessageBox.confirm(
      `解绑 ${row.username} 的微信 (${row.openid})?`,
      '确认解绑',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await http.delete(`/auth/admin/wechat/bind/${row.user_id}`)
    ElMessage.success('已解绑')
    loadAll()
  } catch (e) {
    ElMessage.error('解绑失败: ' + e.message)
  }
}

function formatTime(t) { return dayjs(t).format('YYYY-MM-DD HH:mm:ss') }

onMounted(loadAll)
</script>

<style scoped>
.wechat-bindings { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 28px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 16px; }

.filter-bar { display: flex; gap: 8px; margin-bottom: 16px; }

.user-cell { display: flex; flex-direction: column; }
.user-name { font-weight: 600; }
.user-id { font-size: 12px; color: #94a3b8; }
.code { font-family: 'SF Mono', Menlo, monospace; font-size: 12px; background: #f1f5f9; padding: 2px 4px; border-radius: 3px; word-break: break-all; }
.muted { color: #cbd5e1; }

.find-form { display: flex; gap: 8px; margin-bottom: 16px; }
.find-result { margin-top: 16px; }
.kv { display: flex; padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
.kv .k { width: 100px; color: #64748b; }
.kv .v { flex: 1; font-weight: 600; }
</style>
