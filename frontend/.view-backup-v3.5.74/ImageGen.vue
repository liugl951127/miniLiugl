<!--
  @file views/ai/ImageGen.vue (ImageGen 页面)
  @version V3.5.12+ (前端注释补全)
  @description ImageGen 页面
-->
<template>
  <div class="image-gen">
    <el-card>
      <template #header>
        <div class="header">
          <span>🎨 AIGC 图片生成 (V2.7.5 - 自研, 无外部 LLM 依赖)</span>
        </div>
      </template>

      <el-row :gutter="16">
        <!-- 左侧控制 -->
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>⚙️ 生成配置</template>
            <el-form :model="req" label-width="80px">
              <el-form-item label="描述">
                <el-input v-model="req.prompt" type="textarea" :rows="3"
                          placeholder="例: 一座山的日落风景 / 公司 logo / 数据图表 / 蓝色渐变" />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="req.type" placeholder="自动推断" clearable>
                  <el-option v-for="t in imageTypes" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
              <el-form-item label="尺寸">
                <el-radio-group v-model="sizePreset" @change="changeSize">
                  <el-radio-button label="512x512" />
                  <el-radio-button label="1024x1024" />
                  <el-radio-button label="1920x1080" />
                </el-radio-group>
              </el-form-item>
              <el-form-item label="种子">
                <el-input-number v-model="req.seed" :min="0" />
                <el-button size="small" @click="randomSeed" style="margin-left: 8px">🎲 随机</el-button>
              </el-form-item>
              <el-button type="primary" :loading="loading" @click="generate" style="width: 100%">
                🎨 立即生成
              </el-button>
              <el-button @click="inferType" style="width: 100%; margin-top: 8px">
                🔍 推断类型
              </el-button>
            </el-form>
          </el-card>
        </el-col>

        <!-- 右侧展示 -->
        <el-col :span="16">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span>🖼️ 预览</span>
                <el-button-group v-if="result">
                  <el-button size="small" @click="download">💾 下载</el-button>
                </el-button-group>
              </div>
            </template>

            <div v-if="result" class="result">
              <div class="image-wrap">
                <img :src="imageUrl" alt="Generated" />
              </div>

              <el-descriptions :column="2" border size="small" style="margin-top: 12px">
                <el-descriptions-item label="类型">{{ result.type }}</el-descriptions-item>
                <el-descriptions-item label="尺寸">{{ result.width }} x {{ result.height }}</el-descriptions-item>
                <el-descriptions-item label="大小">{{ formatBytes(result.sizeBytes) }}</el-descriptions-item>
                <el-descriptions-item label="格式">{{ result.mime }}</el-descriptions-item>
                <el-descriptions-item label="种子">{{ result.metadata?.seed }}</el-descriptions-item>
                <el-descriptions-item label="耗时">{{ result.metadata?.costMs }}ms</el-descriptions-item>
                <el-descriptions-item label="提示词" :span="2">{{ result.prompt }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <el-empty v-else description="填写描述后点击生成" />
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { generateImage, listImageTypes, inferImageType as inferApi } from '@/api/ai'

const req = ref({ prompt: '蓝色渐变背景', type: '', width: 1024, height: 1024, seed: 42 })
const sizePreset = ref('1024x1024')
const imageTypes = ref(['abstract', 'gradient', 'pattern', 'text', 'scene', 'logo', 'infographic'])
const result = ref(null)
const loading = ref(false)

const imageUrl = computed(() => {
  if (!result.value?.base64) return ''
  return `data:${result.value.mime};base64,${result.value.base64}`
})

function changeSize(label) {
  const [w, h] = label.split('x').map(Number)
  req.value.width = w
  req.value.height = h
}

function randomSeed() {
  req.value.seed = Math.floor(Math.random() * 1_000_000)
}

async function generate() {
  if (!req.value.prompt) {
    ElMessage.warning('请输入描述')
    return
  }
  loading.value = true
  try {
    const res = await generateImage(req.value)
    result.value = res.data
    ElMessage.success('生成成功')
  } catch (e) {
    ElMessage.error('生成失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function inferType() {
  if (!req.value.prompt) return
  try {
    const res = await inferApi(req.value.prompt)
    req.value.type = res.data.type
    ElMessage.info('推断类型: ' + res.data.type)
  } catch (e) {
    ElMessage.error('推断失败')
  }
}

function download() {
  if (!result.value) return
  const link = document.createElement('a')
  link.href = imageUrl.value
  link.download = `aigc-${result.value.type}-${Date.now()}.png`
  link.click()
}

function formatBytes(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(async () => {
  try {
    const res = await listImageTypes()
    imageTypes.value = res.data || imageTypes.value
  } catch (e) { /* 默认值已设置 */ }
})
</script>

<style scoped>
.image-gen { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.image-wrap {
  background: #fafafa;
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 16px;
}
.image-wrap img { max-width: 100%; max-height: 600px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
</style>
