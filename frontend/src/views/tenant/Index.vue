<!-- @file tenant/Index.vue - 租户管理 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>租户管理</h2>
      <el-button type="primary" @click="form = {}; formVisible = true"><el-icon><Plus /></el-icon>新建租户</el-button>
    </div>

    <el-table :data="tenants" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="租户名" />
      <el-table-column prop="owner" label="管理员" />
      <el-table-column label="配额" width="180">
        <template #default="{ row }">
          <span>用户: {{ row.userLimit }} | API: {{ row.apiLimit }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '活跃' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="form = {...row}; formVisible = true">编辑</el-button>
          <el-button size="small" type="danger" @click="toggleTenant(row)">{{ row.active ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑租户' : '新建租户'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="租户名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="管理员邮箱"><el-input v-model="form.owner" /></el-form-item>
        <el-form-item label="用户配额"><el-input-number v-model="form.userLimit" :min="1" /></el-form-item>
        <el-form-item label="API 配额/天"><el-input-number v-model="form.apiLimit" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTenant">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listTenants, createTenant, setTenantStatus } from '@/api/tenant'
import { Plus } from '@element-plus/icons-vue'

const tenants = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = ref({})

async function loadTenants() {
  loading.value = true
  try { tenants.value = (await listTenants()).data || [] }
  catch { tenants.value = [] }
  finally { loading.value = false }
}

async function saveTenant() {
  try { await createTenant(form.value); ElMessage.success('保存成功'); formVisible.value = false; loadTenants() }
  catch { ElMessage.error('保存失败') }
}

async function toggleTenant(t) {
  try { await setTenantStatus(t.id, t.active ? 'INACTIVE' : 'ACTIVE'); ElMessage.success('已更新'); loadTenants() }
  catch { ElMessage.error('操作失败') }
}

onMounted(loadTenants)
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
</style>
