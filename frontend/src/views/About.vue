<template>
  <div class="about-page">
    <div class="hero minimax-card">
      <h1 class="gradient-text" style="font-size: 36px; margin: 0 0 8px;">
        MiniMax 大模型平台
      </h1>
      <p style="color: var(--minimax-text-secondary); margin: 0 0 24px;">
        一站式企业级大模型应用开发平台 · 对标 Minimax / OpenAI Web / Cursor
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/chat')">
          <el-icon><ChatDotRound /></el-icon> 开始对话
        </el-button>
        <el-button size="large" @click="checkHealth">
          <el-icon><CircleCheck /></el-icon> 服务自检
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :sm="12" :md="8" v-for="card in cards" :key="card.title">
        <div class="minimax-card module-card">
          <div class="module-icon" :style="{ background: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <h3 style="margin: 0 0 4px;">{{ card.title }}</h3>
            <p style="margin: 0; color: var(--minimax-text-secondary); font-size: 13px;">
              {{ card.desc }}
            </p>
            <el-tag :type="card.status === 'done' ? 'success' : 'info'" size="small" style="margin-top: 8px;">
              {{ card.status === 'done' ? '✓ 已完成' : `Day ${card.day}` }}
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="minimax-card mt-16">
      <h3>📅 14 天路线图</h3>
      <el-timeline>
        <el-timeline-item
          v-for="item in roadmap"
          :key="item.day"
          :timestamp="`Day ${item.day}`"
          :type="item.status === 'done' ? 'success' : 'primary'"
          :hollow="item.status !== 'done'"
        >
          <strong>{{ item.title }}</strong>
          <p style="margin: 4px 0 0; color: var(--minimax-text-secondary); font-size: 13px;">
            {{ item.desc }}
          </p>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="minimax-card mt-16 tech-stack">
      <h3>🛠️ 技术栈</h3>
      <div class="tags">
        <el-tag v-for="t in techStack" :key="t" effect="plain" style="margin: 4px;">{{ t }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { systemApi } from '@/api/system'
import { ElMessage } from 'element-plus'

const cards = [
  { day: 1, title: '项目骨架', desc: 'Spring Boot + Vue 3 + Docker 一键启动', icon: 'Box', color: '#5b8def', status: 'done' },
  { day: 2, title: '用户鉴权', desc: 'JWT + RBAC + 登录注册', icon: 'User', color: '#10b981', status: 'todo' },
  { day: 3, title: '会话管理', desc: '多会话 / 历史消息持久化', icon: 'ChatDotRound', color: '#f59e0b', status: 'todo' },
  { day: 5, title: '流式对话', desc: 'SSE 实时打字机效果', icon: 'Lightning', color: '#b66dff', status: 'todo' },
  { day: 6, title: '短/长期记忆', desc: 'Redis + 向量库', icon: 'Memory', color: '#ec4899', status: 'todo' },
  { day: 8, title: '知识库 RAG', desc: '文档上传 / 切片 / 检索增强', icon: 'Files', color: '#06b6d4', status: 'todo' }
]

const roadmap = [
  { day: 1, title: '项目骨架 + Docker', desc: '前后端骨架 + 一键启动', status: 'done' },
  { day: 2, title: '用户体系 + JWT', desc: '登录 / 注册 / Token 刷新' },
  { day: 3, title: '会话模块 CRUD', desc: '创建 / 列表 / 删除会话' },
  { day: 4, title: '模型路由层', desc: 'OpenAI 兼容接口' },
  { day: 5, title: '流式对话 SSE', desc: '核心交互能力' },
  { day: 6, title: '短期记忆', desc: 'Redis 上下文管理' },
  { day: 7, title: '长期记忆', desc: '向量库 + 摘要' },
  { day: 8, title: '知识库 RAG', desc: '文档上传 / 检索 / 增强' },
  { day: 9, title: '工具调用', desc: 'Function Calling' },
  { day: 10, title: '管理后台', desc: '用户 / 模型 / 计费' },
  { day: 11, title: '多模态上传', desc: '图片 / 文件' },
  { day: 12, title: '监控埋点', desc: 'Prometheus + Grafana' },
  { day: 13, title: '自检 Bug 修复', desc: '全链路自测' },
  { day: 14, title: '部署文档', desc: '上线手册交付' }
]

const techStack = [
  'Vue 3', 'Element Plus', 'Vite', 'Pinia', 'Vue Router', 'Axios', 'ECharts',
  'Spring Boot 3', 'Java 17', 'MyBatis Plus', 'Spring Security', 'WebFlux',
  'MySQL 8', 'Redis 7', 'Elasticsearch 8', 'MinIO',
  'Prometheus', 'Grafana', 'Docker', 'Nginx'
]

async function checkHealth() {
  try {
    const res = await systemApi.health()
    ElMessage.success(`${res.data.app} v${res.data.version} · ${res.data.day} · 一切就绪`)
  } catch (e) { /* ignore */ }
}
</script>

<style lang="scss" scoped>
.about-page { max-width: 1200px; margin: 0 auto; }
.hero {
  background: linear-gradient(135deg, #1f2a44 0%, #0b1220 100%);
  color: #fff;
  text-align: center;
  padding: 48px 24px;
  p { color: #aab4cf !important; }
}
.hero-actions { display: flex; gap: 12px; justify-content: center; }
.module-card { display: flex; gap: 16px; align-items: flex-start; height: 100%; }
.module-icon {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.tags { display: flex; flex-wrap: wrap; }
</style>
