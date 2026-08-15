<!-- @file agent/Multi.vue - 多智能体协作 V6.8 -->
<template>
  <div class="page-card">
    <div class="page-header"><h2>多智能体协作</h2></div>
    <el-card title="协作设置">
      <el-form label-width="100px">
        <el-form-item label="主 Agent">
          <el-select v-model="config.mainAgent" style="width:100%">
            <el-option label="通用助手" value="general" />
            <el-option label="代码助手" value="code" />
            <el-option label="分析助手" value="analytics" />
          </el-select>
        </el-form-item>
        <el-form-item label="从属 Agent">
          <el-checkbox-group v-model="config.subAgents">
            <el-checkbox label="research">调研员</el-checkbox>
            <el-checkbox label="writer">撰写员</el-checkbox>
            <el-checkbox label="reviewer">审核员</el-checkbox>
            <el-checkbox label="executor">执行员</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="config.task" type="textarea" :rows="4" placeholder="描述协作任务..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="running" @click="startMulti">启动协作</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="results.length" title="协作结果" style="margin-top:16px">
      <div v-for="(r, i) in results" :key="i" style="margin-bottom:12px;padding-bottom:12px;border-bottom:1px solid #f0">
        <el-tag size="small" style="margin-bottom:4px">{{ r.agent }}</el-tag>
        <div style="font-size:13px">{{ r.output }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentApi } from '@/api/agent'
const runMultiAgent = (config) => agentApi.execute(config)

const config = ref({ mainAgent: 'general', subAgents: [], task: '' })
const running = ref(false)
const results = ref([])

async function startMulti() {
  if (!config.value.task.trim()) { ElMessage.warning('请输入任务'); return }
  running.value = true
  results.value = []
  try {
    const r = await runMultiAgent(config.value)
    results.value = r.data || []
    ElMessage.success('协作完成')
  } catch { ElMessage.error('执行失败') }
  finally { running.value = false }
}
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; h2 { margin: 0; font-size: 16px; } }
</style>
