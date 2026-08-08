<!--
  V6.3+ ErrorBoundary 简化 - 去除错误监控
  之前: onErrorCaptured 捕获, 显示 Skeleton + ErrorState
  现在: 不捕获, 错误冒泡到控制台, UI 正常渲染
  Vue 3 中 onErrorCaptured 不是真正的 error boundary, 只能捕获 setup 错误
  去掉它可以避免误报 (比如 401 token 过期时, 业务代码会 throw, 但其实正常)
-->
<template>
  <slot />
</template>

<script setup>
// V6.3+ 移除 onErrorCaptured, 不再触发 ErrorState
// 错误会冒泡到全局 errorHandler (main.js) 或 console
</script>

<style scoped>
/* 之前: .error-boundary-wrap { min-height: 100vh; background: #f8fafc; } */
/* 移除所有错误页相关样式 */
</style>
