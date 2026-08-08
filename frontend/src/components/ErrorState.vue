<!--
  V6.3+ ErrorState 简化 - 默认不显示
  之前: 4 种状态 6 种类别, 各种文案
  现在: 占位组件, 不渲染任何东西
  保留 file 结构以便其他可能的错误场景用
-->
<template>
  <div v-if="false" class="error-state">
    <div class="error-icon">?</div>
    <h2 class="error-title">{{ title }}</h2>
    <p class="error-description">{{ description }}</p>
  </div>
</template>

<script setup>
defineProps({
  error: { type: [Error, String, Object], default: null },
  errorType: { type: String, default: 'unknown' },
  showDetail: { type: Boolean, default: false },
  isDemo: { type: Boolean, default: false }
})
</script>

<style scoped>
.error-state { display: none; }
</style>
