<!-- @file chat/Stream.vue - 流式对话 V6.8 -->
<template>
  <div class="stream-page">
    <div class="stream-header">
      <h2>双向流式对话</h2>
      <el-tag type="success">SSE Streaming</el-tag>
    </div>
    <el-card class="stream-card">
      <div ref="outputRef" class="stream-output">
        <div v-for="(line, i) in outputLines" :key="i" :class="['line', line.type]">{{ line.text }}</div>
        <span v-if="streaming" class="cursor">▊</span>
      </div>
      <el-divider />
      <div class="input-row">
        <el-input v-model="input" placeholder="输入消息，Enter 发送" :disabled="streaming" @keydown.enter="send" />
        <el-button type="primary" :loading="streaming" @click="send" style="margin-left:8px">发送</el-button>
        <el-button @click="outputLines = []">清空</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMessageStream } from '@/api/session'

const input = ref('')
const outputLines = ref([])
const streaming = ref(false)
const outputRef = ref(null)

async function send() {
  if (!input.value.trim() || streaming.value) return
  const text = input.value.trim()
  input.value = ''
  outputLines.value.push({ type: 'user', text: '> ' + text })
  streaming.value = true
  await nextTick()
  scrollBottom()

  try {
    await sendMessageStream('default', { content: text }, (chunk) => {
      if (chunk === '[DONE]') return
      const last = outputLines.value[outputLines.value.length - 1]
      if (last?.type === 'ai') { last.text += chunk }
      else outputLines.value.push({ type: 'ai', text: chunk })
      scrollBottom()
    })
  } catch (e) {
    outputLines.value.push({ type: 'error', text: '错误: ' + (e.message || '') })
  } finally {
    streaming.value = false
  }
}

function scrollBottom() {
  nextTick(() => {
    if (outputRef.value) outputRef.value.scrollTop = outputRef.value.scrollHeight
  })
}
</script>

<style lang="scss" scoped>
.stream-page { max-width: 800px; margin: 0 auto; }
.stream-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; h2 { margin: 0; font-size: 16px; } }
.stream-card { padding: 0; }
.stream-output {
  min-height: 400px; max-height: 60vh; overflow-y: auto; padding: 16px;
  background: #0f172a; color: #e2e8f0; font-family: 'Courier New', monospace; font-size: 13px;
}
.line { margin-bottom: 6px; white-space: pre-wrap; &.user { color: #60a5fa; } &.ai { color: #4ade80; } &.error { color: #f87171; } }
.cursor { animation: blink 1s infinite; }
@keyframes blink { 0%,100% { opacity:1 } 50% { opacity:0 } }
.input-row { display: flex; padding: 12px; }
</style>
