<!--
  @file views/agent/MiniMap.vue (V6.8.13+ 缩略图 - 企业级)
-->
<template>
  <div class="minimap-wrap" v-loading="loading" element-loading-background="rgba(0,0,0,0.4)">
    <div
      v-if="!hasContent"
      class="minimap-empty"
    >
      <div class="minimap-empty-icon">🗺️</div>
      <div class="minimap-empty-text">画布为空</div>
    </div>
    <div
      v-else
      class="minimap"
      :class="{ 'is-empty': !hasContent }"
      @click="onClick"
      ref="mapRef"
    >
      <svg :width="WIDTH" :height="HEIGHT" :viewBox="`0 0 ${worldWidth} ${worldHeight}`">
        <!-- 连线 -->
        <g v-for="edge in (edges || [])" :key="edge.id || `${edge.source}-${edge.target}`">
          <line
            :x1="(getX(edge.source) + 180) * scale + 4"
            :y1="(getY(edge.source) + 30) * scale + 4"
            :x2="getX(edge.target) * scale + 4"
            :y2="(getY(edge.target) + 30) * scale + 4"
            :stroke="getEdgeColor(edge.type)"
            stroke-width="1"
            opacity="0.6"
          />
        </g>
        <!-- 节点 -->
        <g v-for="node in (nodes || [])" :key="node.id">
          <rect
            :x="node.x * scale + 4"
            :y="node.y * scale + 4"
            :width="180 * scale"
            :height="60 * scale"
            :fill="getColor(node.type)"
            :opacity="0.8"
            rx="4"
          />
        </g>
        <!-- 视口框 -->
        <rect
          v-if="viewport && viewport.width"
          :x="(viewport.panX || 0) * scale + 4"
          :y="(viewport.panY || 0) * scale + 4"
          :width="(viewport.width || 0) * scale"
          :height="(viewport.height || 0) * scale"
          fill="none"
          stroke="#a855f7"
          stroke-width="2"
          stroke-dasharray="4,2"
          class="viewport-box"
        />
      </svg>
    </div>
    <BackToTop />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import BackToTop from '@/components/BackToTop.vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  viewport: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['navigate'])

const WIDTH = 180
const HEIGHT = 120
const PADDING = 200

const mapRef = ref(null)

const hasContent = computed(() => Array.isArray(props.nodes) && props.nodes.length > 0)

// 计算世界范围
const worldWidth = computed(() => {
  const list = props.nodes || []
  if (!list.length) return 2000
  const maxX = Math.max(...list.map(n => (n.x || 0) + 180), 1600)
  return Math.max(maxX + PADDING * 2, 2000)
})
const worldHeight = computed(() => {
  const list = props.nodes || []
  if (!list.length) return 1200
  const maxY = Math.max(...list.map(n => (n.y || 0) + 60), 900)
  return Math.max(maxY + PADDING * 2, 1200)
})

const scale = computed(() => Math.min(WIDTH / worldWidth.value, HEIGHT / worldHeight.value))

function getX(id) {
  return (props.nodes || []).find(n => n.id === id)?.x || 0
}
function getY(id) {
  return (props.nodes || []).find(n => n.id === id)?.y || 0
}
function getColor(type) {
  return {
    llm: '#3b82f6', rag: '#10b981', tool: '#f59e0b',
    code: '#8b5cf6', http: '#06b6d4', condition: '#eab308', memory: '#ec4899'
  }[type] || '#6b7280'
}
function getEdgeColor(type) {
  return type === 'control' ? '#eab308' : '#3b82f6'
}

function onClick(e) {
  if (!mapRef.value) return
  const rect = mapRef.value.getBoundingClientRect()
  const x = (e.clientX - rect.left) / WIDTH
  const y = (e.clientY - rect.top) / HEIGHT
  emit('navigate', { x: x * worldWidth.value, y: y * worldHeight.value })
}
</script>

<style scoped>
.minimap-wrap {
  position: relative;
}
.minimap {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 180px;
  height: 120px;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: crosshair;
  backdrop-filter: blur(8px);
  z-index: 10;
}
.minimap.is-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
}
.minimap svg {
  display: block;
}
.viewport-box {
  pointer-events: none;
}
.minimap-empty {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 180px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.4);
  z-index: 10;
}
.minimap-empty-icon {
  font-size: 24px;
  margin-bottom: 4px;
}
.minimap-empty-text {
  font-size: 11px;
}
</style>
