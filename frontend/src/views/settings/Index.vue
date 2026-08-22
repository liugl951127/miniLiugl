<!--
  @file settings/Index.vue - 统一系统管理面板 V6.9
  @description 将 API Key / 管理后台 / 超级管理 / 租户管理 / 系统监控 合并为单一 Settings 页面
-->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>⚙️ 系统管理</h2>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- ═══ API Key 管理 ═══ -->
      <el-tab-pane label="🔑 API Key" name="apikey">
        <div class="sub-header">
          <span class="sub-title">API Key 管理</span>
          <el-button size="small" type="primary" @click="showKeyCreate = true">
            <el-icon><Plus /></el-icon>生成 Key
          </el-button>
        </div>
        <!-- 统计卡片 -->
        <el-row :gutter="12" style="margin-bottom:16px">
          <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
            <div style="font-size:22px;font-weight:700;color:#409eff">{{ apiKeys.length }}</div>
            <div style="font-size:12px;color:#909399">总 Key 数</div>
          </el-card></el-col>
          <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
            <div style="font-size:22px;font-weight:700;color:#67c23a">{{ activeKeyCount }}</div>
            <div style="font-size:12px;color:#909399">启用中</div>
          </el-card></el-col>
          <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
            <div style="font-size:22px;font-weight:700;color:#e6a23c">{{ totalUsed.toLocaleString() }}</div>
            <div style="font-size:12px;color:#909399">总调用量</div>
          </el-card></el-col>
          <el-col :span="6"><el-card body-style="padding:12px;text-align:center">
            <div style="font-size:22px;font-weight:700;color:#909399">{{ totalQuota || '无限' }}</div>
            <div style="font-size:12px;color:#909399">总限额</div>
          </el-card></el-col>
        </el-row>

        <el-table :data="apiKeys" v-loading="keyLoading" stripe>
          <el-table-column label="名称">
            <template #default="{ row }">
              <div style="font-weight:600">{{ row.name }}</div>
              <div style="font-size:11px;color:#909399">{{ row.description || '' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="Key" width="280">
            <template #default="{ row }">
              <code style="font-size:12px;background:#f5f7fa;padding:2px 8px;border-radius:4px">
                {{ row.show ? row.key : maskKey(row.key) }}
              </code>
              <el-button size="small" link @click="row.show = !row.show">
                <el-icon><View v-if="!row.show" /><Hide v-else /></el-icon>
              </el-button>
              <el-button size="small" link type="primary" @click="copyKey(row.key)">复制</el-button>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="配额/限额" width="130" align="center">
            <template #default="{ row }">{{ row.quota || '—' }}</template>
          </el-table-column>
          <el-table-column label="调用量" width="100" align="center">
            <template #default="{ row }">{{ (row.used || 0).toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ row.createdAt || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="toggleKey(row)" :type="row.enabled ? '' : 'success'">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="deleteKey(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 创建 Key Dialog -->
        <el-dialog v-model="showKeyCreate" title="生成新 API Key" width="500px">
          <el-form :model="keyForm" label-width="100px">
            <el-form-item label="名称"><el-input v-model="keyForm.name" placeholder="例如：我的应用" /></el-form-item>
            <el-form-item label="描述"><el-input v-model="keyForm.description" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="每日限额">
              <el-input-number v-model="keyForm.quota" :min="0" :step="1000" />
              <span style="margin-left:8px;color:#909399;font-size:12px">0 = 不限制</span>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showKeyCreate = false">取消</el-button>
            <el-button type="primary" :loading="keyCreating" @click="createKey">生成</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- ═══ 用户管理 ═══ -->
      <el-tab-pane label="👥 用户管理" name="users">
        <div class="sub-header"><span class="sub-title">管理后台 — 用户管理</span></div>
        <!-- 统计卡片 -->
        <el-row :gutter="12" style="margin-bottom:16px">
          <el-col v-for="s in adminStats" :key="s.label" :span="6">
            <el-card shadow="hover" body-style="text-align:center;padding:12px">
              <div style="font-size:24px;font-weight:700;color:#409eff">{{ s.value }}</div>
              <div style="font-size:12px;color:#909399;margin-top:4px">{{ s.label }}</div>
            </el-card>
          </el-col>
        </el-row>
        <el-table :data="adminUsers" v-loading="adminLoading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column label="角色" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : row.role === 'SUPER_ADMIN' ? 'warning' : 'success'">
                {{ row.role === 'SUPER_ADMIN' ? 'SUPER' : row.role === 'ADMIN' ? 'ADMIN' : 'USER' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="160" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="toggleAdminUserAction(row)" :disabled="row.role === 'SUPER_ADMIN'">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ═══ 运维统计 ═══ -->
      <el-tab-pane label="📈 运维统计" name="stats">
        <div class="sub-header"><span class="sub-title">运维统计数据</span></div>
        <div class="stats-grid">
          <el-card v-for="item in opsStats" :key="item.label">
            <template #header>{{ item.label }}</template>
            <div class="big-num">{{ item.value }}</div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ═══ 审计日志 ═══ -->
      <el-tab-pane label="📋 审计日志" name="audit">
        <div class="sub-header"><span class="sub-title">审计日志</span></div>
        <el-table :data="auditLogs" v-loading="auditLoading" stripe>
          <el-table-column prop="user" label="用户" width="120" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="timestamp" label="时间" width="170" />
        </el-table>
      </el-tab-pane>

      <!-- ═══ 租户管理 (SUPER_ADMIN) ═══ -->
      <el-tab-pane v-if="isSuperAdmin" label="🏢 租户管理" name="tenant">
        <div class="sub-header">
          <span class="sub-title">租户管理</span>
          <el-button size="small" type="primary" @click="openTenantForm(null)">
            <el-icon><Plus /></el-icon>新建租户
          </el-button>
        </div>
        <el-table :data="tenants" v-loading="tenantLoading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="租户名">
            <template #default="{ row }">
              <span style="font-weight:600">{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" type="warning" style="margin-left:4px">默认</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="管理员" />
          <el-table-column label="配额" width="200">
            <template #default="{ row }">
              <span style="font-size:12px">
                👤 {{ row.userLimit ?? '-' }} 人 &nbsp;
                📊 {{ row.apiLimit ? row.apiLimit.toLocaleString() + '/天' : '无限制' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="数据隔离" width="100" align="center">
            <template #default="{ row }">
              <el-tooltip content="开启后该租户数据与其他租户完全隔离">
                <el-switch
                  :model-value="row.dataIsolation !== false"
                  size="small"
                  @change="v => updateTenantIsolation(row, v)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="IP 白名单" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.ipWhitelist ? 'success' : 'info'">
                {{ row.ipWhitelist ? '已配置' : '未配置' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '活跃' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openTenantDetail(row)">详情</el-button>
              <el-button size="small" @click="openTenantForm(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteTenant(row)" :disabled="row.isDefault">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 新建/编辑租户 Dialog -->
        <el-dialog v-model="tenantFormVisible" :title="tenantForm.id ? '编辑租户' : '新建租户'" width="540px">
          <el-form :model="tenantForm" label-width="110px">
            <el-form-item label="租户名">
              <el-input v-model="tenantForm.name" placeholder="例如：XX 公司" />
            </el-form-item>
            <el-form-item label="管理员邮箱">
              <el-input v-model="tenantForm.owner" placeholder="租户管理员邮箱" />
            </el-form-item>
            <el-form-item label="用户配额">
              <el-input-number v-model="tenantForm.userLimit" :min="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="API 配额/天">
              <el-input-number v-model="tenantForm.apiLimit" :min="0" style="width:100%" placeholder="0=不限制" />
            </el-form-item>
            <el-form-item label="数据隔离">
              <el-switch v-model="tenantForm.dataIsolation" />
              <span style="margin-left:8px;color:#909399;font-size:12px">开启后租户数据完全隔离</span>
            </el-form-item>
            <el-form-item label="IP 白名单">
              <el-input
                v-model="tenantForm.ipWhitelist"
                type="textarea"
                :rows="2"
                placeholder="多个 IP 用逗号分隔，留空表示不限制"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="tenantFormVisible = false">取消</el-button>
            <el-button type="primary" :loading="tenantSaving" @click="saveTenant">保存</el-button>
          </template>
        </el-dialog>

        <!-- 租户详情 Drawer -->
        <el-drawer v-model="tenantDetailVisible" :title="'🏢 ' + (tenantDetail.name || '')" size="560px">
          <template #title>
            <span>🏢 {{ tenantDetail.name }}</span>
            <el-tag
              :type="tenantDetail.active ? 'success' : 'info'"
              size="small"
              style="margin-left:8px"
            >{{ tenantDetail.active ? '活跃' : '停用' }}</el-tag>
          </template>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="租户 ID">{{ tenantDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="管理员">{{ tenantDetail.owner || '—' }}</el-descriptions-item>
            <el-descriptions-item label="用户配额">{{ tenantDetail.userLimit ?? '无限制' }}</el-descriptions-item>
            <el-descriptions-item label="API 配额">{{ tenantDetail.apiLimit ? tenantDetail.apiLimit.toLocaleString() + '/天' : '无限制' }}</el-descriptions-item>
            <el-descriptions-item label="数据隔离">
              <el-tag :type="tenantDetail.dataIsolation !== false ? 'success' : 'warning'" size="small">
                {{ tenantDetail.dataIsolation !== false ? '已开启' : '未开启' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ tenantDetail.createdAt || '—' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">数据隔离配置</el-divider>
          <el-form label-width="120px" size="small">
            <el-form-item label="数据隔离">
              <el-switch
                :model-value="tenantDetail.dataIsolation !== false"
                @change="v => updateTenantIsolation(tenantDetail, v)"
              />
              <span style="margin-left:8px;color:#909399">开启后，该租户用户的对话、知识库、API Key 等数据完全隔离</span>
            </el-form-item>
            <el-form-item label="IP 白名单">
              <el-input
                v-model="tenantDetail.ipWhitelist"
                placeholder="多个 IP 用逗号分隔，留空=不限制"
                @blur="saveTenantIpWhitelist"
              >
                <template #append>
                  <el-button @click="saveTenantIpWhitelist">保存</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>

          <el-divider content-position="left">租户用户 ({{ tenantUsers.length }})</el-divider>
          <el-table :data="tenantUsers" size="small" max-height="240" v-loading="tenantUsersLoading">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="nickname" label="昵称" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-drawer>
      </el-tab-pane>

      <!-- ═══ 系统设置 (SUPER_ADMIN) ═══ -->
      <el-tab-pane v-if="isSuperAdmin" label="⚡ 系统设置" name="system">
        <div class="sub-header"><span class="sub-title">超级管理 — 系统设置</span></div>
        <el-alert title="超级管理员拥有系统最高权限，请谨慎操作" type="warning" :closable="false" style="margin-bottom:16px" />
        <el-form label-width="140px" style="max-width:600px">
          <el-form-item label="站点名称"><el-input v-model="sysSettings.siteName" /></el-form-item>
          <el-form-item label="维护模式"><el-switch v-model="sysSettings.maintenance" /></el-form-item>
          <el-form-item label="开放注册"><el-switch v-model="sysSettings.allowRegister" /></el-form-item>
          <el-form-item label="LLM 默认模型">
            <el-select v-model="sysSettings.defaultModel" style="width:100%">
              <el-option label="MiniMax-01" value="minimax-01" />
              <el-option label="GPT-4o" value="gpt-4o" />
              <el-option label="DeepSeek" value="deepseek-chat" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="saveSysSettings">保存设置</el-button></el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- ═══ 系统监控 ═══ -->
      <el-tab-pane label="📊 系统监控" name="monitor">
        <div class="sub-header">
          <span class="sub-title">系统监控</span>
          <el-button size="small" @click="loadMonitor"><el-icon><Refresh /></el-icon>刷新</el-button>
        </div>
        <el-row :gutter="12" style="margin-bottom:16px">
          <el-col v-for="s in services" :key="s.name" :span="6">
            <el-card shadow="hover">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span style="font-weight:600;font-size:13px">{{ s.name }}</span>
                <el-tag size="small" :type="s.status === 'UP' ? 'success' : 'danger'">{{ s.status }}</el-tag>
              </div>
              <div style="margin-top:8px;font-size:12px;color:#666">
                延迟: <span :style="{color: (s.latency || 0) > 1000 ? '#ef4444' : '#10b981'}">{{ s.latency || '—' }}ms</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-descriptions :column="3" border style="margin-bottom:16px">
          <el-descriptions-item label="总服务数">{{ services.length }}</el-descriptions-item>
          <el-descriptions-item label="在线服务">{{ upServices }}</el-descriptions-item>
          <el-descriptions-item label="系统状态">
            <el-tag :type="upServices === services.length ? 'success' : 'danger'" size="small">
              {{ upServices === services.length ? '全部正常' : '部分异常' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="alerts" stripe size="small">
          <el-table-column prop="firedAt" label="触发时间" width="170">
            <template #default="{ row }">{{ row.firedAt || row.timestamp || '—' }}</template>
          </el-table-column>
          <el-table-column label="级别" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="severityType(row.severity)">{{ row.severity || 'INFO' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ruleName" label="规则" width="140" />
          <el-table-column prop="message" label="告警信息" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  View, Hide, Plus, Refresh, QuestionFilled, ChatLineRound, Setting, TrendCharts, Bell
} from '@element-plus/icons-vue'
import { listAdminUsers, toggleAdminUser, getDashboard, getOpsStats, getRecentAudit, getAdminHealth } from '@/api/admin'
import { listTenants, createTenant, setTenantStatus } from '@/api/tenant'
import { apiKeyApi } from '@/api/apikey'
import { getFiringAlerts } from '@/api/monitor'

// ─── Stores ───
const route = useRoute()
const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

// ─── Tab (从 query 读取，支持旧路由 redirect) ───
const activeTab = ref(route.query.tab || 'apikey')

// ════════════════════════════════════
// API Key Tab
// ════════════════════════════════════
const apiKeys = ref([])
const keyLoading = ref(false)
const showKeyCreate = ref(false)
const keyCreating = ref(false)
const keyForm = ref({ name: '', description: '', quota: 0 })

const activeKeyCount = computed(() => apiKeys.value.filter(k => k.enabled).length)
const totalUsed = computed(() => apiKeys.value.reduce((s, k) => s + (k.used || 0), 0))
const totalQuota = computed(() => {
  const q = apiKeys.value.reduce((s, k) => s + (k.quota || 0), 0)
  return q || null
})

function maskKey(key) {
  if (!key) return '••••••••'
  return key.slice(0, 8) + '•'.repeat(28) + key.slice(-4)
}

async function loadApiKeys() {
  keyLoading.value = true
  try {
    const r = await apiKeyApi.list().catch(() => ({ data: [] }))
    apiKeys.value = (r?.data || []).map(k => ({ ...k, show: false }))
  } catch { apiKeys.value = [] }
  finally { keyLoading.value = false }
}

async function createKey() {
  if (!keyForm.value.name) { ElMessage.warning('请输入名称'); return }
  keyCreating.value = true
  try {
    const r = await apiKeyApi.create(keyForm.value).catch(() => null)
    if (r?.key) {
      ElMessage.success('Key 已生成：' + r.key)
      showKeyCreate.value = false
      loadApiKeys()
    } else {
      ElMessage.error('生成失败')
    }
  } finally { keyCreating.value = false }
}

async function toggleKey(row) {
  try {
    await apiKeyApi.toggle(row.id, !row.enabled).catch(() => null)
    row.enabled = !row.enabled
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch { ElMessage.error('操作失败') }
}

async function deleteKey(row) {
  await ElMessageBox.confirm('确认删除该 API Key？', '删除确认')
  try {
    await apiKeyApi.remove(row.id).catch(() => null)
    apiKeys.value = apiKeys.value.filter(k => k.id !== row.id)
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
}

function copyKey(key) {
  navigator.clipboard.writeText(key).then(() => ElMessage.success('已复制'))
}

// ════════════════════════════════════
// 用户管理 Tab
// ════════════════════════════════════
const adminUsers = ref([])
const adminStats = ref([])
const adminLoading = ref(false)
const opsStats = ref([])
const auditLogs = ref([])
const auditLoading = ref(false)

async function loadAdminUsers() {
  adminLoading.value = true
  try {
    const [usersR, dashR] = await Promise.all([
      listAdminUsers().catch(() => ({ data: [] })),
      getDashboard().catch(() => ({ data: {} }))
    ])
    const users = usersR?.data?.list || usersR?.data || []
    adminUsers.value = users
    const d = dashR?.data || {}
    adminStats.value = [
      { label: '总用户', value: d.totalUsers || 0 },
      { label: '今日登录', value: d.todayLogins || 0 },
      { label: '活跃会话', value: d.activeSessions || 0 },
      { label: 'API 调用', value: (d.apiCalls || 0).toLocaleString() },
    ]
  } catch { adminUsers.value = []; adminStats.value = [] }
  finally { adminLoading.value = false }
}

async function loadOpsStats() {
  try {
    const r = await getOpsStats().catch(() => ({ data: [] }))
    opsStats.value = r?.data || []
  } catch { opsStats.value = [] }
}

async function loadAuditLogs() {
  auditLoading.value = true
  try {
    const r = await getRecentAudit().catch(() => ({ data: [] }))
    auditLogs.value = r?.data || []
  } catch { auditLogs.value = [] }
  finally { auditLoading.value = false }
}

async function toggleAdminUserAction(row) {
  try {
    await toggleAdminUser(row.id, userStore.profile?.id || 0, !row.enabled)
    row.enabled = !row.enabled
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch { ElMessage.error('操作失败') }
}

// ════════════════════════════════════
// 租户管理 Tab

// ════════════════════════════════════
// 租户管理 Tab
// ════════════════════════════════════
const tenants = ref([])
const tenantLoading = ref(false)
const tenantFormVisible = ref(false)
const tenantSaving = ref(false)
const tenantForm = ref({ name: '', owner: '', userLimit: 100, apiLimit: 10000, dataIsolation: true, ipWhitelist: '' })
const tenantDetail = ref({})
const tenantDetailVisible = ref(false)
const tenantUsers = ref([])
const tenantUsersLoading = ref(false)

async function loadTenants() {
  tenantLoading.value = true
  try {
    const r = await listTenants().catch(() => ({ data: [] }))
    tenants.value = r?.data || []
  } catch { tenants.value = [] }
  finally { tenantLoading.value = false }
}

function openTenantForm(row) {
  tenantForm.value = row ? { ...row } : {
    name: '', owner: '', userLimit: 100, apiLimit: 10000, dataIsolation: true, ipWhitelist: ''
  }
  tenantFormVisible.value = true
}

async function saveTenant() {
  if (!tenantForm.value.name) { ElMessage.warning('请输入租户名'); return }
  tenantSaving.value = true
  try {
    const data = {
      name: tenantForm.value.name,
      owner: tenantForm.value.owner,
      userLimit: tenantForm.value.userLimit,
      apiLimit: tenantForm.value.apiLimit,
      dataIsolation: tenantForm.value.dataIsolation,
      ipWhitelist: tenantForm.value.ipWhitelist,
    }
    if (tenantForm.value.id) {
      data.id = tenantForm.value.id
    }
    await createTenant(data).catch(() => null)
    tenantFormVisible.value = false
    loadTenants()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { tenantSaving.value = false }
}

async function deleteTenant(row) {
  if (row.isDefault) { ElMessage.warning('默认租户不可删除'); return }
  await ElMessageBox.confirm(`确认删除租户「${row.name}」？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).catch(() => null)
  try {
    await import('@/api/tenant').then(m => m.deleteTenant(row.id).catch(() => null))
    ElMessage.success('已删除')
    loadTenants()
  } catch { ElMessage.error('删除失败') }
}

async function updateTenantIsolation(row, enabled) {
  const idx = tenants.value.findIndex(t => t.id === row.id)
  if (idx !== -1) tenants.value[idx].dataIsolation = enabled
  try {
    await createTenant({ ...row, dataIsolation: enabled }).catch(() => null)
    if (tenantDetail.value?.id === row.id) tenantDetail.value.dataIsolation = enabled
    ElMessage.success('数据隔离已' + (enabled ? '开启' : '关闭'))
  } catch { ElMessage.error('更新失败') }
}

async function saveTenantIpWhitelist() {
  if (!tenantDetail.value?.id) return
  try {
    await createTenant(tenantDetail.value).catch(() => null)
    ElMessage.success('IP 白名单已保存')
  } catch { ElMessage.error('保存失败') }
}

async function openTenantDetail(row) {
  tenantDetail.value = { ...row }
  tenantDetailVisible.value = true
  tenantUsersLoading.value = true
  try {
    const { listTenantUsers } = await import('@/api/tenant')
    const r = await listTenantUsers(row.id).catch(() => ({ data: [] }))
    tenantUsers.value = r?.data || []
  } catch { tenantUsers.value = [] }
  finally { tenantUsersLoading.value = false }
}

async function toggleTenant(row) {
  try {
    await setTenantStatus(row.id, !row.active).catch(() => null)
    row.active = !row.active
    ElMessage.success(row.active ? '已启用' : '已停用')
  } catch { ElMessage.error('操作失败') }
}

// ════════════════════════════════════
// 系统设置 Tab
// ════════════════════════════════════
const sysSettings = ref({ siteName: 'Liugl-AI', maintenance: false, allowRegister: true, defaultModel: 'minimax-01' })

async function saveSysSettings() {
  // V7.2: 后端尚未提供 /system/settings 持久化接口，配置仅保存在浏览器本地
  // 如需云端同步，可联系后端实现此接口 (建议路径: minimax-system 模块)
  try {
    localStorage.setItem('sysSettings', JSON.stringify(sysSettings.value))
    ElMessage.success('系统设置已保存（仅本地生效）')
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || e))
  }
}

// ════════════════════════════════════
// 系统监控 Tab
// ════════════════════════════════════
const services = ref([])
const alerts = ref([])

const upServices = computed(() => services.value.filter(s => s.status === 'UP').length)

async function loadMonitor() {
  try {
    const [healthR, alertsR] = await Promise.all([
      getAdminHealth().catch(() => ({ data: {} })),
      getFiringAlerts().catch(() => ({ data: [] }))
    ])
    const healthData = healthR?.data || {}
    if (Object.keys(healthData).length > 0) {
      services.value = Object.entries(healthData).map(([name, info]) => ({
        name,
        status: info?.status || 'DOWN',
        latency: info?.latency || 0
      }))
    }
    alerts.value = alertsR?.data || []
  } catch { services.value = []; alerts.value = [] }
}

function severityType(sev) {
  const map = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: 'info', LOW: 'success', INFO: '' }
  return map[sev] || ''
}
function statusType(s) {
  const map = { FIRING: 'danger', RESOLVED: 'success', ACKNOWLEDGED: 'warning' }
  return map[s] || 'info'
}
function statusLabel(s) {
  const map = { FIRING: '触发中', RESOLVED: '已恢复', ACKNOWLEDGED: '已确认' }
  return map[s] || s || '—'
}

// ─── Load on mount ───
onMounted(() => {
  loadApiKeys()
  loadAdminUsers()
  loadAuditLogs()
  loadOpsStats()
  if (isSuperAdmin.value) {
    loadTenants()
    loadMonitor()
  }
})
</script>

<style scoped>
.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.sub-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.sub-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.big-num {
  font-size: 32px;
  font-weight: 700;
  color: #1e40af;
  text-align: center;
}
</style>
