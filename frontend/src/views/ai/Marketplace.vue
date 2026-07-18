<!--
  @file views/ai/Marketplace.vue (AI 市场)
  @version V3.5.12+ (前端注释补全)
  @description AI 市场
-->
<template>
  <div class="marketplace-container">
    <div class="mp-header">
      <h1>🏪 AI Agent 市场 <span class="badge">V2.9.0</span></h1>
      <p class="sub">浏览 / 上传 / 评分 · 用户共建的 Agent 生态</p>
    </div>

    <!-- 统计 -->
    <el-row :gutter="16" v-if="stats">
      <el-col :span="6">
        <el-card class="kpi">
          <div class="kpi-label">总 Agent</div>
          <div class="kpi-value">{{ stats.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi success">
          <div class="kpi-label">已发布</div>
          <div class="kpi-value">{{ stats.published }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi warn">
          <div class="kpi-label">待审核</div>
          <div class="kpi-value">{{ stats.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi primary">
          <div class="kpi-label">总使用</div>
          <div class="kpi-value">{{ stats.totalUsage.toLocaleString() }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card style="margin-top: 16px">
      <el-form :inline="true" size="small">
        <el-form-item label="分类">
          <el-select v-model="filterCategory" placeholder="全部分类" clearable style="width: 160px">
            <el-option label="全部" value="" />
            <el-option label="购物" value="SHOPPING" />
            <el-option label="酒店" value="HOTEL" />
            <el-option label="娱乐" value="ENTERTAINMENT" />
            <el-option label="教育" value="EDUCATION" />
            <el-option label="旅行" value="TRAVEL" />
            <el-option label="生产" value="PRODUCTIVITY" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filterKeyword" placeholder="名称/描述" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filterSort" style="width: 120px">
            <el-option label="最新" value="" />
            <el-option label="评分" value="rating" />
            <el-option label="使用" value="usage" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadAgents" :loading="loading">🔍 搜索</el-button>
        <el-button type="success" @click="showUpload = true" icon="Plus">📤 上传 Agent</el-button>
      </el-form>
    </el-card>

    <!-- Agent 卡片网格 -->
    <el-row :gutter="16" style="margin-top: 16px" v-loading="loading">
      <el-col :span="6" v-for="agent in agents" :key="agent.id" style="margin-bottom: 16px">
        <el-card class="agent-card" shadow="hover" @click.native="showDetail(agent)">
          <div class="agent-header">
            <span class="agent-icon">{{ agent.icon || '🤖' }}</span>
            <div class="agent-meta">
              <div class="agent-name">{{ agent.name }}</div>
              <div class="agent-author">by {{ agent.authorName }}</div>
            </div>
            <el-tag size="small" :type="categoryType(agent.category)">{{ agent.category }}</el-tag>
          </div>
          <div class="agent-desc">{{ agent.description }}</div>
          <div class="agent-tags">
            <el-tag v-for="tag in (agent.tags || '').split(',').filter(t => t)" :key="tag" size="small" effect="plain" style="margin-right: 4px">
              {{ tag }}
            </el-tag>
          </div>
          <div class="agent-stats">
            <span class="rating">⭐ {{ (agent.avgRating || 0).toFixed(1) }}</span>
            <span class="usage">📊 {{ agent.usageCount }} 次</span>
            <span class="version">v{{ agent.version }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && agents.length === 0" description="暂无 Agent" />

    <!-- 上传对话框 -->
    <el-dialog v-model="showUpload" title="📤 上传 Agent" width="640px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="uploadForm.name" placeholder="例如: 智能旅行规划师" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" style="width:100%">
            <el-option label="购物" value="SHOPPING" />
            <el-option label="酒店" value="HOTEL" />
            <el-option label="娱乐" value="ENTERTAINMENT" />
            <el-option label="教育" value="EDUCATION" />
            <el-option label="旅行" value="TRAVEL" />
            <el-option label="生产" value="PRODUCTIVITY" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标 (Emoji)">
          <el-input v-model="uploadForm.icon" placeholder="🤖" maxlength="4" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="uploadForm.visibility">
            <el-radio value="PRIVATE">私有 (仅自己)</el-radio>
            <el-radio value="PUBLIC">公开 (需审核)</el-radio>
            <el-radio value="UNLISTED">凭链接 (审核自动通过)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="uploadForm.tags" placeholder="逗号分隔, 如: 旅行,LBS,推荐" />
        </el-form-item>
        <el-form-item label="能力">
          <el-input v-model="uploadForm.capabilities" placeholder="逗号分隔, 如: travel_plan,poi_search" />
        </el-form-item>
        <el-form-item label="定义 JSON" required>
          <el-input
            v-model="uploadForm.definitionJson"
            type="textarea"
            :rows="8"
            placeholder='{"capabilities":["..."],"tools":["..."],"systemPrompt":"..."}'
          />
          <div class="hint">含 capabilities/tools/systemPrompt 的 JSON 对象</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetail_" :title="detail?.name" width="720px" v-if="detail">
      <div class="detail-header">
        <span class="agent-icon big">{{ detail.icon }}</span>
        <div>
          <h2>{{ detail.name }}</h2>
          <p>{{ detail.description }}</p>
          <el-tag size="small">{{ detail.category }}</el-tag>
          <el-tag size="small" type="info" style="margin-left:8px">v{{ detail.version }}</el-tag>
          <el-tag size="small" type="success" style="margin-left:8px">⭐ {{ (detail.avgRating || 0).toFixed(1) }}</el-tag>
        </div>
      </div>
      <el-divider />
      <h4>能力</h4>
      <p><code>{{ detail.capabilities }}</code></p>
      <h4>使用</h4>
      <p>{{ detail.usageCount }} 次</p>
      <h4>作者</h4>
      <p>{{ detail.authorName }}</p>
      <el-divider />
      <h4>评分 ({{ detail.ratingCount }})</h4>
      <div class="rate-section">
        <el-rate v-model="myRating" :max="5" show-text />
        <el-button type="primary" size="small" @click="submitRating" :disabled="!myRating">提交评分</el-button>
      </div>
      <h4>评论</h4>
      <el-input v-model="myComment" type="textarea" :rows="2" placeholder="说说你的看法..." />
    </el-dialog>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { marketplaceApi } from '@/api/marketplace'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const stats = ref(null)
const agents = ref([])
const loading = ref(false)
const filterCategory = ref('')
const filterKeyword = ref('')
const filterSort = ref('')

const showUpload = ref(false)
const uploading = ref(false)
const uploadForm = reactive({
  name: '', description: '', category: 'CUSTOM', icon: '🤖',
  visibility: 'PRIVATE', tags: '', capabilities: '',
  definitionJson: '{"capabilities":[],"tools":[],"systemPrompt":""}'
})

const showDetail_ = ref(false)
const detail = ref(null)
const myRating = ref(0)
const myComment = ref('')

const categoryType = (cat) => {
  const map = {
    SHOPPING: 'danger', HOTEL: 'warning', ENTERTAINMENT: 'success',
    EDUCATION: 'primary', TRAVEL: 'info', PRODUCTIVITY: '',
    CUSTOM: ''
  }
  return map[cat] || ''
}

const loadStats = async () => {
  try {
    const res = await marketplaceApi.stats()
    stats.value = res.data
  } catch (e) { console.warn(e) }
}

const loadAgents = async () => {
  loading.value = true
  try {
    const res = await marketplaceApi.browse({
      category: filterCategory.value || undefined,
      keyword: filterKeyword.value || undefined,
      sortBy: filterSort.value || undefined
    })
    agents.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const submitUpload = async () => {
  if (!uploadForm.name) {
    ElMessage.warning('请填写名称')
    return
  }
  uploading.value = true
  try {
    const res = await marketplaceApi.upload({
      ...uploadForm,
      authorId: userStore.profile?.id || 0,
      authorName: userStore.profile?.username || 'anonymous',
      version: '1.0.0'
    })
    if (res.data?.code === 0) {
      ElMessage.success('上传成功! ' + (uploadForm.visibility === 'PUBLIC' ? '待审核' : '已发布'))
      showUpload.value = false
      loadAgents()
      loadStats()
    } else {
      ElMessage.error(res.data?.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

const showDetail = async (agent) => {
  try {
    const res = await marketplaceApi.detail(agent.agentKey)
    detail.value = res.data
    showDetail_.value = true
    myRating.value = 0
    myComment.value = ''
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

const submitRating = async () => {
  if (!myRating.value) {
    ElMessage.warning('请选择评分')
    return
  }
  try {
    const res = await marketplaceApi.rate(detail.value.agentKey, {
      userId: userStore.profile?.id || 0,
      username: userStore.profile?.username || 'anonymous',
      rating: myRating.value,
      comment: myComment.value
    })
    if (res.data?.code === 0) {
      ElMessage.success('评分成功')
      // 重新加载详情
      const r2 = await marketplaceApi.detail(detail.value.agentKey)
      detail.value = r2.data
    } else {
      ElMessage.error(res.data?.message || '评分失败')
    }
  } catch (e) {
    ElMessage.error('评分失败: ' + e.message)
  }
}

onMounted(async () => {
  await loadStats()
  await loadAgents()
})
</script>

<style scoped>
.marketplace-container { padding: 20px; }
.mp-header h1 { margin: 0 0 4px 0; font-size: 24px; }
.badge { background: #67c23a; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }
.sub { color: #909399; margin: 0 0 16px 0; font-size: 13px; }

.kpi { text-align: center; }
.kpi-label { color: #909399; font-size: 12px; }
.kpi-value { font-size: 24px; font-weight: 600; margin-top: 4px; }
.kpi.success { border-left: 3px solid #67c23a; }
.kpi.warn { border-left: 3px solid #e6a23c; }
.kpi.primary { border-left: 3px solid #409eff; }

.agent-card { cursor: pointer; height: 180px; display: flex; flex-direction: column; }
.agent-card :deep(.el-card__body) { padding: 12px; flex: 1; }
.agent-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.agent-icon { font-size: 32px; }
.agent-icon.big { font-size: 64px; }
.agent-meta { flex: 1; min-width: 0; }
.agent-name { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.agent-author { font-size: 11px; color: #909399; }
.agent-desc { font-size: 12px; color: #606266; line-height: 1.4; height: 36px; overflow: hidden; }
.agent-tags { margin: 8px 0; }
.agent-stats { display: flex; justify-content: space-between; font-size: 11px; color: #909399; }
.hint { color: #909399; font-size: 11px; margin-top: 4px; }

.detail-header { display: flex; align-items: center; gap: 16px; }
.detail-header h2 { margin: 0; font-size: 20px; }
.detail-header p { margin: 4px 0 8px 0; color: #606266; font-size: 13px; }
.rate-section { display: flex; align-items: center; gap: 12px; margin: 8px 0 12px; }
h4 { margin: 12px 0 6px 0; font-size: 13px; color: #303133; }
</style>
