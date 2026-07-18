<!--
  @file views/memory/Index.vue (入口/列表)
  @version V3.5.12+ (前端注释补全)
  @description 入口/列表
-->
<template>
  <div class="memory-page">
    <div class="page-header">
      <h2>🧠 记忆管理</h2>
      <div class="actions">
        <el-input v-model="sessionInput" placeholder="输入 Session ID" style="width: 200px" clearable>
          <template #append>
            <el-button @click="loadShortTerm"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-button type="primary" @click="activeTab = 'long'; loadLongTerm()">
          <el-icon><Refresh /></el-icon> 刷新长期
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="32" color="#5b8def"><ChatDotRound /></el-icon>
            <div>
              <div class="stat-num">{{ shortTermMsgs.length }}</div>
              <div class="stat-label">短期消息数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="32" color="#67c23a"><DataAnalysis /></el-icon>
            <div>
              <div class="stat-num">{{ shortTermSize }}</div>
              <div class="stat-label">上下文字符</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="32" color="#e6a23c"><Files /></el-icon>
            <div>
              <div class="stat-num">{{ longTermMemories.length }}</div>
              <div class="stat-label">长期记忆条数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="32" color="#f56c6c"><Star /></el-icon>
            <div>
              <div class="stat-num">{{ prefs.length }}</div>
              <div class="stat-label">偏好设置</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="mem-tabs">
      <!-- Tab 1: 短期记忆 (按 session) -->
      <el-tab-pane label="💬 短期记忆 (会话上下文)" name="short">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>短期记忆 - Session #{{ sessionInput || '?' }}</span>
              <div>
                <el-button size="small" @click="showAppend = true" :disabled="!sessionInput">
                  <el-icon><Plus /></el-icon> 追加消息
                </el-button>
                <el-button size="small" type="warning" @click="handleSummarize" :disabled="!sessionInput">
                  <el-icon><MagicStick /></el-icon> 触发摘要
                </el-button>
                <el-button size="small" type="danger" @click="handleClear" :disabled="!sessionInput">
                  <el-icon><Delete /></el-icon> 清空
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="shortTermMsgs" v-loading="loading.short" stripe size="small">
            <el-table-column label="#" type="index" width="50" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'user' ? 'primary' : 'success'" size="small">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="内容" prop="content" min-width="400" show-overflow-tooltip />
            <el-table-column label="时间" prop="timestamp" width="170" />
            <template #empty>
              <el-empty :description="sessionInput ? '该会话暂无消息' : '请先输入 Session ID'" />
            </template>
          </el-table>

          <div v-if="summary" class="summary-box">
            <strong>📝 摘要:</strong> {{ summary }}
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 长期记忆 (向量召回) -->
      <el-tab-pane :label="`🧠 长期记忆 (${longTermMemories.length})`" name="long">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>长期记忆 - 用户 #{{ userId }}</span>
              <div>
                <el-input v-model="recallQuery" placeholder="测试召回 query" style="width: 200px" size="small" />
                <el-button size="small" type="primary" @click="handleRecall" :loading="loading.recall">
                  <el-icon><Search /></el-icon> 召回
                </el-button>
                <el-button size="small" type="success" @click="showStore = true">
                  <el-icon><Plus /></el-icon> 手动存
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="longTermMemories" v-loading="loading.long" stripe>
            <el-table-column label="ID" prop="id" width="70" />
            <el-table-column label="内容" prop="content" min-width="300" show-overflow-tooltip />
            <el-table-column label="角色" prop="role" width="90" />
            <el-table-column label="重要性" prop="importance" width="90">
              <template #default="{ row }">
                <el-rate v-model="row.importance" disabled :max="5" />
              </template>
            </el-table-column>
            <el-table-column label="会话" prop="sessionId" width="100" />
            <el-table-column label="时间" prop="createdAt" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="handleDeleteLong(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 召回结果 -->
          <div v-if="recallHits.length" class="recall-box">
            <h4>🔍 召回结果 ({{ recallHits.length }})</h4>
            <el-card v-for="(hit, i) in recallHits" :key="i" class="recall-card" shadow="hover">
              <div class="recall-meta">
                <el-tag>命中 {{ i + 1 }}</el-tag>
                <el-tag type="success">相似度: {{ (hit.score || 0).toFixed(3) }}</el-tag>
              </div>
              <div class="recall-content">{{ hit.content }}</div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 3: 偏好设置 -->
      <el-tab-pane :label="`⭐ 偏好设置 (${prefs.length})`" name="pref">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户偏好</span>
              <el-button size="small" type="primary" @click="showPref = true">
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </template>

          <el-table :data="prefs" v-loading="loading.pref" stripe>
            <el-table-column label="键" prop="key" min-width="160">
              <template #default="{ row }"><code>{{ row.key }}</code></template>
            </el-table-column>
            <el-table-column label="值" prop="value" min-width="200" />
            <el-table-column label="来源" prop="source" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.source || 'manual' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" prop="updatedAt" width="170" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="editPref(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 追加短期消息对话框 -->
    <el-dialog v-model="showAppend" title="追加短期消息" width="480px">
      <el-form :model="newMsg" label-width="60px">
        <el-form-item label="角色">
          <el-radio-group v-model="newMsg.role">
            <el-radio value="user">user</el-radio>
            <el-radio value="assistant">assistant</el-radio>
            <el-radio value="system">system</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="newMsg.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAppend = false">取消</el-button>
        <el-button type="primary" @click="handleAppend">追加</el-button>
      </template>
    </el-dialog>

    <!-- 存储长期记忆对话框 -->
    <el-dialog v-model="showStore" title="手动存储长期记忆" width="480px">
      <el-form :model="newLong" label-width="80px">
        <el-form-item label="角色">
          <el-input v-model="newLong.role" placeholder="user" />
        </el-form-item>
        <el-form-item label="会话 ID">
          <el-input v-model="newLong.sessionId" placeholder="e.g. 1001" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="newLong.content" type="textarea" :rows="4" placeholder="e.g. 用户喜欢川菜" />
        </el-form-item>
        <el-form-item label="重要性">
          <el-rate v-model="newLong.importance" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStore = false">取消</el-button>
        <el-button type="primary" @click="handleStore">存储</el-button>
      </template>
    </el-dialog>

    <!-- 偏好对话框 -->
    <el-dialog v-model="showPref" :title="prefForm.key ? '编辑偏好' : '新增偏好'" width="400px">
      <el-form :model="prefForm" label-width="80px">
        <el-form-item label="键">
          <el-input v-model="prefForm.key" placeholder="e.g. language" :disabled="!!prefForm.id" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="prefForm.value" placeholder="e.g. zh-CN" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="prefForm.source" placeholder="manual" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPref = false">取消</el-button>
        <el-button type="primary" @click="handleSavePref">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, DataAnalysis, Files, Star, Plus, Search, MagicStick, Delete, Refresh } from '@element-plus/icons-vue'
