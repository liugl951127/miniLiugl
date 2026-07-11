<template>
  <el-dropdown @command="onSwitch" trigger="click">
    <span class="lang-trigger">
      <el-icon><Position /></el-icon>
      <span class="lang-text">{{ currentLabel }}</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh" :disabled="lang === 'zh'">🇨🇳 简体中文</el-dropdown-item>
        <el-dropdown-item command="en" :disabled="lang === 'en'">🇺🇸 English</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { Position } from '@element-plus/icons-vue'
import { currentLang, setLang } from '@/i18n'

const lang = computed(() => currentLang())
const currentLabel = computed(() => lang.value === 'zh' ? '中文' : 'EN')

function onSwitch(cmd) {
  setLang(cmd)
  // 提示
  setTimeout(() => location.reload(), 100)  // 简单实现, 让所有组件刷新
}
</script>

<style scoped>
.lang-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  color: #666;
  transition: all 0.2s;
}
.lang-trigger:hover {
  background: #f0f0f0;
  color: #409EFF;
}
.lang-text { font-size: 13px; }
</style>
