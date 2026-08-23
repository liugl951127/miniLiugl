<!--
  @file agent/NodePalette.vue - 节点库面板 (V8.0)
-->
<template>
  <div class="node-palette">
    <div class="palette-title">节点库</div>
    <div
      v-for="nt in nodeTypes" :key="nt.type"
      class="palette-node"
      :style="{ borderLeftColor: nt.color }"
      draggable="true"
      @dragstart="$emit('drag-start', $event, nt)"
      @contextmenu.stop.prevent="$emit('context-menu', $event, nt)"
    >
      <el-icon><component :is="iconMap[nt.icon]" /></el-icon>
      <span>{{ nt.label }}</span>
    </div>
  </div>
</template>

<script setup>
import {
  VideoPlay, ChatDotRound, Tools, Operation, Files, DataLine,
  Connection, Folder
} from '@element-plus/icons-vue'

const iconMap = {
  VideoPlay, ChatDotRound, Tools, Operation, Files, DataLine, Connection, Folder
}

defineProps({
  nodeTypes: { type: Array, required: true }
})
defineEmits(['drag-start', 'context-menu'])
</script>

<style scoped>
.node-palette {
  width: 160px; background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  padding: 12px 8px;
  overflow-y: auto;
}
.palette-title {
  font-size: 12px; color: #64748b; font-weight: 600;
  margin-bottom: 8px; padding: 0 4px;
}
.palette-node {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; background: white;
  border-left: 3px solid; border-radius: 6px;
  margin-bottom: 6px; cursor: grab;
  font-size: 13px; color: #1e293b;
  transition: all 0.15s;
}
.palette-node:hover {
  transform: translateX(2px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.06);
}
.palette-node:active { cursor: grabbing; }
</style>
