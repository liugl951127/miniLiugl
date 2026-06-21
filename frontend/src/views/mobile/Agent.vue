<template>
  <div class="m-agent">
    <van-nav-bar title="Agent 自主任务" fixed :border="false" />

    <div class="content">
      <van-cell-group inset title="🎯 任务目标">
        <van-field
          v-model="goal"
          type="textarea"
          rows="3"
          autosize
          maxlength="500"
          show-word-limit
          placeholder="给 Agent 一个目标, 它会自主规划 → 调工具 → 反思"
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
        loading-text="ReAct 执行中..."
        @click="run"
        class="run-btn"
        :disabled="!goal.trim()"
      >
        <template #icon>
          <span v-if="!running">🚀</span>
        </template>
        {{ running ? 'Agent 思考中...' : '执行 ReAct' }}
      </van-button>

      <!-- 思考过程 -->
      <div v-if="steps.length" class="timeline">
        <van-cell-group inset title="🧠 思考过程">
          <van-steps direction="vertical" :active="steps.length - 1" active-color="#409eff">
            <van-step v-for="(s, i) in steps" :key="i">
              <div class="step-header">
                <van-tag type="primary" size="small">Round {{ s.round }}</van-tag>
                <van-tag :type="actionTagType(s.action)" size="small">{{ s.action }}</van-tag>
                <van-tag v-if="s.durationMs" plain size="small" type="warning">⏱ {{ s.durationMs }}ms</van-tag>
              </div>
              <div class="step-thought" v-if="s.thinking">
                <van-icon name="chat-o" size="12" />💭 {{ s.thinking }}
              </div>
              <div class="step-args" v-if="s.arguments">
                <van-icon name="down" size="12" />📥 {{ s.arguments }}
              </div>
              <div class="step-obs" v-if="s.observation">
                <van-icon name="eye-o" size="12" />👁 {{ s.observation }}
              </div>
            </van-step>
          </van-steps>
        </van-cell-group>
      </div>

      <!-- 最终答案 -->
      <div v-if="finalAnswer" class="final-answer">
        <van-cell-group inset title="✨ 最终答案">
          <div class="answer-content">
            <MarkdownView :content="finalAnswer" />
          </div>
        </van-cell-group>
      </div>

      <!-- 空状态提示 -->
      <van-empty
        v-if="!steps.length && !finalAnswer && !running"
        description="输入任务目标开始执行"
        class="empty-hint"
      />
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

function actionTagType(action: string) {
  const map: Record<string, string> = {
    think: 'primary', execute: 'success', search: 'warning',
    calculate: 'default', done: 'primary', finish: 'success'
  }
  return (map[action] || 'default') as any
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
.timeline { padding: 0 16px; }
.step-header { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 6px; }
.step-thought, .step-args, .step-obs {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 4px;
  margin: 4px 0;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}
.step-thought { background: #f0f9ff; color: #409eff; }
.step-args { background: #fdf6ec; color: #e6a23c; }
.step-obs { background: #f0f9eb; color: #67c23a; }
.final-answer { padding: 0 16px 16px; }
.answer-content { padding: 0 16px 16px; font-size: 14px; line-height: 1.6; }
.empty-hint { margin-top: 40px; }
</style>
