# V3.6.0 chat TTS 语音播报（Web Speech API + 移动端 P0）

## 1. V3.5.99 之后

V3.5.99 加了 STT（语音输入）。V3.6.0 配对加 **TTS（语音播报）** — 完整语音交互闭环。

## 2. V3.6.0 改

### 2.1 chat/Index.vue V3.6.0 加 TTS (874 → 1016 行)

V3.6.0 新增 7 项:
1. **TTS 播报** - Web Speech API `speechSynthesis` + `SpeechSynthesisUtterance`
2. **自动播报** - AI 答完 (`onDone`) 自动读
3. **手动播放** - ChatMessage 组件每条 AI 消息加 ▶️ 按钮
4. **ttsSpeaking 状态** - 播报中显示 "播报中..." Tag
5. **ttsSupported 检测** - Chrome/Edge/Safari 支持
6. **Markdown 清理** - 去除 HTML/markdown/链接/图片标签
7. **试音按钮** - 🔊 工具栏快速试听

```vue
<!-- V3.6.0+ 工具栏加 TTS 自动播报 -->
<div class="input-toolbar">
  <el-checkbox v-model="useStream">流式</el-checkbox>
  <el-checkbox v-model="useTools">{{ t('chat.tools') }}</el-checkbox>
  <el-checkbox v-model="useRag">RAG</el-checkbox>
  <el-checkbox v-model="autoSpeak">
    🔊 TTS
    <el-tag v-if="ttsSpeaking" type="success" effect="dark" class="tts-tag">
      播报中...
    </el-tag>
  </el-checkbox>
  <el-button
    v-if="ttsSupported && !autoSpeak"
    :icon="ttsSpeaking ? VideoPause : VideoPlay"
    size="small"
    plain
    @click="toggleTTSTest"
  />
</div>
```

```vue
<!-- ChatMessage 组件加播放按钮 -->
<el-button
  v-if="!streaming && content"
  text
  size="small"
  :type="ttsActive ? 'primary' : ''"
  @click="$emit('speak', content)"
  :title="ttsActive ? '停止播报' : '播放'"
>
  <el-icon><component :is="ttsActive ? VideoPause : VideoPlay" /></el-icon>
  {{ ttsActive ? '停止' : '播放' }}
</el-button>
```

### 2.2 TTS 核心逻辑 (V3.6.0)

```js
// === V3.6.0+ 语音播报 (TTS, Web Speech API speechSynthesis) ===
const ttsSupported = ref(false)
const ttsSpeaking = ref(false)
const autoSpeak = ref(false)
let ttsUtterance = null

function checkTTSSupport() {
  if (typeof window === 'undefined') return
  ttsSupported.value = 'speechSynthesis' in window
}

function speak(text) {
  if (!ttsSupported.value || !text) return
  // 停止之前的播报
  window.speechSynthesis.cancel()

  // 清理 markdown / HTML
  const cleanText = text
    .replace(/<[^>]+>/g, '')      // 去除 HTML
    .replace(/[*#_`>~\\-]+/g, '') // 去除 markdown
    .replace(/\\[(.+?)\\]\\(.+?\\)/g, '$1')  // 去除链接
    .replace(/!\\[(.+?)\\]\\(.+?\\)/g, '$1') // 去除图片
    .trim()

  if (!cleanText) return

  ttsUtterance = new SpeechSynthesisUtterance(cleanText)
  ttsUtterance.lang = 'zh-CN'
  ttsUtterance.rate = 1.0
  ttsUtterance.pitch = 1.0
  ttsUtterance.volume = 1.0

  ttsUtterance.onstart = () => { ttsSpeaking.value = true }
  ttsUtterance.onend = () => { ttsSpeaking.value = false }
  ttsUtterance.onerror = (e) => { ttsSpeaking.value = false }

  window.speechSynthesis.speak(ttsUtterance)
}

// onDone 时自动播报
onDone: () => {
  aiMsg.streaming = false
  streaming.value = false
  scrollToBottom()
  if (autoSpeak.value && aiMsg.content) {
    speak(aiMsg.content)
  }
}
```

### 2.3 ChatMessage.vue 加 TTS 按钮 + emit speak

```js
// ChatMessage.vue (V3.6.0+)
const emit = defineEmits(['openSource', 'retry', 'like', 'speak'])
const props = defineProps({
  ttsActive: { type: Boolean, default: false },
})
```

## 3. i18n (4 keys, V3.6.0)

```js
chat.tts: {
  title: '语音播报' / 'Text-to-Speech',
  auto: '自动播报' / 'Auto-speak',
  speaking: '播报中' / 'Speaking',
  unsupported: '当前浏览器不支持 TTS' / 'Browser does not support TTS',
}
```

## 4. 浏览器兼容性

| 浏览器 | STT | TTS | 备注 |
|--------|-----|-----|------|
| Chrome (Desktop) | ✅ | ✅ | `webkitSpeechRecognition` + `speechSynthesis` |
| Edge (Desktop) | ✅ | ✅ | `SpeechRecognition` + `speechSynthesis` |
| Safari (iOS 14.5+) | ✅ | ✅ | 移动端 P0 体验闭环 |
| Chrome (Android) | ✅ | ✅ | 移动端 P0 |
| Firefox | ⚠️ | ✅ | STT 不支持, TTS 支持 |

## 5. 完整语音交互闭环 (V3.5.99 + V3.6.0)

```
用户 → 麦克风按钮 (V3.5.99)
   ↓
Web Speech API 识别
   ↓
input.value += "你好"
   ↓
用户点发送 (Enter / 按钮)
   ↓
AI 流式响应
   ↓
onDone 完成
   ↓
autoSpeak ?  speak(content)  (V3.6.0+ 自动播报)
   ↓
speechSynthesis.speak()  → 扬声器
   ↓
用户听 AI 答
   ↓
不满意 ? 点 "🔄 重试" 或 "▶️ 播放" 重听
```

## 6. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| vite build 0 错 | ✅ 53s |
| **ci-check 9/9** | ✅ |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 7. 累计 55 个版本 (V3.5.46-V3.6.0)
