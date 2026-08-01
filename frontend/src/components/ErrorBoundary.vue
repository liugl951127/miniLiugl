<!--
  @file components/ErrorBoundary.vue (V3.5.93+)
  全局错误边界 - 捕获子组件错误, 显示友好错误页
-->
<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <el-icon :size="64" color="#ef4444"><CircleClose /></el-icon>
      <h1>出错了</h1>
      <p class="error-message">{{ error.message || '未知错误' }}</p>
      <p class="error-hint">
        V3.5.93+ ErrorBoundary - 防止单页白屏
      </p>
      <div class="error-actions">
        <el-button type="primary" :icon="Refresh" @click="reload">重新加载</el-button>
        <el-button :icon="Back" @click="goHome">返回首页</el-button>
        <el-button :icon="ChatLineRound" @click="goChat">访客试用</el-button>
      </div>
      <details v-if="error.stack" class="error-stack">
        <summary>错误堆栈 (开发模式)</summary>
        <pre>{{ error.stack }}</pre>
      </details>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { Refresh, Back, ChatLineRound, CircleClose } from '@element-plus/icons-vue'
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
