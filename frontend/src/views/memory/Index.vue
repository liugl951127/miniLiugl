<template>
  <div class="placeholder-page">
    <el-empty :description="`${title} - 即将上线`">
      <template #image>
        <el-icon :size="80" color="#5b8def"><component :is="iconComp" /></el-icon>
      </template>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ChatDotRound, Files, Cpu, Setting } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '对话' }
})

const iconComp = computed(() => {
  if (props.title.includes('对话')) return ChatDotRound
  if (props.title.includes('知识')) return Files
  if (props.title.includes('记忆')) return Cpu
  return Setting
})
</script>

<style lang="scss" scoped>
.placeholder-page {
  display: flex; align-items: center; justify-content: center;
  min-height: 70vh;
}
</style>
