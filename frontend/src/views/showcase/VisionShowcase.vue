<!--
  视觉对决 (V4) - 多 Vision 模型同时看图
  支持:
    - 上传/拖拽图片
    - 选 1-3 个 Vision 模型同时识别
    - 横向对比识别结果
  后端: minimax-multimodal 8088 + minimax-model 8083
-->
<!--
  @file views/showcase/VisionShowcase.vue (VisionShowcase 页面)
  @version V3.5.12+ (前端注释补全)
  @description VisionShowcase 页面
-->
<template>
  <div class="vision">
    <header class="header">
      <h1>👁 视觉对决</h1>
      <p class="subtitle">同一张图片, 多个 Vision 模型同时识别, 看谁看得更准</p>
      <div class="badges">
        <span class="badge">Vision LLM</span>
        <span class="badge">并发识别</span>
        <span class="badge">支持多模态</span>
      </div>
    </header>

    <!-- 上传区 -->
    <section class="upload-panel">
      <div
        class="drop-zone"
        :class="{ dragging }"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <div v-if="!imageUrl" class="dz-empty">
          <el-icon :size="48"><UploadFilled /></el-icon>
          <p>拖拽图片到此处, 或点击选择</p>
          <p class="tip">支持 PNG / JPEG / GIF / WebP</p>
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
          <el-button type="primary" @click="$refs.fileInput.click()">选择图片</el-button>
        </div>
        <div v-else class="dz-preview">
          <img :src="imageUrl" alt="preview" />
          <div class="dz-actions">
            <el-button size="small" @click="clearImage">换一张</el-button>
          </div>
        </div>
      </div>

      <div class="vision-prompt">
        <label>❓ 提问</label>
        <el-input
          v-model="visionPrompt"
          type="textarea"
          :rows="2"
          placeholder="例如: 这张图片里有什么? 描述一下场景"
        />
        <div class="quick-prompts">
          <el-tag
            v-for="qp in quickPrompts"
            :key="qp"
            class="qp-tag"
            @click="visionPrompt = qp"
          >{{ qp }}</el-tag>
        </div>
      </div>

      <div class="vision-models">
        <label>👁 选择 Vision 模型 (1-3 个)</label>
        <div class="model-grid">
          <div
            v-for="m in visionModels"
            :key="m.code"
            :class="['model-chip', { active: selectedModels.includes(m.code) }]"
            @click="toggleModel(m.code)"
          >
            <span class="model-name">{{ m.name }}</span>
            <span class="model-provider">{{ m.provider }}</span>
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button
          type="primary"
          size="large"
          :icon="View"
          :loading="analyzing"
          :disabled="!canAnalyze"
          @click="startAnalyze"
        >
          {{ analyzing ? '👁 识别中…' : '🚀 开始识别' }}
        </el-button>
      </div>
    </section>

    <!-- 识别结果 -->
    <section v-if="results.length" class="results-grid">
      <div
        v-for="(r, idx) in results"
        :key="r.modelCode + idx"
        :class="['result-card', `status-${r.status}`, r.fastest && 'is-fastest']"
      >
        <header class="rc-header">
          <h3>{{ r.modelName }}</h3>
          <span class="rc-latency">{{ r.latencyMs }}ms</span>
          <span v-if="r.fastest" class="fastest-tag">⚡ 最快</span>
          <span class="rc-status" :class="`status-${r.status}`">
            {{ r.status === 'ok' ? '✓' : '✗' }}
          </span>
        </header>
        <div class="rc-content">
          <pre v-if="r.status === 'ok'">{{ r.content }}</pre>
          <div v-else class="rc-error">{{ r.error }}</div>
        </div>
        <footer v-if="r.status === 'ok'" class="rc-footer">
          <span>输出 {{ r.completionTokens }} tokens</span>
          <el-rate v-model="r.score" :max="5" size="small" @change="rate(r)" />
        </footer>
      </div>
    </section>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, View } from '@element-plus/icons-vue'
import http from '@/api/http'

const imageFile = ref(null)
const imageUrl = ref('')
const imageBase64 = ref('')
const dragging = ref(false)
const visionPrompt = ref('这张图片里有什么? 请详细描述场景, 物体, 颜色, 文字 (如果有的话) 等')
const analyzing = ref(false)
const results = ref([])

const visionModels = [
  { code: 'gpt-4o-mini', name: 'GPT-4o mini', provider: 'OpenAI' },
  { code: 'gpt-4o', name: 'GPT-4o', provider: 'OpenAI' },
  { code: 'Liugl-AI-VL-01', name: 'Liugl-AI-VL-01', provider: 'Liugl-AI' },
  { code: 'Qwen/Qwen2-VL-72B-Instruct', name: 'Qwen2-VL 72B', provider: 'SiliconFlow' },
  { code: 'qwen-vl-max', name: 'Qwen-VL Max', provider: 'DashScope' },
  { code: 'THUDM/glm-4v-plus', name: 'GLM-4V Plus', provider: 'SiliconFlow' },
]
const selectedModels = ref(['gpt-4o-mini', 'qwen-vl-max'])

const quickPrompts = [
  '这张图片里有什么?',
  '详细描述这张图片的场景和物体',
  '图片里有文字吗? 提取所有文字',
  '这张图片的色彩风格是什么?',
  '图片里的人在做什么? 他们的情绪如何?',
]

const canAnalyze = computed(() =>
  imageBase64.value && visionPrompt.value.trim() && selectedModels.value.length && !analyzing.value
)

