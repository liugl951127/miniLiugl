<!--
  @file views/analytics/Nl2Sql.vue (Nl2Sql 页面)
  @version V3.5.12+ (前端注释补全)
  @description Nl2Sql 页面
-->
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>💬 NL2SQL 实验室 (V5.31)</span>
          <span style="color:#909399;font-size:12px">用自然语言问数据库, 自动生成 SQL</span>
        </div>
      </template>

      <div class="layout">
        <div class="left">
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            placeholder="例: 统计最近 7 天每天的新增用户数"
          />
          <div style="margin-top:8px">
            <el-button type="primary" :loading="asking" @click="ask">生成 SQL</el-button>
            <el-button @click="explain" :disabled="!result?.sql">解释 SQL</el-button>
            <el-button @click="dryRun" :disabled="!result?.sql" type="warning">试运行</el-button>
          </div>

          <el-divider>历史</el-divider>
          <el-timeline>
            <el-timeline-item v-for="h in history" :key="h.id" :timestamp="h.createdAt">
              <div class="hist-q">{{ h.question }}</div>
              <code class="hist-sql">{{ h.sql }}</code>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="right">
          <template v-if="result">
            <h4>生成的 SQL</h4>
            <pre class="sql-block">{{ result.sql }}</pre>
            <h4>说明</h4>
            <p>{{ result.explanation }}</p>

            <template v-if="dryRunResult">
              <h4>试运行结果</h4>
              <el-table :data="dryRunResult.rows" stripe size="small" max-height="300">
                <el-table-column
                  v-for="col in dryRunResult.columns"
                  :key="col"
                  :prop="col"
                  :label="col"
                />
              </el-table>
              <p style="color:#909399;font-size:12px;margin-top:8px">
                共 {{ dryRunResult.total }} 行, 限制 1000 行预览
              </p>
            </template>
          </template>
          <el-empty v-else description="左侧输入问题, 点击生成" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { nl2sqlAsk, nl2sqlExplain, nl2qlFeedback, nl2sqlHistory, dryRunQuery } from '@/api/analytics'

const question = ref('')
const asking = ref(false)
const result = ref(null)
const dryRunResult = ref(null)
const history = ref([])

async function ask() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  asking.value = true
  try {
    const res = await nl2sqlAsk({ question: question.value })
    result.value = res.data
    dryRunResult.value = null
    loadHistory()
  } catch (e) {} finally { asking.value = false }
}

async function explain() {
  if (!result.value?.sql) return
  const res = await nl2sqlExplain(result.value.sql)
  result.value = { ...result.value, explanation: res.data?.explanation }
}

async function dryRun() {
  if (!result.value?.sql) return
  const res = await dryRunQuery({ sql: result.value.sql, datasourceId: result.value.datasourceId })
  dryRunResult.value = res.data
}

async function loadHistory() {
  const res = await nl2sqlHistory({ page: 1, size: 10 })
  history.value = res.data?.records || []
}

onMounted(loadHistory)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.layout { display: flex; gap: 16px; }
.left, .right { flex: 1; }
.sql-block {
  background: #2d3748; color: #e2e8f0; padding: 12px;
  border-radius: 4px; font-size: 13px; overflow: auto;
}
.hist-q { font-size: 13px; }
.hist-sql { font-size: 11px; color: #909399; }
</style>
