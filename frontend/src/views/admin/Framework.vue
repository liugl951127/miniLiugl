<!--
  @file views/admin/Framework.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Framework.vue (V3.5.48)
  @description AI 框架 - Agent 执行/路由 + 权限管理 + 记忆统计 + 产品搜索
  - 10 端点: agents/execute, agents/route, agents, permission/{grant,revoke,revoke-all,list}, memory/{stats,clear}, products/search
-->
<template>
  <div class="page-framework page">
    <el-card>
      <template #header>
        <div class="header">
          <span>🧬 AI 框架 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <el-radio-group v-model="tab" size="small">
            <el-radio-button value="agents">🤖 Agent</el-radio-button>
            <el-radio-button value="permission">🔐 权限</el-radio-button>
            <el-radio-button value="memory">🧠 记忆</el-radio-button>
            <el-radio-button value="products">📦 产品</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- Agent -->
      <div v-if="tab === 'agents'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>Agent 列表</template>
              <el-table :data="agents" border>
                <el-table-column prop="code" label="编码" width="200" />
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="type" label="类型" width="120" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>执行 / 路由</template>
              <el-tabs v-model="agentTab">
                <el-tab-pane label="执行" name="execute">
                  <el-form label-position="top">
                    <el-form-item label="Agent 编码">
                      <el-input v-model="execForm.code" />
                    </el-form-item>
                    <el-form-item label="输入参数 (JSON)">
                      <el-input v-model="execForm.input" type="textarea" :rows="3" placeholder="{&quot;text&quot;: &quot;...&quot;}" />
                    </el-form-item>
                    <el-form-item label="Session ID">
                      <el-input v-model="execForm.sessionId" />
                    </el-form-item>
                    <el-button type="primary" @click="onExecute" :loading="executing">▶ 执行</el-button>
                  </el-form>
                  <div v-if="execResult" class="result-block">
                    <h5>执行结果</h5>
                    <pre>{{ JSON.stringify(execResult, null, 2) }}</pre>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="路由" name="route">
                  <el-form label-position="top">
                    <el-form-item label="用户输入">
                      <el-input v-model="routeForm.text" type="textarea" :rows="3" />
                    </el-form-item>
                    <el-button @click="onRoute" type="primary" :loading="routing">🛤 路由</el-button>
                  </el-form>
                  <div v-if="routeResult" class="result-block">
                    <el-descriptions :column="1" border>
                      <el-descriptions-item label="路由到 Agent">{{ routeResult.agent }}</el-descriptions-item>
                      <el-descriptions-item label="置信度">
                        <el-progress :percentage="(routeResult.confidence || 0) * 100" />
                      </el-descriptions-item>
                      <el-descriptions-item label="理由">{{ routeResult.reason }}</el-descriptions-item>
                    </el-descriptions>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 权限 -->
      <div v-if="tab === 'permission'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>授予权限</template>
              <el-form label-position="top">
                <el-form-item label="用户 ID"><el-input v-model="permForm.userId" /></el-form-item>
                <el-form-item label="资源类型">
                  <el-select v-model="permForm.resource" style="width: 100%">
                    <el-option label="Agent" value="agent" />
                    <el-option label="Tool" value="tool" />
                    <el-option label="Model" value="model" />
                    <el-option label="KB" value="kb" />
                  </el-select>
                </el-form-item>
                <el-form-item label="资源编码"><el-input v-model="permForm.resourceCode" /></el-form-item>
                <el-form-item label="权限">
                  <el-checkbox-group v-model="permForm.permissions">
                    <el-checkbox value="read">读</el-checkbox>
                    <el-checkbox value="write">写</el-checkbox>
                    <el-checkbox value="execute">执行</el-checkbox>
                    <el-checkbox value="admin">管理</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                <el-button @click="onGrant" type="primary">+ 授权</el-button>
                <el-button @click="onRevoke" type="danger">- 撤销</el-button>
                <el-button @click="onRevokeAll" type="warning">⚠ 撤销该用户所有权限</el-button>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>
                <div class="header">
                  <span>权限列表 ({{ permissions.length }})</span>
                  <el-button size="small" @click="loadPermissions" :icon="Refresh">刷新</el-button>
                </div>
              </template>
              <el-table :data="permissions" border>
                <el-table-column prop="userId" label="用户" width="100" />
                <el-table-column prop="resource" label="资源类型" width="120" />
                <el-table-column prop="resourceCode" label="资源" />
                <el-table-column prop="permissions" label="权限" width="200">
                  <template #default="{ row }">
                    <el-tag v-for="p in row.permissions" :key="p" size="small" style="margin-right: 4px">{{ p }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="grantedAt" label="授权时间" width="180" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 记忆 -->
      <div v-if="tab === 'memory'" class="tab-section">
        <el-row :gutter="16">
          <el-col :span="6"><div class="stat-card"><div class="num">{{ memoryStats.shortTermCount || 0 }}</div><div>短期记忆</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ memoryStats.longTermCount || 0 }}</div><div>长期记忆</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ memoryStats.factCount || 0 }}</div><div>事实数</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="num">{{ memoryStats.userCount || 0 }}</div><div>用户数</div></div></el-col>
        </el-row>
        <el-button style="margin-top: 16px" type="danger" @click="onClearMemory">🗑 清空所有记忆</el-button>
      </div>

      <!-- 产品搜索 -->
      <div v-if="tab === 'products'" class="tab-section">
        <el-form label-position="top">
          <el-form-item label="搜索关键词">
            <el-input v-model="productForm.keyword" placeholder="搜索产品">
              <template #append><el-button @click="onProductSearch" type="primary">🔍 搜索</el-button></template>
            </el-input>
          </el-form-item>
        </el-form>
        <el-table :data="products" border v-if="products.length">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="category" label="类别" width="120" />
          <el-table-column prop="price" label="价格" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="description" label="描述" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { frameworkAgentExecute, frameworkAgentRoute, frameworkAgentsList, frameworkPermissionList, frameworkPermissionGrant, frameworkPermissionRevoke, frameworkPermissionRevokeAll, frameworkMemoryStats, frameworkMemoryClear, frameworkProductSearch } from '@/api/ai'

