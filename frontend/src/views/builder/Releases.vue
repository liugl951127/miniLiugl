<!--
  @file builder/Releases.vue - 发布管理 (V1.0)
  路由: /builder/releases
  功能: 版本时间线 / 差异对比 / 一键回滚 / 标签
-->
<template>
  <div class="releases-page">
    <el-row :gutter="16">
      <!-- 左侧: 版本时间线 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>📦 发布历史</span>
              <el-button size="small" type="primary" :icon="Plus">新建发布</el-button>
            </div>
          </template>
          <div class="release-timeline">
            <div v-for="r in releases" :key="r.id"
              class="release-item" :class="{ active: current === r.id }"
              @click="current = r.id">
              <div class="ri-marker" :class="r.tag">
                <span v-if="r.tag === 'current'">★</span>
                <span v-else>{{ r.version.split('.')[0] }}</span>
              </div>
              <div class="ri-content">
                <div class="ri-head">
                  <span class="ri-version">v{{ r.version }}</span>
                  <el-tag v-if="r.tag === 'current'" type="success" size="small">当前</el-tag>
                  <el-tag v-else-if="r.tag === 'rollback'" type="warning" size="small">回滚</el-tag>
                  <el-tag v-else-if="r.tag === 'failed'" type="danger" size="small">失败</el-tag>
                </div>
                <div class="ri-title">{{ r.title }}</div>
                <div class="ri-meta">
                  <span>👤 {{ r.author }}</span>
                  <span>🕐 {{ r.time }}</span>
                </div>
                <div class="ri-changes">
                  <el-tag v-for="c in r.changes" :key="c" size="small" :type="changeColor(c)" effect="plain">
                    {{ c }}
                  </el-tag>
                </div>
                <div class="ri-actions">
                  <el-button size="small" link :icon="View" @click.stop="viewDiff(r)">差异</el-button>
                  <el-button size="small" link :icon="RefreshLeft" @click.stop="rollback(r)" :disabled="r.tag === 'current'">回滚</el-button>
                  <el-button size="small" link :icon="Download" @click.stop="download(r)">下载</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧: 详情/差异 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>📊 版本详情 · v{{ currentRelease.version }}</span>
              <div>
                <el-button-group size="small">
                  <el-button :type="diffMode === 'visual' ? 'primary' : ''" @click="diffMode = 'visual'">可视化</el-button>
                  <el-button :type="diffMode === 'yaml' ? 'primary' : ''" @click="diffMode = 'yaml'">YAML</el-button>
                </el-button-group>
              </div>
            </div>
          </template>

          <!-- 概览 -->
          <div class="detail-grid">
            <div class="dg-item">
              <div class="dg-label">版本</div>
              <div class="dg-value">v{{ currentRelease.version }}</div>
            </div>
            <div class="dg-item">
              <div class="dg-label">作者</div>
              <div class="dg-value">{{ currentRelease.author }}</div>
            </div>
            <div class="dg-item">
              <div class="dg-label">发布时间</div>
              <div class="dg-value">{{ currentRelease.time }}</div>
            </div>
            <div class="dg-item">
              <div class="dg-label">智能体数</div>
              <div class="dg-value">{{ currentRelease.agentCount }}</div>
            </div>
            <div class="dg-item">
              <div class="dg-label">副本数</div>
              <div class="dg-value">{{ currentRelease.replicas }}</div>
            </div>
            <div class="dg-item">
              <div class="dg-label">镜像 Tag</div>
              <div class="dg-value mono">v{{ currentRelease.version }}</div>
            </div>
          </div>

          <el-divider />

          <!-- 变更说明 -->
          <h4>📝 变更说明</h4>
          <p class="changelog">{{ currentRelease.changelog }}</p>

          <h4 style="margin-top:16px">🔧 变更项</h4>
          <div class="changes-list">
            <div v-for="(c, i) in currentRelease.changeList" :key="i" class="change-row" :class="c.type">
              <span class="cr-tag">{{ c.type === 'add' ? '+' : c.type === 'remove' ? '−' : '~' }}</span>
              <span class="cr-path">{{ c.path }}</span>
              <span class="cr-desc">{{ c.desc }}</span>
            </div>
          </div>

          <el-divider />

          <!-- 可视化差异 -->
          <h4>🎨 智能体变更可视化</h4>
          <div class="agent-diff">
            <div v-for="a in currentRelease.agentDiff" :key="a.name" class="ad-item" :class="a.change">
              <div class="ad-emoji">{{ a.emoji }}</div>
              <div class="ad-name">{{ a.name }}</div>
              <el-tag v-if="a.change === 'added'" type="success" size="small">新增</el-tag>
              <el-tag v-else-if="a.change === 'modified'" type="warning" size="small">修改</el-tag>
              <el-tag v-else-if="a.change === 'removed'" type="danger" size="small">删除</el-tag>
            </div>
          </div>
        </el-card>

        <!-- 部署历史 -->
        <el-card shadow="never" style="margin-top:12px">
          <template #header><span>📈 部署统计</span></template>
          <el-row :gutter="12">
            <el-col :span="6"><div class="stat-box"><div class="sb-num">12</div><div class="sb-label">总发布</div></div></el-col>
            <el-col :span="6"><div class="stat-box"><div class="sb-num success">10</div><div class="sb-label">成功</div></div></el-col>
            <el-col :span="6"><div class="stat-box"><div class="sb-num warn">1</div><div class="sb-label">回滚</div></div></el-col>
            <el-col :span="6"><div class="stat-box"><div class="sb-num danger">1</div><div class="sb-label">失败</div></div></el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部 -->
    <div class="action-bar">
      <el-button size="large" round :icon="RefreshLeft" @click="$router.push('/builder/monitor')">返回监控</el-button>
      <el-button size="large" round type="primary" :icon="Promotion" @click="newRelease">
        创建新发布 →
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, RefreshLeft, Download, Promotion } from '@element-plus/icons-vue'

