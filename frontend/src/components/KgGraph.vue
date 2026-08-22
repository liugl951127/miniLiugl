<!--
  @file KgGraph.vue - 可复用的 D3 力导向图谱渲染组件 (T2)
  @props
    - entities: Array<{id, name, type, _hit?}>   节点列表
    - relations: Array<{src, tgt, rel, weight}>  边列表
    - selectedKb: Number?                        当前知识库 id (用于在 title 提示)
    - highlightId: String?                       高亮单个节点
  @emits
    - entity-click: {id, name, type}
    - relation-click: {src, tgt, rel}
-->
<template>
  <div class="kg-graph">
    <div class="kg-graph-toolbar">
      <span class="kg-zoom-info">缩放 {{ Math.round(zoomScale * 100) }}%</span>
      <el-button size="small" link @click="resetView">重置视图</el-button>
      <span v-if="props.entities.length" class="kg-stats-mini">
        {{ props.entities.length }} 节点 / {{ props.relations.length }} 边
      </span>
    </div>
    <div class="kg-canvas" ref="canvasRef">
      <svg ref="svgRef" class="kg-svg">
        <defs>
          <marker id="kg-arrowhead" markerWidth="10" markerHeight="7" refX="25" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="#909399" />
          </marker>
          <marker id="kg-arrowhead-hit" markerWidth="10" markerHeight="7" refX="25" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="#67c23a" />
          </marker>
        </defs>
        <g ref="zoomGroup">
          <g class="links-layer" ref="linksLayer"></g>
          <g class="nodes-layer" ref="nodesLayer"></g>
        </g>
      </svg>
      <div v-if="!props.entities.length" class="kg-graph-empty">无图谱数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'

const props = defineProps({
  entities: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
  selectedKb: { type: Number, default: null },
  highlightId: { type: String, default: '' }
})
const emit = defineEmits(['entity-click', 'relation-click'])

// refs
const canvasRef = ref(null)
const svgRef = ref(null)
const zoomGroup = ref(null)
const linksLayer = ref(null)
const nodesLayer = ref(null)

const canvasW = ref(800)
const canvasH = ref(520)
const zoomScale = ref(1)

// D3 handles
let simulation = null
let svgSel = null
let zoomBehavior = null

// Node color mapping
const NODE_COLORS = {
  PERSON: '#409EFF',
  ORG: '#67C23A',
  CONCEPT: '#E6A23C',
  PRODUCT: '#F56C6C',
  PLACE: '#909399',
  EVENT: '#7C3AED',
  OTHER: '#7C3AED',
  DEFAULT: '#7C3aED'
}
function nodeColor(type) {
  if (!type) return NODE_COLORS.DEFAULT
  return NODE_COLORS[type.toUpperCase()] || NODE_COLORS.DEFAULT
}

function truncateName(name, maxLen = 10) {
  if (!name) return ''
  return String(name).length > maxLen ? String(name).slice(0, maxLen) + '…' : String(name)
}

function initD3() {
  if (!svgRef.value) return
  svgSel = d3.select(svgRef.value)
    .attr('width', canvasW.value)
    .attr('height', canvasH.value)

  zoomBehavior = d3.zoom()
    .scaleExtent([0.2, 3])
    .on('zoom', (event) => {
      zoomScale.value = event.transform.k
      d3.select(zoomGroup.value).attr('transform', event.transform)
    })

  svgSel.call(zoomBehavior).on('dblclick.zoom', null)

  simulation = d3.forceSimulation()
    .force('link', d3.forceLink().id(d => d.id).distance(120).strength(0.5))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(canvasW.value / 2, canvasH.value / 2))
    .force('collision', d3.forceCollide().radius(40))
    .on('tick', ticked)
}

function buildGraphData() {
  // 节点: 把 entities (id/name/type) 映射成 d3 节点
  const nodeIds = new Set()
  const nodes = props.entities
    .filter(e => e && (e.id !== undefined && e.id !== null))
    .map(e => {
      nodeIds.add(String(e.id))
      return {
        id: String(e.id),
        name: e.name || e.label || String(e.id),
        type: e.type || 'OTHER',
        _hit: !!e._hit
      }
    })

  // 关系: 兼容多种字段命名 (src/source/from, tgt/target/to, rel/label/type)
  const links = []
  for (const r of (props.relations || [])) {
    const src = r.src ?? r.source ?? r.from ?? r.fromId
    const tgt = r.tgt ?? r.target ?? r.to ?? r.toId
    const rel = r.rel ?? r.label ?? r.type ?? '关联'
    if (src === undefined || tgt === undefined) continue
    const sId = String(src)
    const tId = String(tgt)
    if (!nodeIds.has(sId) || !nodeIds.has(tId)) continue
    links.push({ source: sId, target: tId, label: rel, weight: r.weight || 1 })
  }
  return { nodes, links }
}

