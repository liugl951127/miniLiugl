<!--
  @file views/agent/MiniMap.vue (V6.3+ 缩略图)
-->
<template>
  <div class="minimap" @click="onClick" ref="mapRef">
    <svg :width="WIDTH" :height="HEIGHT" :viewBox="`0 0 ${worldWidth} ${worldHeight}`">
      <!-- 节点 -->
      <g v-for="node in nodes" :key="node.id">
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
      <!-- 连线 -->
      <g v-for="edge in edges" :key="edge.id">
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
      <!-- 视口框 -->
      <rect
        :x="viewport.panX * scale + 4"
        :y="viewport.panY * scale + 4"
        :width="viewport.width * scale"
        :height="viewport.height * scale"
        fill="none"
        stroke="#a855f7"
        stroke-width="2"
        stroke-dasharray="4,2"
        class="viewport-box"
      />
    </svg>
  </div>
  <BackToTop />
</template>

<script setup>
import { ref, computed } from 'vue'
import BackToTop from '@/components/BackToTop.vue'

const props = defineProps({
  nodes: { type: Array, required: true },
  edges: { type: Array, required: true },
  viewport: { type: Object, required: true }
})
const emit = defineEmits(['navigate'])

const WIDTH = 180
const HEIGHT = 120
const PADDING = 200

const mapRef = ref(null)

// 计算世界范围
const worldWidth = computed(() => {
  const maxX = Math.max(...(props.nodes || []).map(n => n.x + 180), 1600)
  return Math.max(maxX + PADDING * 2, 2000)
})
const worldHeight = computed(() => {
  const maxY = Math.max(...props.nodes.map(n => n.y + 60), 900)
  return Math.max(maxY + PADDING * 2, 1200)
})

const scale = computed(() => Math.min(WIDTH / worldWidth.value, HEIGHT / worldHeight.value))

function getX(id) {
  return props.nodes.find(n => n.id === id)?.x || 0
}
function getY(id) {
  return props.nodes.find(n => n.id === id)?.y || 0
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
  const rect = mapRef.value.getBoundingClientRect()
  const x = (e.clientX - rect.left) / WIDTH
  const y = (e.clientY - rect.top) / HEIGHT
  emit('navigate', { x: x * worldWidth.value, y: y * worldHeight.value })
}
</script>

<style scoped>
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
}
.minimap svg {
  display: block;
}
.viewport-box {
  pointer-events: none;
}
</style>
