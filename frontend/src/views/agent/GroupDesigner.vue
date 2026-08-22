<!--
  @file views/agent/GroupDesigner.vue
  @description 智能体群编排设计器 (T1-frontend-designer)
  - 顶部: 群下拉 + 策略 + 加载/保存
  - 左: 候选池 (HTML5 拖入)
  - 右: 成员区 (拖拽排序 + role/position)
  - 底: 目标 + 运行
  - 抽屉: SSE 实时步骤
-->
<template>
  <div class="page-card group-designer">
    <div class="page-header">
      <h2>🧩 智能体群编排</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-tag size="small" type="info">V1.0 · T1</el-tag>
      </div>
    </div>

    <!-- ===== 顶部行 ===== -->
    <el-card class="top-bar" body-style="padding:12px 16px">
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <div class="field-label">群组</div>
          <el-select
            v-model="currentGroupId"
            placeholder="选择群组"
            style="width:100%"
            @change="onGroupChange"
          >
            <el-option
              v-for="g in groups"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            />
          </el-select>
        </el-col>

        <el-col :span="8">
          <div class="field-label">协作策略</div>
          <el-radio-group v-model="strategy" size="default">
            <el-radio-button
              v-for="s in strategies"
              :key="s.value"
              :value="s.value"
            >{{ s.label }}</el-radio-button>
          </el-radio-group>
        </el-col>

        <el-col :span="10" style="text-align:right">
          <el-button :loading="loadingMembers" @click="loadMembers">
            <el-icon><Refresh /></el-icon>加载成员
          </el-button>
          <el-button
            type="primary"
            :loading="saving"
            :disabled="!hasUnsavedChanges"
            @click="saveAll"
          >
            <el-icon><DocumentCopy /></el-icon>
            {{ hasUnsavedChanges ? '保存全部' : '已保存' }}
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- ===== 主体: 左候选池 + 右成员区 ===== -->
    <el-row :gutter="12" style="margin-top:12px">
      <!-- 候选池 -->
      <el-col :span="9">
        <el-card class="panel" body-style="padding:0">
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span>🧪 可用智能体 (拖入右侧)</span>
              <el-tag size="small">{{ candidatePool.length }} 个</el-tag>
            </div>
          </template>
          <div class="pool-list">
            <div
              v-for="a in candidatePool"
              :key="a.code"
              class="pool-item"
              draggable="true"
              @dragstart="onPoolDragStart($event, a)"
            >
              <el-avatar :size="34" :style="{ background: a.color }">
                <el-icon><component :is="a.icon" /></el-icon>
              </el-avatar>
              <div class="pool-info">
                <div class="pool-name">{{ a.displayName }}</div>
                <div class="pool-desc">{{ a.description }}</div>
              </div>
              <el-icon class="pool-drag"><Rank /></el-icon>
            </div>
            <el-empty
              v-if="!candidatePool.length"
              description="无可用 Agent"
              :image-size="60"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 成员区 -->
      <el-col :span="15">
        <el-card class="panel" body-style="padding:0">
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span>🎯 群成员 (按顺序执行)</span>
              <div style="display:flex;gap:8px;align-items:center">
                <el-tag size="small" :type="strategyColor">{{ strategy }}</el-tag>
                <el-tag size="small">{{ members.length }} 个</el-tag>
              </div>
            </div>
          </template>
          <div
            class="drop-zone"
            :class="{ 'drop-active': dropActive }"
            @dragover.prevent="onDragOver"
            @dragleave="onDragLeave"
            @drop="onDropToZone"
          >
            <div
              v-for="(m, idx) in members"
              :key="m._key"
              class="member-card"
              draggable="true"
              @dragstart="onMemberDragStart($event, idx)"
              @dragover.prevent="onMemberDragOver($event, idx)"
              @drop.stop="onMemberDrop($event, idx)"
            >
              <div class="member-pos">{{ idx + 1 }}</div>
              <el-avatar :size="36" :style="{ background: agentColor(m.agentCode) }">
                <el-icon><component :is="agentIcon(m.agentCode)" /></el-icon>
              </el-avatar>
              <div class="member-info">
                <div class="member-name">
                  {{ agentDisplayName(m.agentCode) }}
                  <el-tag
                    v-if="m._isNew"
                    size="small"
                    type="success"
                    style="margin-left:6px"
                  >新</el-tag>
                  <el-tag
                    v-else-if="m._isDeleted"
                    size="small"
                    type="danger"
                    style="margin-left:6px"
                  >删</el-tag>
                  <el-tag
                    v-else-if="m._isModified"
                    size="small"
                    type="warning"
                    style="margin-left:6px"
                  >改</el-tag>
                </div>
                <div class="member-meta">
                  <span>code: {{ m.agentCode }}</span>
                </div>
              </div>
              <el-select
                v-model="m.role"
                size="small"
                style="width:110px"
                @change="markModified(m)"
              >
                <el-option label="MANAGER" value="MANAGER" />
                <el-option label="WORKER"  value="WORKER" />
                <el-option label="CRITIC"  value="CRITIC" />
              </el-select>
              <el-button
                size="small"
                type="danger"
                link
                @click="removeMemberLocal(m)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>

            <el-empty
              v-if="!members.length"
              description="将左侧 Agent 拖到这里开始编排"
              :image-size="80"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 底部: 目标 + 运行 ===== -->
    <el-card class="run-bar" body-style="padding:12px 16px" style="margin-top:12px">
      <el-row :gutter="12" align="middle">
        <el-col :span="20">
          <div class="field-label">任务目标</div>
          <el-input
            v-model="goal"
            type="textarea"
            :rows="2"
            placeholder="例如: 写一份 Q3 销售报告并附上优化建议"
          />
        </el-col>
        <el-col :span="4" style="text-align:right">
          <el-button
            type="primary"
            :loading="running"
            :disabled="!canRun"
            size="large"
            @click="run"
            style="width:100%"
          >
            <el-icon v-if="!running"><CaretRight /></el-icon>
            {{ running ? '运行中…' : '▶ 运行' }}
          </el-button>
        </el-col>
      </el-row>
      <div class="run-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>需先保存成员配置才能运行; SSE 流式输出实时追加到右侧抽屉</span>
      </div>
    </el-card>

    <!-- ===== 抽屉: 实时运行结果 ===== -->
    <el-drawer
      v-model="drawerVisible"
      title="📡 实时运行结果 (SSE)"
      size="60%"
      direction="rtl"
      destroy-on-close
    >
      <div class="drawer-content">
        <!-- 策略/总览 -->
        <div class="run-summary">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="群组">{{ currentGroupId }}</el-descriptions-item>
            <el-descriptions-item label="策略">
              <el-tag size="small" :type="strategyColor">{{ strategy }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="成员">{{ members.length }} 个</el-descriptions-item>
            <el-descriptions-item label="目标" :span="3">
              <div style="max-height:48px;overflow:auto">{{ goal || '-' }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 步骤列表 -->
        <div v-if="!steps.length && running" class="empty-loading">
          <el-icon class="rotating" :size="32"><Loading /></el-icon>
          <div>正在连接后端, 等待首个 step…</div>
        </div>
        <div v-if="!steps.length && !running && !finalResult" class="empty-tip">
          <el-icon :size="32"><VideoPlay /></el-icon>
          <div>点击「▶ 运行」开始流式执行</div>
        </div>

        <div
          v-for="(s, i) in steps"
          :key="i"
          class="step-card"
          :class="'step-' + s.status"
        >
          <div class="step-head">
            <div class="step-head-left">
              <el-avatar :size="32" :style="{ background: agentColor(s.agentCode) }">
                <el-icon><component :is="agentIcon(s.agentCode)" /></el-icon>
              </el-avatar>
              <div>
                <div class="step-agent">
                  {{ s.agentCode || 'unknown' }}
                  <el-tag
                    v-if="s.role"
                    size="small"
                    :type="roleTagType(s.role)"
                    style="margin-left:6px"
                  >{{ s.role }}</el-tag>
                </div>
                <div class="step-meta">
                  Step {{ i + 1 }}
                </div>
              </div>
            </div>
            <div>
              <el-tag
                v-if="s.status === 'running'"
                type="warning"
                size="small"
                effect="dark"
              >
                <el-icon class="rotating"><Loading /></el-icon> 进行中
              </el-tag>
              <el-tag v-else-if="s.status === 'done'" type="success" size="small">
                ✅ 完成
              </el-tag>
              <el-tag v-else-if="s.status === 'error'" type="danger" size="small">
                ❌ 失败
              </el-tag>
            </div>
          </div>
          <div class="step-body">
            <pre v-if="s.content" class="step-content">{{ s.content }}</pre>
            <div v-else class="step-content-placeholder">等待内容…</div>
          </div>
        </div>

        <!-- 最终结果 -->
        <div v-if="finalResult" class="final-block">
          <el-divider content-position="left">
            <span style="font-weight:600">🏁 最终结果</span>
          </el-divider>
          <el-card
            :class="['final-card', finalResult.success ? 'success' : 'failed']"
            body-style="padding:12px"
          >
            <div v-if="finalResult.finalAnswer" class="final-answer">
              <pre>{{ finalResult.finalAnswer }}</pre>
            </div>
            <el-descriptions
              v-if="finalResult"
              :column="3"
              border
              size="small"
              style="margin-top:8px"
            >
              <el-descriptions-item label="成功">
                <el-tag
                  :type="finalResult.success ? 'success' : 'danger'"
                  size="small"
                >{{ finalResult.success ? '✅' : '❌' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="总步数">{{ finalResult.totalSteps || steps.length }}</el-descriptions-item>
              <el-descriptions-item label="策略">{{ finalResult.strategy || strategy }}</el-descriptions-item>
              <el-descriptions-item v-if="finalResult.error" label="错误" :span="3">
                <span style="color: var(--el-color-danger)">{{ finalResult.error }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, DocumentCopy, CaretRight, Delete, Rank, Loading,
  InfoFilled, VideoPlay,
  UserFilled, ChatDotRound, DataAnalysis, Brush, Cpu, Promotion,
  Document, EditPen, SetUp,
} from '@element-plus/icons-vue'
import { agentGroupApi } from '@/api/agentGroup'

// ===== 候选池 (硬编码示例) =====
const candidatePool = ref([
  {
    code: 'mgr', displayName: '协调 Agent', description: '拆解任务、分发给 worker',
    icon: 'SetUp', color: '#5b8def'
  },
  {
    code: 'echo-analyzer', displayName: '分析 Agent', description: '信息提取、模式识别',
    icon: 'DataAnalysis', color: '#10b981'
  },
  {
    code: 'echo-writer', displayName: '写作 Agent', description: '长文写作、文案润色',
    icon: 'EditPen', color: '#f59e0b'
  },
  {
    code: 'echo-coder', displayName: '编程 Agent', description: '代码生成、调试、Bug 修复',
    icon: 'Cpu', color: '#a855f7'
  },
  {
    code: 'echo-summarizer', displayName: '摘要 Agent', description: '长文摘要、关键信息提取',
    icon: 'Document', color: '#0ea5e9'
  },
  {
    code: 'echo-translator', displayName: '翻译 Agent', description: '多语言互译、术语本地化',
    icon: 'Promotion', color: '#ec4899'
  },
  {
    code: 'echo-reviewer', displayName: '审核 Agent', description: '内容审核、Critic 评分',
    icon: 'ChatDotRound', color: '#ef4444'
  },
  {
    code: 'echo-broadcaster', displayName: '广播 Agent', description: '消息分发、通知推送',
    icon: 'Brush', color: '#14b8a6'
  },
])

// ===== 状态 =====
const groups = ref([{ id: 1, name: '默认群' }])
const currentGroupId = ref(1)
const strategy = ref('PIPELINE')
const strategies = ref([
  { value: 'PIPELINE', label: '顺序' },
  { value: 'PARALLEL', label: '并行' },
  { value: 'DEBATE',   label: '辩论' },
])

const members = ref([])           // 编辑中的成员列表
const originalMembers = ref([])   // 服务端原始 (用于 diff)
const loadingMembers = ref(false)
const saving = ref(false)

const goal = ref('')
const running = ref(false)
const drawerVisible = ref(false)
const steps = ref([])
const finalResult = ref(null)

const dropActive = ref(false)
let dragSourceIdx = -1
let dragFromPool = null

// ===== 计算属性 =====
const strategyColor = computed(() => ({
  PIPELINE: 'primary',
  PARALLEL: 'success',
  DEBATE:   'warning',
}[strategy.value] || 'info'))

const hasUnsavedChanges = computed(() => {
  if (members.value.length !== originalMembers.value.length) return true
  return members.value.some(m => m._isNew || m._isDeleted || m._isModified)
})

const canRun = computed(() =>
  !running.value
  && currentGroupId.value
  && goal.value.trim()
  && members.value.filter(m => !m._isDeleted).length > 0
)

// ===== 启动 =====
onMounted(async () => {
  await Promise.all([loadGroups(), loadStrategies()])
  await loadMembers()
})

// ===== 加载 =====
async function loadGroups() {
  try {
    const r = await agentGroupApi.listGroups()
    const list = Array.isArray(r) ? r
      : (r?.data?.data || r?.data || [])
    if (Array.isArray(list) && list.length) {
      groups.value = list.map(g => ({
        id: g.id ?? g.groupId,
        name: g.name || g.groupName || `群 #${g.id ?? g.groupId}`,
      }))
      if (!groups.value.find(g => g.id === currentGroupId.value)) {
        currentGroupId.value = groups.value[0].id
      }
    }
  } catch {
    // 兜底已在 API 里
  }
}

async function loadStrategies() {
  try {
    const r = await agentGroupApi.getStrategies()
    const list = Array.isArray(r) ? r
      : (r?.data?.data || r?.data || [])
    if (Array.isArray(list) && list.length) {
      // 兼容多种返回结构
      strategies.value = list.map(s => ({
        value: s.value ?? s.name,
        label: s.label ?? s.name ?? s.value,
        description: s.description ?? s.usage ?? '',
      }))
    }
  } catch {
    // 默认值已在 state
  }
}

async function loadMembers() {
  if (!currentGroupId.value) {
    ElMessage.warning('请先选择群组')
    return
  }
  loadingMembers.value = true
  try {
    const r = await agentGroupApi.listMembers(currentGroupId.value)
    const list = r?.data?.data || r?.data || r || []
    const arr = Array.isArray(list) ? list : []
    // 标准化 + 按 position 排序
    const sorted = [...arr].sort((a, b) => (a.position || 0) - (b.position || 0))
    members.value = sorted.map(m => ({ ...m, _key: 's' + m.memberId, _isNew: false, _isDeleted: false, _isModified: false }))
    originalMembers.value = JSON.parse(JSON.stringify(members.value))
    ElMessage.success(`已加载 ${members.value.length} 个成员`)
  } catch (e) {
    members.value = []
    originalMembers.value = []
    ElMessage.warning('加载成员失败: ' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    loadingMembers.value = false
  }
}

function onGroupChange() {
  members.value = []
  originalMembers.value = []
  loadMembers()
}

// ===== 保存 (按差异) =====
async function saveAll() {
  if (!currentGroupId.value) {
    ElMessage.warning('请先选择群组')
    return
  }
  saving.value = true
  try {
    // 1. 删除 _isDeleted 的
    const toDelete = members.value.filter(m => m._isDeleted && m.memberId)
    for (const m of toDelete) {
      await agentGroupApi.removeMember(currentGroupId.value, m.memberId)
    }
    // 2. 新增 _isNew 的
    const toAdd = members.value.filter(m => m._isNew && !m._isDeleted)
    const createdIdMap = {} // agentCode -> memberId
    for (const m of toAdd) {
      const r = await agentGroupApi.addMember(currentGroupId.value, {
        agentCode: m.agentCode,
        role: m.role,
        position: m.position,
        configJson: m.configJson || '',
        enabled: m.enabled ?? 1,
      })
      const created = r?.data?.data || r?.data || r
      if (created?.memberId) {
        createdIdMap[m._key] = created.memberId
        m.memberId = created.memberId
      }
      m._isNew = false
      m._isModified = false
    }
    // 3. 更新 _isModified 的
    const toUpdate = members.value.filter(m => m._isModified && m.memberId && !m._isDeleted)
    for (const m of toUpdate) {
      await agentGroupApi.updateMember(currentGroupId.value, m.memberId, {
        role: m.role,
        position: m.position,
        configJson: m.configJson || '',
        enabled: m.enabled ?? 1,
      })
      m._isModified = false
    }
    // 4. 真正移除 _isDeleted (从数组)
    members.value = members.value.filter(m => !m._isDeleted)
    // 5. 重排 position (按当前顺序 0..n-1)
    if (members.value.length) {
      const order = members.value.map((m, i) => ({ memberId: m.memberId, position: i }))
      try {
        await agentGroupApi.reorder(currentGroupId.value, order)
      } catch (e) {
        // 重排失败不致命
        console.warn('[GroupDesigner] reorder 失败:', e?.message)
      }
      // 本地 position 同步
      members.value.forEach((m, i) => { m.position = i })
    }
    // 6. 刷新原始
    originalMembers.value = JSON.parse(JSON.stringify(members.value))
    ElMessage.success(`保存成功: 删 ${toDelete.length} / 新增 ${toAdd.length} / 改 ${toUpdate.length}`)
  } catch (e) {
    ElMessage.error('保存失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// ===== 拖拽: 候选池 → 成员区 =====
function onPoolDragStart(e, agent) {
  dragFromPool = agent
  dragSourceIdx = -1
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('text/plain', 'pool:' + agent.code)
}

function onDragOver(e) {
  e.preventDefault()
  e.dataTransfer.dropEffect = dragFromPool ? 'copy' : 'move'
  dropActive.value = true
}

function onDragLeave() {
  dropActive.value = false
}

function onDropToZone(e) {
  e.preventDefault()
  dropActive.value = false
  if (!dragFromPool) return
  addMemberFromPool(dragFromPool)
  dragFromPool = null
}

function addMemberFromPool(agent) {
  const position = members.value.filter(m => !m._isDeleted).length
  members.value.push({
    memberId: null,
    groupId: currentGroupId.value,
    agentCode: agent.code,
    role: position === 0 ? 'MANAGER' : 'WORKER',
    position,
    configJson: '',
    enabled: 1,
    _key: 'n' + Date.now() + Math.random().toString(36).slice(2, 6),
    _isNew: true,
    _isDeleted: false,
    _isModified: false,
  })
}

// ===== 拖拽: 成员区内部排序 =====
function onMemberDragStart(e, idx) {
  dragSourceIdx = idx
  dragFromPool = null
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', 'member:' + idx)
}

function onMemberDragOver(e, idx) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

function onMemberDrop(e, idx) {
  e.preventDefault()
  if (dragFromPool) {
    // 从池拖到具体位置
    insertAt(dragFromPool, idx)
    dragFromPool = null
    return
  }
  if (dragSourceIdx < 0 || dragSourceIdx === idx) return
  const arr = [...members.value]
  const [moved] = arr.splice(dragSourceIdx, 1)
  arr.splice(idx, 0, moved)
  members.value = arr
  // 重写 position + 标记已改
  members.value.forEach((m, i) => {
    if (m.position !== i) {
      m.position = i
      if (!m._isNew) m._isModified = true
    }
  })
  dragSourceIdx = -1
}

function insertAt(agent, idx) {
  const position = idx
  members.value.splice(idx, 0, {
    memberId: null,
    groupId: currentGroupId.value,
    agentCode: agent.code,
    role: members.value.length === 0 ? 'MANAGER' : 'WORKER',
    position,
    configJson: '',
    enabled: 1,
    _key: 'n' + Date.now() + Math.random().toString(36).slice(2, 6),
    _isNew: true,
    _isDeleted: false,
    _isModified: false,
  })
  members.value.forEach((m, i) => {
    if (m.position !== i) {
      m.position = i
      if (!m._isNew) m._isModified = true
    }
  })
}

// ===== 编辑 =====
function markModified(m) {
  if (!m._isNew && !m._isDeleted) m._isModified = true
}

function removeMemberLocal(m) {
  if (m._isNew) {
    // 直接移除
    members.value = members.value.filter(x => x._key !== m._key)
  } else {
    m._isDeleted = true
  }
  // 重排 position
  members.value.filter(x => !x._isDeleted).forEach((m, i) => {
    if (m.position !== i) {
      m.position = i
      if (!m._isNew) m._isModified = true
    }
  })
}

// ===== 展示辅助 =====
function agentColor(code) {
  const a = candidatePool.value.find(x => x.code === code)
  return a?.color || '#5b8def'
}
function agentIcon(code) {
  const a = candidatePool.value.find(x => x.code === code)
  return a?.icon || 'UserFilled'
}
function agentDisplayName(code) {
  const a = candidatePool.value.find(x => x.code === code)
  return a?.displayName || code
}
function roleTagType(role) {
  return { MANAGER: 'primary', WORKER: 'success', CRITIC: 'warning' }[role] || 'info'
}

// ===== SSE 运行 =====
async function run() {
  if (hasUnsavedChanges.value) {
    try {
      await ElMessageBox.confirm('有未保存的修改, 是否先保存再运行?', '提示', {
        confirmButtonText: '保存并运行',
        cancelButtonText: '直接运行',
        type: 'warning',
      })
      await saveAll()
    } catch {
      // 用户选 "直接运行"
    }
  }
  running.value = true
  steps.value = []
  finalResult.value = null
  drawerVisible.value = true
  try {
    const res = await agentGroupApi.runStream(currentGroupId.value, {
      goal: goal.value,
      strategy: strategy.value,
      tools: [],
    })
    if (!res.ok) throw new Error('运行失败: HTTP ' + res.status)
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const chunks = buf.split('\n\n')
      buf = chunks.pop()
      for (const chunk of chunks) {
        const ev = {}
        chunk.split('\n').forEach(l => {
          if (l.startsWith('event:')) ev.name = l.slice(7).trim()
          if (l.startsWith('data:')) ev.data = (ev.data || '') + l.slice(6)
        })
        if (!ev.name || !ev.data) continue
        let data
        try { data = JSON.parse(ev.data) } catch { continue }
        handleSseEvent(ev.name, data)
      }
    }
  } catch (e) {
    ElMessage.error('运行失败: ' + (e?.response?.data?.message || e?.message || '未知错误'))
  } finally {
    running.value = false
  }
}

function handleSseEvent(name, data) {
  if (name === 'step-start') {
    steps.value.push({ ...data, content: '', status: 'running' })
  } else if (name === 'step-token') {
    const s = steps.value.find(x => x.agentCode === data.agentCode)
    if (s) s.content = (s.content || '') + (data.content || '')
  } else if (name === 'step-end') {
    const s = steps.value.find(x => x.agentCode === data.agentCode)
    if (s) s.status = 'done'
  } else if (name === 'final') {
    finalResult.value = data
    ElMessage.success(data.success ? '执行成功' : '执行失败: ' + (data.error || ''))
  } else if (name === 'error') {
    ElMessage.error('运行出错: ' + (data.message || ''))
  }
}
</script>

<style lang="scss" scoped>
.group-designer {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    h2 { margin: 0; font-size: 18px; }
  }

  .field-label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 4px;
  }

  .panel :deep(.el-card__body) { padding: 0; }
  .panel { height: 480px; display: flex; flex-direction: column; }
  .panel :deep(.el-card__header) { padding: 10px 14px; }

  .pool-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
  }

  .pool-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 10px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    margin-bottom: 6px;
    background: var(--el-fill-color-blank);
    cursor: grab;
    transition: all 0.15s;
    &:hover {
      border-color: var(--el-color-primary);
      box-shadow: 0 2px 6px rgba(91, 141, 239, 0.15);
    }
    &:active { cursor: grabbing; }
    .pool-info { flex: 1; min-width: 0; }
    .pool-name { font-weight: 600; font-size: 13px; }
    .pool-desc {
      font-size: 11px;
      color: var(--el-text-color-secondary);
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .pool-drag { color: var(--el-text-color-placeholder); }
  }

  .drop-zone {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    min-height: 200px;
    transition: background 0.15s;
    &.drop-active { background: rgba(91, 141, 239, 0.08); }
  }

  .member-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 10px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    margin-bottom: 6px;
    background: var(--el-fill-color-blank);
    cursor: grab;
    transition: all 0.15s;
    &:hover { box-shadow: 0 2px 6px rgba(0,0,0,0.06); }
    &:active { cursor: grabbing; }
    .member-pos {
      width: 22px; height: 22px;
      border-radius: 50%;
      background: var(--el-color-primary);
      color: #fff;
      display: flex; align-items: center; justify-content: center;
      font-size: 11px; font-weight: 700;
      flex-shrink: 0;
    }
    .member-info { flex: 1; min-width: 0; }
    .member-name { font-weight: 600; font-size: 13px; }
    .member-meta {
      font-size: 11px;
      color: var(--el-text-color-secondary);
    }
  }

  .run-tip {
    margin-top: 6px;
    font-size: 11px;
    color: var(--el-text-color-secondary);
    display: flex; align-items: center; gap: 4px;
  }
}

.drawer-content {
  padding: 0 16px 16px;
}

.run-summary { margin-bottom: 12px; }

.empty-loading, .empty-tip {
  text-align: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  > * + * { margin-top: 8px; }
}

.step-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 10px;
  background: var(--el-fill-color-blank);
  overflow: hidden;
  transition: all 0.2s;
  &.step-running { border-color: var(--el-color-warning); }
  &.step-done { border-color: var(--el-color-success-light-5); }
  &.step-error { border-color: var(--el-color-danger-light-5); }
}

.step-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.step-head-left { display: flex; align-items: center; gap: 8px; }
.step-agent { font-weight: 600; font-size: 13px; }
.step-meta { font-size: 11px; color: var(--el-text-color-secondary); }

.step-body { padding: 10px 12px; }
.step-content {
  margin: 0;
  font-family: var(--el-font-family);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 240px;
  overflow-y: auto;
  background: var(--el-fill-color-light);
  padding: 8px 10px;
  border-radius: 4px;
  line-height: 1.6;
}
.step-content-placeholder {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  font-style: italic;
}

.final-card.success { border-color: var(--el-color-success-light-5); }
.final-card.failed { border-color: var(--el-color-danger-light-5); }
.final-answer pre {
  margin: 0;
  font-family: var(--el-font-family);
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  max-height: 400px;
  overflow-y: auto;
}

.rotating { animation: rotating 1.4s linear infinite; }
@keyframes rotating {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>
