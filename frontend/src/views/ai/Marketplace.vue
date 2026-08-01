<!--
  @file views/ai/Marketplace.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/Marketplace.vue (AI 市场)
  @version V3.5.12+ (前端注释补全)
  @description AI 市场
-->
<template>
  <div class="page-marketplace">
    <!-- 1. page-header -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('marketplace.title') }} <el-tag size="small" type="info">V2.9.0</el-tag></h2>
        <p class="page-subtitle">浏览 / 上传 / 评分 · 用户共建的 Agent 生态</p>
      </div>
      <el-button-group>
        <el-input v-model="search" placeholder="搜索 Agent..." clearable style="width: 240px" />
        <el-button :icon="Refresh" @click="loadAll" />
        <el-button type="primary" :icon="Upload" @click="showUpload = true">上传</el-button>
      </el-button-group>
    </header>

    <!-- 2. section: 4 KPI -->
    <section class="section" v-if="stats">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="总 Agent" :value="stats.total" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="已发布" :value="stats.published" :value-style="{ color: '#10b981' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="总下载" :value="stats.downloads" :value-style="{ color: '#6366f1' }" /></el-card></el-col>
        <el-col :xs="12" :sm="6"><el-card shadow="hover" class="kpi-card"><el-statistic title="平均评分" :value="stats.avgRating" :precision="1" suffix="/5" :value-style="{ color: '#a855f7' }" /></el-card></el-col>
      </el-row>
    </section>

    <!-- 3. section: 分类筛选 + 排序 -->
    <section class="section">
      <el-card shadow="hover">
        <el-row :gutter="16" align="middle">
          <el-col :xs="24" :sm="12">
            <el-radio-group v-model="filterCategory">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="CHAT">对话</el-radio-button>
              <el-radio-button label="TOOL">工具</el-radio-button>
              <el-radio-button label="WORKFLOW">工作流</el-radio-button>
              <el-radio-button label="MULTIMODAL">多模态</el-radio-button>
            </el-radio-group>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-select v-model="sortBy" style="width: 200px; float: right">
              <el-option label="最新发布" value="newest" />
              <el-option label="最多下载" value="downloads" />
              <el-option label="最高评分" value="rating" />
            </el-select>
          </el-col>
        </el-row>
      </el-card>
    </section>

    <!-- 4. section: Agent 网格 -->
    <section class="section">
      <h3 class="section-title">📦 Agent 列表 ({{ filteredAgents.length }})</h3>
      <el-row :gutter="16">
        <el-col v-for="agent in filteredAgents" :key="agent.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card shadow="hover" class="agent-card">
            <div class="agent-icon">{{ agent.icon || '🤖' }}</div>
            <h4 class="agent-name">{{ agent.name }}</h4>
            <p class="agent-desc">{{ agent.description }}</p>
            <div class="agent-meta">
              <el-tag size="small">{{ agent.category }}</el-tag>
              <el-rate :model-value="agent.rating" :max="5" disabled show-score :score-template="agent.rating.toFixed(1)" />
            </div>
            <div class="agent-stats">
              <span>⬇️ {{ agent.downloads }}</span>
              <span>⭐ {{ agent.rating.toFixed(1) }}</span>
            </div>
            <div class="agent-actions">
              <el-button size="small" :icon="Download" @click="downloadAgent(agent)" type="primary" plain>下载</el-button>
              <el-button size="small" :icon="View" @click="viewAgent(agent)">详情</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <EmptyState :description="'暂无数据'" />
    </section>

    <!-- 5. dialog: 上传 Agent -->
    <el-dialog v-model="showUpload" title="上传 Agent" width="600px">
      <el-form :model="uploadForm" label-width="100px" size="default">
        <el-form-item label="名称"><el-input v-model="uploadForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="uploadForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" style="width: 100%">
            <el-option label="对话" value="CHAT" />
            <el-option label="工具" value="TOOL" />
            <el-option label="工作流" value="WORKFLOW" />
            <el-option label="多模态" value="MULTIMODAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="JSON 配置"><el-input v-model="uploadForm.config" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="uploadAgent" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { marketplaceApi } from '@/api/marketplace'
import { useUserStore } from '@/store/user'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
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
