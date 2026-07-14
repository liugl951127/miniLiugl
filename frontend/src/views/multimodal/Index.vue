<!--
  Multimodal 多模态页 (V3.5.5+ 新增)
  对应后端模块: minimax-multimodal (端口 8087)
  API 路径: /api/v1/multimodal/*
  功能: 图像描述 / 多图分析 / 多模态上传 / 视频描述
-->
<template>
  <div class="multimodal-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <span class="title">
            <el-icon><PictureFilled /></el-icon>
            Multimodal 多模态
          </span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="mm-tabs">
        <!-- Tab 1: 图像描述 -->
        <el-tab-pane label="图像描述" name="describe">
          <el-form label-width="100px">
            <el-form-item label="上传图片">
              <el-upload
                ref="imgUploadRef"
                :auto-upload="false"
                :limit="1"
                :on-change="onImageChange"
                accept="image/*"
              >
                <el-button :icon="Upload">选择图片</el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 JPG/PNG/WebP, 最大 10MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item label="图片预览" v-if="imageUrl">
              <el-image :src="imageUrl" style="max-width: 300px; max-height: 300px" fit="contain" />
            </el-form-item>
            <el-form-item label="自定义 prompt">
              <el-input v-model="describePrompt" type="textarea" :rows="2"
                        placeholder="请详细描述这张图片" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="describeLoading"
                         @click="describeImage" :disabled="!imageFile">
                开始描述
              </el-button>
            </el-form-item>
            <el-form-item v-if="describeResult" label="描述结果">
              <el-card shadow="never" class="result-card">
                <pre class="result-text">{{ describeResult }}</pre>
              </el-card>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 2: 多图分析 -->
        <el-tab-pane label="多图对比" name="multi">
          <el-form label-width="100px">
            <el-form-item label="多张图片">
              <el-upload
                ref="multiUploadRef"
                :auto-upload="false"
                :limit="5"
                :multiple="true"
                :on-change="onMultiChange"
                accept="image/*"
              >
                <el-button :icon="Upload">选择 1-5 张</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item v-if="multiUrls.length" label="图片预览">
              <div class="multi-previews">
                <el-image v-for="(u, i) in multiUrls" :key="i" :src="u"
                          style="width: 120px; height: 120px; margin-right: 8px"
                          fit="cover" />
              </div>
            </el-form-item>
            <el-form-item label="对比问题">
              <el-input v-model="multiPrompt" type="textarea" :rows="2"
                        placeholder="这几张图有什么不同?" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="multiLoading"
                         @click="describeMulti" :disabled="multiFiles.length === 0">
                开始对比
              </el-button>
            </el-form-item>
            <el-form-item v-if="multiResult" label="对比结果">
              <el-card shadow="never" class="result-card">
                <pre class="result-text">{{ multiResult }}</pre>
              </el-card>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 3: 提供商信息 -->
        <el-tab-pane label="提供商" name="provider">
          <el-descriptions :column="2" border v-if="providerInfo">
            <el-descriptions-item label="默认提供商">{{ providerInfo.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模型">{{ providerInfo.model || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支持能力" :span="2">
              <el-tag v-for="c in (providerInfo.capabilities || [])" :key="c" style="margin-right: 4px">
                {{ c }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最大图片" :span="2">
              {{ providerInfo.maxImages || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="最大文件" :span="2">
              {{ providerInfo.maxFileSize || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="加载中..." />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { PictureFilled, Upload, MagicStick } from '@element-plus/icons-vue'
import http from '@/api/http'
import { describeImageOld as describeImageApi, getMultimodalInfoOld as getInfoApi } from '@/api/multimodal'

const activeTab = ref('describe')

// 图像描述
const imgUploadRef = ref()
const imageFile = ref(null)
const imageUrl = ref('')
const describePrompt = ref('请详细描述这张图片')
const describeLoading = ref(false)
const describeResult = ref('')

// 多图对比
const multiUploadRef = ref()
const multiFiles = ref([])
const multiUrls = ref([])
const multiPrompt = ref('这几张图有什么不同?')
const multiLoading = ref(false)
const multiResult = ref('')

// 提供商
const providerInfo = ref(null)

const onImageChange = (file) => {
  imageFile.value = file.raw
  imageUrl.value = URL.createObjectURL(file.raw)
}

const onMultiChange = (file) => {
  multiFiles.value.push(file.raw)
  multiUrls.value.push(URL.createObjectURL(file.raw))
}

const describeImage = async () => {
  describeLoading.value = true
  try {
    // 转 base64 (minimax-multimodal 接收 imageBase64 + mimeType)
    const base64 = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result.split(',')[1])
      reader.onerror = reject
      reader.readAsDataURL(imageFile.value)
    })
    const res = await describeImageApi(base64, imageFile.value.type, describePrompt.value)
    describeResult.value = typeof res === 'string' ? res : (res.data?.description || JSON.stringify(res, null, 2))
  } catch (e) {
    describeResult.value = '❌ ' + e.message
  } finally {
    describeLoading.value = false
  }
}

const describeMulti = async () => {
  multiLoading.value = true
  try {
    const formData = new FormData()
    multiFiles.value.forEach(f => formData.append('files', f))
    formData.append('prompt', multiPrompt.value)
    const res = await http.post('/api/v1/multimodal/describe/multi', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    multiResult.value = typeof res === 'string' ? res : (res.data?.result || JSON.stringify(res, null, 2))
  } catch (e) {
    multiResult.value = '❌ ' + e.message
  } finally {
    multiLoading.value = false
  }
}

const loadProvider = async () => {
  try {
    const res = await http.get('/api/v1/multimodal/providers/default')
    providerInfo.value = res.data || res
  } catch (e) {
    console.warn('加载提供商信息失败:', e.message)
  }
}

onMounted(loadProvider)
</script>

<style lang="scss" scoped>
.multimodal-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.mm-tabs { padding: 0 16px; }
.multi-previews { display: flex; flex-wrap: wrap; }
.result-card { background: #f5f7fa; }
.result-text {
  white-space: pre-wrap; word-break: break-word;
  font-family: inherit; line-height: 1.6; margin: 0;
  max-height: 500px; overflow: auto;
}
</style>