const tab = ref('agents')
const toast = useToast()
const agentTab = ref('execute')
const executing = ref(false)
const routing = ref(false)

const agents = ref([])
const execForm = reactive({ code: '', input: '{}', sessionId: '' })
const execResult = ref(null)
const routeForm = reactive({ text: '' })
const routeResult = ref(null)

const permForm = reactive({ userId: '', resource: 'agent', resourceCode: '', permissions: ['read'] })
const permissions = ref([])

const memoryStats = reactive({})
const productForm = reactive({ keyword: '' })
const products = ref([])

async function loadAgents() {
  try { const r = await frameworkAgentsList(); agents.value = r.data || [] } catch (e) {}
}

async function loadPermissions() {
  try { const r = await frameworkPermissionList(); permissions.value = r.data || [] } catch (e) {}
}

async function loadMemoryStats() {
  try { const r = await frameworkMemoryStats(); Object.assign(memoryStats, r.data || {}) } catch (e) {}
}

async function onExecute() {
  if (!execForm.code) { toast.warning('请输入 Agent 编码'); return }
  executing.value = true
  try {
    const r = await frameworkAgentExecute({
      code: execForm.code,
      input: JSON.parse(execForm.input || '{}'),
      sessionId: execForm.sessionId
    })
    execResult.value = r.data
  } catch (e) {} finally { executing.value = false }
}

async function onRoute() {
  if (!routeForm.text) { toast.warning('请输入'); return }
  routing.value = true
  try {
    const r = await frameworkAgentRoute({ text: routeForm.text })
    routeResult.value = r.data
  } catch (e) {} finally { routing.value = false }
}

async function onGrant() {
  try { await frameworkPermissionGrant(permForm); toast.success('已授权'); loadPermissions() } catch (e) {}
}
async function onRevoke() {
  try { await frameworkPermissionRevoke(permForm); toast.success('已撤销'); loadPermissions() } catch (e) {}
}
async function onRevokeAll() {
  try {
    await ElMessageBox.confirm(`撤销用户 ${permForm.userId} 所有权限?`, '警告', { type: 'warning' })
    await frameworkPermissionRevokeAll({ userId: permForm.userId })
    toast.success('已撤销')
    loadPermissions()
  } catch (e) { if (e !== 'cancel') {} }
}

async function onClearMemory() {
  try {
    await ElMessageBox.confirm('清空所有记忆数据?', '危险', { type: 'error' })
    await frameworkMemoryClear({ scope: 'all' })
    toast.success('已清空')
    loadMemoryStats()
  } catch (e) { if (e !== 'cancel') {} }
}

async function onProductSearch() {
  if (!productForm.keyword) return
  try { const r = await frameworkProductSearch({ keyword: productForm.keyword }); products.value = r.data || [] } catch (e) {}
}

onMounted(() => {
  loadAgents()
  loadMemoryStats()
})
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.tab-section { padding: 8px 0; }
.stat-card { padding: 16px; background: #fff; border-radius: 4px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.stat-card .num { font-size: 24px; font-weight: 600; color: #409eff; margin-bottom: 4px; }
.result-block { margin-top: 12px; padding: 12px; background: #f5f7fa; border-radius: 4px; }
pre { margin: 0; font-size: 12px; }
</style>