function updateGraph() {
  if (!simulation || !linksLayer.value || !nodesLayer.value) return
  const { nodes, links } = buildGraphData()

  // ── links ──
  const linkSel = d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .data(links, d => `${d.source.id || d.source}-${d.target.id || d.target}`)

  linkSel.exit().remove()
  const linkEnter = linkSel.enter()
    .append('g')
    .attr('class', 'link-group')
    .style('cursor', 'pointer')
    .on('click', (event, d) => {
      event.stopPropagation()
      emit('relation-click', {
        src: d.source.id || d.source,
        tgt: d.target.id || d.target,
        rel: d.label
      })
    })

  linkEnter.append('line')
    .attr('class', 'link-line')
    .attr('stroke', '#dcdfe6')
    .attr('stroke-width', 1.5)
    .attr('marker-end', 'url(#kg-arrowhead)')

  linkEnter.append('text')
    .attr('class', 'link-label')
    .attr('text-anchor', 'middle')
    .attr('dy', -4)
    .attr('font-size', 10)
    .attr('fill', '#909399')
    .text(d => d.label || '')

  // ── nodes ──
  const nodeSel = d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .data(nodes, d => d.id)

  nodeSel.exit().remove()
  const nodeEnter = nodeSel.enter()
    .append('g')
    .attr('class', 'node-group')
    .style('cursor', 'pointer')

  nodeEnter.append('circle')
    .attr('class', 'node-circle')
    .attr('r', 22)
    .attr('fill', d => nodeColor(d.type))
    .attr('opacity', d => d._hit ? 1 : 0.85)
    .attr('stroke', d => d._hit ? '#67c23a' : '#fff')
    .attr('stroke-width', d => d._hit ? 3 : 2)

  nodeEnter.append('text')
    .attr('class', 'node-label')
    .attr('text-anchor', 'middle')
    .attr('dy', 4)
    .attr('font-size', 11)
    .attr('fill', '#fff')
    .attr('font-weight', 500)
    .text(d => truncateName(d.name))

  const nodeMerge = nodeEnter.merge(nodeSel)

  nodeMerge
    .on('click', (event, d) => {
      event.stopPropagation()
      emit('entity-click', { id: d.id, name: d.name, type: d.type })
    })
    .on('mouseenter', (event, d) => {
      highlightNeighbors(d.id)
    })
    .on('mouseleave', () => {
      resetHighlight()
    })

  // drag
  const drag = d3.drag()
    .on('start', (event, d) => {
      if (!event.active) simulation.alphaTarget(0.3).restart()
      d.fx = d.x
      d.fy = d.y
    })
    .on('drag', (event, d) => {
      d.fx = event.x
      d.fy = event.y
    })
    .on('end', (event, d) => {
      if (!event.active) simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    })
  nodeMerge.call(drag)

  // refresh sim with new node/link arrays
  simulation.nodes(nodes)
  simulation.force('link').links(links)
  simulation.alpha(1).restart()
}

function ticked() {
  if (!linksLayer.value || !nodesLayer.value) return
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .each(function (d) {
      const g = d3.select(this)
      const line = g.select('line')
      const text = g.select('text')
      const sx = d.source.x
      const sy = d.source.y
      const tx = d.target.x
      const ty = d.target.y
      const dx = tx - sx
      const dy = ty - sy
      const dr = Math.sqrt(dx * dx + dy * dy) || 1
      const offset = 26
      const endX = tx - (dx / dr) * offset
      const endY = ty - (dy / dr) * offset
      line
        .attr('x1', sx)
        .attr('y1', sy)
        .attr('x2', endX)
        .attr('y2', endY)
      text
        .attr('x', (sx + endX) / 2)
        .attr('y', (sy + endY) / 2 - 4)
    })

  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .attr('transform', d => `translate(${d.x},${d.y})`)
}

function highlightNeighbors(nodeId) {
  const neighborIds = new Set([nodeId])
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .each(function (d) {
      const sId = d.source.id || d.source
      const tId = d.target.id || d.target
      if (sId === nodeId || tId === nodeId) {
        neighborIds.add(sId)
        neighborIds.add(tId)
      }
    })
  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .style('opacity', d => neighborIds.has(d.id) ? 1 : 0.2)
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .style('opacity', function () {
      const sId = this.__data__.source.id || this.__data__.source
      const tId = this.__data__.target.id || this.__data__.target
      return (sId === nodeId || tId === nodeId) ? 1 : 0.1
    })
}

function resetHighlight() {
  d3.select(nodesLayer.value)
    .selectAll('g.node-group')
    .style('opacity', 1)
  d3.select(linksLayer.value)
    .selectAll('g.link-group')
    .style('opacity', 1)
}

function resetView() {
  if (svgSel && zoomBehavior) {
    svgSel.transition().duration(500).call(zoomBehavior.transform, d3.zoomIdentity)
  }
}

function handleResize() {
  if (canvasRef.value) {
    canvasW.value = canvasRef.value.offsetWidth || 800
    canvasH.value = canvasRef.value.offsetHeight || 520
    if (svgRef.value) {
      d3.select(svgRef.value)
        .attr('width', canvasW.value)
        .attr('height', canvasH.value)
      if (simulation) {
        simulation.force('center', d3.forceCenter(canvasW.value / 2, canvasH.value / 2))
        simulation.alpha(0.3).restart()
      }
    }
  }
}

// Watch props -> re-render
watch(() => [props.entities, props.relations], () => {
  if (simulation) {
    nextTick(() => updateGraph())
  }
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    handleResize()
    initD3()
    updateGraph()
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  if (simulation) {
    simulation.stop()
    simulation = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.kg-graph {
  display: flex;
  flex-direction: column;
  width: 100%;
}
.kg-graph-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.kg-zoom-info { font-variant-numeric: tabular-nums; }
.kg-stats-mini { margin-left: auto; }
.kg-canvas {
  position: relative;
  width: 100%;
  height: 520px;
  border-radius: 4px;
  background: #fafafa;
  overflow: hidden;
}
.kg-svg {
  display: block;
  width: 100%;
  height: 100%;
}
.kg-graph-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
:deep(.node-circle) {
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
  transition: all 0.2s;
}
:deep(.node-group:hover .node-circle) {
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.2));
  transform: scale(1.1);
}
:deep(.link-line) {
  transition: stroke 0.2s, stroke-width 0.2s;
}
:deep(.link-group:hover .link-line) {
  stroke: #409eff;
  stroke-width: 2;
}
:deep(.link-label) {
  pointer-events: none;
  font-family: 'Helvetica Neue', Arial, sans-serif;
}
</style>
