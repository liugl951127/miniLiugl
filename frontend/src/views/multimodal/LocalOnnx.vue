<!--
  @file multimodal/LocalOnnx.vue - 本地 ONNX 多模态 V7.6 (router shell)
  路由: /multimodal/local (默认 → /multimodal/local/image)

  V7.6 改造 (告别 16 tab):
  - /local/image   - 图片 (分类 + 检测 + 相似度)
  - /local/audio   - 语音 (转写 + VAD)
  - /local/video   - 视频 (分析)
  - /local/llm     - 语言 (Embedding + Qwen2.5)
-->
<template>
  <PageStandard
    title="🎨 本地多模态智能"
    subtitle="基于 ONNX Runtime · ResNet50/YOLO/Whisper/BGE/Qwen2.5"
  >
    <template #actions>
      <el-tag :type="onlineCount === totalModels ? 'success' : 'info'" size="small">
        {{ onlineCount }} / {{ totalModels }} 模型就绪
      </el-tag>
    </template>

    <el-card shadow="never" class="models-card">
      <template #header>
        <div class="card-header" @click="modelsExpanded = !modelsExpanded">
          <span>📊 模型状态</span>
          <el-icon>
            <component :is="modelsExpanded ? 'ArrowDown' : 'ArrowRight'" />
          </el-icon>
        </div>
      </template>
      <el-collapse-transition>
        <div v-show="modelsExpanded" class="models-grid">
          <div v-for="m in models" :key="m.key" class="model-chip" :class="{ ready: m.ready }">
            <span class="m-icon">{{ m.icon }}</span>
            <div>
              <div class="m-name">{{ m.name }}</div>
              <el-tag v-if="m.ready" type="success" size="small">就绪</el-tag>
              <el-tag v-else type="warning" size="small">未就绪</el-tag>
            </div>
          </div>
        </div>
      </el-collapse-transition>
    </el-card>

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
        <component :is="Component" :models="models" @status-loaded="onlineCount = $event" />
      </transition>
    </router-view>
  </PageStandard>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import PageStandard from '@/components/PageStandard.vue'
import { multimodalApi } from '@/api/multimodal'

const route = useRoute()
const models = ref([
  { key: 'resnet50', name: 'ResNet50',     icon: '🏷️', ready: false, path: '' },
  { key: 'clip',     name: 'CLIP',         icon: '🔗', ready: false, path: '' },
  { key: 'yolo',     name: 'YOLOv8',       icon: '🎯', ready: false, path: '' },
  { key: 'whisper',  name: 'Whisper STT',  icon: '🎙️', ready: false, path: '' },
  { key: 'vad',      name: 'Silero VAD',   icon: '🔊', ready: false, path: '' },
  { key: 'video',    name: '视频分析',     icon: '🎬', ready: false, path: '' },
  { key: 'bge',      name: 'BGE Embedding',icon: '📐', ready: false, path: '' },
  { key: 'qwen',     name: 'Qwen2.5',      icon: '💬', ready: false, path: '' }
])
const modelsExpanded = ref(true)
const onlineCount = ref(0)
const totalModels = computed(() => models.value.length)

const tabs = [
  { path: '/multimodal/local/image', label: '图片', icon: '🖼️' },
  { path: '/multimodal/local/audio', label: '语音', icon: '🎙️' },
  { path: '/multimodal/local/video', label: '视频', icon: '🎬' },
  { path: '/multimodal/local/llm',   label: '语言', icon: '💬' }
]

function isActive(path) {
  return route.path === path || (path === '/multimodal/local/image' && route.path === '/multimodal/local')
}

async function loadStatus() {
  try {
    const res = await multimodalApi.status()
    if (res.code === 0) {
      const d = res.data
      const map = {
        resnet50: d.resnet50, clip: d.clip, yolo: d.yolo,
        whisper: d.whisper, vad: d.vad, video: d.video,
        bge: d.bge, qwen: d.qwen
      }
      for (const m of models.value) {
        const info = map[m.key]
        if (info) {
          m.ready = info.ready ?? info.available ?? false
          m.path = info.path || m.path
        }
      }
      onlineCount.value = models.value.filter(m => m.ready).length
    }
  } catch (e) { console.error('loadStatus', e) }
}

onMounted(loadStatus)
</script>

<style scoped>
.models-card { margin-bottom: 16px; border-radius: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.models-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}
.model-chip {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px;
  background: #f8fafc; border: 1px solid #e2e8f0;
}
.model-chip.ready { background: #f0fdf4; border-color: #bbf7d0; }
.m-icon { font-size: 20px; }
.m-name { font-weight: 600; color: #1e293b; font-size: 0.9em; }
.sub-nav {
  display: flex; gap: 4px; background: #f1f5f9;
  border-radius: 10px; padding: 4px; margin-bottom: 16px; width: fit-content;
}
.sub-nav-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 8px;
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
