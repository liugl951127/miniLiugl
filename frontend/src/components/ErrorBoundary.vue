<!--
  V3.6.21+ ErrorBoundary
  错误时: 先显示 Skeleton (0.5s), 再切到 ErrorState
  防止用户看到空白闪烁
-->
<template>
  <slot v-if="!error" />
  <div v-else class="error-boundary-wrap">
    <transition name="error-fade" mode="out-in">
      <div v-if="showDetail" key="state" class="error-boundary-content">
        <ErrorState
          :error="error"
          :error-type="getErrorType(error)"
          :show-detail="true"
          @retry="reload"
        />
      </div>
      <div v-else key="skeleton" class="error-boundary-skeleton">
        <el-skeleton :rows="5" animated />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import ErrorState from './ErrorState.vue'

const error = ref(null)
const showDetail = ref(false)

// V3.6.21+ 短延迟 (500ms) 后显示 ErrorState, 先 Skeleton 过渡
onErrorCaptured((err) => {
  if (error.value) return false
  error.value = err
  setTimeout(() => {
    showDetail.value = true
  }, 500)
  return false
})

function reload() {
  location.reload()
}

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

<style scoped>
.error-boundary-wrap {
  min-height: 100vh;
  background: #f8fafc;
}
.error-boundary-content {
  padding: 40px 20px;
}
.error-boundary-skeleton {
  padding: 60px 40px;
  max-width: 800px;
  margin: 40px auto;
}
.error-fade-enter-active, .error-fade-leave-active {
  transition: opacity 0.2s ease;
}
.error-fade-enter-from, .error-fade-leave-to {
  opacity: 0;
}
</style>
