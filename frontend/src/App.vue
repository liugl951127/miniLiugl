<template>
  <PwaStatusBar />
  <router-view />
</template>

<script setup>
import PwaStatusBar from '@/components/PwaStatusBar.vue'
// V2.8.9: 加载 usePwa 以触发 SW 注册 (在 app 加载时)
import { usePwa } from '@/composables/usePwa'
const { swVersion } = usePwa()
</script>

<style lang="scss">
#app {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  color: var(--el-text-color-primary);
  height: 100%;
}
</style>
