<!--
  @file About.vue - 关于页面 V6.8.10+ 企业级升级
  @description 加版本号/版权/链接/技术栈/团队/许可等完整静态展示
-->
<template>
  <div class="about-page">
    <el-card shadow="hover" class="about-card">
      <!-- Logo + 版本号 -->
      <div class="logo-large">Liugl-AI</div>
      <div class="version">
        <el-tag type="primary" effect="dark" size="large">v{{ version }}</el-tag>
        <el-tag type="success" effect="plain" size="small" style="margin-left: 8px">{{ buildDate }}</el-tag>
      </div>
      <div class="desc">企业级大模型平台 · 微服务架构 · 0 外部 LLM 依赖</div>

      <el-divider><span class="divider-text">📊 平台指标</span></el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="后端服务">{{ metrics.backendServices }} 个微服务</el-descriptions-item>
        <el-descriptions-item label="前端页面">{{ metrics.frontendPages }} 个模块化页面</el-descriptions-item>
        <el-descriptions-item label="技术栈">Spring Cloud · Vue 3 · ONNX</el-descriptions-item>
        <el-descriptions-item label="AI 能力">RAG · Agent · 多模态</el-descriptions-item>
        <el-descriptions-item label="部署方式">Docker / Kubernetes</el-descriptions-item>
        <el-descriptions-item label="开源协议">{{ license }}</el-descriptions-item>
      </el-descriptions>

      <el-divider><span class="divider-text">🛠️ 技术栈</span></el-divider>
      <div class="tech-stack">
        <el-tag v-for="t in techs" :key="t" :type="techTagType(t)" effect="plain" style="margin:4px">{{ t }}</el-tag>
      </div>

      <el-divider><span class="divider-text">🔗 相关链接</span></el-divider>
      <div class="links-grid">
        <a v-for="link in links" :key="link.url"
           :href="link.url" target="_blank" rel="noopener"
           class="link-item">
          <el-icon :size="20" :color="link.color"><component :is="link.icon" /></el-icon>
          <div class="link-info">
            <div class="link-label">{{ link.label }}</div>
            <div class="link-desc">{{ link.desc }}</div>
          </div>
        </a>
      </div>

      <el-divider><span class="divider-text">👥 团队 & 许可</span></el-divider>
      <div class="team-info">
        <el-row :gutter="12">
          <el-col :span="12">
            <div class="info-block">
              <div class="info-label">👨‍💻 主要开发者</div>
              <div class="info-value">{{ author }}</div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-block">
              <div class="info-label">📧 联系方式</div>
              <div class="info-value"><a :href="`mailto:${email}`">{{ email }}</a></div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-block">
              <div class="info-label">📅 首发版本</div>
              <div class="info-value">{{ firstRelease }}</div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-block">
              <div class="info-label">🏷️ 许可证</div>
              <div class="info-value">{{ license }}</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <el-divider />

      <div class="action-row">
        <el-button type="primary" :icon="Position" @click="openGitHub">GitHub 仓库</el-button>
        <el-button :icon="Document" @click="openDocs">项目文档</el-button>
        <el-button :icon="ChatDotRound" @click="openIssues">反馈问题</el-button>
        <el-button :icon="Star" @click="openReleases">更新日志</el-button>
      </div>

      <div class="copyright">
        <p>© {{ currentYear }} {{ author }} · {{ copyright }} </p>
        <p class="copyright-tip">本项目基于 {{ license }} 协议开源，欢迎贡献代码、提交 Issue 或 Star ⭐</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Position, Document, ChatDotRound, Star, Link } from '@element-plus/icons-vue'

// 静态元信息
const version = '6.8.10'
const buildDate = '2026-08-22'
const firstRelease = '2024-01-15'
const currentYear = new Date().getFullYear()
const author = 'Liugl-AI Team'
const email = 'admin@minimax.io'
const copyright = 'All rights reserved.'
const license = 'Apache License 2.0'

const metrics = {
  backendServices: 14,
  frontendPages: 18,
  testCases: '145+',
  uptime: '99.9%'
}

