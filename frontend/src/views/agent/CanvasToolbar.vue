<!--
  @file agent/CanvasToolbar.vue - 画布工具栏 (V8.0)
-->
<template>
  <div class="canvas-toolbar">
    <span style="font-size:14px;font-weight:600;color:#303133">Agent 画布</span>
    <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
      <!-- Undo / Redo -->
      <el-tooltip content="撤销 (Ctrl+Z)" placement="bottom">
        <el-button size="small" @click="$emit('undo')" :disabled="!canUndo">
          <el-icon><RefreshLeft /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="重做 (Ctrl+Y)" placement="bottom">
        <el-button size="small" @click="$emit('redo')" :disabled="!canRedo">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
      </el-tooltip>

      <el-divider direction="vertical" />

      <el-tooltip content="复制 (Ctrl+C)" placement="bottom">
        <el-button size="small" @click="$emit('copy')" :disabled="!hasSelection">
          <el-icon><CopyDocument /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="粘贴 (Ctrl+V)" placement="bottom">
        <el-button size="small" @click="$emit('paste')" :disabled="!hasClipboard">
          <el-icon><DocumentCopy /></el-icon>
        </el-button>
      </el-tooltip>

      <el-divider direction="vertical" />

      <el-tooltip content="搜索节点" placement="bottom">
        <el-button size="small" @click="$emit('search')">
          <el-icon><Search /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="版本历史" placement="bottom">
        <el-button size="small" :type="hasVersions ? 'info' : ''" @click="$emit('history')">
          <el-icon><Clock /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="快捷键 (F1)" placement="bottom">
        <el-button size="small" @click="$emit('shortcuts')">
          <el-icon><QuestionFilled /></el-icon>
        </el-button>
      </el-tooltip>

      <el-divider direction="vertical" />

      <el-button size="small" @click="$emit('load')"><el-icon><Refresh /></el-icon>加载</el-button>
      <el-button size="small" @click="$emit('new')"><el-icon><Plus /></el-icon>新建</el-button>
      <el-button type="primary" size="small" @click="$emit('save')"><el-icon><FolderChecked /></el-icon>保存</el-button>
      <el-button type="success" size="small" :loading="running" @click="$emit('run')">
        <el-icon><VideoPlay /></el-icon>执行
      </el-button>
      <el-button v-if="running && multiRunMode" type="danger" size="small" @click="$emit('stop')">⏹ 停止</el-button>
      <el-button size="small" :type="multiRunMode ? 'warning' : ''" :disabled="running" @click="$emit('toggle-multi')">
        {{ multiRunMode ? '⚡ 多Agent ON' : '🤖 多Agent' }}
      </el-button>
      <el-button size="small" @click="$emit('auto-layout')"><el-icon><Grid /></el-icon>自动布局</el-button>
      <el-button size="small" @click="$emit('export')"><el-icon><Download /></el-icon>导出</el-button>
      <el-button size="small" @click="$emit('import')"><el-icon><Upload /></el-icon>导入</el-button>
      <el-button size="small" @click="$emit('clear')"><el-icon><Delete /></el-icon>清空</el-button>
    </div>
  </div>
</template>

<script setup>
import { RefreshLeft, RefreshRight, CopyDocument, DocumentCopy, Search, Clock, QuestionFilled,
  Refresh, Plus, FolderChecked, VideoPlay, Grid, Download, Upload, Delete } from '@element-plus/icons-vue'

defineProps({
  canUndo: { type: Boolean, default: false },
  canRedo: { type: Boolean, default: false },
  hasSelection: { type: Boolean, default: false },
  hasClipboard: { type: Boolean, default: false },
  hasVersions: { type: Boolean, default: false },
  running: { type: Boolean, default: false },
  multiRunMode: { type: Boolean, default: false }
})

defineEmits([
  'undo', 'redo', 'copy', 'paste',
  'search', 'history', 'shortcuts',
  'load', 'new', 'save', 'run', 'stop', 'toggle-multi',
  'auto-layout', 'export', 'import', 'clear'
])
</script>

<style scoped>
.canvas-toolbar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; background: white;
  border-bottom: 1px solid #e2e8f0;
}
</style>
