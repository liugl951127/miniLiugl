<!--
  @file multimodal/Index.vue - 多模态中心 V7.7 (router shell)
  路由: /multimodal (默认 → /multimodal/overview)

  V7.7 改造 (告别 1511 行单文件 10 模块):
  - /multimodal/overview  - 9 模块总览
  - /multimodal/image     - 图片 (生成 + 理解)
  - /multimodal/audio     - 语音 (TTS + ASR)
  - /multimodal/video     - 视频 (生成 + 理解 + 人脸)
  - /multimodal/document  - 文档 + 音乐
  - /multimodal/local     - (已存在) ONNX 本地智能
-->
<template>
  <PageStandard
    title="🎨 多模态能力中心"
    subtitle="9 个能力模块 · 文字/图像/语音/视频/文档"
  >
    <div class="sub-nav">
      <router-link
        v-for="tab in tabs" :key="tab.path" :to="tab.path"
        class="sub-nav-item" :class="{ active: isActive(tab.path) }"
      >
        <span class="icon">{{ tab.icon }}</span>
        <span class="label">{{ tab.label }}</span>
      </router-link>
    </div>

    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </PageStandard>
</template>

<script setup>
import { useRoute } from 'vue-router'
import PageStandard from '@/components/PageStandard.vue'

const route = useRoute()

const tabs = [
  { path: '/multimodal/overview', label: '概览',     icon: '🏠' },
  { path: '/multimodal/image',    label: '图像',     icon: '🖼️' },
  { path: '/multimodal/audio',    label: '语音',     icon: '🎙️' },
  { path: '/multimodal/video',    label: '视频',     icon: '🎬' },
  { path: '/multimodal/document', label: '文档音乐', icon: '📄' },
  { path: '/multimodal/local',    label: '本地 ONNX', icon: '⚡' }
]

function isActive(path) {
  return route.path === path || (path === '/multimodal/overview' && route.path === '/multimodal')
}
</script>

<style scoped>
.sub-nav {
  display: flex; gap: 4px; background: #f1f5f9;
  border-radius: 10px; padding: 4px; margin-bottom: 16px; width: fit-content;
  flex-wrap: wrap;
}
.sub-nav-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; border-radius: 8px;
  color: #64748b; text-decoration: none; font-size: 0.9em; transition: all 0.2s;
}
.sub-nav-item:hover { background: rgba(255, 255, 255, 0.6); color: #1e293b; }
.sub-nav-item.active {
  background: white; color: #1e293b; font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.sub-nav-item .icon { font-size: 1.1em; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
