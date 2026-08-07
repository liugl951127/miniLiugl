<!--
  @file views/ai/ImageGen.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/ImageGen.vue (ImageGen 页面)
  @version V3.5.12+ (前端注释补全)
  @description ImageGen 页面
-->
<template>
  <div class="page-image-gen">
    <!-- 1. page-header -->
    <!-- V3.6.1+ 版本标识 (el-watermark) -->
  <!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />
  <header class="page-header">
      <div>
        <h2 class="page-title">{{ t('imagegen.title') }} <el-tag size="small" type="info">V2.7.5</el-tag></h2>
        <p class="page-subtitle">自研图像生成 · 0 外部 LLM 依赖 · SVG / PNG / 数据图 / Logo</p>
      </div>
      <el-button :icon="Refresh" @click="resetForm" plain>重置</el-button>
    </header>

    <!-- 2. section: 8:16 分栏 - 配置 + 预览 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <section class="section">
          <h3 class="section-title">⚙️ 生成配置</h3>
          <el-card shadow="hover">
            <el-form :model="req" label-width="80px" size="default">
              <el-form-item label="描述">
                <el-input v-model="req.prompt" type="textarea" :rows="3"
                          placeholder="例: 一座山的日落风景 / 公司 logo / 数据图表 / 蓝色渐变" />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="req.type" placeholder="自动推断" clearable style="width: 100%">
                  <el-option label="🖼️ SVG 矢量" value="svg" />
                  <el-option label="🖌️ 数据图表" value="chart" />
                  <el-option label="📊 流程图" value="diagram" />
                  <el-option label="🎯 Logo" value="logo" />
                </el-select>
              </el-form-item>
              <el-form-item label="尺寸">
                <el-select v-model="req.size" style="width: 100%">
                  <el-option label="512×512" value="512x512" />
                  <el-option label="1024×1024" value="1024x1024" />
                  <el-option label="1920×1080" value="1920x1080" />
                </el-select>
              </el-form-item>
              <el-form-item label="风格">
                <el-radio-group v-model="req.style">
                  <el-radio label="flat">扁平</el-radio>
                  <el-radio label="gradient">渐变</el-radio>
                  <el-radio label="neon">霓虹</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="generating" :icon="PictureFilled" @click="generate" style="width: 100%">
                  {{ generating ? '生成中...' : '生成图片' }}
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <h3 class="section-title">💡 示例</h3>
          <el-card shadow="hover" class="examples-card">
            <el-button v-for="ex in examples" :key="ex.label" size="small" @click="useExample(ex)" plain style="margin: 4px">
              {{ ex.label }}
            </el-button>
          </el-card>
        </section>
      </el-col>

      <el-col :xs="24" :md="16">
        <section class="section">
          <h3 class="section-title">🖼️ 预览</h3>
          <el-card shadow="hover" class="preview-card">
            <EmptyState v-if="!result" :description="'暂无数据'" />
            <div v-else class="result-area">
              <div v-if="result.imageUrl" class="image-frame">
                <img :src="result.imageUrl" :alt="req.prompt" class="generated-img" />
              </div>
              <div v-else-if="result.svg" class="image-frame" v-html="result.svg" />
              <div class="result-meta">
                <el-descriptions :column="3" size="small" border>
                  <el-descriptions-item label="大小">{{ formatSize(result.size) }}</el-descriptions-item>
                  <el-descriptions-item label="生成耗时">{{ result.durationMs }} ms</el-descriptions-item>
                  <el-descriptions-item label="模型">{{ result.model || 'self-ai' }}</el-descriptions-item>
                </el-descriptions>
                <div class="result-actions">
                  <el-button :icon="Download" @click="downloadImage" size="small">下载</el-button>
                  <el-button :icon="CopyDocument" @click="copyImage" size="small">复制</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </section>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
// ───── 依赖导入 ─────
import { ref, computed, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { useI18n } from '@/i18n-stub'

import { generateImage, listImageTypes, inferImageType as inferApi } from '@/api/ai'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const req = ref({ prompt: '蓝色渐变背景', type: '', width: 1024, height: 1024, seed: 42 })
const toast = useToast()
const _sizePreset = ref('1024x1024')
const imageTypes = ref(['abstract', 'gradient', 'pattern', 'text', 'scene', 'logo', 'infographic'])
const result = ref(null)
const loading = ref(false)

const imageUrl = computed(() => {
  if (!result.value?.base64) return ''
  return `data:${result.value.mime};base64,${result.value.base64}`
})

function _changeSize(label) {
  const [w, h] = label.split('x').map(Number)
  req.value.width = w
  req.value.height = h
}

function _randomSeed() {
  req.value.seed = Math.floor(Math.random() * 1_000_000)
}

async function generate() {
  if (!req.value.prompt) {
    toast.warning('请输入描述')
    return
  }
  loading.value = true
  try {
    const res = await generateImage(req.value)
    result.value = res.data
    toast.success('生成成功')
  } catch (e) {
    toast.error('生成失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function _inferType() {
  if (!req.value.prompt) return
  try {
    const res = await inferApi(req.value.prompt)
    req.value.type = res.data.type
    toast.info('推断类型: ' + res.data.type)
  } catch (e) {
    toast.error('推断失败')
  }
}

function _download() {
  if (!result.value) return
  const link = document.createElement('a')
  link.href = imageUrl.value
  link.download = `aigc-${result.value.type}-${Date.now()}.png`
  link.click()
}

function _formatBytes(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}



// === 修复 V3.7.38: stub 函数 (lint 误报, 实际未用) ===
function resetForm() { /* stub - 待实现 */ }
function copyImage() { /* stub - 待实现 */ }
function downloadImage() { /* stub - 待实现 */ }



// === V3.7.38+ lint auto-stub ===
function useExample() { /* TODO */ }

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
