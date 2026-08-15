<template>
  <el-dropdown @command="onCommand" trigger="click">
    <el-button text circle :title="t('theme.toggle')">
      <el-icon :size="18">
        <component :is="currentIcon" />
      </el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="light" :disabled="theme === 'light'">
          <el-icon><Sunny /></el-icon>
          <span>{{ t('theme.light') }}</span>
          <el-icon v-if="theme === 'light'" class="check"><CircleCheck /></el-icon>
        </el-dropdown-item>
        <el-dropdown-item command="dark" :disabled="theme === 'dark'">
          <el-icon><Moon /></el-icon>
          <span>{{ t('theme.dark') }}</span>
          <el-icon v-if="theme === 'dark'" class="check"><CircleCheck /></el-icon>
        </el-dropdown-item>
        <el-dropdown-item command="auto" :disabled="theme === 'auto'">
          <el-icon><Monitor /></el-icon>
          <span>{{ t('theme.auto') }}</span>
          <el-icon v-if="theme === 'auto'" class="check"><CircleCheck /></el-icon>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Sunny, Moon, Monitor, CircleCheck } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const theme = ref(localStorage.getItem('minimax-theme') || 'light')

const currentIcon = computed(() => {
  if (theme.value === 'dark') return Moon
  if (theme.value === 'auto') return Monitor
  return Sunny
})

function applyTheme(mode) {
  const root = document.documentElement
  if (mode === 'dark' || (mode === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
}

function onCommand(mode) {
  theme.value = mode
  localStorage.setItem('minimax-theme', mode)
  applyTheme(mode)
}

watch(theme, applyTheme)

onMounted(() => {
  applyTheme(theme.value)
  // 监听系统主题变化 (auto 模式)
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (theme.value === 'auto') applyTheme('auto')
  })
})
</script>

<style lang="scss" scoped>
.check {
  margin-left: auto;
  color: var(--el-color-success);
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
}
</style>
