<!--
  @file views/analytics/Schema.vue (Schema)
  @version V3.5.12+ (前端注释补全)
  @description Schema
-->
<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>🗂️ Schema 浏览 (V5.31)</span>
          <el-select v-model="dsId" placeholder="选择数据源" style="width:240px" @change="loadDbs">
            <el-option v-for="s in sources" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
      </template>

      <div class="layout">
        <!-- 左: 树 -->
        <div class="tree-panel">
          <el-tree
            v-if="dsId"
            :data="treeData"
            :props="{ label: 'label', children: 'children' }"
            node-key="key"
            :load="loadNode"
            lazy
            @node-click="onNodeClick"
            :default-expanded-keys="expanded"
          />
          <el-empty v-else description="请选择数据源" />
        </div>

        <!-- 右: 表详情 -->
        <div class="detail-panel">
          <template v-if="tableDetail">
            <h3>{{ tableDetail.tableName }}</h3>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item v-for="c in tableDetail.columns" :key="c.name" :label="c.name">
                <el-tag size="small">{{ c.type }}</el-tag>
                <span v-if="c.comment" style="margin-left:8px;color:#909399">{{ c.comment }}</span>
              </el-descriptions-item>
            </el-descriptions>
            <el-button type="primary" plain @click="loadProfile" style="margin-top:12px">
              查看画像
            </el-button>
            <pre v-if="profile" class="profile">{{ JSON.stringify(profile, null, 2) }}</pre>
          </template>
          <el-empty v-else description="左侧点击表查看详情" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, onMounted, computed } from 'vue'
import { listDataSources, listDatabases, listTables, describeTable, profileTable } from '@/api/analytics'

const sources = ref([])
const dsId = ref(null)
const dbs = ref([])
const tables = ref([])
const tableDetail = ref(null)
const profile = ref(null)
const expanded = ref([])

const treeData = computed(() =>
  dbs.value.map(db => ({ key: `db:${db}`, label: `📁 ${db}`, children: [] }))
)

async function loadSources() {
  const res = await listDataSources()
  sources.value = res.data || []
  if (sources.value.length && !dsId.value) {
    dsId.value = sources.value[0].id
    await loadDbs()
  }
}

async function loadDbs() {
  if (!dsId.value) return
  const res = await listDatabases(dsId.value)
  dbs.value = res.data || []
  tableDetail.value = null
  profile.value = null
}

async function loadNode(node, resolve) {
  if (node.level === 0) return resolve(treeData.value)
  if (node.level === 1) {
    // 数据库节点 → 加载表
    const dbName = node.data.key.replace('db:', '')
    const res = await listTables(dsId.value, dbName)
    const ts = res.data || []
    resolve(ts.map(t => ({ key: `t:${dbName}.${t}`, label: `📄 ${t}` })))
  }
}

async function onNodeClick(node) {
  if (!node.key.startsWith('t:')) return
  const [db, table] = node.key.replace('t:', '').split('.')
  tableDetail.value = null
  profile.value = null
  const res = await describeTable(dsId.value, db, table)
  tableDetail.value = res.data
}

async function loadProfile() {
  if (!tableDetail.value) return
  const [db, table] = tableDetail.value.tableName.split('.')
  const res = await profileTable(dsId.value, db, table)
  profile.value = res.data
}

onMounted(loadSources)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.layout { display: flex; gap: 16px; height: 600px; }
.tree-panel { width: 280px; overflow: auto; border-right: 1px solid #ebeef5; padding-right: 8px; }
.detail-panel { flex: 1; overflow: auto; }
.profile { background: #f5f7fa; padding: 12px; border-radius: 4px; font-size: 12px; max-height: 400px; overflow: auto; }
</style>
