<!--
  @file views/agent/NodePalette.vue (V6.3+ 7 类节点面板)
-->
<template>
  <div class="node-palette">
    <div class="palette-header">
      <h3>🧩 节点库</h3>
      <p>7 类节点</p>
    </div>
    <div
      v-for="nodeType in nodeTypes"
      :key="nodeType.type"
      class="palette-item"
      :style="{ borderColor: nodeType.color }"
      draggable="true"
      @dragstart="$emit('drag-start', $event, nodeType)"
      @click="$emit('click', nodeType)"
    >
      <div class="palette-icon" :style="{ background: nodeType.color }">
        {{ nodeType.icon }}
      </div>
      <div class="palette-info">
        <div class="palette-name">{{ nodeType.name }}</div>
        <div class="palette-desc">{{ nodeType.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  nodeTypes: { type: Array, required: true }
})
defineEmits(['drag-start', 'click'])
</script>

<style scoped>
.node-palette {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 12px;
  overflow-y: auto;
}
.palette-header h3 {
  margin: 0 0 4px;
  font-size: 14px;
  color: white;
}
.palette-header p {
  margin: 0 0 12px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
}
.palette-item:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(4px);
}
.palette-item:active {
  cursor: grabbing;
}
.palette-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.palette-info {
  flex: 1;
  min-width: 0;
}
.palette-name {
  font-size: 12px;
  font-weight: 600;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.palette-desc {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
