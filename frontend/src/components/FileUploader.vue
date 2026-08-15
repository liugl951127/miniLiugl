<template>
  <div
    class="file-uploader"
    :class="{ active: active, disabled }"
    @dragenter.prevent.stop="onDragEnter"
    @dragover.prevent.stop="onDragOver"
    @dragleave.prevent.stop="onDragLeave"
    @drop.prevent.stop="onDrop"
    @click="trigger"
  >
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      :multiple="multiple"
      :disabled="disabled"
      style="display:none"
      @change="onFileChange"
    />
    
    <el-icon class="upload-icon" :size="48">
      <UploadFilled />
    </el-icon>
    <h4 class="upload-title">{{ title || t('uploader.title') }}</h4>
    <p class="upload-desc">{{ description || t('uploader.description') }}</p>
    
    <div class="upload-actions">
      <el-button type="primary" :disabled="disabled" @click.stop="trigger">
        <el-icon><Plus /></el-icon>
        {{ t('uploader.selectFile') }}
      </el-button>
      <span class="upload-hint">{{ hint || t('uploader.hint', { size: maxSizeMB }) }}</span>
    </div>
    
    <div v-if="files.length" class="file-list" @click.stop>
      <div v-for="(f, i) in files" :key="i" class="file-item">
        <el-icon><Document /></el-icon>
        <div class="file-info">
          <div class="file-name">{{ f.name }}</div>
          <div class="file-size">{{ formatSize(f.size) }}</div>
        </div>
        <el-progress
          v-if="f.progress !== undefined"
          :percentage="f.progress"
          :stroke-width="4"
          class="file-progress"
        />
        <el-button link size="small" @click="remove(i)">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, Plus, Document, Close } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  accept: { type: String, default: '*' },
  multiple: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  maxSize: { type: Number, default: 10 * 1024 * 1024 },
  maxSizeMB: { type: Number, default: 10 },
  title: String,
  description: String,
  hint: String,
  autoUpload: { type: Boolean, default: false },
  uploadUrl: String
})

const emit = defineEmits(['update:modelValue', 'change', 'exceed', 'upload'])

const { t } = useI18n()
const fileInput = ref(null)
const active = ref(false)
const files = ref([...props.modelValue])

function trigger() {
  if (!props.disabled) fileInput.value?.click()
}

function onFileChange(e) {
  handleFiles(Array.from(e.target.files || []))
  e.target.value = ''
}

function onDragEnter() { active.value = true }
function onDragOver() { active.value = true }
function onDragLeave() { active.value = false }
function onDrop(e) {
  active.value = false
  handleFiles(Array.from(e.dataTransfer.files || []))
}

function handleFiles(list) {
  const valid = list.filter(f => {
    if (f.size > props.maxSize) {
      ElMessage.warning(t('uploader.tooBig', { name: f.name, size: props.maxSizeMB }))
      emit('exceed', f)
      return false
    }
    return true
  })
  files.value = props.multiple ? [...files.value, ...valid] : valid.slice(0, 1)
  emit('update:modelValue', files.value)
  emit('change', files.value)
  if (props.autoUpload) {
    valid.forEach(uploadFile)
  }
}

function uploadFile(file) {
  // V7.1: 真实上传 (支持 uploadUrl prop 或默认 /ai/multimodal/image/upload)
  file.progress = 0
  const url = props.uploadUrl || '/api/v1/ai/multimodal/image/upload'
  const formData = new FormData()
  formData.append('file', file)

  const xhr = new XMLHttpRequest()
  xhr.open('POST', url)

  // JWT 认证 token
  const token = localStorage.getItem('minimax_access_token')
  if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)

  xhr.upload.onprogress = (e) => {
    if (e.lengthComputable) {
      file.progress = Math.round((e.loaded / e.total) * 100)
    }
  }

  xhr.onload = () => {
    file.progress = 100
    if (xhr.status >= 200 && xhr.status < 300) {
      try {
        const res = JSON.parse(xhr.responseText)
        file.response = res.data || res
        file.url = res.data?.url || res.url || ''
      } catch {
        file.response = xhr.responseText
      }
      emit('upload', file)
    } else {
      file.error = '上传失败: ' + (xhr.statusText || xhr.status)
      ElMessage.error(file.error)
    }
  }

  xhr.onerror = () => {
    file.progress = 0
    file.error = '网络错误，上传失败'
    ElMessage.error('网络错误，上传失败')
  }

  xhr.send(formData)
}

function remove(i) {
  files.value.splice(i, 1)
  emit('update:modelValue', files.value)
  emit('change', files.value)
}

function formatSize(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}
</script>

<style lang="scss" scoped>
.file-uploader {
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: var(--el-fill-color-blank);
  
  &:hover:not(.disabled) {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
  
  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-7);
    transform: scale(1.01);
  }
  
  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.upload-icon {
  color: var(--el-color-primary);
  margin-bottom: 12px;
}

.upload-title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.upload-desc {
  margin: 0 0 16px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.upload-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.file-list {
  margin-top: 20px;
  text-align: left;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  margin-bottom: 6px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.file-progress {
  width: 80px;
}
</style>
