<template>
  <div class="kg-container">
    <div class="kg-header">
      <h1>🕸️ 知识图谱 <span class="badge">V2.0</span></h1>
      <p class="sub">实体-关系图谱 · N 跳查询 · 最短路径</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card>
          <template #header><span>➕ 添加实体</span></template>
          <el-form :inline="false" size="default">
            <el-form-item label="名称"><el-input v-model="newEntity.name" placeholder="e.g. 张三" /></el-form-item>
            <el-form-item label="类型">
              <el-select v-model="newEntity.type" style="width:100%">
                <el-option label="人物 person" value="person" />
                <el-option label="地点 place" value="place" />
                <el-option label="组织 org" value="org" />
                <el-option label="概念 concept" value="concept" />
                <el-option label="事件 event" value="event" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述"><el-input v-model="newEntity.description" /></el-form-item>
            <el-form-item label="重要性">
              <el-rate v-model="newEntity.importance" :max="10" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createEntity">添加</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top:16px">
          <template #header><span>🔍 搜索实体</span></template>
          <el-input v-model="searchKw" @keyup.enter="doSearch" placeholder="输入关键词">
            <template #append><el-button @click="doSearch">搜索</el-button></template>
          </el-input>
          <el-scrollbar style="margin-top:12px;max-height:300px">
            <div v-for="e in entities" :key="e.id"
                 class="entity-item" :class="{ active: selectedEntity?.id === e.id }"
                 @click="selectEntity(e)">
              <el-tag size="small">{{ e.entityType }}</el-tag>
              <strong>{{ e.name }}</strong>
              <span v-if="e.description" class="desc">— {{ e.description }}</span>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-if="selectedEntity">
          <template #header>
            <span>🌐 {{ selectedEntity.name }} 的关联</span>
            <el-button-group style="margin-left:12px">
              <el-button size="small" :type="hop===1?'primary':''" @click="setHop(1)">1 跳</el-button>
              <el-button size="small" :type="hop===2?'primary':''" @click="setHop(2)">2 跳</el-button>
            </el-button-group>
          </template>

          <el-empty v-if="!neighbors.length" description="无关联" />
          <el-scrollbar v-else style="height:500px">
            <div v-for="(n, i) in neighbors" :key="i" class="neighbor">
              <el-tag :type="n.hop===1?'success':'info'" size="small">
                {{ n.hop === 1 ? '1跳' : '2跳' }}
              </el-tag>
              <el-tag size="small" style="margin-left:6px">{{ n.entity.entityType }}</el-tag>
              <strong style="margin-left:6px">{{ n.entity.name }}</strong>
              <span class="via">via {{ n.via }}</span>
              <el-button v-if="n.hop === 1" text type="primary"
                         @click="createRelationTo(n.entity.id)">↔ 建关系</el-button>
            </div>
          </el-scrollbar>
        </el-card>

        <el-card v-else>
          <el-empty description="选择一个实体查看关联" />
        </el-card>

        <el-card v-if="selectedEntity" style="margin-top:16px">
          <template #header><span>➕ 添加关系</span></template>
          <el-form :inline="true">
            <el-form-item label="目标实体">
              <el-select v-model="relForm.toId" filterable style="width:180px">
                <el-option v-for="e in entities" :key="e.id" :label="e.name" :value="e.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="关系类型">
              <el-input v-model="relForm.type" placeholder="e.g. works_at" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitRelation">创建</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userId = localStorage.getItem('user_id') || '1'
const token = localStorage.getItem('access_token') || ''

const newEntity = reactive({ name: '', type: 'person', description: '', importance: 5 })
const searchKw = ref('')
const entities = ref<any[]>([])
const selectedEntity = ref<any>(null)
const neighbors = ref<any[]>([])
const hop = ref(1)
const relForm = reactive({ toId: null as number | null, type: '' })

function auth() { return { headers: { Authorization: `Bearer ${token}` } } }

async function createEntity() {
  if (!newEntity.name) { ElMessage.warning('请输入名称'); return }
  try {
    await axios.post(`${API}/api/v1/agent/kg/entities`,
      { userId, ...newEntity, importance: newEntity.importance }, auth())
    ElMessage.success('已创建')
    newEntity.name = ''; newEntity.description = ''
    doSearch()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function doSearch() {
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/kg/entities/search`,
      { params: { userId, keyword: searchKw.value || 'a', limit: 50 }, ...auth() })
    entities.value = data.data || []
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

async function selectEntity(e: any) {
  selectedEntity.value = e
  await loadNeighbors()
}

async function setHop(h: number) {
  hop.value = h
  await loadNeighbors()
}

async function loadNeighbors() {
  if (!selectedEntity.value) return
  const url = hop.value === 1
    ? `${API}/api/v1/agent/kg/entities/${selectedEntity.value.id}/neighbors`
    : `${API}/api/v1/agent/kg/entities/${selectedEntity.value.id}/2hop`
  try {
    const { data } = await axios.get(url, auth())
    neighbors.value = data.data || []
  } catch (e: any) { ElMessage.error(e?.message) }
}

async function createRelationTo(toId: number) {
  relForm.toId = toId
  relForm.type = 'related_to'
}

async function submitRelation() {
  if (!relForm.toId || !relForm.type) { ElMessage.warning('请填写完整'); return }
  try {
    await axios.post(`${API}/api/v1/agent/kg/relations`, {
      userId, fromId: selectedEntity.value.id, toId: relForm.toId,
      type: relForm.type, weight: 1.0
    }, auth())
    ElMessage.success('关系已创建')
    await loadNeighbors()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message) }
}

onMounted(doSearch)
</script>

<style scoped>
.kg-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.kg-header h1 { display:flex; align-items:center; gap:10px; }
.badge {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.sub { color: #666; margin-bottom: 20px; }
.entity-item {
  padding: 8px 10px; cursor: pointer; border-radius: 4px; transition: all 0.2s;
  display: flex; align-items: center; gap: 6px;
}
.entity-item:hover { background: #f5f7fa; }
.entity-item.active { background: #ecf5ff; }
.entity-item .desc { color: #999; font-size: 12px; }
.neighbor {
  padding: 10px; border-bottom: 1px dashed #eee; display: flex; align-items: center;
}
.via { color: #999; margin-left: 8px; font-size: 12px; font-style: italic; }
</style>
