<!--
  @file knowledge/Kg.vue - 知识图谱页 (V7.6 from Index.vue 73-149 提取)
  路由: /knowledge/kg
-->
<template>
  <div class="kg-page">
    <div class="toolbar">
      <el-select v-model="kgKbId" placeholder="选择知识库" size="small" style="width:200px" @change="handleLoadKg">
        <el-option v-for="kb in kbs" :key="kb.id" :label="kb.name" :value="kb.id" />
      </el-select>
      <el-button type="primary" size="small" :loading="kgBuilding" @click="handleBuildKg">
        <el-icon><MagicStick /></el-icon>从文档构建图谱
      </el-button>
      <el-button size="small" :loading="kgLoading" @click="handleLoadKg">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
      <el-tag v-if="kgStats.entities || kgStats.relations" type="info">
        {{ kgStats.entities }} 实体 / {{ kgStats.relations }} 关系
      </el-tag>
      <div style="flex:1"></div>
      <el-input
        v-model="kgSearchKw"
        placeholder="搜索实体"
        size="small"
        style="width:160px"
        clearable
        @keyup.enter="handleSearch"
        @clear="clearSearchHighlight"
      />
      <el-button size="small" @click="handleSearch">搜索</el-button>
      <el-button size="small" type="success" @click="openReasoner">
        <el-icon><Connection /></el-icon>关系推理
      </el-button>
    </div>

    <el-card shadow="never" class="graph-card">
      <KgGraph
        v-if="kgEntities.length"
        :entities="kgEntities"
        :relations="kgRelations"
        :selected-kb="kgKbId"
        @entity-click="onEntityClick"
        @relation-click="onRelationClick"
      />
      <EmptyState
        v-else
        :title="kgLoading ? '加载中...' : '尚未构建图谱'"
        :description="kgLoading ? '' : '选个知识库, 点「从文档构建图谱」'"
        :is-loading="kgLoading"
      />
    </el-card>

    <!-- 关系推理弹窗 -->
    <el-dialog v-model="reasonerVisible" title="🔗 关系推理 (BFS 路径)" width="640px">
      <div class="reasoner-input">
        <el-input v-model="reasonerSrc" placeholder="起点实体" size="small" />
        <span class="arrow">→</span>
        <el-input v-model="reasonerTgt" placeholder="终点实体" size="small" />
        <el-button type="primary" size="small" :loading="reasonerLoading" @click="runReasoner">推理</el-button>
      </div>
      <div v-if="reasonerPaths.length" class="reasoner-paths">
        <div v-for="(p, i) in reasonerPaths" :key="i" class="reasoner-path">
          <el-tag :type="i === 0 ? 'success' : 'info'">路径 {{ i+1 }} (跳数 {{ p.hops }})</el-tag>
          <div class="path-nodes">
            <template v-for="(n, idx) in p.path" :key="idx">
              <strong>{{ n }}</strong>
              <el-icon v-if="idx < p.path.length - 1" style="margin:0 6px"><Right /></el-icon>
            </template>
          </div>
        </div>
      </div>
      <EmptyState
        v-else-if="reasonerRan && !reasonerLoading"
        title="未找到连通路径"
        compact
      />
      <EmptyState
        v-else-if="!reasonerRan"
        title="填两个实体名, 点「推理」找最短路径"
        compact
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh, Connection, Right } from '@element-plus/icons-vue'
import KgGraph from '@/components/KgGraph.vue'
import EmptyState from '@/components/EmptyState.vue'
import { kgApi, getKg, buildKg, searchKg, reasonKg } from '@/api/kg'
void kgApi  // 显式标记: kgApi 已加载, 用具体函数

const props = defineProps({
  kbs: { type: Array, default: () => [] }
})

const kgKbId = ref(null)
const kgEntities = ref([])
const kgRelations = ref([])
const kgStats = reactive({ entities: 0, relations: 0 })
const kgLoading = ref(false)
const kgBuilding = ref(false)
const kgSearchKw = ref('')

const reasonerVisible = ref(false)
const reasonerSrc = ref('')
const reasonerTgt = ref('')
const reasonerPaths = ref([])
const reasonerLoading = ref(false)
const reasonerRan = ref(false)

async function handleLoadKg() {
  if (!kgKbId.value) return
  kgLoading.value = true
  try {
    const res = await getKg(kgKbId.value)
    if (res.code === 0 && res.data) {
      kgEntities.value = res.data.entities || []
      kgRelations.value = res.data.relations || []
      kgStats.entities = kgEntities.value.length
      kgStats.relations = kgRelations.value.length
    }
  } catch (e) {
    ElMessage.error('加载图谱失败: ' + e.message)
  } finally {
    kgLoading.value = false
  }
}

async function handleBuildKg() {
  if (!kgKbId.value) return ElMessage.warning('请先选择知识库')
  kgBuilding.value = true
  try {
    const res = await buildKg(kgKbId.value)
    if (res.code === 0) {
      ElMessage.success(`构建完成: ${res.data?.entities || 0} 实体 / ${res.data?.relations || 0} 关系`)
      await handleLoadKg()
    } else {
      ElMessage.error(res.message || '构建失败')
    }
  } catch (e) {
    ElMessage.error('构建失败: ' + e.message)
  } finally {
    kgBuilding.value = false
  }
}

async function handleSearch() {
  if (!kgKbId.value || !kgSearchKw.value.trim()) return
  try {
    const res = await searchKg(kgKbId.value, kgSearchKw.value)
    if (res.code === 0 && res.data?.length) {
      ElMessage.success(`找到 ${res.data.length} 个相关实体`)
    } else {
      ElMessage.info('未找到匹配实体')
    }
  } catch (e) {
    ElMessage.error('搜索失败: ' + e.message)
  }
}

function clearSearchHighlight() {
  // KgGraph 组件内部高亮 (暂通过重新加载)
  loadKg()
}

function openReasoner() {
  if (!kgKbId.value) return ElMessage.warning('请先选择知识库')
  reasonerVisible.value = true
  reasonerRan.value = false
  reasonerPaths.value = []
}

async function runReasoner() {
  if (!reasonerSrc.value || !reasonerTgt.value) {
    return ElMessage.warning('请输入起点和终点')
  }
  reasonerLoading.value = true
  try {
    const res = await reasonKg(reasonerSrc.value, reasonerTgt.value)
    reasonerRan.value = true
    if (res.code === 0) {
      reasonerPaths.value = res.data || []
    } else {
      ElMessage.error(res.message || '推理失败')
    }
  } catch (e) {
    ElMessage.error('推理失败: ' + e.message)
  } finally {
    reasonerLoading.value = false
  }
}

function onEntityClick(entity) {
  console.log('entity click', entity)
}
function onRelationClick(rel) {
  console.log('relation click', rel)
}

onMounted(() => {
  if (props.kbs.length && !kgKbId.value) {
    kgKbId.value = props.kbs[0].id
    loadKg()
  }
})
</script>

<style scoped>
.kg-page { padding: 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }
.graph-card { min-height: 560px; }
.reasoner-input { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; }
.reasoner-input .arrow { align-self: center; color: #94a3b8; }
.reasoner-paths { display: flex; flex-direction: column; gap: 8px; }
.reasoner-path { padding: 8px 12px; background: #f5f7fa; border-radius: 4px; }
.path-nodes { margin-top: 6px; font-size: 13px; }
.path-nodes strong { color: var(--el-color-primary); }
</style>
