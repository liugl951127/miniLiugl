<!--
  @file components/ErrorBoundary.vue (Day 37)
  V6.3+ → V7.0: 恢复错误捕获 + 静默通知联动
  - onErrorCaptured: 捕获后代组件错误
  - ElNotification: 右下角静默提示，不打断用户操作
  - 错误信息不渲染到页面，不阻塞 UI
  - 静默机制: notification 的 duration=0（不自动关闭）+ showClose=true（用户手动关）
  - 与 Monitor 静默 (AlertEngine.silencedUntil) 是两个独立概念，这里是前端静默通知
-->
<template>
  <slot />
</template>

<script setup>
/**
 * Day 37: ErrorBoundary 错误捕获 + 静默通知
 *
 * 注意: Vue 3 onErrorCaptured 只能捕获以下错误:
 *   - 组件渲染时 throw 的错误
 *   - 生命周期钩子中的错误
 *   - setup 函数中的错误
 *   - 组件事件处理器中的错误
 *
 * 无法捕获: 异步回调 (setTimeout/Promise.then/fetch.then) 内的错误
 * 这类错误由 main.js 的 app.config.errorHandler 全局处理
 */
import { onErrorCaptured } from 'vue'
import { ElNotification } from 'element-plus'
import logger from '@/utils/logger'

// 静默通知计数，用于去重短时间内的重复错误
let errorCount = 0
const DEDUP_WINDOW_MS = 3000 // 3 秒内同类错误只通知一次
let lastErrorKey = ''
let lastErrorTime = 0

onErrorCaptured((err, instance, info) => {
  // 构建错误键（用于去重）
  const errMsg = String(err?.message || err || 'Unknown error')
  const errKey = errMsg.slice(0, 80) // 用前 80 字符做键

  const now = Date.now()
  const isDuplicate = errKey === lastErrorKey && (now - lastErrorTime) < DEDUP_WINDOW_MS

  if (!isDuplicate) {
    lastErrorKey = errKey
    lastErrorTime = now

    // 静默通知：右下角不自动消失，用户可手动关闭
    ElNotification({
      type: 'error',
      title: '⚠️ 组件错误（已降级）',
      message: errMsg,
      duration: 0,          // 不自动关闭
      showClose: true,
      position: 'bottom-right',
      offset: 60 + (errorCount % 5) * 70, // 多个通知垂直堆叠
      // 静默特性: 不打断用户，不弹 Modal，console 只记录一次
    })

    errorCount++
    console.error('[ErrorBoundary] captured:', err, 'info:', info)
    // V6.8.1+ 集成 logger
    logger.error('ErrorBoundary', err?.message || String(err), {
      info,
      stack: err?.stack?.split('\n').slice(0, 5).join('\n'),
      componentName: instance?.$options?.name || instance?.type?.__name
    })
  }

  // 返回 false 阻止错误继续传播；返回 true 或 void 继续传播
  // 这里静默处理，不阻止传播，保持 UI 降级可用
  return false
})
</script>

<style scoped>
/* ErrorBoundary 本身不渲染额外 DOM，样式由 ElNotification 全局接管 */
</style>
