<!--
  @file chat/ChatInput.vue - 输入区 (V7.8)
-->
<template>
  <div class="chat-input">
    <!-- 附件预览 -->
    <div v-if="attachments.length" class="attachment-row">
      <div v-for="(a, i) in attachments" :key="i" class="attachment-chip">
        <el-icon><Picture v-if="a.type === 'image'" /><VideoCamera v-else /></el-icon>
        <span class="att-name">{{ a.name }}</span>
        <el-button :icon="Close" size="small" link @click="removeAttachment(i)" />
      </div>
    </div>

    <!-- 文本输入 -->
    <el-input
      :model-value="text"
      type="textarea"
      :rows="3"
      :placeholder="placeholder"
      @update:model-value="$emit('update:text', $event)"
      @keydown.ctrl.enter="send"
      @keydown.meta.enter="send"
    />

    <!-- 工具栏 -->
    <div class="input-footer">
      <div class="input-tools">
        <el-upload
          :show-file-list="false" :auto-upload="false"
          :on-change="onImageUpload" accept="image/*"
        >
          <el-button :icon="Picture" size="small" link title="图片">图片</el-button>
        </el-upload>
        <el-upload
          :show-file-list="false" :auto-upload="false"
          :on-change="onVideoUpload" accept="video/*"
        >
          <el-button :icon="VideoCamera" size="small" link title="视频">视频</el-button>
        </el-upload>
        <el-upload
          :show-file-list="false" :auto-upload="false"
          :on-change="onAudioUpload" accept="audio/*"
        >
          <el-button :icon="Microphone" size="small" link title="音频">音频</el-button>
        </el-upload>
      </div>
      <el-button
        type="primary" :loading="sending"
        :disabled="!canSend" :icon="Promotion"
        @click="send"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Picture, VideoCamera, Microphone, Close, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  text: { type: String, default: '' },
  attachments: { type: Array, default: () => [] },
  sending: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入消息... (Ctrl+Enter 发送)' }
})
const emit = defineEmits([
  'update:text', 'update:attachments', 'send',
  'attach-image', 'attach-video', 'attach-audio'
])

const canSend = computed(() => props.text.trim().length > 0 || props.attachments.length > 0)

function onImageUpload(file) {
  if (!file?.raw) return
  emit('update:attachments', [...props.attachments, {
    type: 'image', name: file.name, file: file.raw
  }])
}
function onVideoUpload(file) {
  if (!file?.raw) return
  emit('update:attachments', [...props.attachments, {
    type: 'video', name: file.name, file: file.raw
  }])
}
function onAudioUpload(file) {
  if (!file?.raw) return
  emit('update:attachments', [...props.attachments, {
    type: 'audio', name: file.name, file: file.raw
  }])
}
function removeAttachment(i) {
  const next = [...props.attachments]
  next.splice(i, 1)
  emit('update:attachments', next)
}
function send() {
  if (!canSend.value || props.sending) return
  emit('send')
}
</script>

<style scoped>
.chat-input {
  border-top: 1px solid #e2e8f0;
  padding: 16px 20px 20px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}
.attachment-row {
  display: flex; flex-wrap: wrap; gap: 6px;
  margin-bottom: 10px;
}
.attachment-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 10px;
  background: linear-gradient(135deg, #eff6ff 0%, #ede9fe 100%);
  border: 1px solid #c7d2fe;
  border-radius: 14px;
  font-size: 12px; color: #4338ca;
  font-weight: 500;
}
.att-name { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.input-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 10px;
}
.input-tools { display: flex; gap: 2px; }
.input-tools :deep(.el-button) {
  color: #64748b; font-weight: 500;
}
.input-tools :deep(.el-button:hover) {
  color: #6366f1; background: #eef2ff;
}
</style>
