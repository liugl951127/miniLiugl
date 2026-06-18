<!--
  文生图 ShowCase (V4.1)
  - 5 个真实模型 (FLUX / SDXL / Kolors / 通义万相 / DALL-E)
  - mock 模式: 用 prompt 哈希生成 SVG 渐变占位图
  - 真实模式: SILICONFLOW_API_KEY 调用
-->
<template>
  <div class="imagegen">
    <header class="header">
      <h1>🎨 文生图 (Text-to-Image)</h1>
      <p class="subtitle">输入文字描述, 多个模型同时生成图片, 横向对比效果</p>
      <div class="badges">
        <span class="badge">FLUX.1 Schnell</span>
        <span class="badge">SDXL</span>
        <span class="badge">Kolors</span>
        <span class="badge">通义万相</span>
        <span class="badge">DALL-E 3</span>
      </div>
    </header>

    <section class="input-panel">
      <el-input v-model="prompt" type="textarea" :rows="3"
                placeholder="描述要生成的图片, 例如: 一只可爱的橘猫坐在窗台上看日落, 油画风格" />
      <div class="quick-prompts">
        <span class="qp-label">快速:</span>
        <el-tag v-for="qp in quickPrompts" :key="qp.t" class="qp-tag" @click="prompt = qp.t">
          {{ qp.t }}
        </el-tag>
      </div>

      <div class="settings">
        <div class="form-row">
          <div class="form-item half">
            <label>数量: {{ n }}</label>
            <el-slider v-model="n" :min="1" :max="4" :step="1" />
          </div>
          <div class="form-item half">
            <label>尺寸</label>
            <el-select v-model="size" style="width: 100%">
              <el-option label="1024×1024 (方形)" value="1024x1024" />
              <el-option label="1024×768 (横屏)" value="1024x768" />
              <el-option label="768×1024 (竖屏)" value="768x1024" />
              <el-option label="512×512 (快速)" value="512x512" />
            </el-select>
          </div>
        </div>
        <div class="form-item">
          <label>选择模型 (1-4 个)</label>
          <div class="model-grid">
            <div v-for="m in availableModels" :key="m.code"
                 :class="['model-chip', { active: selectedModels.includes(m.code) }]"
                 @click="toggleModel(m.code)">
              <span class="model-name">{{ m.displayName }}</span>
              <span class="model-provider">{{ m.provider }}</span>
            </div>
          </div>
        </div>
        <el-button type="primary" size="large" :icon="Picture" :loading="generating"
                   :disabled="!canGenerate" @click="generate" block>
          {{ generating ? '🎨 生成中...' : '🚀 批量生成' }}
        </el-button>
      </div>
    </section>

    <!-- 结果画廊 -->
    <section v-if="results.length" class="gallery">
      <h2>🖼 生成结果 ({{ results.length }} 张)</h2>
      <div class="grid">
        <div v-for="(r, idx) in results" :key="idx" class="card">
          <div class="card-img">
            <img :src="r.image" :alt="r.prompt" />
            <span v-if="r.mock" class="mock-tag">Mock</span>
            <span class="latency-tag">{{ r.latencyMs }}ms</span>
          </div>
          <div class="card-info">
            <div class="card-model">{{ r.modelName }}</div>
            <div class="card-meta">
              <span>{{ r.size }}</span>
              <el-rate v-model="r.score" :max="5" size="small" @change="rate(r)" />
            </div>
            <div class="card-actions">
              <el-button size="small" @click="downloadImage(r)">下载</el-button>
              <el-button size="small" @click="copyPrompt(r)">复制 prompt</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import http from '@/api/http'

const prompt = ref('一只可爱的橘猫坐在窗台上看日落, 油画风格, 暖色调')
const n = ref(1)
const size = ref('1024x1024')
const generating = ref(false)
const results = ref([])

const availableModels = [
  { code: 'black-forest-labs/FLUX.1-schnell', displayName: 'FLUX.1 Schnell', provider: 'SiliconFlow' },
  { code: 'stabilityai/stable-diffusion-xl-base-1.0', displayName: 'SDXL 1.0', provider: 'SiliconFlow' },
  { code: 'Kwai-Kolors/Kolors', displayName: '快手 Kolors', provider: 'SiliconFlow' },
  { code: 'wanx-v1', displayName: '通义万相 v1', provider: 'DashScope' },
  { code: 'dall-e-3', displayName: 'DALL-E 3', provider: 'OpenAI' },
  { code: 'mock', displayName: 'Mock (SVG 渐变)', provider: 'Mock' },
]
const selectedModels = ref(['black-forest-labs/FLUX.1-schnell', 'Kwai-Kolors/Kolors', 'mock'])

