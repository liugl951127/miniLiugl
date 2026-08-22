<!--
  @file views/agent/NodePalette.vue (V6.8.13+ 节点面板 - 企业级)
-->
<template>
  <div class="node-palette" v-loading="loading">
    <div class="palette-header">
      <h3>🧩 节点库</h3>
      <p>{{ nodeTypes?.length || 0 }} 类节点 · 拖拽或点击添加到画布</p>
    </div>

    <el-input
      v-model="search"
      placeholder="搜索节点类型"
      size="small"
      clearable
      :prefix-icon="Search"
      style="margin-bottom:8px"
    />

    <el-empty
      v-if="!filteredTypes.length"
      :description="search ? `未找到 \"${search}\" 相关节点` : '暂无可用节点'"
      :image-size="60"
    />

    <div
      v-for="nodeType in filteredTypes"
      :key="nodeType.type"
      class="palette-item"
      :style="{ borderColor: nodeType.color }"
      draggable="true"
      @dragstart="onDragStart($event, nodeType)"
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
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  nodeTypes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['drag-start', 'click'])

const search = ref('')

const filteredTypes = computed(() => {
  const list = props.nodeTypes || []
  if (!search.value) return list
  const kw = search.value.toLowerCase()
  return list.filter(n =>
    (n.name || '').toLowerCase().includes(kw) ||
    (n.desc || '').toLowerCase().includes(kw) ||
    (n.type || '').toLowerCase().includes(kw)
  )
})

function onDragStart(e, nodeType) {
  emit('drag-start', e, nodeType)
}
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
  margin-top: 2px;
}
</style>