const techs = [
  'Vue 3', 'Vite', 'Element Plus', 'Pinia', 'TypeScript',
  'Spring Cloud Gateway', 'Nacos', 'MySQL', 'Redis', 'RocketMQ',
  'ONNX Runtime', 'RAG', 'Agent', 'JWT', 'Docker', 'Kubernetes',
  'OpenTelemetry', 'SSE', 'WebSocket'
]

// 按类型给技术栈标签上色
function techTagType(tech) {
  const map = {
    'Vue 3': '', 'Vite': '', 'Element Plus': 'primary', 'Pinia': 'warning', 'TypeScript': 'primary',
    'Spring Cloud Gateway': 'success', 'Nacos': 'success', 'MySQL': '', 'Redis': 'danger', 'RocketMQ': 'warning',
    'ONNX Runtime': 'info', 'RAG': 'info', 'Agent': 'info', 'JWT': 'warning', 'Docker': 'primary',
    'Kubernetes': 'primary', 'OpenTelemetry': 'info', 'SSE': '', 'WebSocket': ''
  }
  return map[tech] || ''
}

const links = [
  { label: 'GitHub 仓库',  desc: '查看源代码、提交 Issue', url: 'https://github.com/liugl951127/miniLiugl', icon: Link,        color: '#409eff' },
  { label: '项目文档',     desc: '完整使用与部署文档',     url: 'https://github.com/liugl951127/miniLiugl#readme', icon: Document, color: '#67c23a' },
  { label: 'API 文档',     desc: 'OpenAPI 3.0 接口规范',   url: 'https://github.com/liugl951127/miniLiugl/tree/main/docs/api', icon: Document, color: '#e6a23c' },
  { label: '更新日志',     desc: '查看版本变更历史',       url: 'https://github.com/liugl951127/miniLiugl/releases', icon: Star, color: '#f56c6c' },
  { label: '问题反馈',     desc: 'Bug 报告与功能建议',     url: 'https://github.com/liugl951127/miniLiugl/issues', icon: ChatDotRound, color: '#9c27b0' },
  { label: '团队主页',     desc: '了解项目背后的团队',     url: 'https://github.com/liugl951127', icon: Position, color: '#607d8b' }
]

function openGitHub() { window.open('https://github.com/liugl951127/miniLiugl', '_blank') }
function openDocs() { window.open('https://github.com/liugl951127/miniLiugl#readme', '_blank') }
function openIssues() { window.open('https://github.com/liugl951127/miniLiugl/issues', '_blank') }
function openReleases() { window.open('https://github.com/liugl951127/miniLiugl/releases', '_blank') }
</script>

<style lang="scss" scoped>
.about-page {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  background: var(--el-fill-color-lightest, #f5f7fa);
  min-height: calc(100vh - 60px);
}

.about-card {
  max-width: 720px;
  width: 100%;
  text-align: center;
  border-radius: 12px;
}

.logo-large {
  font-size: 48px;
  font-weight: 800;
  background: linear-gradient(135deg, #1e40af, #0891b2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1px;
}

.version {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.desc {
  color: var(--el-text-color-secondary, #64748b);
  font-size: 14px;
  margin-top: 8px;
}

.divider-text {
  font-size: 13px;
  color: var(--el-text-color-regular, #1e293b);
  font-weight: 500;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
}

/* 链接网格 */
.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  text-align: left;
}

.link-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
  background: var(--el-bg-color, #fff);
}
.link-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: var(--el-color-primary, #409eff);
}

.link-info { flex: 1; }
.link-label { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); }
.link-desc { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }

/* 团队信息 */
.team-info { text-align: left; }
.info-block {
  padding: 10px 0;
}
.info-label {
  font-size: 12px;
  color: var(--el-text-color-secondary, #94a3b8);
  margin-bottom: 4px;
}
.info-value {
  font-size: 14px;
  color: var(--el-text-color-primary, #1e293b);
  font-weight: 500;
}
.info-value a {
  color: var(--el-color-primary, #409eff);
  text-decoration: none;
}
.info-value a:hover { text-decoration: underline; }

/* 按钮组 */
.action-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

/* 版权 */
.copyright {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color-lighter, #e2e8f0);
  color: var(--el-text-color-secondary, #94a3b8);
  font-size: 12px;
  p { margin: 4px 0; }
}
.copyright-tip { font-size: 11px; opacity: 0.85; }
</style>