import * as memApi from '@/api/memory'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id || 1)

const activeTab = ref('short')
const sessionInput = ref('')
const shortTermMsgs = ref([])
const shortTermSize = ref(0)
const summary = ref('')
const longTermMemories = ref([])
const recallHits = ref([])
const recallQuery = ref('')
const prefs = ref([])

const showAppend = ref(false)
const showStore = ref(false)
const showPref = ref(false)

const newMsg = reactive({ role: 'user', content: '' })
const newLong = reactive({ role: 'user', sessionId: '', content: '', importance: 3 })
const prefForm = reactive({ id: null, key: '', value: '', source: 'manual' })

const loading = reactive({ short: false, long: false, recall: false, pref: false })

async function loadShortTerm() {
  if (!sessionInput.value) return ElMessage.warning('请先输入 Session ID')
  loading.short = true
  try {
    const [msgs, size, sum] = await Promise.all([
      memApi.getShortTerm(sessionInput.value),
      memApi.shortTermSize(sessionInput.value),
      memApi.getSummary(sessionInput.value).catch(() => ({ data: { data: '' } }))
    ])
    shortTermMsgs.value = msgs.data?.data || msgs.data || []
    shortTermSize.value = size.data?.data || size.data || 0
    summary.value = sum.data?.data || sum.data || ''
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.short = false }
}

