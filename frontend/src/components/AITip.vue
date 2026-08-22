<!--
  V6.3+ AI 智能提示组件
  显示上下文相关的下一步操作建议
-->
<template>
  <transition name="el-fade-in">
    <div v-if="visible" class="ai-tip" :class="`tip-${type}`">
      <div class="tip-icon">
        <el-icon><MagicStick /></el-icon>
      </div>
      <div class="tip-content">
        <div class="tip-title">{{ title }}</div>
        <div class="tip-desc">{{ description }}</div>
      </div>
      <div v-if="action" class="tip-action">
        <el-button type="primary" size="small" @click="onAction">
          {{ action.label }}
        </el-button>
      </div>
      <el-button text :icon="Close" @click="dismiss" class="tip-close" />
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Close } from '@element-plus/icons-vue'

const props = defineProps({
  context: { type: String, required: true },
  dismissKey: { type: String, default: null }
})

const router = useRouter()
const visible = ref(true)

const TIPS = {
  'no-data': {
    title: '还没有数据',
    description: '系统检测到当前模块没有数据, 是否立即创建一个?',
    action: { label: '立即创建', type: 'create' },
    type: 'info'
  },
  'first-login': {
    title: '👋 欢迎使用',
    description: '建议先创建一个 API Key, 然后开始对话',
    action: { label: '前往创建', to: '/apikey' },
    type: 'success'
  },
  'admin-new': {
    title: '💡 提示',
    description: '您是管理员, 建议先了解系统架构和用户管理',
    action: { label: '查看文档', type: 'docs' },
    type: 'info'
  },
  'chat-empty': {
    title: '开始第一次对话',
    description: '试试输入: "你好, 请介绍一下自己"',
    action: null,
    type: 'primary'
  },
  'usage-low': {
    title: '📊 提示',
    description: '过去 7 天使用率较低, 可考虑活跃一下账号',
    action: { label: '查看统计', to: '/admin/stats' },
    type: 'warning'
  }
}

const tip = computed(() => TIPS[props.context] || {
  title: '💡 提示',
  description: '系统检测到当前状态, 请关注',
  action: null,
  type: 'info'
})

const title = computed(() => tip.value.title)
const description = computed(() => tip.value.description)
const action = computed(() => tip.value.action)
const type = computed(() => tip.value.type || 'info')

onMounted(() => {
  if (props.dismissKey) {
    const dismissed = localStorage.getItem(`ai_tip_dismissed_${props.dismissKey}`)
    if (dismissed) visible.value = false
  }
})

function onAction() {
  if (action.value?.to) {
    router.push(action.value.to)
  } else if (action.value?.type === 'create') {
    ElMessage.info('点击"创建"按钮开始')
  } else if (action.value?.type === 'docs') {
    window.open('https://github.com/liugl951127/miniLiugl', '_blank')
  }
}

function dismiss() {
  visible.value = false
  if (props.dismissKey) {
    localStorage.setItem(`ai_tip_dismissed_${props.dismissKey}`, '1')
  }
}
</script>

<style lang="scss" scoped>
.ai-tip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 16px;
}
.tip-info { background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%); border: 1px solid #93c5fd; }
.tip-success { background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%); border: 1px solid #6ee7b7; }
.tip-warning { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); border: 1px solid #fcd34d; }
.tip-primary { background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%); border: 1px solid #c4b5fd; }
.tip-icon { font-size: 24px; color: #6366f1; flex-shrink: 0; }
.tip-content { flex: 1;
  .tip-title { font-weight: 600; color: #1e293b; margin-bottom: 2px; }
  .tip-desc { font-size: 13px; color: #475569; }
}
.tip-action { flex-shrink: 0; }
.tip-close { flex-shrink: 0; margin-left: 8px; }
</style>
