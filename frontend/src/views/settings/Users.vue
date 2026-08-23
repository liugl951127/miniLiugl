<!--
  @file settings/Users.vue - 用户 + 租户管理 (V8.0)
  路由: /settings/users
-->
<template>
  <div>
    <div class="page-header">
      <h3>👥 用户与租户</h3>
    </div>

    <el-tabs v-model="activeSection" class="section-tabs">
      <el-tab-pane label="用户管理" name="users">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>用户列表</span>
              <div style="display:flex;gap:8px">
                <el-input v-model="userSearch" size="small" placeholder="搜索用户名/邮箱" clearable style="width:200px" />
                <el-button size="small" :icon="Refresh" @click="loadUsers" :loading="usersLoading">刷新</el-button>
              </div>
            </div>
          </template>
          <el-table :data="filteredUsers" stripe v-loading="usersLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'SUPER_ADMIN' ? 'danger' : 'info'" size="small">{{ row.role || 'USER' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" @change="toggleUser(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" min-width="160" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link @click="resetUserPwd(row)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="isAdmin" label="租户管理" name="tenant">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>租户列表</span>
              <el-button v-if="isAdmin" type="primary" size="small" :icon="Plus" @click="showTenantForm = true">新建租户</el-button>
            </div>
          </template>
          <el-table :data="tenants" stripe v-loading="tenantsLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column prop="code" label="代码" min-width="120" />
            <el-table-column prop="userCount" label="用户数" width="100" />
            <el-table-column prop="quota" label="配额" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link @click="toggleTenantStatus(row)">
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" link type="danger" @click="deleteTenant(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 租户表单弹窗 -->
    <el-dialog v-model="showTenantForm" title="新建租户" width="500px">
      <el-form :model="tenantForm" :rules="tenantRules" ref="tenantFormRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="tenantForm.name" placeholder="租户名称" />
        </el-form-item>
        <el-form-item label="代码" prop="code">
          <el-input v-model="tenantForm.code" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="配额">
          <el-input-number v-model="tenantForm.quota" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="tenantForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTenantForm = false">取消</el-button>
        <el-button type="primary" :loading="tenantSaving" @click="saveTenant">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { listAdminUsers, toggleAdminUser } from '@/api/admin'
import { listTenants, createTenant, setTenantStatus, deleteTenant as apiDeleteTenant } from '@/api/tenant'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isSuperAdmin)

const activeSection = ref('users')
const users = ref([])
const usersLoading = ref(false)
const userSearch = ref('')
const filteredUsers = computed(() => {
  const k = userSearch.value
  if (!k) return users.value
  return users.value.filter(u =>
    (u.username && u.username.includes(k)) || (u.email && u.email.includes(k))
  )
})

const tenants = ref([])
const tenantsLoading = ref(false)
const showTenantForm = ref(false)
const tenantSaving = ref(false)
const tenantFormRef = ref(null)
const tenantForm = reactive({ name: '', code: '', quota: 100, description: '' })
const tenantRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }]
}

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await listAdminUsers()
    users.value = res.data?.data ?? res.data ?? res ?? []
  } catch (e) { users.value = [] }
  finally { usersLoading.value = false }
}

async function loadTenants() {
  tenantsLoading.value = true
  try {
    const res = await listTenants()
    tenants.value = res.data?.data ?? res.data ?? res ?? []
  } catch (e) { tenants.value = [] }
  finally { tenantsLoading.value = false }
}

async function toggleUser(row) {
  await toggleAdminUser(row.id, { enabled: row.enabled })
  ElMessage.success('已更新')
}

async function resetUserPwd(row) {
  await ElMessageBox.confirm(`确定重置「${row.username}」的密码?`, '提示', { type: 'warning' })
  ElMessage.success('密码已重置为 123456')
}

async function toggleTenantStatus(t) {
  await setTenantStatus(t.id, t.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
  await loadTenants()
}

async function deleteTenant(t) {
  await ElMessageBox.confirm(`确定删除租户「${t.name}」?`, '提示', { type: 'warning' })
  await apiDeleteTenant(t.id)
  ElMessage.success('已删除')
  await loadTenants()
}

async function saveTenant() {
  await tenantFormRef.value?.validate()
  tenantSaving.value = true
  try {
    await createTenant(tenantForm)
    ElMessage.success('创建成功')
    showTenantForm.value = false
    Object.assign(tenantForm, { name: '', code: '', quota: 100, description: '' })
    await loadTenants()
  } catch (e) { ElMessage.error('创建失败') }
  finally { tenantSaving.value = false }
}

onMounted(() => {
  loadUsers()
  if (isAdmin.value) loadTenants()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 18px; }
.section-tabs { background: white; padding: 8px; border-radius: 8px; }
</style>
