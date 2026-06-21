<!--
  V3.1: 多租户管理控制台
  adminLiugl (SUPER_ADMIN) 专属
  功能: 租户列表 / 创建 / 启停 / 配额 / 删除 / 查看用户
-->
<template>
  <div class="tenant-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1>🏢 {{ t('tenant.title') }}</h1>
        <p class="sub">{{ t('tenant.subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        {{ t('tenant.newTenant') }}
      </el-button>
    </div>

    <!-- 概览 KPI -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6">
        <div class="kpi-card kpi-blue">
          <div class="kpi-icon"><el-icon><OfficeBuilding /></el-icon></div>
          <div class="kpi-content">
            <div class="kpi-value">{{ tenants.length }}</div>
            <div class="kpi-label">{{ t('tenant.total') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-green">
          <div class="kpi-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="kpi-content">
            <div class="kpi-value">{{ activeCount }}</div>
            <div class="kpi-label">{{ t('tenant.activeCount') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-purple">
          <div class="kpi-icon"><el-icon><User /></el-icon></div>
          <div class="kpi-content">
            <div class="kpi-value">{{ totalUsers }}</div>
            <div class="kpi-label">{{ t('tenant.users') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-amber">
          <div class="kpi-icon"><el-icon><Warning /></el-icon></div>
          <div class="kpi-content">
            <div class="kpi-value">{{ disabledCount }}</div>
            <div class="kpi-label">{{ t('tenant.disabled') }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 租户表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>🏢 {{ t('tenant.list') }}</span>
          <el-button text @click="loadTenants"><el-icon><Refresh /></el-icon> {{ t('common.refresh') }}</el-button>
        </div>
      </template>

      <el-table :data="tenants" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="code" label="代码" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="plan" label="套餐" width="120">
          <template #default="{ row }">
            <el-tag :type="planType(row.plan)" size="small">{{ row.plan }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? t('tenant.active') : t('tenant.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxUsers" label="用户上限" width="90" align="center" />
        <el-table-column label="月度配额" width="130" align="right">
          <template #default="{ row }">
            <span class="quota-text">
              {{ formatQuota(row.usedQuota) }} / {{ formatQuota(row.monthlyQuota) }}
            </span>
            <el-progress
              :percentage="quotaPercent(row)"
              :color="quotaColor(quotaPercent(row))"
              :stroke-width="6"
              style="width:100px;display:inline-block;margin-left:6px;vertical-align:middle"
            />
          </template>
        </el-table-column>
        <el-table-column prop="contactEmail" label="联系人" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewUsers(row)">
              <el-icon><User /></el-icon> {{ t('common.users') }}
            </el-button>
            <el-button
              size="small"
              text
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              <el-icon><Switch /></el-icon>
              {{ row.status === 1 ? t('tenant.disable') : t('tenant.enable') }}
            </el-button>
            <el-button
              v-if="row.code !== 'default'"
              size="small"
              text
              type="danger"
              @click="deleteTenant(row)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑租户弹窗 -->
    <el-dialog v-model="createVisible" :title="editingTenant ? t('tenant.editTenant') : t('tenant.newTenant')" width="520px">
      <el-form :model="form" label-width="100" ref="formRef">
        <el-form-item :label="t('tenant.code')" prop="code" required>
          <el-input v-model="form.code" :placeholder="t('tenant.codePlaceholder')" :disabled="!!editingTenant" />
        </el-form-item>
        <el-form-item :label="t('tenant.name')" prop="name" required>
          <el-input v-model="form.name" :placeholder="t('tenant.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('tenant.plan')" prop="plan">
          <el-select v-model="form.plan" style="width:100%">
            <el-option :label="t('tenant.freePlan')" value="free" />
            <el-option :label="t('tenant.proPlan')" value="pro" />
            <el-option :label="t('tenant.enterprisePlan')" value="enterprise" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('tenant.userLimit')" prop="maxUsers">
          <el-input-number v-model="form.maxUsers" :min="1" :max="10000" style="width:100%" />
        </el-form-item>
        <el-form-item :label="t('tenant.monthlyQuota')" prop="monthlyQuota">
          <el-input-number v-model="form.monthlyQuota" :min="0" :step="10000" style="width:100%" />
        </el-form-item>
        <el-form-item :label="t('tenant.qpsLimit')" prop="qpsLimit">
          <el-input-number v-model="form.qpsLimit" :min="1" :max="10000" style="width:100%" />
        </el-form-item>
        <el-form-item :label="t('tenant.contactEmail')" prop="contactEmail">
          <el-input v-model="form.contactEmail" placeholder="admin@company.com" />
        </el-form-item>
        <el-form-item :label="t('tenant.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="t('tenant.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTenant">
          {{ editingTenant ? t('common.save') : t('common.create') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 配额调整弹窗 -->
    <el-dialog v-model="quotaVisible" :title="t('tenant.adjustQuota')" width="420px">
      <el-form label-width="110">
        <el-form-item :label="`${currentTenant?.name} ${t('tenant.currentQuota')}`">
          <span class="quota-text">{{ formatQuota(currentTenant?.monthlyQuota || 0) }}</span>
        </el-form-item>
        <el-form-item :label="t('tenant.newQuota')">
          <el-input-number v-model="newQuota" :min="0" :step="10000" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitQuota">
          {{ t('tenant.confirmAdjust') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 租户用户列表弹窗 -->
    <el-dialog v-model="usersVisible" :title="`${currentTenant?.name} - ${t('tenant.userList')}`" width="600px">
      <el-table :data="tenantUsers" stripe v-loading="usersLoading">
        <el-table-column prop="id" :label="t('common.id')" width="70" />
        <el-table-column prop="username" :label="t('tenant.username')" width="150" />
        <el-table-column prop="nickname" :label="t('tenant.nickname')" />
        <el-table-column prop="email" :label="t('tenant.email')" />
        <el-table-column :label="t('common.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? t('tenant.active') : t('tenant.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Switch, Delete, OfficeBuilding, CircleCheck, User, Warning
} from '@element-plus/icons-vue'
import { useTenantStore } from '@/store/tenant'
import { t } from '@/i18n'

const tenantStore = useTenantStore()

const tenants = computed(() => tenantStore.tenants)
const loading = computed(() => tenantStore.loading)

// KPI
const activeCount = computed(() => tenants.value.filter(t => t.status === 1).length)
const disabledCount = computed(() => tenants.value.filter(t => t.status === 0).length)
const totalUsers = computed(() => {
  return tenants.value.reduce((sum, t) => sum + (t.userCount || 0), 0)
})

// 创建表单
const createVisible = ref(false)
const editingTenant = ref(null)
const submitting = ref(false)
const formRef = ref()
const form = ref({
  code: '',
  name: '',
  plan: 'pro',
  maxUsers: 100,
  monthlyQuota: 1000000,
  qpsLimit: 500,
  contactEmail: '',
  remark: '',
})

function openCreateDialog() {
  editingTenant.value = null
  form.value = { code: '', name: '', plan: 'pro', maxUsers: 100, monthlyQuota: 1000000, qpsLimit: 500, contactEmail: '', remark: '' }
  createVisible.value = true
}

async function submitTenant() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning(t('tenant.codeAndNameRequired'))
    return
  }
  submitting.value = true
  try {
    await tenantStore.createTenant(form.value)
    ElMessage.success(editingTenant.value ? t('tenant.saveSuccess') : t('tenant.createSuccess'))
    createVisible.value = false
    await loadTenants()
  } catch (e) {
    ElMessage.error(e?.message || t('tenant.operationFailed'))
  } finally {
    submitting.value = false
  }
}

// 启停
async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? t('tenant.enable') : t('tenant.disable')
  try {
    await ElMessageBox.confirm(t('tenant.confirmToggle') + '「' + row.name + '」？', label, { type: 'warning' })
    await tenantStore.toggleStatus(row.id, newStatus)
    ElMessage.success(label + t('tenant.success'))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || label + t('tenant.failed'))
  }
}

// 配额
const quotaVisible = ref(false)
const currentTenant = ref(null)
const newQuota = ref(0)

function openQuotaDialog(row) {
  currentTenant.value = row
  newQuota.value = row.monthlyQuota
  quotaVisible.value = true
}

async function submitQuota() {
  try {
    await tenantStore.setQuota(currentTenant.value.id, newQuota.value)
    ElMessage.success(t('tenant.quotaUpdated'))
    quotaVisible.value = false
  } catch (e) {
    ElMessage.error(e?.message || t('tenant.updateFailed'))
  }
}

// 删除
async function deleteTenant(row) {
  if (row.code === 'default') {
    ElMessage.warning(t('tenant.defaultNotDeletable'))
    return
  }
  try {
    await ElMessageBox.confirm(t('tenant.confirmDelete') + '「' + row.name + '」' + t('tenant.deleteWarning'), t('tenant.dangerous'), { type: 'error' })
    await tenantStore.removeTenant(row.id)
    ElMessage.success(t('tenant.deleted'))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || t('tenant.deleteFailed'))
  }
}

// 用户列表
const usersVisible = ref(false)
const tenantUsers = ref([])
const usersLoading = ref(false)

async function viewUsers(row) {
  currentTenant.value = row
  usersVisible.value = true
  usersLoading.value = true
  try {
    tenantUsers.value = await tenantStore.fetchTenantUsers(row.id)
  } catch (e) {
    ElMessage.error(t('tenant.loadUsersFailed'))
  } finally {
    usersLoading.value = false
  }
}

// 加载
async function loadTenants() {
  try {
    await tenantStore.fetchTenants()
  } catch (e) {
    ElMessage.error(t('tenant.loadFailed'))
  }
}

// 格式化
function formatQuota(n) {
  if (!n) return '0'
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(0) + 'k'
  return String(n)
}

function quotaPercent(row) {
  if (!row.monthlyQuota) return 0
  return Math.min(100, Math.round((row.usedQuota / row.monthlyQuota) * 100))
}

function quotaColor(pct) {
  if (pct >= 90) return '#f56c6c'
  if (pct >= 70) return '#e6a23c'
  return '#67c23a'
}

function planType(plan) {
  const map = { free: 'info', pro: 'success', enterprise: 'warning' }
  return map[plan] || 'info'
}

onMounted(() => {
  loadTenants()
})
</script>

<style scoped>
.tenant-page { padding: 24px; }

.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 24px;
}
.page-header h1 { margin: 0 0 4px; font-size: 24px; }
.page-header .sub { margin: 0; color: #909399; font-size: 14px; }

.kpi-row { margin-bottom: 20px; }
.kpi-card {
  display: flex; align-items: center; gap: 16px;
  padding: 20px 16px; border-radius: 12px;
  color: #fff;
}
.kpi-blue  { background: linear-gradient(135deg, #409eff, #66b1ff); }
.kpi-green { background: linear-gradient(135deg, #67c23a, #85ce61); }
.kpi-purple { background: linear-gradient(135deg, #9c27b0, #ba68c8); }
.kpi-amber { background: linear-gradient(135deg, #e6a23c, #f5c77e); }
.kpi-icon .el-icon { font-size: 32px; opacity: 0.9; }
.kpi-value { font-size: 28px; font-weight: 700; line-height: 1; }
.kpi-label { font-size: 13px; opacity: 0.85; margin-top: 4px; }

.table-card { margin-top: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.quota-text { font-size: 13px; color: #606266; }
</style>
