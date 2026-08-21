<!-- @file About.vue - 关于页面 V6.8 -->
<template>
  <div class="about-page">
    <el-card shadow="hover" class="about-card">
      <div class="logo-large">Liugl-AI</div>
      <div class="version">V6.8.10</div>
      <div class="desc">企业级大模型平台 · 微服务架构</div>
      <el-divider />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="后端服务">14 个微服务</el-descriptions-item>
        <el-descriptions-item label="前端页面">18 个模块化页面</el-descriptions-item>
        <el-descriptions-item label="技术栈">Spring Cloud · Vue 3 · ONNX</el-descriptions-item>
        <el-descriptions-item label="AI 能力">RAG · Agent · 多模态</el-descriptions-item>
        <el-descriptions-item label="部署方式">Docker / K8s</el-descriptions-item>
        <el-descriptions-item label="开源协议">Apache 2.0</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <div class="tech-stack">
        <el-tag v-for="t in techs" :key="t" style="margin:4px">{{ t }}</el-tag>
      </div>
      <div style="margin-top:24px;text-align:center">
        <el-button type="primary" @click="openGitHub">GitHub</el-button>
        <el-button @click="openDocs">文档</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
const techs = ['Vue 3', 'Vite', 'Element Plus', 'Pinia', 'Spring Cloud Gateway', 'Nacos', 'MySQL', 'Redis', 'ONNX Runtime', 'RAG', 'Agent', 'JWT', 'Docker']
function openGitHub() { window.open('https://github.com/liugl951127/miniLiugl', '_blank') }
function openDocs() { window.open('https://github.com/liugl951127/miniLiugl#readme', '_blank') }
</script>

<style lang="scss" scoped>
.about-page { display: flex; justify-content: center; padding: 40px 0; }
.about-card { max-width: 640px; width: 100%; text-align: center; }
.logo-large { font-size: 48px; font-weight: 800; background: linear-gradient(135deg, #1e40af, #0891b2); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.version { font-size: 16px; color: var(--el-text-color-regular); margin-top: 8px; }
.desc { color: var(--el-text-color-secondary); font-size: 14px; margin-top: 4px; }
.tech-stack { display: flex; flex-wrap: wrap; justify-content: center; gap: 4px; }
</style>
