<!--
  视频生成 ShowCase (V4.2)
  - 6 模型: Sora / 可灵 / CogVideoX / 万相 / AnimateDiff / Mock
  - mock 模式: 演示 + 帧序列
-->
<!--
  @file views/showcase/VideoGenShowcase.vue (VideoGenShowcase 页面)
  @version V3.5.12+ (前端注释补全)
  @description VideoGenShowcase 页面
-->
<template>
  <div class="videogen">
    <header class="header">
      <h1>🎬 {{ t('showcase.videoGen') }}</h1>
      <p class="subtitle">{{ t('showcase.videoGenSubtitle') }}</p>
      <div class="badges">
        <span class="badge">Sora</span>
        <span class="badge">{{ t('showcase.kling') }}</span>
        <span class="badge">CogVideoX</span>
        <span class="badge">{{ t('showcase.wanxiang') }}</span>
      </div>
    </header>

    <section class="input-panel">
      <el-input v-model="prompt" type="textarea" :rows="3"
                :placeholder="t('showcase.describeScene')" />
      <div class="quick-prompts">
        <el-tag v-for="qp in quickPrompts" :key="qp" class="qp" @click="prompt = qp">
          {{ qp.slice(0, 30) }}...
        </el-tag>
      </div>

      <div class="settings">
        <div class="form-row">
          <div class="form-item half">
            <label>{{ t('showcase.duration') }}: {{ duration }}s</label>
            <el-slider v-model="duration" :min="2" :max="10" :step="1" />
          </div>
          <div class="form-item half">
            <label>{{ t('showcase.resolution') }}</label>
            <el-select v-model="resolution" style="width: 100%">
              <el-option label="720P (1280×720)" value="720P" />
              <el-option label="1080P (1920×1080)" value="1080P" />
              <el-option label="480P (854×480, 快)" value="480P" />
            </el-select>
          </div>
        </div>
        <div class="form-item">
          <label>{{ t('showcase.models') }}</label>
          <div class="model-grid">
            <div v-for="m in availableModels" :key="m.code"
                 :class="['model-chip', { active: selectedModels.includes(m.code) }]"
                 @click="toggleModel(m.code)">
              <span class="model-name">{{ m.displayName }}</span>
              <span class="model-provider">{{ m.provider }}</span>
            </div>
          </div>
        </div>
        <el-button type="primary" size="large" :icon="VideoCamera" :loading="generating"
                   :disabled="!canGenerate" @click="generate" block>
          {{ generating ? '🎬 ' + t('showcase.generating') : '🚀 ' + t('showcase.generateVideos') }}
        </el-button>
      </div>
    </section>

    <!-- 结果 -->
    <section v-if="results.length" class="results">
      <h2>🎥 {{ t('showcase.results') }} ({{ results.length }})</h2>
      <div class="grid">
        <div v-for="(r, idx) in results" :key="idx" class="video-card">
          <div class="vc-preview">
            <div v-if="r.status === 'generating'" class="vc-loading">
              <div class="spinner"></div>
              <p>{{ r.progress }}%</p>
              <p class="vc-tip">{{ t('showcase.rendering') }}</p>
            </div>
            <div v-else-if="r.status === 'done'" class="vc-done">
              <!-- Mock 视频: 用动画 SVG + 文字 -->
              <svg viewBox="0 0 400 225" class="vc-svg">
                <defs>
                  <linearGradient :id="`g${idx}`" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0" :stop-color="r.color1" />
                    <stop offset="1" :stop-color="r.color2" />
                  </linearGradient>
                </defs>
                <rect width="400" height="225" :fill="`url(#g${idx})`"/>
                <text x="200" y="120" font-size="18" fill="white" text-anchor="middle" font-family="sans-serif" font-weight="bold" opacity="0.9">
                  {{ r.prompt.slice(0, 30) }}
                </text>
                <text x="200" y="145" font-size="12" fill="white" text-anchor="middle" opacity="0.6">
                  {{ r.modelName }} · {{ r.duration }}s · {{ r.resolution }}
                </text>
              </svg>
              <div class="vc-play-overlay" @click="playMock(r)">
                <el-icon :size="48"><VideoPlay /></el-icon>
              </div>
            </div>
            <div v-else-if="r.status === 'error'" class="vc-error">
              <el-icon :size="32"><WarningFilled /></el-icon>
              <p>{{ r.error }}</p>
            </div>
          </div>
          <div class="vc-info">
            <div class="vc-model">{{ r.modelName }}</div>
            <div class="vc-meta">
              <span>⏱ {{ r.duration }}s</span>
              <span>📺 {{ r.resolution }}</span>
              <el-tag v-if="r.mock" size="small" type="info">{{ t('showcase.mock') }}</el-tag>
            </div>
            <el-rate v-model="r.score" :max="5" size="small" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoCamera, VideoPlay, WarningFilled } from '@element-plus/icons-vue'
import { t } from '@/i18n'

const prompt = ref('一只橘猫在草地上追蝴蝶, 慢动作, 电影质感, 黄金时刻光线')
const duration = ref(5)
const resolution = ref('720P')
const generating = ref(false)
const results = ref([])

