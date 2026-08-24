<!--
  @file Dashboard.vue - 工作台首页 V8.0
  路由: /dashboard
  功能: 快速访问各模块 · 今日统计 · 推荐操作
-->
<template>
  <div class="dashboard-page">
    <!-- 欢迎语 -->
    <div class="welcome-card">
      <div class="welcome-text">
        <h1>👋 {{ greeting }}，{{ userName }}</h1>
        <p>今天是 {{ today }}，{{ dayTip }}</p>
      </div>
      <div class="welcome-emoji">✨</div>
    </div>

    <!-- 核心指标 -->
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="$router.push('/chat')">
          <div class="stat-icon stat-icon-blue"><el-icon><ChatDotRound /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.sessions || 0 }}</div>
            <div class="stat-label">我的会话</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="$router.push('/knowledge')">
          <div class="stat-icon stat-icon-green"><el-icon><Files /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.docs || 0 }}</div>
            <div class="stat-label">知识文档</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="$router.push('/analytics')">
          <div class="stat-icon stat-icon-amber"><el-icon><DataAnalysis /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.calls || 0 }}</div>
            <div class="stat-label">今日调用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="$router.push('/monitor')">
          <div class="stat-icon stat-icon-red"><el-icon><Monitor /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.health || '🟢 正常' }}</div>
            <div class="stat-label">服务健康</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速入口 -->
    <el-card style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>⚡ 快速入口</span>
          <span style="font-size:12px;color:var(--el-text-color-secondary)">{{ shortcuts.length }} 个常用功能</span>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col v-for="s in shortcuts" :key="s.path" :xs="8" :sm="6" :md="4">
          <div class="shortcut-card" @click="$router.push(s.path)">
            <div class="sc-icon" :style="{ background: s.bg, color: s.fg }">{{ s.icon }}</div>
            <div class="sc-name">{{ s.name }}</div>
            <div class="sc-desc">{{ s.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 公告 + 最近活动 -->
    <el-row :gutter="12">
      <el-col :span="14">
        <el-card>
          <template #header><span>📰 平台公告</span></template>
          <div v-for="(n, i) in notices" :key="i" class="notice-item">
            <el-tag size="small" :type="n.type" effect="plain" style="margin-right:8px">{{ n.tag }}</el-tag>
            <span class="notice-text">{{ n.text }}</span>
            <span class="notice-date">{{ n.date }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><span>📈 最近活动</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="(a, i) in activities" :key="i"
              :timestamp="a.time" :type="a.type"
            >
              {{ a.text }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ChatDotRound, Files, DataAnalysis, Monitor } from '@element-plus/icons-vue'

const userStore = useUserStore()
const userName = computed(() => userStore.profile?.nickname || userStore.profile?.username || '访客')
const hour = new Date().getHours()
const greeting = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
const dayTip = hour < 12 ? '一杯咖啡, 开始新的一天 🚀' : hour < 18 ? '加油, 下午继续保持 💪' : '复盘一下, 准备下班 🌙'

const stats = ref({ sessions: 0, docs: 0, calls: 0, health: '🟢' })

const shortcuts = [
  { path: '/chat',           name: 'AI 对话',     icon: '💬', desc: '立即开始对话',  bg: '#dbeafe', fg: '#2563eb' },
  { path: '/agent',          name: 'Agent 画布',   icon: '🤖', desc: '拖拽编排 Agent', bg: '#fae8ff', fg: '#a21caf' },
  { path: '/multimodal',     name: '多模态',       icon: '🎨', desc: '图片/语音/视频', bg: '#fef3c7', fg: '#d97706' },
  { path: '/multimodal/local', name: '本地 AI',    icon: '⚡', desc: 'ONNX 本地推理',  bg: '#d1fae5', fg: '#059669' },
  { path: '/knowledge',      name: '知识库',       icon: '📚', desc: 'RAG 检索',       bg: '#e0e7ff', fg: '#4338ca' },
  { path: '/analytics',      name: '数据分析',     icon: '📊', desc: '调用统计',       bg: '#ffe4e6', fg: '#be123c' },
  { path: '/model',          name: '模型管理',     icon: '🧠', desc: '训练/接入模型',  bg: '#cffafe', fg: '#0e7490' },
  { path: '/rule',           name: '规则助手',     icon: '✨', desc: 'NL 规则生成',    bg: '#fce7f3', fg: '#be185d' }
]

const notices = [
  { tag: '新功能', type: 'success', text: '本地 ONNX 智能升级上线 (V7.4) - 支持 Whisper / BGE / Qwen2.5', date: '2026-08-23' },
  { tag: '优化', type: 'primary', text: 'NL 规则助手 V7.5 - LLM 驱动 6 大业务模板, 视觉化条件/动作',   date: '2026-08-22' },
  { tag: '修复', type: 'warning', text: '401/403 鉴权统一修复, token 失效自动跳转登录',                date: '2026-08-20' },
  { tag: '文档', type: 'info',    text: '29 个测试案例 + HTML 测试报告已发布',                          date: '2026-08-18' }
]

const activities = ref([
  { time: '刚刚',  type: 'primary',  text: '欢迎使用 Liugl-AI 平台' },
  { time: '1 天前', type: 'success', text: 'ONNX 多模态智能升级完成' },
  { time: '3 天前', type: 'warning', text: 'NL 规则助手 V7.5 上线' }
])

onMounted(() => {
  // 异步加载真实数据 (best-effort, 不阻塞)
  Promise.allSettled([
    import('@/api/session').then(m => m.listSessions().catch(() => ({}))),
    import('@/api/kg').then(m => m.kgApi ? m.kgApi.list().catch(() => ({})) : {}),
    import('@/api/analytics').then(m => m.getStatsOverview ? m.getStatsOverview().catch(() => ({})) : {})
  ]).then(([s, d, c]) => {
    stats.value.sessions = ((s.value && s.value.data) || []).length || 0
    stats.value.docs = ((d.value && d.value.data) || {}).entities || 0
    stats.value.calls = ((c.value && c.value.data && c.value.data.data) || {}).todayCalls || 0
  })
})
</script>

<style scoped>
.dashboard-page { padding: 0; }

.welcome-card {
  display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
  color: white; border-radius: 16px; padding: 28px 32px;
  margin-bottom: 16px; box-shadow: 0 4px 20px rgba(99, 102, 241, 0.2);
}
.welcome-text h1 { margin: 0 0 6px; font-size: 22px; }
.welcome-text p { margin: 0; opacity: 0.9; font-size: 14px; }
.welcome-emoji { font-size: 48px; opacity: 0.9; }

.stat-card {
  display: flex; align-items: center; gap: 12px;
  cursor: pointer; transition: all 0.2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--liugl-shadow); }
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
}
.stat-icon-blue  { background: var(--stat-blue-bg);   color: var(--stat-blue-fg);   }
.stat-icon-green { background: var(--stat-green-bg);  color: var(--stat-green-fg);  }
.stat-icon-amber { background: var(--stat-amber-bg);  color: var(--stat-amber-fg);  }
.stat-icon-red   { background: var(--stat-red-bg);    color: var(--stat-red-fg);    }
.stat-value { font-size: 22px; font-weight: 700; color: var(--liugl-text); }
.stat-label { font-size: 12px; color: var(--liugl-text-secondary); margin-top: 2px; }

.shortcut-card {
  display: flex; flex-direction: column; align-items: center;
  padding: 16px 8px; border-radius: 12px; cursor: pointer;
  transition: all 0.2s; text-align: center;
  border: 1px solid transparent;
}
.shortcut-card:hover {
  background: var(--liugl-bg-elevated); border-color: var(--liugl-border); transform: translateY(-2px);
}
.sc-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; margin-bottom: 8px;
}
.sc-name { font-size: 13px; font-weight: 600; color: var(--liugl-text); }
.sc-desc { font-size: 11px; color: var(--liugl-text-secondary); margin-top: 2px; }

.notice-item {
  display: flex; align-items: center; padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}
.notice-item:last-child { border-bottom: none; }
.notice-text { flex: 1; font-size: 13px; color: var(--liugl-text); }
.notice-date { font-size: 11px; color: var(--liugl-text-secondary); }
</style>