const router = useRouter()
const current = ref('v1.0.0')
const diffMode = ref('visual')

const releases = ref([
  { id: 'v1.0.0', version: '1.0.0', title: '首次发布 · 智能客服系统', author: 'admin@minimax.io', time: '2026-08-24 13:30', tag: 'current', changes: ['feat', 'docs'], agentCount: 3, replicas: 2,
    changelog: '正式发布 Agent Forge v1.0.0, 包含 3 个智能体 (小课/小助/小审), 部署到生产 K8s 集群。',
    changeList: [
      { type: 'add', path: 'agents/xiaoke', desc: '新增课程顾问智能体' },
      { type: 'add', path: 'agents/xiaozhu', desc: '新增退费专员智能体' },
      { type: 'add', path: 'agents/xiaoshen', desc: '新增质检员智能体' },
      { type: 'add', path: 'tools/search', desc: '集成课程搜索工具' }
    ],
    agentDiff: [
      { name: '小课', emoji: '📚', change: 'added' },
      { name: '小助', emoji: '💰', change: 'added' },
      { name: '小审', emoji: '🔍', change: 'added' }
    ]
  },
  { id: 'v0.9.0', version: '0.9.0', title: 'Beta 版本 · 内部测试', author: 'dev@minimax.io', time: '2026-08-20 18:45', tag: 'rollback', changes: ['feat'], agentCount: 2, replicas: 1,
    changelog: 'Beta 内部测试版本, 只包含 2 个智能体, 部署到 staging。',
    changeList: [
      { type: 'add', path: 'agents/xiaoke', desc: '课程顾问 Beta 版' },
      { type: 'add', path: 'agents/xiaozhu', desc: '退费专员 Beta 版' }
    ],
    agentDiff: [
      { name: '小课', emoji: '📚', change: 'added' },
      { name: '小助', emoji: '💰', change: 'added' }
    ]
  },
  { id: 'v0.8.0', version: '0.8.0', title: 'Alpha 探索 · 失败', author: 'dev@minimax.io', time: '2026-08-15 10:20', tag: 'failed', changes: ['feat', 'fix'], agentCount: 1, replicas: 1,
    changelog: 'Alpha 探索, 部署失败, 镜像构建错误已修复。',
    changeList: [
      { type: 'add', path: 'agents/xiaoke', desc: '首个智能体原型' }
    ],
    agentDiff: [
      { name: '小课', emoji: '📚', change: 'added' }
    ]
  }
])

const currentRelease = computed(() => releases.value.find(r => r.id === current.value))

function changeColor(c) {
  if (c === 'feat') return 'success'
  if (c === 'fix') return 'warning'
  if (c === 'docs') return 'info'
  return ''
}

