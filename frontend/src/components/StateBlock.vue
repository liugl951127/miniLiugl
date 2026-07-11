<template>
  <div class="state-block">
    <el-empty v-if="type === 'empty'" :description="message" :image-size="100">
      <slot></slot>
    </el-empty>
    <div v-else-if="type === 'loading'" class="loading-block">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p class="loading-text">{{ message || '加载中...' }}</p>
    </div>
    <el-result v-else-if="type === 'error'" icon="error" :title="title || '加载失败'" :sub-title="message">
      <template #extra>
        <slot name="action">
          <el-button type="primary" @click="$emit('retry')">🔄 重试</el-button>
        </slot>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { Loading } from '@element-plus/icons-vue'
defineProps({
  type: { type: String, default: 'empty' },  // empty / loading / error
  title: String,
  message: { type: String, default: '暂无数据' }
})
defineEmits(['retry'])
</script>

<style scoped>
.state-block { padding: 24px; text-align: center; }
.loading-block { padding: 40px; }
.loading-text { color: #909399; margin-top: 12px; font-size: 14px; }
.is-loading { animation: rotating 2s linear infinite; color: #409EFF; }
@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
