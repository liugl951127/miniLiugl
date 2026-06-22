<template>
  <div class="super-container">
    <div class="super-header">
      <h1>👑 超级管理员控制台 <span class="badge">adminLiugl 专属</span></h1>
      <p class="sub">🔑 你是平台的唯一超级管理员 — 拥有所有权限</p>
    </div>

    <el-row :gutter="20" v-if="meInfo">
      <el-col :span="24">
        <el-card class="welcome">
          <div class="welcome-content">
            <div>
              <h2>{{ meInfo.message }}</h2>
              <el-tag size="large" type="danger">角色: {{ meInfo.role }}</el-tag>
            </div>
            <div class="capabilities">
              <h3>你的能力</h3>
              <ul>
                <li v-for="c in meInfo.capabilities" :key="c">✅ {{ c }}</li>
              </ul>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>👥 平台用户</span>
              <el-button size="small" @click="loadUsers">刷新</el-button>
            </div>
          </template>
          <el-table :data="users" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="160" />
            <el-table-column prop="nickname" label="昵称" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最后登录" width="180">
              <template #default="{ row }">
                {{ row.lastLoginAt ? row.lastLoginAt.substring(0, 19) : '从未' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" size="small" type="warning"
                           @click="disableUser(row)">禁用</el-button>
                <el-button v-else size="small" type="success"
                           @click="enableUser(row)">启用</el-button>
                <el-button size="small" type="primary" plain
                           @click="resetPwd(row)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card>
          <template #header><span>📊 平台统计</span></template>
          <el-statistic title="总用户" :value="users.length" />
          <el-statistic title="活跃用户" :value="activeCount" :value-style="{ color: '#67c23a' }" />
          <el-statistic title="禁用用户" :value="users.length - activeCount" :value-style="{ color: '#f56c6c' }" />
        </el-card>

        <el-card style="margin-top:16px">
          <template #header><span>🛠️ 快速操作</span></template>
          <div class="quick-actions">
            <el-button type="primary" plain @click="exportData">📥 导出全量数据</el-button>
            <el-button type="warning" plain @click="clearCache">🧹 清空缓存</el-button>
            <el-button type="info" plain @click="viewAudit">📜 查看审计日志</el-button>
          </div>
        </el-card>

        <el-card style="margin-top:16px" v-if="meInfo">
          <template #header><span>🔐 账号信息</span></template>
          <p>用户名: <code>adminLiugl</code></p>
          <p>邮箱: {{ meInfo.message.includes('adminLiugl') ? 'liugl951127@gmail.com' : '—' }}</p>
          <p>权限等级: <el-tag type="danger">最高 (SUPER_ADMIN)</el-tag></p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()
const router = useRouter()

const meInfo = ref<any>(null)
const users = ref<any[]>([])

const activeCount = computed(() => users.value.filter(u => u.status === 1).length)

function auth() {
  return { headers: { Authorization: `Bearer ${userStore.accessToken}` } }
}

async function loadMe() {
  try {
    const { data } = await axios.get(`${API}/api/v1/auth/super/me`, auth())
    meInfo.value = data.data
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message)
  }
}

async function loadUsers() {
  try {
    const { data } = await axios.get(`${API}/api/v1/auth/super/users`, auth())
    users.value = data.data || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message)
  }
}

async function disableUser(row: any) {
  if (row.username === 'adminLiugl') {
    ElMessage.warning('不能禁用自己')
    return
  }
  await ElMessageBox.confirm(`确认禁用 ${row.username}?`, '警告', { type: 'warning' })
  try {
    await axios.post(`${API}/api/v1/auth/super/users/${row.id}/disable`, {}, auth())
    ElMessage.success('已禁用')
    loadUsers()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function enableUser(row: any) {
  try {
    await axios.post(`${API}/api/v1/auth/super/users/${row.id}/enable`, {}, auth())
    ElMessage.success('已启用')
    loadUsers()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function resetPwd(row: any) {
  if (row.username === 'adminLiugl') {
    ElMessage.warning('不能重置超级管理员密码')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('新密码 (默认 Temp@123456)', '重置密码', {
      inputValue: 'Temp@123456'
    })
    const { data } = await axios.post(
      `${API}/api/v1/auth/super/users/${row.id}/reset-pwd?newPwd=${value}`, {}, auth())
    ElMessageBox.alert(`新密码: ${data.data}`, '重置成功', { type: 'success' })
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e?.message) }
}

// V1.8: 三个快速操作实现
function exportData() {
  // 导出全量用户 (JSON 下载)
  const blob = new Blob([JSON.stringify(users.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `minimax-users-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('用户数据已导出')
}
function clearCache() {
  ElMessageBox.confirm('确认清空 Redis 缓存? 仅影响短期记忆和会话状态, 不会丢数据', '清空缓存', { type: 'warning' })
    .then(() => ElMessage.success('缓存已清空 (本地提示, 需后端端点 /api/v1/admin/cache/clear)'))
    .catch(() => {})
}
function viewAudit() {
  // 跳到管理后台 → 审计页
  router.push('/admin/dashboard')
}

onMounted(async () => {
  await loadMe()
  await loadUsers()
})
</script>

<style scoped>
.super-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.super-header h1 { display:flex; align-items:center; gap:10px; }
.badge {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin-bottom: 20px; }
.welcome { background: linear-gradient(135deg, #fff5f5 0%, #fffaf0 100%); }
.welcome-content { display: flex; gap: 30px; align-items: flex-start; }
.welcome-content h2 { margin: 0 0 10px; color: #f56c6c; }
.capabilities h3 { margin: 0 0 8px; color: #666; }
.capabilities ul { margin: 0; padding-left: 20px; }
.capabilities li { margin: 4px 0; color: #555; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.quick-actions { display: flex; flex-direction: column; gap: 10px; }
.quick-actions .el-button { width: 100%; }
code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
</style>
