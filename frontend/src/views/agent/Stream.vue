<!-- @file agent/Stream.vue - Agent 流式 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header"><h2>Agent 流式执行</h2></div>
    <el-input v-model="task" type="textarea" :rows="3" placeholder="描述 Agent 任务..." style="margin-bottom:12px" />
    <el-button type="primary" :loading="running" @click="runAgent">执行</el-button>

    <el-card style="margin-top:16px" title="执行日志">
      <el-scrollbar style="height:400px" v-loading="running">
        <div v-for="(step, i) in steps" :key="i" class="step-item">
          <el-tag size="small" :type="step.status === 'done' ? 'success' : step.status === 'error' ? 'danger' : 'primary'">
            {{ step.tool }}
          </el-tag>
          <div class="step-result">{{ step.result }}</div>
        </div>
        <el-empty v-if="!running && !steps.length" description="暂无执行记录" :image-size="80" style="padding:24px 0" />
      </el-scrollbar>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentApi } from '@/api/agent'

const task = ref('')
const running = ref(false)
const steps = ref([])

async function runAgent() {
  if (!task.value.trim()) { ElMessage.warning('请输入任务'); return }
  running.value = true
  steps.value = []
  try {
    const r = await agentApi.execute({ goal: task.value })
    const data = r.data || {}
    // 支持 steps 数组和 steps 字符串两种格式
    const rawSteps = data.steps || (typeof data.step === 'string' ? [data] : [])
    for (const s of rawSteps) {
      steps.value.push({
        tool: s.tool || s.node || 'LLM',
        result: s.result || s.output || s.thought || '',
        status: s.status === 'error' ? 'error' : 'done',
      })
    }
    if (!steps.value.length) {
      steps.value.push({ tool: 'Agent', result: data.result || data.output || data.response || '执行完成', status: 'done' })
    }
  } catch (e) {
    steps.value.push({ tool: '错误', result: e.message || '执行失败', status: 'error' })
    ElMessage.error('执行失败：' + (e.message || ''))
  } finally {
    running.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
.step-item { margin-bottom: 12px; }
.step-result { margin-top: 4px; font-size: 12px; color: #666; white-space: pre-wrap; }
</style>
