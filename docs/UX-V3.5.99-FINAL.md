# V3.5.99 chat 语音输入（Web Speech API + 移动端 P0）

## 1. V3.5.98 之后

V3.5.98 给 chat 加了 RAG 知识库 + Agent 模式切换。V3.5.99 继续:
**chat/Index 加 Web Speech API 语音输入** — 移动端 P0 体验。

## 2. V3.5.99 改

### 2.1 chat/Index.vue V3.5.99 加语音输入 (747 → 874 行)

V3.5.99 新增 8 项:
1. **Web Speech API 集成** - `window.SpeechRecognition` / `webkitSpeechRecognition`
2. **麦克风按钮** - `Microphone` icon, 80×80 大按钮 (移动端友好)
3. **录音状态** - `voiceRecording: boolean`, 红色 pulse 动画
4. **中间结果** - `voiceInterim: string`, 实时显示识别中
5. **最终结果** - `voiceResult: string`, 自动追加到 input
6. **错误处理** - `ElMessage.error` 显示识别失败原因
7. **浏览器支持检测** - `voiceSupported: boolean`, Chrome/Edge/Safari 支持
8. **语音状态面板** - 黄渐变背景, pulse 动画

```vue
<!-- V3.5.99+ 语音输入按钮 -->
<div class="input-row">
  <el-input v-model="input" type="textarea" :rows="4" class="input-textarea" />
  <el-button
    :icon="voiceRecording ? VideoPause : Microphone"
    :type="voiceRecording ? 'danger' : 'default'"
    :loading="voiceProcessing"
    @click="toggleVoice"
    size="large"
    circle
    class="voice-btn"
  />
</div>

<!-- 语音状态面板 -->
<transition name="slide-up">
  <div v-if="voiceRecording || voiceResult" class="voice-panel">
    <el-icon class="voice-icon" :class="{ recording: voiceRecording }">
      <Microphone />
    </el-icon>
    <div class="voice-content">
      <div v-if="voiceRecording" class="voice-status">
        <span class="voice-dot"></span>
        正在聆听... {{ voiceInterim }}
      </div>
      <div v-else-if="voiceResult" class="voice-result">
        ✓ 识别完成: {{ voiceResult }}
      </div>
    </div>
    <el-button text :icon="CircleClose" @click="resetVoice" size="small">关闭</el-button>
  </div>
</transition>
```

### 2.2 语音识别核心逻辑 (V3.5.99)

```js
// === V3.5.99+ 语音输入 (Web Speech API) ===
const voiceSupported = ref(false)
const voiceRecording = ref(false)
const voiceProcessing = ref(false)
const voiceResult = ref('')
const voiceInterim = ref('')
let voiceRecognition = null

function checkVoiceSupport() {
  if (typeof window === 'undefined') return
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  voiceSupported.value = !!SR
}

function initVoice() {
  if (!voiceSupported.value) return null
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  const recognition = new SR()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = false
  recognition.maxAlternatives = 1

  recognition.onstart = () => { voiceRecording.value = true; ... }
  recognition.onresult = (e) => {
    // 区分 interim / final
    for (let i = e.resultIndex; i < e.results.length; i++) {
      const transcript = e.results[i][0].transcript
      if (e.results[i].isFinal) {
        voiceResult.value = transcript.trim()
        input.value += (input.value ? ' ' : '') + transcript.trim()
      } else {
        voiceInterim.value += transcript
      }
    }
  }
  recognition.onerror = (e) => ElMessage.error(`语音识别失败: ${e.error}`)
  recognition.onend = () => { voiceRecording.value = false; ... }
  return recognition
}

async function toggleVoice() {
  if (!voiceSupported.value) {
    ElMessage.warning('当前浏览器不支持语音输入 (Chrome/Edge/Safari 支持)')
    return
  }
  if (voiceRecording.value) {
    voiceRecognition?.stop()
  } else {
    voiceRecognition = initVoice()
    voiceRecognition?.start()
  }
}
```

### 2.3 语音状态面板样式 (V3.5.99)

```scss
.voice-btn {
  height: 80px !important;
  width: 80px !important;
  font-size: 24px !important;
}

.voice-panel {
  margin-top: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 8px;
  border: 1px solid #f59e0b;
}

.voice-icon.recording {
  color: #dc2626;
  animation: pulse 1.5s infinite;
}

.voice-dot {
  width: 8px;
  height: 8px;
  background: #dc2626;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}
```

## 3. i18n (V3.5.99, 4 keys)

```js
chat.voice: {
  title: '语音输入' / 'Voice Input',
  unsupported: '当前浏览器不支持语音' / 'Browser does not support voice',
  failed: '语音识别失败' / 'Voice recognition failed',
  placeholder: '点击麦克风开始...' / 'Click mic to start...',
}
```

## 4. 浏览器兼容性

| 浏览器 | 支持 | 备注 |
|--------|------|------|
| Chrome (Desktop) | ✅ | `webkitSpeechRecognition` |
| Edge (Desktop) | ✅ | `SpeechRecognition` |
| Safari (iOS 14.5+) | ✅ | `webkitSpeechRecognition` |
| Chrome (Android) | ✅ | `webkitSpeechRecognition` |
| Firefox | ⚠️ | 不支持 (ElMessage 警告) |

## 5. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| vite build 0 错 | ✅ 54s |
| **ci-check 9/9** | ✅ |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 6. 累计 54 个版本 (V3.5.46-V3.5.99)
