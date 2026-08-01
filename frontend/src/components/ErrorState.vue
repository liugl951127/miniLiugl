<!--
  V3.6.10+ 统一错误态组件
  替代 22 view 散乱的错误提示
  支持 6 类错误 (401/403/404/500+/网络/业务)
-->
<template>
  <div class="error-state" :class="`error-${errorType}`">
    <div class="error-icon">
      <span class="error-emoji">{{ errorEmoji }}</span>
    </div>
    <h3 class="error-title">{{ errorTitle }}</h3>
    <p class="error-desc">{{ errorDescription }}</p>
    <div v-if="errorDetail && showDetail" class="error-detail">
      <code>{{ errorDetail }}</code>
    </div>
    <div class="error-actions">
      <el-button :icon="Refresh" @click="handleRetry">重试</el-button>
      <el-button :icon="Back" @click="handleHome">返回首页</el-button>
      <el-button v-if="isDemo" type="primary" :icon="Promotion" @click="handleDemo">
        访客试用
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useUserStore } from '@/store/user'
import { Refresh, Back, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  error: { type: [Error, Object], default: null },
  errorType: {
    type: String,
    default: 'unknown',
    validator: (v) => ['auth', 'forbidden', 'notfound', 'server', 'network', 'business', 'unknown'].includes(v),
  },
  showDetail: { type: Boolean, default: false },
  isDemo: { type: Boolean, default: false },
})

const emit = defineEmits(['retry'])
const router = useRouter()
const userStore = useUserStore()
const { errorClassify } = useErrorHandler()

const classified = computed(() => {
  if (props.error) return errorClassify(props.error)
  return { type: props.errorType, title: '', description: '' }
})

const errorEmoji = computed(() => {
  const map = {
    auth: '🔒',
    forbidden: '🚫',
    notfound: '🔍',
    server: '💥',
    network: '📡',
    business: '⚠️',
    unknown: '❓',
  }
  return map[classified.value.type] || map.unknown
})

const errorTitle = computed(() => {
  return classified.value.title || '出错了'
})

const errorDescription = computed(() => {
  return classified.value.description || '请稍后重试或联系管理员'
})

const errorDetail = computed(() => {
  if (props.error instanceof Error) return props.error.message
  if (typeof props.error === 'string') return props.error
  return ''
})

function handleRetry() {
  emit('retry')
}

function handleHome() {
  router.push('/')
}

function handleDemo() {
  userStore.enterDemoMode?.()
  router.push('/chat')
}
</script>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  min-height: 360px;
}
.error-icon {
  font-size: 80px;
  margin-bottom: 16px;
}
.error-emoji {
  line-height: 1;
}
.error-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
}
.error-desc {
  margin: 0 0 16px;
  font-size: 14px;
  color: #64748b;
  max-width: 400px;
  line-height: 1.6;
}
.error-detail {
  margin-bottom: 16px;
  max-width: 600px;
}
.error-detail code {
  display: block;
  padding: 10px 12px;
  background: #f1f5f9;
  border-radius: 4px;
  font-size: 12px;
  color: #475569;
  text-align: left;
  word-break: break-all;
}
.error-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.error-server .error-title { color: #dc2626; }
.error-network .error-title { color: #f59e0b; }
</style>
