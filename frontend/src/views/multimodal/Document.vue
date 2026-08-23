<!--
  @file multimodal/Document.vue - 文档/音乐 (V7.7)
  路由: /multimodal/document
  包含: 文档理解 + 音乐生成
-->
<template>
  <div class="document-page">
    <el-tabs v-model="activeTab" class="feature-tabs">
      <!-- 文档理解 -->
      <el-tab-pane label="文档问答" name="doc">
        <el-card v-loading="docLoading">
          <template #header><span>📄 文档理解 <el-tag size="small" type="warning">Beta</el-tag></span></template>
          <el-upload drag :auto-upload="false" :on-change="onDocUpload" :show-file-list="false" accept=".pdf,.txt,.doc,.docx,.png,.jpg">
            <el-icon class="el-icon--upload"><Document /></el-icon>
            <div class="el-upload__text">拖拽文档, 或<em>点击上传</em></div>
            <div class="el-upload__tip">支持 PDF / Word / TXT / 图片</div>
          </el-upload>
          <el-input v-if="docFile" v-model="docQuestion" type="textarea" :rows="2" placeholder="问点啥" style="margin-top: 12px" />
          <el-button v-if="docFile" type="primary" :loading="docLoading" :icon="ChatLineRound" @click="askDoc" style="margin-top: 8px">提问</el-button>
          <div v-if="docAnswer" class="result-text" style="margin-top: 12px">
            <h4>回答</h4>
            <el-input v-model="docAnswer" type="textarea" :rows="4" readonly />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 音乐生成 -->
      <el-tab-pane label="音乐生成" name="music">
        <el-card v-loading="musicLoading">
          <template #header><span>🎵 音乐生成 <el-tag size="small" type="warning">Beta</el-tag></span></template>
          <el-input v-model="musicPrompt" type="textarea" :rows="2" placeholder="描述音乐风格, 例如: 轻松的爵士钢琴" />
          <el-input v-model="musicLyrics" type="textarea" :rows="2" placeholder="歌词 (可选)" style="margin-top: 8px" />
          <el-button type="primary" :loading="musicLoading" :icon="Microphone" @click="generateMusic" style="margin-top: 12px">生成</el-button>
          <audio v-if="musicUrl" :src="musicUrl" controls style="width: 100%; margin-top: 12px" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, ChatLineRound, Microphone } from '@element-plus/icons-vue'
import { docUpload, docAsk, musicGenerate } from '@/api/multimodal'

const activeTab = ref('doc')

const docFile = ref(null)
const docId = ref(null)
const docQuestion = ref('')
const docLoading = ref(false)
const docAnswer = ref('')

const musicPrompt = ref('')
const musicLyrics = ref('')
const musicLoading = ref(false)
const musicUrl = ref('')

async function onDocUpload(file) {
  if (!file?.raw) return
  docFile.value = file.raw
  docAnswer.value = ''
  // 自动上传
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res = await docUpload(fd)
    if (res.code === 0) {
      docId.value = res.data?.id || res.data?.docId
      ElMessage.success('文档已上传')
    } else {
      ElMessage.warning('上传失败, 可继续提问 (降级处理)')
    }
  } catch (e) {
    console.error('doc upload', e)
  }
}

async function askDoc() {
  if (!docId.value) return ElMessage.warning('文档上传失败, 无法提问')
  if (!docQuestion.value.trim()) return ElMessage.warning('请输入问题')
  docLoading.value = true
  try {
    const res = await docAsk(docId.value, docQuestion.value)
    if (res.code === 0) {
      docAnswer.value = res.data?.answer || res.data?.text || JSON.stringify(res.data)
    } else ElMessage.error(res.message || '失败')
  } finally { docLoading.value = false }
}

async function generateMusic() {
  if (!musicPrompt.value.trim()) return ElMessage.warning('请输入描述')
  musicLoading.value = true
  try {
    const res = await musicGenerate({ prompt: musicPrompt.value, lyrics: musicLyrics.value })
    if (res.code === 0) {
      musicUrl.value = res.data?.url || res.data?.audioUrl || ''
      ElMessage.success('生成完成')
    } else ElMessage.error(res.message || '生成失败')
  } finally { musicLoading.value = false }
}
</script>

<style scoped>
.document-page { background: white; border-radius: 12px; padding: 16px; }
.feature-tabs { background: transparent; }
.result-text h4 { margin: 0 0 8px; color: #1e293b; }
</style>
