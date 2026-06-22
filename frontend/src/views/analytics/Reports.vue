<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📈 报告中心 (V5.31)</span>
          <el-button type="primary" @click="openCreate">生成新报告</el-button>
        </div>
      </template>

      <el-table :data="reports" v-loading="loading" stripe>
        <el-table-column prop="reportId" label="ID" width="200" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type || '通用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewReport(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 生成报告弹窗 -->
    <el-dialog v-model="createVisible" title="生成报告" width="520">
      <el-form :model="form" label-width="100">
        <el-form-item label="数据源">
          <el-select v-model="form.datasourceId" style="width:100%">
            <el-option v-for="s in sources" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分析目标">
          <el-input v-model="form.goal" type="textarea" :rows="3" placeholder="例: 分析用户留存" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 报告查看 -->
    <el-dialog v-model="viewVisible" :title="current?.title" width="800" top="5vh">
      <div v-if="current" class="report-md" v-html="renderMd(current.content || current.markdown || JSON.stringify(current, null, 2))" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDataSources, generateReport, getReport } from '@/api/analytics'

const reports = ref([])
const sources = ref([])
const loading = ref(false)
const createVisible = ref(false)
const viewVisible = ref(false)
const generating = ref(false)
const current = ref(null)
const form = ref({ datasourceId: null, title: '', goal: '' })

function renderMd(md) {
  // 极简 markdown 渲染: 标题 + 段落
  return md
    .replace(/^### (.*)$/gm, '<h4>$1</h4>')
    .replace(/^## (.*)$/gm, '<h3>$1</h3>')
    .replace(/^# (.*)$/gm, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

async function loadReports() {
  loading.value = true
  try {
    reports.value = JSON.parse(localStorage.getItem('minimax-reports') || '[]')
  } finally { loading.value = false }
}

async function loadSources() {
  const res = await listDataSources()
  sources.value = res.data || []
}

function openCreate() {
  form.value = { datasourceId: sources.value[0]?.id || null, title: '', goal: '' }
  createVisible.value = true
}

async function doGenerate() {
  generating.value = true
  try {
    const res = await generateReport(form.value)
    const id = res.data?.reportId
    ElMessage.success('报告生成中, ID: ' + id)
    createVisible.value = false
    // 轮询或直接查看
    setTimeout(async () => {
      const r = await getReport(id)
      const newReport = { reportId: id, ...r.data, title: form.value.title, createdAt: new Date().toLocaleString() }
      reports.value = [newReport, ...reports.value]
      localStorage.setItem('minimax-reports', JSON.stringify(reports.value.slice(0, 20)))
    }, 2000)
  } catch (e) {} finally { generating.value = false }
}

async function viewReport(row) {
  const res = await getReport(row.reportId)
  current.value = res.data
  viewVisible.value = true
}

onMounted(() => { loadReports(); loadSources() })
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.report-md {
  line-height: 1.8; max-height: 70vh; overflow: auto;
  padding: 0 16px;
}
.report-md h2 { border-bottom: 2px solid #409eff; padding-bottom: 8px; }
.report-md h3 { color: #303133; margin-top: 16px; }
.report-md code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; }
</style>
