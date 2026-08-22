<!--
  V6.3+ 首次登录用户引导 (V6.3+ 升级版)
  
  4 步引导:
  1. 介绍平台
  2. 演示 AI 智能填单
  3. 演示对话
  4. 演示知识图谱
-->
<template>
  <div v-if="visible" class="onboarding-mask" @click.self="skip">
    <div class="onboarding-container">
      <div class="onboarding-header">
        <div class="onboarding-progress">
          <div 
            v-for="(s, i) in steps" 
            :key="i" 
            class="progress-dot"
            :class="{ active: i === current, done: i < current }"
          />
        </div>
        <button class="onboarding-skip" @click="skip">跳过</button>
      </div>

      <div class="onboarding-content">
        <div class="onboarding-icon">{{ steps[current].icon }}</div>
        <h2 class="onboarding-title">{{ steps[current].title }}</h2>
        <p class="onboarding-desc">{{ steps[current].desc }}</p>
        
        <div v-if="steps[current].demo" class="onboarding-demo">
          <div v-html="steps[current].demo" />
        </div>
      </div>

      <div class="onboarding-footer">
        <span class="step-info">{{ current + 1 }} / {{ steps.length }}</span>
        <div class="step-buttons">
          <button v-if="current > 0" class="btn btn-ghost" @click="prev">上一步</button>
          <button class="btn btn-primary" @click="next">
            {{ current === steps.length - 1 ? '开始体验' : '下一步' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['finish', 'skip'])

const props = defineProps({
  auto: { type: Boolean, default: false }  // 首次登录自动显示
})

const visible = ref(false)
const current = ref(0)
let showTimer = null

const steps = [
  {
    icon: '🚀',
    title: '欢迎使用 Liugl-AI',
    desc: '这是一个企业级 LLM 应用平台, 自研引擎, 数据私有, 支持智能对话、知识库、Agent 自主任务、知识图谱等 20+ 模块。',
    demo: ''
  },
  {
    icon: '✨',
    title: 'AI 智能填单',
    desc: '所有表单都内置了 AI 智能填单功能。点击"✨ 智能填充"按钮, AI 会自动推荐最合适的字段值。',
    demo: '<div class="demo-form"><input placeholder="表单字段" value="AI 推荐值"><button class="demo-btn">✨ 智能填充</button></div>'
  },
  {
    icon: '💬',
    title: '智能对话',
    desc: '进入"智能对话", 可以向 AI 提问、生成内容、查询数据。支持多轮对话和上下文记忆。',
    demo: '<div class="demo-chat"><div class="msg user">你好</div><div class="msg ai">您好! 我是 AI 助手...</div></div>'
  },
  {
    icon: '🕸️',
    title: '知识图谱',
    desc: '知识图谱模块帮你可视化实体关系, 支持拖拽建边、节点搜索、批量导入导出。',
    demo: '<div class="demo-kg"><span class="node">用户</span><span class="edge">→</span><span class="node">订单</span></div>'
  }
]

function next() {
  if (current.value < steps.length - 1) {
    current.value++
  } else {
    finish()
  }
}

function prev() {
  if (current.value > 0) current.value--
}

function skip() {
  visible.value = false
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('minimax_onboarded', '1')
  }
  emit('skip')
}

function finish() {
  visible.value = false
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('minimax_onboarded', '1')
  }
  emit('finish')
}

onMounted(() => {
  if (props.auto) {
    const onboarded = typeof localStorage !== 'undefined' && localStorage.getItem('minimax_onboarded')
    if (!onboarded) {
      // 1s 后显示, 让用户先看到页面
      showTimer = setTimeout(() => { visible.value = true }, 1000)
    }
  }
})

onBeforeUnmount(() => {
  if (showTimer) {
    clearTimeout(showTimer)
    showTimer = null
  }
})

defineExpose({ show: () => { visible.value = true; current.value = 0 } })
</script>

<style scoped>
.onboarding-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.onboarding-container {
  background: white;
  border-radius: 24px;
  width: 100%;
  max-width: 560px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: onboardingIn 0.3s ease;
}
@keyframes onboardingIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.onboarding-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0 24px;
}
.onboarding-progress {
  display: flex;
  gap: 8px;
}
.progress-dot {
  width: 32px;
  height: 4px;
  border-radius: 2px;
  background: #e5e7eb;
  transition: all 0.3s;
}
.progress-dot.active {
  background: linear-gradient(90deg, #409eff, #a855f7);
}
.progress-dot.done {
  background: #67c23a;
}
.onboarding-skip {
  background: none;
  border: none;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
}
.onboarding-skip:hover {
  color: #333;
}
.onboarding-content {
  padding: 24px 32px 32px 32px;
  text-align: center;
}
.onboarding-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: onboardingBounce 1.5s ease infinite;
}
@keyframes onboardingBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.onboarding-title {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(90deg, #409eff, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 12px 0;
}
.onboarding-desc {
  color: #555;
  font-size: 15px;
  line-height: 1.7;
  margin: 0;
}
.onboarding-demo {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9ff, #f5f3ff);
  border-radius: 12px;
  text-align: left;
}
.demo-form {
  display: flex;
  gap: 8px;
}
.demo-form input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 14px;
}
.demo-btn {
  padding: 8px 16px;
  background: linear-gradient(90deg, #409eff, #a855f7);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.demo-chat {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.demo-chat .msg {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  max-width: 80%;
}
.demo-chat .user {
  background: #e0e7ff;
  align-self: flex-end;
}
.demo-chat .ai {
  background: white;
  border: 1px solid #e0e0e0;
  align-self: flex-start;
}
.demo-kg {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}
.demo-kg .node {
  padding: 8px 16px;
  background: white;
  border: 2px solid #409eff;
  border-radius: 8px;
  font-weight: 600;
  color: #409eff;
}
.demo-kg .edge {
  color: #999;
  font-size: 18px;
}
.onboarding-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px 24px 24px;
  border-top: 1px solid #f0f0f0;
}
.step-info {
  color: #999;
  font-size: 13px;
}
.step-buttons {
  display: flex;
  gap: 8px;
}
.btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}
.btn-ghost {
  background: transparent;
  color: #666;
  border: 1px solid #ddd;
}
.btn-ghost:hover {
  background: #f5f5f5;
}
.btn-primary {
  background: linear-gradient(90deg, #409eff, #a855f7);
  color: white;
}
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
</style>
