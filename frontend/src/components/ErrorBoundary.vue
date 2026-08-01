<!--
  @file components/ErrorBoundary.vue (V3.5.93+)
  全局错误边界 - 捕获子组件错误, 显示友好错误页
  V3.6.18+ 修循环更新: errorType 用 function 而非 computed
-->
<template>
  <slot v-if="!error" />
  <!-- V3.6.18+ 修复循环更新, errorType 用 function 而非 computed -->
  <ErrorState
    v-else
    :error="error"
    :error-type="getErrorType(error)"
    :show-detail="true"
    @retry="reload"
  />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import ErrorState from './ErrorState.vue'
import { useRouter } from 'vue-router'

const error = ref(null)
const router = useRouter()

onErrorCaptured((err) => {
  // V3.6.18+ 防止循环: error 改变时强制终止
  if (error.value) return false
  error.value = err
  return false  // 不向上抛
})

function reload() {
  location.reload()
}
function goHome() {
  router.push('/')
}
function goChat() {
  router.push('/chat')
}

// V3.6.18+ 改用 function 而非 computed - 避免依赖追踪
function getErrorType(err) {
  if (!err) return 'unknown'
  if (err?.response?.status === 401) return 'auth'
  if (err?.response?.status === 403) return 'forbidden'
  if (err?.response?.status === 404) return 'notfound'
  if (err?.response?.status >= 500) return 'server'
  if (err?.message?.includes('network') || err?.code === 'NETWORK_ERROR') return 'network'
  if (err?.response?.data?.code && err.response.data.code !== 0) return 'business'
  return 'unknown'
}
</script>

<style lang="scss" scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 40px 20px;
}

.error-content {
  max-width: 600px;
  text-align: center;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

h1 {
  font-size: 24px;
  color: #1e293b;
  margin: 16px 0 8px;
}

.error-message {
  color: #ef4444;
  font-size: 14px;
  font-family: monospace;
  background: #fef2f2;
  padding: 8px 12px;
  border-radius: 6px;
  display: inline-block;
  margin: 8px 0;
}

.error-hint {
  color: #94a3b8;
  font-size: 12px;
  margin: 8px 0 24px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.error-stack {
  margin-top: 24px;
  text-align: left;
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 12px;
  border-radius: 6px;
  max-height: 200px;
  overflow: auto;
}

pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: monospace;
}
</style>
