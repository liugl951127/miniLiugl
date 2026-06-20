<!--
  unionid 跨应用管理后台 (V5.1)
  - 列出所有 unionid 关联
  - 按 unionid 查用户
  - 手动合并账号
-->
<template>
  <div class="unionid-admin">
    <header class="header">
      <h1>🔗 unionid 跨应用管理</h1>
      <p class="subtitle">adminLiugl 专属: 管理跨应用账号打通 + 合并重复账号</p>
    </header>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 列出所有 unionid 关联 -->
      <el-tab-pane label="📊 所有 unionid 关联" name="all">
        <div class="filter-bar">
          <el-input v-model="filterKw" placeholder="搜索用户名/unionid" clearable
                    style="width: 360px" @keyup.enter="loadAll" @clear="loadAll" />
          <el-button type="primary" @click="loadAll">刷新</el-button>
        </div>
        <el-table :data="filteredRels" stripe v-loading="loading" @row-click="showDetail">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="用户" min-width="200">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="user-name">{{ row.nickname || row.username }}</span>
                <span class="user-id">@{{ row.username }} (id={{ row.userId }})</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="UnionID" min-width="200">
            <template #default="{ row }">
              <code class="code">{{ row.unionid }}</code>
            </template>
          </el-table-column>
          <el-table-column label="平台" width="80">
            <template #default="{ row }">
              <el-tag size="small">{{ row.platform }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="应用数" width="80" prop="bindingCount" />
          <el-table-column label="首次 / 最近" width="260">
            <template #default="{ row }">
              <div style="font-size: 12px">
                <div>首次: {{ formatTime(row.firstSeenAt) }}</div>
                <div style="color: #64748b">最近: {{ formatTime(row.lastSeenAt) }}</div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 按 unionid 查 -->
      <el-tab-pane label="🔍 按 UnionID 查" name="find">
        <div class="find-form">
          <el-input v-model="searchUnionid" placeholder="输入完整 unionid" style="width: 520px"
                    @keyup.enter="doFind" />
          <el-button type="primary" :loading="finding" @click="doFind">查找</el-button>
        </div>
        <div v-if="foundUsers && foundUsers.length > 0" class="find-result">
          <el-alert type="success" :closable="false" show-icon style="margin-bottom: 16px">
            <template #title>找到 {{ foundUsers.length }} 个用户, 共享同一 unionid</template>
          </el-alert>
          <div v-for="u in foundUsers" :key="u.userId" class="user-card">
            <div class="user-header">
              <el-avatar :size="48" :src="u.avatar || ''">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div>
                <h4>{{ u.nickname }} <span class="user-id">(@{{ u.username }} id={{ u.userId }})</span></h4>
                <p class="status" :class="getStatusClass(u)">
                  {{ getStatusLabel(u) }}
                </p>
              </div>
            </div>
            <h5>📱 应用绑定 ({{ u.bindings.length }})</h5>
            <el-table :data="u.bindings" size="small" border>
              <el-table-column prop="appType" label="应用" width="100" />
              <el-table-column prop="openid" label="OpenID" min-width="200">
                <template #default="{ row }">
                  <code class="code">{{ row.openid }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="昵称" />
              <el-table-column label="绑定时间" width="170">
                <template #default="{ row }">
                  {{ formatTime(row.boundAt) }}
                </template>
              </el-table-column>
              <el-table-column label="最近登录" width="170">
                <template #default="{ row }">
                  {{ formatTime(row.lastLoginAt) }}
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 合并按钮 -->
          <el-card v-if="foundUsers.length > 1" class="merge-card" shadow="never">
            <template #header><span>🔀 合并重复账号</span></template>
            <p>将源账号 (user_from) 的所有应用绑定转移到目标账号 (user_to), 然后软删源账号</p>
            <el-form :inline="true" :model="mergeForm">
              <el-form-item label="目标账号 ID">
                <el-input v-model="mergeForm.userToId" placeholder="保留此账号" type="number" />
              </el-form-item>
              <el-form-item label="源账号 ID">
                <el-input v-model="mergeForm.userFromId" placeholder="合并后软删" type="number" />
              </el-form-item>
              <el-form-item label="原因">
                <el-input v-model="mergeForm.reason" placeholder="如: 用户投诉" />
              </el-form-item>
              <el-form-item>
                <el-button type="danger" @click="doMerge">合并</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import http from '@/api/http'
import dayjs from 'dayjs'

const activeTab = ref('all')
const loading = ref(false)
const allRels = ref([])
const filterKw = ref('')

const searchUnionid = ref('')
const finding = ref(false)
const foundUsers = ref(null)

const mergeForm = ref({ userToId: '', userFromId: '', reason: '' })

const filteredRels = computed(() => {
  if (!filterKw.value) return allRels.value
  const kw = filterKw.value.toLowerCase()
  return allRels.value.filter(r =>
    (r.username || '').toLowerCase().includes(kw) ||
    (r.unionid || '').toLowerCase().includes(kw) ||
    (r.nickname || '').toLowerCase().includes(kw)
  )
})

async function loadAll() {
  loading.value = true
  try {
    const r = await http.get('/auth/admin/wechat/unionid-relations', { params: { limit: 100 } })
    allRels.value = r.data || []
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function doFind() {
  if (!searchUnionid.value.trim()) {
    ElMessage.warning('请输入 unionid')
    return
  }
  finding.value = true
  try {
    const r = await http.get('/auth/admin/wechat/users-by-unionid',
      { params: { unionid: searchUnionid.value.trim() } })
    foundUsers.value = r.data || []
    if (foundUsers.value.length === 0) {
      ElMessage.info('未找到该 unionid 的用户')
    }
  } catch (e) {
    ElMessage.error('查找失败: ' + e.message)
  } finally {
    finding.value = false
  }
}

function showDetail(row) {
  searchUnionid.value = row.unionid
  activeTab.value = 'find'
  doFind()
}

async function doMerge() {
  if (!mergeForm.value.userToId || !mergeForm.value.userFromId) {
    ElMessage.warning('请填写目标账号 ID 和源账号 ID')
    return
  }
  if (Number(mergeForm.value.userToId) === Number(mergeForm.value.userFromId)) {
    ElMessage.warning('目标账号和源账号不能相同')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将 user_id=${mergeForm.value.userFromId} 的所有应用合并到 user_id=${mergeForm.value.userToId}?\n` +
      `源账号会被软删 (status=0), 所有微信绑定会转移到目标账号`,
      '确认合并',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await http.post('/auth/admin/wechat/merge-accounts', mergeForm.value)
    ElMessage.success('合并成功')
    loadAll()
    doFind()
  } catch (e) {
    ElMessage.error('合并失败: ' + e.message)
  }
}

function formatTime(t) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-' }
function getStatusClass(u) { return u.userId ? 'status-active' : '' }
function getStatusLabel(u) { return u.userId ? '正常' : '已禁用' }

onMounted(loadAll)
</script>

<style scoped>
.unionid-admin { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 28px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 16px; }

.filter-bar { display: flex; gap: 8px; margin-bottom: 16px; }

.user-cell { display: flex; flex-direction: column; }
.user-name { font-weight: 600; }
.user-id { font-size: 12px; color: #94a3b8; }
.user-id-inline { font-size: 12px; color: #94a3b8; font-weight: normal; }
.code { font-family: 'SF Mono', Menlo, monospace; font-size: 12px; background: #f1f5f9; padding: 2px 4px; border-radius: 3px; }

.find-form { display: flex; gap: 8px; margin-bottom: 16px; }

.user-card { background: #f8fafc; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.user-header { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
.user-header h4 { margin: 0; }
.status-active { color: #10b981; font-size: 12px; margin: 4px 0; }

.merge-card { margin-top: 20px; border: 2px dashed #fbbf24; }
</style>