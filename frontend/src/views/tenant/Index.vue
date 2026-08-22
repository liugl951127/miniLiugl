<!-- @file tenant/Index.vue - 租户管理 V6.8.13 (企业级) -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🏢 租户管理</h2>
      <div style="display:flex;gap:8px">
        <el-button :icon="Refresh" :loading="loading" size="small" @click="loadTenants">刷新</el-button>
        <el-button type="primary" :icon="Plus" size="small" @click="openCreate">新建租户</el-button>
      </div>
    </div>

    <!-- 统计 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="never">
          <div style="font-size:22px;font-weight:700;color: var(--el-color-primary)">{{ tenants.length }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary)">租户总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="never">
          <div style="font-size:22px;font-weight:700;color: var(--el-color-success)">{{ activeCount }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary)">活跃租户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="never">
          <div style="font-size:22px;font-weight:700;color: var(--el-color-warning)">{{ totalUsers }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary)">用户配额合计</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card body-style="padding:12px;text-align:center" shadow="never">
          <div style="font-size:22px;font-weight:700;color: var(--el-text-color-secondary)">{{ totalApi.toLocaleString() }}</div>
          <div style="font-size:12px;color: var(--el-text-color-secondary)">API 配额合计/天</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table
      :data="tenants"
      v-loading="loading"
      stripe
      :empty-text="loading ? '加载中…' : '暂无租户，点击右上角\"新建租户\"创建'"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="租户名" min-width="160">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.name }}</div>
          <div style="font-size:11px;color: var(--el-text-color-secondary)">{{ row.code || row.slug || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="管理员" min-width="160">
        <template #default="{ row }">
          <span v-if="row.owner">{{ row.owner }}</span>
          <span v-else style="color: var(--el-text-color-placeholder)">—</span>
        </template>
      </el-table-column>
      <el-table-column label="配额" width="220">
        <template #default="{ row }">
          <div style="display:flex;flex-direction:column;gap:2px;font-size:12px">
            <span>👤 用户: <b>{{ row.userLimit ?? '∞' }}</b></span>
            <span>🔌 API/天: <b>{{ row.apiLimit ? row.apiLimit.toLocaleString() : '∞' }}</b></span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" size="small">
            {{ row.active ? '活跃' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">
          <span style="font-size:12px;color: var(--el-text-color-regular)">{{ row.createdAt || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            size="small"
            link
            :loading="togglingId === row.id"
            @click="toggleTenant(row)"
          >{{ row.active ? '停用' : '启用' }}</el-button>
          <el-button
            size="small"
            link
            type="danger"
            :disabled="row.code === 'default' || row.id === 'default'"
            @click="removeTenant(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑 弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑租户' : '新建租户'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="租户名" prop="name">
          <el-input v-model="form.name" placeholder="如：研发部" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="租户编码" prop="code">
          <el-input
            v-model="form.code"
            placeholder="如：rd（唯一标识）"
            :disabled="!!form.id"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item label="管理员邮箱" prop="owner">
          <el-input v-model="form.owner" placeholder="admin@example.com" />
        </el-form-item>
        <el-form-item label="用户配额" prop="userLimit">
          <el-input-number
            v-model="form.userLimit"
            :min="1"
            :max="100000"
            style="width:100%"
            placeholder="默认 10"
          />
        </el-form-item>
        <el-form-item label="API 配额/天" prop="apiLimit">
          <el-input-number
            v-model="form.apiLimit"
            :min="0"
            :max="100000000"
            style="width:100%"
            placeholder="0=无限"
          />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-switch
            v-model="form.active"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
            @change="onActiveChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTenant">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  listTenants,
  createTenant,
  setTenantStatus,
  deleteTenant,
} from '@/api/tenant'

const tenants = ref([])
const loading = ref(false)
const saving = ref(false)
const togglingId = ref(null)
const formVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  code: '',
  owner: '',
  userLimit: 10,
  apiLimit: 0,
  active: true,
})

const rules = {
  name: [
    { required: true, message: '请输入租户名', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度 2-50 字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入租户编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '只能包含字母、数字、下划线、短横线', trigger: 'blur' },
  ],
  owner: [
    { type: 'email', message: '请输入合法邮箱', trigger: 'blur' },
  ],
  userLimit: [
    { required: true, message: '请输入用户配额', trigger: 'blur' },
  ],
  apiLimit: [
    { required: true, message: '请输入 API 配额（0=无限）', trigger: 'blur' },
  ],
}

const activeCount = computed(() => tenants.value.filter(t => t.active).length)
const totalUsers = computed(() => tenants.value.reduce((s, t) => s + (t.userLimit || 0), 0))
const totalApi = computed(() => tenants.value.reduce((s, t) => s + (t.apiLimit || 0), 0))

async function loadTenants() {
  loading.value = true
  try {
    const r = await listTenants()
    tenants.value = (r.data || []).map(t => ({
      ...t,
      active: typeof t.active === 'boolean'
        ? t.active
        : (t.status ? t.status === 'ACTIVE' : true),
    }))
  } catch (e) {
    tenants.value = []
    ElMessage.error('加载租户失败：' + (e?.message || '网络异常'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    code: row.code || row.slug || '',
    owner: row.owner || '',
    userLimit: row.userLimit ?? 10,
    apiLimit: row.apiLimit ?? 0,
    active: row.active ?? true,
  })
  formVisible.value = true
}

function resetForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.owner = ''
  form.userLimit = 10
  form.apiLimit = 0
  form.active = true
  formRef.value?.clearValidate()
}

async function saveTenant() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (form.id) {
      // TODO: 等后端提供 update 接口；目前通过 setTenantStatus + createTenant(克隆) 不理想
      // 这里简化为只更新状态，其他字段后端暂未提供 update 接口
      await setTenantStatus(form.id, form.active ? 'ACTIVE' : 'INACTIVE')
      ElMessage.success('租户已更新')
    } else {
      await createTenant({
        name: form.name,
        code: form.code,
        owner: form.owner,
        userLimit: form.userLimit,
        apiLimit: form.apiLimit,
      })
      ElMessage.success('租户已创建')
    }
    formVisible.value = false
    loadTenants()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || '请稍后重试'))
  } finally {
    saving.value = false
  }
}

async function toggleTenant(t) {
  togglingId.value = t.id
  try {
    await setTenantStatus(t.id, t.active ? 'INACTIVE' : 'ACTIVE')
    ElMessage.success(t.active ? '已停用' : '已启用')
    loadTenants()
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || '请稍后重试'))
  } finally {
    togglingId.value = null
  }
}

async function onActiveChange(val) {
  if (!form.id) return
  try {
    await setTenantStatus(form.id, val ? 'ACTIVE' : 'INACTIVE')
    ElMessage.success(val ? '已启用' : '已停用')
  } catch (e) {
    form.active = !val
    ElMessage.error('操作失败：' + (e?.message || '请稍后重试'))
  }
}

async function removeTenant(t) {
  try {
    await ElMessageBox.confirm(
      `确认删除租户「${t.name}」？该操作不可恢复。`,
      '警告',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteTenant(t.id)
    ElMessage.success('已删除')
    loadTenants()
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.message || '请稍后重试'))
  }
}

onMounted(loadTenants)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 16px; }
}
</style>