const quickPrompts = [
  { t: '一只可爱的橘猫坐在窗台上看日落, 油画风格, 暖色调' },
  { t: '未来城市的夜景, 霓虹灯, 赛博朋克风格, 4K' },
  { t: '中国水墨画风格的山水, 云雾缭绕' },
  { t: '一只宇航员漂浮在太空中, 望着蓝色地球' },
  { t: '一杯冒着热气的咖啡, 早晨阳光, 浅景深' },
]

const canGenerate = computed(() =>
  prompt.value.trim() && selectedModels.value.length > 0 && !generating.value)

function toggleModel(code) {
  const i = selectedModels.value.indexOf(code)
  if (i >= 0) selectedModels.value.splice(i, 1)
  else if (selectedModels.value.length < 4) selectedModels.value.push(code)
}

async function generate() {
  if (!canGenerate.value) return
  generating.value = true
  results.value = []
  try {
    const tasks = selectedModels.value.map(async (code) => {
      try {
        const r = await http.post('/api/v1/imagegen/generate', {
          prompt: prompt.value, model: code, size: size.value, n: n.value, base64: true
        })
        if (r && r.data) {
          const images = r.data.images || []
          for (let i = 0; i < images.length; i++) {
            results.value.push({
              modelCode: code,
              modelName: availableModels.find(m => m.code === code)?.displayName || code,
              image: images[i],
              size: r.data.size,
              latencyMs: r.data.latencyMs,
              mock: r.data.mock,
              prompt: prompt.value,
              score: 0,
            })
          }
        }
      } catch (e) {
        results.value.push({
          modelCode: code,
          modelName: availableModels.find(m => m.code === code)?.displayName || code,
          image: '',
          size: size.value,
          latencyMs: 0,
          mock: false,
          prompt: prompt.value,
          score: 0,
          error: e.message,
        })
      }
    })
    await Promise.all(tasks)
    ElMessage.success(`生成完成! ${results.value.length} 张图`)
  } finally {
    generating.value = false
  }
}

function downloadImage(r) {
  const a = document.createElement('a')
  a.href = r.image
  a.download = `${r.modelCode.replace(/[/\\:]/g, '_')}-${Date.now()}.svg`
  a.click()
  ElMessage.success('已下载')
}

async function copyPrompt(r) {
  await navigator.clipboard.writeText(r.prompt)
  ElMessage.success('prompt 已复制')
}

function rate(r) {
  console.log('rate:', r.modelCode, r.score)
  ElMessage.success(`已为 ${r.modelName} 打分 ${r.score} 星`)
}
</script>

<style scoped>
.imagegen { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #ec4899, #f43f5e); color: #fff; border-radius: 12px; font-size: 12px; }
.input-panel { background: #fff; border-radius: 12px; padding: 24px; margin: 24px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.quick-prompts { margin-top: 12px; display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.qp-label { color: #64748b; font-size: 13px; }
.qp-tag { cursor: pointer; }
.settings { margin-top: 20px; }
.form-row { display: flex; gap: 12px; margin-bottom: 14px; }
.form-item { margin-bottom: 14px; }
.form-item.half { flex: 1; }
label { display: block; font-weight: 600; margin-bottom: 6px; color: #334155; font-size: 13px; }
.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.model-chip { padding: 8px 12px; border: 2px solid #e2e8f0; border-radius: 8px; cursor: pointer; }
.model-chip.active { border-color: #ec4899; background: linear-gradient(135deg, #fdf2f8, #fce7f3); }
.model-name { display: block; font-weight: 600; font-size: 13px; }
.model-provider { display: block; font-size: 11px; color: #64748b; }

.gallery { margin-top: 24px; }
.gallery h2 { font-size: 24px; margin: 0 0 16px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.card-img { position: relative; aspect-ratio: 1; background: #f1f5f9; }
.card-img img { width: 100%; height: 100%; object-fit: cover; }
.mock-tag, .latency-tag { position: absolute; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.mock-tag { top: 8px; left: 8px; background: rgba(0,0,0,.7); color: #fff; }
.latency-tag { top: 8px; right: 8px; background: rgba(34,197,94,.9); color: #fff; }
.card-info { padding: 12px; }
.card-model { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.card-meta { display: flex; justify-content: space-between; align-items: center; color: #64748b; font-size: 12px; }
.card-actions { display: flex; gap: 6px; margin-top: 8px; }
</style>
