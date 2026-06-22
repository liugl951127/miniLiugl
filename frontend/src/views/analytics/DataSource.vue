<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📊 数据源管理 (V5.31)</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建数据源</el-button>
        </div>
      </template>

      <el-table :data="sources" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jdbcUrl" label="JDBC URL" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="testOne(row)">测试</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑数据源' : '新建数据源'" width="560">
      <el-form :model="form" label-width="100">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="ClickHouse" value="clickhouse" />
            <el-option label="Doris" value="doris" />
          </el-select>
        </el-form-item>
        <el-form-item label="JDBC URL">
          <el-input v-model="form.jdbcUrl" placeholder="jdbc:mysql://host:3306/db?..." />
        </el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listDataSources, createDataSource, updateDataSource, deleteDataSource, testDataSource
} from '@/api/analytics'

const sources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({ id: null, name: '', type: 'mysql', jdbcUrl: '', username: '', password: '' })

async function load() {
  loading.value = true
  try {
    const res = await listDataSources()
    sources.value = res.data || []
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

function openCreate() {
  form.value = { id: null, name: '', type: 'mysql', jdbcUrl: '', username: '', password: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await updateDataSource(form.value.id, form.value)
    } else {
      await createDataSource(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (e) {}
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除数据源 ${row.name}?`, '提示', { type: 'warning' })
    await deleteDataSource(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') {} }
}

async function testOne(row) {
  try {
    const res = await testDataSource({ id: row.id })
    if (res.data?.success) ElMessage.success('连接成功')
    else ElMessage.error(res.data?.message || '连接失败')
  } catch (e) {}
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
