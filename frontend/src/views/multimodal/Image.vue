<!--
  @file multimodal/Image.vue - 图像能力 (V7.7)
  路由: /multimodal/image
  包含: 图片生成 + 图片理解
-->
<template>
  <div class="image-page">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- 图片生成 -->
      <el-tab-pane label="图片生成" name="image-gen">
        <el-card v-loading="imgLoading">
          <template #header>
            <div class="card-header">
              <span>🎨 图片生成</span>
              <el-tag size="small" type="success">真实 API</el-tag>
              <el-select v-model="selectedModel" placeholder="选择模型" style="width: 220px; margin-left: auto">
                <el-option-group label="🏷️ 自研模型">
                  <el-option v-for="m in selfModels" :key="m.modelCode" :label="m.displayName || m.modelCode" :value="m.modelCode" />
                </el-option-group>
                <el-option-group label="☁️ 云端模型">
                  <el-option v-for="m in cloudModels" :key="m.modelCode" :label="m.displayName || m.modelCode" :value="m.modelCode" />
                </el-option-group>
              </el-select>
            </div>
          </template>
          <el-input v-model="imgPrompt" type="textarea" :rows="3" placeholder="描述想生成的图片, 例如: 赛博朋克风格的城市夜景" />
          <div class="action-row">
            <el-button type="primary" :loading="imgLoading" :icon="MagicStick" @click="generateImage">生成</el-button>
            <span v-if="imgResult?.url" class="result-url">生成完成 ↓</span>
          </div>
          <div v-if="imgResult?.url" class="image-preview">
            <img :src="imgResult.url" alt="generated" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 图片理解 -->
      <el-tab-pane label="图片理解" name="image-understand">
        <el-card v-loading="understandLoading">
          <template #header><span>🔍 图片理解</span></template>
          <el-upload drag :auto-upload="false" :on-change="onImageUpload" :show-file-list="false" accept="image/*">
            <el-icon class="el-icon--upload"><Picture /></el-icon>
            <div class="el-upload__text">拖拽图片, 或<em>点击上传</em></div>
          </el-upload>
          <el-input v-if="uploadedUrl" v-model="understandPrompt" type="textarea" :rows="2"
            placeholder="可选: 具体问题, 例如: 图里有什么?" style="margin-top: 12px" />
          <el-button v-if="uploadedUrl" type="primary" :loading="understandLoading" :icon="Search" @click="analyzeImage" style="margin-top: 8px">分析</el-button>
          <div v-if="uploadedUrl" class="image-preview">
            <img :src="uploadedUrl" alt="uploaded" style="max-width: 300px" />
          </div>
          <div v-if="understandResult" class="result-text">
            <h4>分析结果</h4>
            <el-input v-model="understandResult" type="textarea" :rows="4" readonly />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Picture, Search } from '@element-plus/icons-vue'
import { listEnabledModels } from '@/api/model'
import { visionAnalyze, imageGenGenerate } from '@/api/multimodal'

const activeTab = ref('image-gen')

const imgPrompt = ref('')
const imgLoading = ref(false)
const imgResult = ref(null)
const selectedModel = ref('')
const selfModels = ref([])
const cloudModels = ref([])

const uploadedUrl = ref('')
const understandPrompt = ref('')
const understandLoading = ref(false)
const understandResult = ref('')

async function loadModels() {
  try {
    const res = await listEnabledModels()
    const list = Array.isArray(res) ? res : (res?.data || [])
    selfModels.value = list.filter(m => m.category === 'imageGen' && m.source === 'self')
    cloudModels.value = list.filter(m => m.category === 'imageGen' && m.source === 'cloud')
  } catch (e) { console.error('loadModels', e) }
}

async function generateImage() {
  if (!imgPrompt.value.trim()) return ElMessage.warning('请输入描述')
  imgLoading.value = true
  try {
    const res = await imageGenGenerate({ prompt: imgPrompt.value, model: selectedModel.value || undefined })
    if (res.code === 0) {
      imgResult.value = res.data
      ElMessage.success('生成完成')
    } else ElMessage.error(res.message || '生成失败')
  } finally { imgLoading.value = false }
}

function onImageUpload(file) {
  if (!file?.raw) return
  uploadedUrl.value = URL.createObjectURL(file.raw)
  understandResult.value = ''
}

async function analyzeImage() {
  if (!uploadedUrl.value) return ElMessage.warning('请上传图片')
  understandLoading.value = true
  try {
    const res = await visionAnalyze(uploadedUrl.value, understandPrompt.value, undefined)
    if (res.code === 0) {
      understandResult.value = res.data?.text || res.data?.description || JSON.stringify(res.data)
    } else ElMessage.error(res.message || '分析失败')
  } finally { understandLoading.value = false }
}

onMounted(loadModels)
</script>

<style scoped>
.image-page { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.card-header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.action-row { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
.result-url { color: #67c23a; font-size: 0.9em; }
.image-preview { margin-top: 12px; }
.image-preview img { max-width: 100%; max-height: 400px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.result-text { margin-top: 12px; }
.result-text h4 { margin: 0 0 8px; color: #1e293b; }
</style>
