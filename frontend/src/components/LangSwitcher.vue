<!--
  语言切换器 (V4.2)
  - 中/英 双语
  - 持久化到 localStorage
-->
<template>
  <el-dropdown trigger="click" @command="switchLang">
    <span class="lang-trigger">
      <el-icon><Position /></el-icon>
      {{ currentLabel }}
      <el-icon><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh" :disabled="current === 'zh'">
          🇨🇳 中文
        </el-dropdown-item>
        <el-dropdown-item command="en" :disabled="current === 'en'">
          🇺🇸 English
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Position, ArrowDown } from '@element-plus/icons-vue'
import { setLang, currentLang } from '@/i18n'

const current = ref(currentLang())
const currentLabel = computed(() => current.value === 'zh' ? '中文' : 'EN')

function switchLang(lang) {
  setLang(lang)
  current.value = lang
  // 触发 router 重渲染
  setTimeout(() => location.reload(), 100)
}
</script>

<style scoped>
.lang-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  color: #64748b;
  font-size: 13px;
}
.lang-trigger:hover { background: #f1f5f9; color: #334155; }
</style>
