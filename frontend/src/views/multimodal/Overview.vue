<!--
  @file multimodal/Overview.vue - 多模态模块总览 (V7.7)
  路由: /multimodal/overview
-->
<template>
  <div class="overview-page">
    <div class="module-grid">
      <el-card v-for="m in modules" :key="m.key" shadow="hover" class="mod-card" @click="goTo(m)">
        <div class="m-icon">{{ m.icon }}</div>
        <div class="m-name">{{ m.name }}</div>
        <div class="m-desc">{{ m.desc }}</div>
        <el-tag v-if="m.beta" size="small" type="warning" style="margin-top: 6px">Beta</el-tag>
        <el-tag v-else size="small" type="success" style="margin-top: 6px">就绪</el-tag>
      </el-card>
    </div>

    <el-card shadow="never" class="tip-card">
      <template #header><span>💡 快速上手</span></template>
      <ol class="tips">
        <li>选一个能力模块 (比如「图片生成」)</li>
        <li>选模型 (自研/云端)</li>
        <li>填输入, 点「开始」</li>
        <li>需要本地推理? 切到「本地 ONNX」标签</li>
      </ol>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const modules = [
  { key: 'image-gen', name: '图片生成', icon: '🎨', desc: '文生图 / 图生图', route: '/multimodal/image', beta: false },
  { key: 'image-understand', name: '图片理解', icon: '🔍', desc: '上传图片 AI 分析', route: '/multimodal/image', beta: false },
  { key: 'tts', name: '语音合成', icon: '🔊', desc: '文本转语音 TTS', route: '/multimodal/audio', beta: false },
  { key: 'asr', name: '语音识别', icon: '🎤', desc: '音频转文本 ASR', route: '/multimodal/audio', beta: false },
  { key: 'face', name: '人脸识别', icon: '👤', desc: '摄像头拍照 · 人脸分析', route: '/multimodal/video', beta: false },
  { key: 'video-understand', name: '视频理解', icon: '🎥', desc: '上传视频 · AI 分析', route: '/multimodal/video', beta: false },
  { key: 'video-gen', name: '视频生成', icon: '🎬', desc: '文本生成视频', route: '/multimodal/video', beta: true },
  { key: 'music', name: '音乐生成', icon: '🎵', desc: '文本生成音乐', route: '/multimodal/document', beta: true },
  { key: 'doc', name: '文档理解', icon: '📄', desc: 'PDF/图片问答', route: '/multimodal/document', beta: true }
]

function goTo(m) {
  router.push(m.route)
}
</script>

<style scoped>
.overview-page { display: flex; flex-direction: column; gap: 16px; }
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}
.mod-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 12px;
}
.mod-card:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.08); }
.m-icon { font-size: 32px; margin-bottom: 6px; }
.m-name { font-weight: 600; color: #1e293b; }
.m-desc { font-size: 11px; color: #64748b; margin: 4px 0; }
.tip-card { border-radius: 12px; }
.tips { margin: 0; padding-left: 20px; color: #475569; line-height: 1.8; }
</style>
