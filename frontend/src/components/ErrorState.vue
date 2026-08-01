<!--
  V3.6.18+ 统一错误态组件
  替代 22 view 散乱的错误提示
  支持 6 类错误 (401/403/404/500+/网络/业务)
  简化: 不调 useErrorHandler, 避免循环更新 (V3.6.18 修复)
-->
<template>
  <div class="error-state" :class="`error-${type}`">
    <div class="error-icon">
      <span class="error-emoji">{{ emoji }}</span>
    </div>
    <h3 class="error-title">{{ title }}</h3>
    <p class="error-desc">{{ description }}</p>
    <div v-if="detailText && showDetail" class="error-detail">
      <code>{{ detailText }}</code>
    </div>
    <div class="error-actions">
      <el-button :icon="Refresh" @click="emit('retry')">重试</el-button>
      <el-button :icon="Back" @click="goHome">返回首页</el-button>
      <el-button v-if="showDemo" type="primary" :icon="Promotion" @click="goDemo">
        访客试用
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Back, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  error: { type: [Error, Object], default: null },
  // V3.6.18+ errorType 必填, 不再 derived computed
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

// V3.6.18+ 全部用 computed, 但只用 props (不变依赖)
const type = computed(() => props.errorType)

const emoji = computed(() => {
  const map = {
    auth: '🔒',
    forbidden: '🚫',
    notfound: '🔍',
    server: '💥',
    network: '📡',
    business: '⚠️',
    unknown: '❓',
  }
  return map[props.errorType] || map.unknown
})

const title = computed(() => {
  const map = {
    auth: '需要登录',
    forbidden: '无权限访问',
    notfound: '资源不存在',
    server: '服务器错误',
    network: '网络异常',
    business: '操作失败',
    unknown: '出错了',
  }
  return map[props.errorType] || map.unknown
})

const description = computed(() => {
  const map = {
    auth: '登录已过期或权限不足, 请重新登录',
    forbidden: '您没有权限访问此资源',
    notfound: '请求的资源不存在或已被删除',
    server: '服务暂时不可用, 请稍后重试',
    network: '请检查网络连接',
    business: '操作未能完成, 请稍后重试',
    unknown: '请稍后重试或联系管理员',
  }
  return map[props.errorType] || map.unknown
})

const detailText = computed(() => {
  if (props.error instanceof Error) return props.error.message
  if (typeof props.error === 'string') return props.error
  return ''
})

const showDemo = computed(() => {
  // V3.6.18+ 简化: 不查 userStore, 只看 isDemo prop
  return props.isDemo
})

function goHome() {
  router.push('/')
}

function goDemo() {
  router.push('/chat?demo=1')
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
