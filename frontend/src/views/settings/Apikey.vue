<!--
  @file settings/Apikey.vue - API Key 管理 (V8.0)
  路由: /settings/apikey
-->
<template>
  <div>
    <div class="page-header">
      <h3>🔑 API Key 管理</h3>
    </div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>已生成的 Key</span>
          <el-button type="primary" size="small" :icon="Plus" @click="showCreate = true">生成 Key</el-button>
        </div>
      </template>

      <el-row :gutter="12" style="margin-bottom:12px">
        <el-col :span="6">
          <el-card body-style="padding:14px" shadow="hover">
            <div style="font-size:12px;color:var(--el-text-color-secondary)">总数</div>
            <div style="font-size:24px;font-weight:700;color:var(--el-color-primary)">{{ keys.length }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card body-style="padding:14px" shadow="hover">
            <div style="font-size:12px;color:var(--el-text-color-secondary)">已启用</div>
            <div style="font-size:24px;font-weight:700;color:var(--el-color-success)">{{ activeCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card body-style="padding:14px" shadow="hover">
            <div style="font-size:12px;color:var(--el-text-color-secondary)">总调用</div>
            <div style="font-size:24px;font-weight:700;color:var(--el-color-warning)">{{ totalCalls }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card body-style="padding:14px" shadow="hover">
            <div style="font-size:12px;color:var(--el-text-color-secondary)">今日调用</div>
            <div style="font-size:24px;font-weight:700;color:var(--el-color-info)">{{ todayCalls }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="keys" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="Key" min-width="280">
          <template #default="{ row }">
            <code style="font-family:monospace;font-size:12px;background:#f1f5f9;padding:2px 6px;border-radius:4px">
              {{ row.keyPrefix }}...{{ row.keySuffix }}
            </code>
            <el-button size="small" link :icon="CopyDocument" @click="copyKey(row)">复制</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="scopes" label="权限" width="120">
          <template #default="{ row }">
            <el-tag v-for="s in (row.scopes || ['read'])" :key="s" size="small" style="margin:1px">{{ s }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleKey(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="calls" label="调用" width="100" sortable />
        <el-table-column prop="lastUsed" label="最后使用" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="editKey(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="deleteKey(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建弹窗 -->
    <el-dialog v-model="showCreate" title="生成新 Key" width="480px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如: 生产环境 Key" />
        </el-form-item>
        <el-form-item label="权限" prop="scopes">
          <el-checkbox-group v-model="form.scopes">
            <el-checkbox value="read">读取</el-checkbox>
            <el-checkbox value="write">写入</el-checkbox>
            <el-checkbox value="admin">管理</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="form.expiresAt" type="datetime" placeholder="选择过期时间" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveKey">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CopyDocument } from '@element-plus/icons-vue'
import { apiKeyApi } from '@/api/apikey'

const keys = ref([])
const loading = ref(false)
const showCreate = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', scopes: ['read'], expiresAt: null })
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  scopes: [{ required: true, message: '请选择权限', trigger: 'change' }]
}

const activeCount = computed(() => keys.value.filter(k => k.enabled).length)
const totalCalls = computed(() => keys.value.reduce((s, k) => s + (k.calls || 0), 0))
const todayCalls = computed(() => 0)

async function loadAll() {
  loading.value = true
  try {
    const res = await apiKeyApi.list()
    keys.value = res.data?.data ?? res.data ?? res ?? []
  } catch (e) { keys.value = [] }
  finally { loading.value = false }
}

async function saveKey() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const res = await apiKeyApi.create(form)
    if (res.code === 0) {
      const newKey = res.data?.key || 'sk-xxx'
      ElMessageBox.alert(`请妥善保存此 Key: ${newKey}`, '生成成功', { type: 'success' })
    } else ElMessage.error(res.message)
    showCreate.value = false
    loadAll()
  } finally { saving.value = false }
}

async function toggleKey(row) {
  await apiKeyApi.update(row.id, { enabled: row.enabled })
  ElMessage.success('已更新')
}

function copyKey(row) {
  navigator.clipboard?.writeText(row.keyPrefix + '...' + row.keySuffix)
  ElMessage.success('已复制')
}

function editKey(row) {
  Object.assign(form, { name: row.name, scopes: row.scopes || ['read'], expiresAt: row.expiresAt })
  showCreate.value = true
}

async function deleteKey(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」?`, '提示', { type: 'warning' })
  await apiKeyApi.remove(row.id)
  ElMessage.success('已删除')
  loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 18px; }
</style>