async function handleAppend() {
  if (!newMsg.content.trim()) return ElMessage.warning('请输入内容')
  try {
    await memApi.appendShortTerm(sessionInput.value, { ...newMsg })
    ElMessage.success('已追加')
    showAppend.value = false
    newMsg.content = ''
    await loadShortTerm()
  } catch (e) {
    ElMessage.error('追加失败')
  }
}

async function handleSummarize() {
  try {
    await memApi.summarize(sessionInput.value)
    ElMessage.success('摘要生成中...')
    setTimeout(loadShortTerm, 1500)
  } catch (e) {
    ElMessage.error('触发摘要失败')
  }
}

async function handleClear() {
  await ElMessageBox.confirm('确认清空该会话的短期记忆?', '警告', { type: 'warning' })
  try {
    await memApi.clearShortTerm(sessionInput.value)
    ElMessage.success('已清空')
    await loadShortTerm()
  } catch (e) {
    ElMessage.error('清空失败')
  }
}

async function loadLongTerm() {
  loading.long = true
  try {
    const res = await memApi.recentLongTerm(userId.value, 100)
    longTermMemories.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.long = false }
}

async function handleRecall() {
  if (!recallQuery.value.trim()) return ElMessage.warning('请输入查询')
  loading.recall = true
  try {
    const res = await memApi.recallLongTerm({
      userId: userId.value,
      query: recallQuery.value,
      topK: 5
    })
    recallHits.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('召回失败')
  } finally { loading.recall = false }
}

async function handleStore() {
  if (!newLong.content.trim()) return ElMessage.warning('请输入内容')
  try {
    await memApi.storeLongTerm({
      userId: userId.value,
      sessionId: Number(newLong.sessionId) || 0,
      role: newLong.role,
      content: newLong.content,
      importance: newLong.importance
    })
    ElMessage.success('已存储')
    showStore.value = false
    Object.assign(newLong, { role: 'user', sessionId: '', content: '', importance: 3 })
    await loadLongTerm()
  } catch (e) {
    ElMessage.error('存储失败')
  }
}

async function handleDeleteLong(row) {
  await ElMessageBox.confirm('确认删除该长期记忆?', '警告', { type: 'warning' })
  try {
    await memApi.deleteLongTerm(row.id, userId.value)
    ElMessage.success('已删除')
    await loadLongTerm()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function loadPrefs() {
  loading.pref = true
  try {
    const res = await memApi.listPref(userId.value)
    prefs.value = res.data?.data || res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally { loading.pref = false }
}

function editPref(row) {
  Object.assign(prefForm, { id: row.id || null, key: row.key, value: row.value, source: row.source })
  showPref.value = true
}

async function handleSavePref() {
  if (!prefForm.key.trim() || !prefForm.value.trim()) return ElMessage.warning('键值必填')
  try {
    await memApi.setPref(userId.value, prefForm.key, prefForm.value, prefForm.source || 'manual')
    ElMessage.success('已保存')
    showPref.value = false
    Object.assign(prefForm, { id: null, key: '', value: '', source: 'manual' })
    await loadPrefs()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(async () => {
  await loadLongTerm()
  await loadPrefs()
})
</script>

<style scoped>
.memory-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.actions { display: flex; gap: 12px; align-items: center; }
.stats-row { margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; gap: 12px; }
.stat-num { font-size: 22px; font-weight: 600; color: #303133; }
.stat-label { font-size: 13px; color: #909399; }
.mem-tabs { background: #fff; border-radius: 8px; padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header > div { display: flex; gap: 8px; align-items: center; }
.summary-box { margin-top: 16px; padding: 12px; background: linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%); border-radius: 6px; font-size: 14px; line-height: 1.6; }
.recall-box { margin-top: 20px; }
.recall-box h4 { margin-bottom: 12px; }
.recall-card { margin-bottom: 12px; }
.recall-meta { display: flex; gap: 8px; margin-bottom: 8px; }
.recall-content { color: #303133; line-height: 1.6; }
code { font-family: 'JetBrains Mono', monospace; color: #d63384; padding: 2px 6px; background: #f5f7fa; border-radius: 3px; }
</style>