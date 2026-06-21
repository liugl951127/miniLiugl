<!--
  跨应用绑定页 (V5.1)
  - 显示当前用户 unionid 关联的所有应用
  - 引导添加新应用
-->
<template>
  <div class="cross-app-page">
    <header class="page-header">
      <h1>🔗 {{ t('user.crossAppBinding') }}</h1>
      <p class="subtitle">{{ t('user.crossAppBindingDesc') }}</p>
    </header>
    <CrossAppBinding />
  </div>
</template>

<script setup>
import CrossAppBinding from '@/components/CrossAppBinding.vue'
import { t } from '@/i18n'
</script>

<style scoped>
.cross-app-page { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-header h1 { margin: 0 0 4px; font-size: 28px; }
.subtitle { color: #64748b; margin: 0 0 16px; }
</style>