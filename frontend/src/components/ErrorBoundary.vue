<!--
  @file components/ErrorBoundary.vue (V3.5.93+)
  全局错误边界 - 捕获子组件错误, 显示友好错误页
-->
<template>
  <slot v-if="!error" />
  <!-- V3.6.12+ 集成 ErrorState 组件 -->
  <ErrorState
    v-else
    :error="error"
    :error-type="errorType"
    :show-detail="true"
    @retry="reload"
  />
</template>

<script setup>
import { ref, onErrorCaptured, computed } from 'vue'
import ErrorState from './ErrorState.vue'

import { useRouter } from 'vue-router'

const error = ref(null)
const router = useRouter()

onErrorCaptured((err) => {
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