function viewDiff(r) {
  current.value = r.id
  ElMessage.info(`显示 v${r.version} 详情`)
}

async function rollback(r) {
  await ElMessageBox.confirm(
    `确定回滚到 v${r.version}? 这会替换当前版本 (v1.0.0) 的所有 Pod`,
    '回滚确认',
    { type: 'warning', confirmButtonText: '确认回滚' }
  )
  ElMessage.success(`已发起回滚到 v${r.version}`)
}

function download(r) {
  ElMessage.success(`已下载 v${r.version} 配置文件`)
}

function newRelease() {
  ElMessageBox.prompt('请输入新版本号 (语义化版本)', '创建新发布', {
    inputValue: '1.1.0',
    inputPattern: /^\d+\.\d+\.\d+$/,
    inputErrorMessage: '格式错误, 例如: 1.1.0'
  }).then(({ value }) => {
    ElMessage.success(`v${value} 已创建`)
  }).catch(() => {})
}
</script>

<style scoped>
.releases-page { max-width: 1400px; margin: 0 auto; }

.release-timeline { display: flex; flex-direction: column; gap: 12px; }
.release-item {
  display: flex; gap: 12px; padding: 14px; background: #fafbfc;
  border-radius: 10px; border: 2px solid transparent; cursor: pointer;
  transition: all 0.2s; position: relative;
}
.release-item::before {
  content: ''; position: absolute; left: 23px; top: -12px; bottom: -12px;
  width: 2px; background: #e2e8f0;
}
.release-item:first-child::before { top: 50%; }
.release-item:last-child::before { bottom: 50%; }
.release-item:hover { background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.release-item.active { border-color: #6366f1; background: linear-gradient(180deg, #f5f7ff 0%, #fafbff 100%); }

.ri-marker {
  width: 28px; height: 28px; border-radius: 50%;
  background: #6366f1; color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0; z-index: 1;
}
.ri-marker.rollback { background: #f59e0b; }
.ri-marker.failed { background: #ef4444; }

.ri-content { flex: 1; }
.ri-head { display: flex; align-items: center; gap: 6px; }
.ri-version { font-weight: 700; color: #1e293b; }
.ri-title { font-size: 13px; color: #1e293b; margin: 4px 0; }
.ri-meta { display: flex; gap: 12px; font-size: 11px; color: #64748b; }
.ri-changes { display: flex; gap: 4px; margin: 6px 0; }
.ri-actions { display: flex; gap: 4px; margin-top: 6px; }

.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.dg-item { padding: 10px 14px; background: #fafbfc; border-radius: 8px; }
.dg-label { font-size: 11px; color: #64748b; }
.dg-value { font-size: 14px; font-weight: 600; color: #1e293b; margin-top: 2px; }
.dg-value.mono { font-family: monospace; }

.changelog {
  background: #f8fafc; padding: 12px; border-radius: 8px;
  border-left: 3px solid #6366f1; color: #334155; line-height: 1.6; font-size: 13px;
  margin: 0;
}

.changes-list { display: flex; flex-direction: column; gap: 6px; }
.change-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-radius: 6px; font-size: 13px;
  background: #fafbfc;
}
.change-row.add { background: #f0fdf4; }
.change-row.remove { background: #fef2f2; }
.change-row.modify { background: #fffbeb; }
.cr-tag {
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; color: white;
}
.change-row.add .cr-tag { background: #10b981; }
.change-row.remove .cr-tag { background: #ef4444; }
.change-row.modify .cr-tag { background: #f59e0b; }
.cr-path { font-family: monospace; color: #475569; }
.cr-desc { color: #1e293b; }

.agent-diff { display: flex; gap: 8px; flex-wrap: wrap; }
.ad-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #fafbfc; border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.ad-emoji { font-size: 20px; }
.ad-name { font-weight: 500; }

.stat-box {
  text-align: center; padding: 16px; background: #fafbfc;
  border-radius: 8px; border: 1px solid #f1f5f9;
}
.sb-num { font-size: 24px; font-weight: 700; color: #1e293b; }
.sb-num.success { color: #10b981; }
.sb-num.warn { color: #f59e0b; }
.sb-num.danger { color: #ef4444; }
.sb-label { font-size: 12px; color: #64748b; margin-top: 4px; }

.action-bar { display: flex; justify-content: space-between; margin-top: 16px; }
</style>