function toggleModel(code) {
  const i = selectedModels.value.indexOf(code)
  if (i >= 0) selectedModels.value.splice(i, 1)
  else if (selectedModels.value.length < 3) selectedModels.value.push(code)
  else ElMessage.warning('最多选 3 个')
}

function onFileChange(e) {
  const f = e.target.files[0]
  if (!f) return
  loadFile(f)
}
function onDrop(e) {
  dragging.value = false
  const f = e.dataTransfer.files[0]
  if (f && f.type.startsWith('image/')) loadFile(f)
}
function loadFile(f) {
  imageFile.value = f
  const reader = new FileReader()
  reader.onload = (ev) => {
    imageUrl.value = ev.target.result
    imageBase64.value = ev.target.result  // data:image/png;base64,xxx
  }
  reader.readAsDataURL(f)
}
function clearImage() {
  imageFile.value = null
  imageUrl.value = ''
  imageBase64.value = ''
  results.value = []
}

async function startAnalyze() {
  if (!canAnalyze.value) return
  analyzing.value = true
  results.value = selectedModels.value.map(code => ({
    modelCode: code,
    modelName: visionModels.find(m => m.code === code)?.name || code,
    status: 'pending', content: '', latencyMs: 0, completionTokens: 0, score: 0,
  }))

  // 并发调每个 Vision 模型
  const tasks = selectedModels.value.map(async (code) => {
    const t0 = Date.now()
    try {
      // 通过 /api/v1/multimodal/vision/analyze (V4.1 待实现)
      // 临时调 model 接口: 把图片塞到消息里
      const r = await http.post('/api/v1/models/chat', {
        model: code,
        prompt: visionPrompt.value,
        images: [imageBase64.value],
        maxTokens: 512,
        temperature: 0.3,
      })
      const dt = Date.now() - t0
      const idx = results.value.findIndex(x => x.modelCode === code)
      if (idx >= 0 && r && r.data) {
        results.value[idx] = {
          ...results.value[idx],
          status: 'ok',
          content: r.data.content,
          latencyMs: r.data.latencyMs || dt,
          completionTokens: r.data.completionTokens || 0,
        }
      }
    } catch (e) {
      const idx = results.value.findIndex(x => x.modelCode === code)
      if (idx >= 0) {
        results.value[idx] = {
          ...results.value[idx],
          status: 'error',
          error: e.message,
          latencyMs: Date.now() - t0,
        }
      }
    }
  })
  await Promise.all(tasks)

  // 标记最快
  const okResults = results.value.filter(r => r.status === 'ok')
  if (okResults.length) {
    const min = Math.min(...okResults.map(r => r.latencyMs))
    results.value.forEach(r => {
      if (r.status === 'ok' && r.latencyMs === min) r.fastest = true
    })
  }
  analyzing.value = false
  ElMessage.success(`识别完成! ${okResults.length}/${results.value.length} 成功`)
}

function rate(r) {
  ElMessage.success(`已为 ${r.modelName} 打分 ${r.score} 星`)
}
</script>

<style scoped>
.vision { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge {
  display: inline-block;
  padding: 2px 10px;
  margin-right: 8px;
  background: linear-gradient(135deg, #ec4899, #db2777);
  color: #fff;
  border-radius: 12px;
  font-size: 12px;
}

.upload-panel {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin: 24px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,.1);
}
.drop-zone {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 24px;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;
  margin-bottom: 20px;
}
.drop-zone.dragging { border-color: #ec4899; background: #fdf2f8; }
.dz-empty { text-align: center; color: #64748b; }
.dz-empty p { margin: 8px 0; }
.dz-empty .tip { font-size: 12px; color: #94a3b8; }
.dz-preview { width: 100%; }
.dz-preview img { max-width: 100%; max-height: 400px; border-radius: 8px; }
.dz-actions { text-align: center; margin-top: 12px; }

.vision-prompt, .vision-models { margin-bottom: 20px; }
label { display: block; font-weight: 600; margin-bottom: 8px; color: #334155; }
.quick-prompts { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.qp-tag { cursor: pointer; }

.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.model-chip {
  padding: 8px 12px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
}
.model-chip.active { border-color: #ec4899; background: linear-gradient(135deg, #fdf2f8, #fce7f3); }
.model-name { display: block; font-weight: 600; font-size: 13px; }
.model-provider { display: block; font-size: 11px; color: #64748b; }

.actions { text-align: center; }

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 16px;
  margin-top: 24px;
}
.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  border: 2px solid #e2e8f0;
}
.result-card.is-fastest { border-color: #fbbf24; }
.rc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.rc-header h3 { margin: 0; font-size: 16px; flex: 1; }
.rc-latency { background: #f1f5f9; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.fastest-tag { color: #f59e0b; font-size: 12px; font-weight: 600; }
.rc-status { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-ok { background: #dcfce7; color: #15803d; }
.status-error { background: #fee2e2; color: #b91c1c; }

.rc-content {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  min-height: 120px;
  max-height: 360px;
  overflow-y: auto;
  margin-bottom: 12px;
}
.rc-content pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-family: inherit; font-size: 13px; line-height: 1.6; }
.rc-error { color: #b91c1c; }
.rc-footer { display: flex; align-items: center; justify-content: space-between; padding-top: 8px; border-top: 1px solid #e2e8f0; font-size: 13px; color: #64748b; }
</style>