const availableModels = [
  { code: 'sora', displayName: 'Sora (OpenAI)', provider: 'OpenAI' },
  { code: 'kling', displayName: '可灵 (快手)', provider: 'Kuaishou' },
  { code: 'cogvideox', displayName: 'CogVideoX (智谱)', provider: 'Zhipu' },
  { code: 'wanx2.1', displayName: '通义万相 2.1', provider: 'DashScope' },
  { code: 'animatediff', displayName: 'AnimateDiff (硅基流动)', provider: 'SiliconFlow' },
  { code: 'mock', displayName: 'Mock (沙箱)', provider: 'Mock' },
]
const selectedModels = ref(['sora', 'kling', 'mock'])

const quickPrompts = [
  '一只橘猫在草地上追蝴蝶, 慢动作',
  '未来城市的航拍, 霓虹灯光, 4K',
  '樱花飘落的公园, 阳光透过花瓣',
  '一杯咖啡倒进杯子的特写, 浅景深',
  '太空中的宇航员, 地球在背景',
]

const canGenerate = computed(() =>
  prompt.value.trim() && selectedModels.value.length > 0 && !generating.value)

function toggleModel(code) {
  const i = selectedModels.value.indexOf(code)
  if (i >= 0) selectedModels.value.splice(i, 1)
  else if (selectedModels.value.length < 3) selectedModels.value.push(code)
}

function hashColor(s) {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) & 0xFFFFFF
  return '#' + h.toString(16).padStart(6, '0')
}

async function generate() {
  if (!canGenerate.value) return
  generating.value = true
  results.value = []
  // mock: 快速演示, 真实 API 需要 30-300s
  const tasks = selectedModels.value.map(async (code) => {
    const r = ref({
      modelCode: code,
      modelName: availableModels.find(m => m.code === code)?.displayName || code,
      status: 'generating',
      progress: 0,
      prompt: prompt.value,
      duration: duration.value,
      resolution: resolution.value,
      mock: code === 'mock' || true,
      score: 0,
      color1: hashColor(code + prompt.value),
      color2: hashColor(prompt.value + code),
    })
    results.value.push(r.value)
    // 模拟进度
    for (let p = 0; p <= 100; p += 5) {
      if (!generating.value) { r.value.status = 'error'; r.value.error = '已取消'; return }
      await new Promise(s => setTimeout(s, 50 + Math.random() * 50))
      r.value.progress = p
    }
    r.value.status = 'done'
    r.value.progress = 100
  })
  await Promise.all(tasks)
  generating.value = false
  ElMessage.success(`生成完成! ${results.value.length} 个视频`)
}

function playMock(r) {
  ElMessage.info('Mock 模式: 真实部署时这里播放生成的 mp4')
}
</script>

<style scoped>
.videogen { max-width: 1400px; margin: 0 auto; padding: 24px; }
.header h1 { font-size: 32px; margin: 0 0 8px; }
.subtitle { color: #64748b; margin: 0 0 12px; }
.badge { display: inline-block; padding: 2px 10px; margin-right: 6px;
  background: linear-gradient(135deg, #dc2626, #b91c1c); color: #fff; border-radius: 12px; font-size: 12px; }

.input-panel { background: #fff; border-radius: 12px; padding: 24px; margin: 24px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.quick-prompts { margin-top: 12px; display: flex; gap: 6px; flex-wrap: wrap; }
.qp { cursor: pointer; }
.settings { margin-top: 20px; }
.form-row { display: flex; gap: 12px; margin-bottom: 14px; }
.form-item { margin-bottom: 14px; }
.form-item.half { flex: 1; }
label { display: block; font-weight: 600; margin-bottom: 6px; color: #334155; font-size: 13px; }
.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.model-chip { padding: 8px 12px; border: 2px solid #e2e8f0; border-radius: 8px; cursor: pointer; }
.model-chip.active { border-color: #dc2626; background: linear-gradient(135deg, #fef2f2, #fee2e2); }
.model-name { display: block; font-weight: 600; font-size: 13px; }
.model-provider { display: block; font-size: 11px; color: #64748b; }

.results { margin-top: 24px; }
.results h2 { font-size: 24px; margin: 0 0 16px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.video-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.vc-preview { position: relative; aspect-ratio: 16/9; background: #0f172a; }
.vc-loading { position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #fff; }
.spinner { width: 48px; height: 48px; border: 4px solid #1e293b; border-top-color: #dc2626;
  border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 12px; }
@keyframes spin { to { transform: rotate(360deg); } }
.vc-tip { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.vc-done { position: relative; }
.vc-svg { width: 100%; height: 100%; display: block; }
.vc-play-overlay { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0); color: #fff; cursor: pointer; transition: all .2s; }
.vc-play-overlay:hover { background: rgba(0,0,0,0.4); }
.vc-error { position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #fca5a5; padding: 12px; text-align: center; }
.vc-info { padding: 12px; }
.vc-model { font-weight: 600; margin-bottom: 4px; }
.vc-meta { display: flex; gap: 12px; font-size: 12px; color: #64748b; margin-bottom: 8px; }
</style>
