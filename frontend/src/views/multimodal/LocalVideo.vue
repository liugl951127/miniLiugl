<!--
  @file multimodal/LocalVideo.vue - 本地视频智能 (V7.6)
  路由: /multimodal/local/video
-->
<template>
  <div class="local-video">
    <el-card>
      <el-upload drag :auto-upload="false" :on-change="onVideoFile" :show-file-list="false"
        accept="video/*,.mp4,.mov,.avi" v-loading="loading">
        <el-icon class="el-icon--upload"><VideoPlay /></el-icon>
        <div class="el-upload__text">拖拽视频, 或<em>点击上传</em></div>
        <div class="el-upload__tip">MP4/MOV/AVI, 复用 ResNet50 + Whisper</div>
      </el-upload>
      <el-form v-if="videoUrl" style="margin-top: 16px">
        <el-form-item>
          <video :src="videoUrl" controls style="max-width: 100%; max-height: 360px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="runAnalyze">开始分析</el-button>
        </el-form-item>
      </el-form>
      <div v-if="result" class="video-result">
        <h3>分析结果 <el-tag size="small">{{ result.costMs }}ms</el-tag></h3>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="时长">{{ result.media?.durationSec?.toFixed(1) }}s</el-descriptions-item>
          <el-descriptions-item label="分辨率">{{ result.media?.width }}×{{ result.media?.height }}</el-descriptions-item>
          <el-descriptions-item label="帧率">{{ result.media?.fps?.toFixed(1) }} fps</el-descriptions-item>
          <el-descriptions-item label="轨道">{{ (result.media?.hasVideo ? 'V' : '') + (result.media?.hasAudio ? 'A' : '') }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="result.timeline?.length" style="margin-top: 16px">
          <h4>🎬 关键场景 ({{ result.timeline.length }} 段)</h4>
          <el-table :data="result.timeline" stripe>
            <el-table-column label="时间">
              <template #default="{ row }">{{ row.start?.toFixed(1) }}s ~ {{ row.end?.toFixed(1) }}s</template>
            </el-table-column>
            <el-table-column label="场景">
              <template #default="{ row }">
                <span style="font-weight:600">{{ row.labelCn }}</span> / {{ row.labelEn }}
              </template>
            </el-table-column>
            <el-table-column label="置信度">
              <template #default="{ row }">
                <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-if="result.transcript" style="margin-top: 16px">
          <h4>📝 音轨转写 <el-tag size="small">{{ result.asrCostMs }}ms</el-tag></h4>
          <el-input v-model="result.transcript" type="textarea" :rows="4" readonly />
        </div>
        <el-empty v-if="!result.timeline?.length && !result.transcript" description="视频无有效内容或模型未就绪" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import { multimodalApi } from '@/api/multimodal'

const currentVideo = ref(null)
const videoUrl = ref('')
const result = ref(null)
const loading = ref(false)

function onVideoFile(file) {
  if (!file?.raw) return
  currentVideo.value = file.raw
  videoUrl.value = URL.createObjectURL(file.raw)
  result.value = null
}

async function runAnalyze() {
  if (!currentVideo.value) return ElMessage.warning('请上传视频')
  loading.value = true
  try {
    const res = await multimodalApi.analyzeVideo(currentVideo.value)
    if (res.code === 0) {
      result.value = res.data
      ElMessage.success('分析完成')
    } else if (res.code === 1001) ElMessage.warning(res.message)
    else ElMessage.error(res.message || '失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.local-video { background: white; border-radius: 12px; padding: 16px; }
.video-result { margin-top: 16px; }
h3, h4 { margin: 0 0 12px; color: #1e293b; }
</style>
