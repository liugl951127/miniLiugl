<template>
  <div class="m-agent">
    <van-nav-bar title="Agent 自主任务" fixed :border="false" />

    <div class="content">
      <van-cell-group inset title="任务目标">
        <van-field
          v-model="goal"
          type="textarea"
          rows="3"
          autosize
          placeholder="给 Agent 一个目标, 它会自主规划→调工具→反思"
        />
      </van-cell-group>

      <div class="quick-tips">
        <van-tag v-for="t in tips" :key="t" plain type="primary" @click="goal = t" class="tip">
          {{ t }}
        </van-tag>
      </div>

      <van-button
        type="primary"
        block
        :loading="running"
        @click="run"
        class="run-btn"
      >
        🚀 执行 ReAct
      </van-button>

      <!-- 思考过程 -->
      <div v-if="steps.length" class="timeline">
        <van-steps direction="vertical" :active="steps.length - 1" active-color="#ee0a24">
          <van-step v-for="(s, i) in steps" :key="i">
            <h4>Round {{ s.round }} - {{ s.action }}</h4>
            <div class="step-thought" v-if="s.thinking">💭 {{ s.thinking }}</div>
            <div class="step-args" v-if="s.arguments">📥 {{ s.arguments }}</div>
            <div class="step-obs" v-if="s.observation">👁️ {{ s.observation }}</div>
            <div class="step-time" v-if="s.durationMs">⏱️ {{ s.durationMs }}ms</div>
          </van-step>
        </van-steps>
      </div>

      <!-- 最终答案 -->
      <div v-if="finalAnswer" class="final-answer">
        <van-cell-group inset title="✨ 最终答案">
          <div class="answer-content">
            <MarkdownView :content="finalAnswer" />
          </div>
        </van-cell-group>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showToast } from 'vant'
import axios from 'axios'
import MarkdownView from '@/components/MarkdownView.vue'
import { useUserStore } from '@/store/user'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()

const goal = ref('查一下北京明天天气, 然后用 calculator 算 100-25*3')
const tips = [
  '查北京天气',
  '算 123 * 456 - 789',
  '生成一段问候语',
  '查询我的历史',
]
const running = ref(false)
const steps = ref<any[]>([])
const finalAnswer = ref('')

function auth() {
  return { headers: { Authorization: `Bearer ${userStore.accessToken}` } }
}

async function run() {
  if (!goal.value.trim()) {
    showToast('请输入任务目标')
    return
  }
  running.value = true
  steps.value = []
  finalAnswer.value = ''
  try {
    const { data } = await axios.post(`${API}/api/v1/agent/run`, {
      userId: userStore.profile?.id || 1,
      goal: goal.value,
      tools: ['get_time', 'calculator', 'http_get', 'random_number'],
    }, auth())
    const r = data.data
    steps.value = r.steps || []
    finalAnswer.value = r.success ? r.answer : '❌ ' + (r.answer || '执行失败')
  } catch (e: any) {
    showToast('执行失败: ' + (e?.response?.data?.message || e?.message))
  } finally {
    running.value = false
  }
}
</script>

<style scoped>
.m-agent { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 60px; }
.quick-tips { padding: 8px 16px; display: flex; flex-wrap: wrap; gap: 8px; }
.tip { cursor: pointer; }
.run-btn { margin: 16px; }
.timeline { padding: 16px; }
.step-thought, .step-args, .step-obs, .step-time {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 4px;
  margin: 4px 0;
  line-height: 1.4;
}
.step-thought { background: #f0f9ff; }
.step-args { background: #fdf6ec; }
.step-obs { background: #f0f9eb; }
.step-time { color: #909399; }
.final-answer { padding: 16px; }
.answer-content { padding: 0 16px 16px; font-size: 14px; line-height: 1.6; }
</style>
