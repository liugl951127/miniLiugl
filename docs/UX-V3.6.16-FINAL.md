# V3.6.16 完整语音交互链路 (STT + 打字机 + TTS)

## 1. V3.6.15 之后

V3.6.15 优化了 Vite dev 启动 (冷启动 1.5s, 缓存 < 1s)。V3.6.16 继续完整语音交互:
- **useSpeechCall 升级** (V3.6.16+, 247 → 311 行) — 状态机 + 5 大能力
- **chat/Index 集成打字机 + TTS** — 流式字符 + 流式播报同步
- **4 状态机** idle → listening → processing → speaking

## 2. V3.6.16 改

### 2.1 useSpeechCall 状态机 (V3.6.16+)

```
idle ⇄ listening (STT 识别) ⇄ processing (AI 生成) ⇄ speaking (TTS 播报)
```

**5 大能力**:
1. **MediaRecorder** (WebRTC 录音)
2. **Web Speech API STT** (流式识别, interimResults)
3. **AudioContext + AnalyserNode** (音量可视化 20 根音量条)
4. **Web Speech API TTS** (流式播报, speakStream)
5. **打字机集成** (typewriterType 回调)

**新增 API**:
- `setCallbacks({ onRecognized, onTypewriter, onSpeak })` — chat 注入回调
- `speakStream(text, { onStart, onEnd })` — 流式 TTS
- `cancelTTS()` / `stopSpeaking()` — 取消 TTS
- `setProcessing()` / `setListening()` — 状态机控制
- `state` ref (idle | listening | processing | speaking)
- `stateLabel` computed (🎙️ 听你说 / 🤖 AI 处理中 / 🔊 AI 播报中 / 空闲)

### 2.2 chat/Index 集成 (V3.6.16+, 1525 行)

**完整链路**:
```
用户说话 → STT (流式识别)
  → onRecognized(text) 填入输入框
  → 用户按回车 / 自动发送
  → sendMessageStream (流式)
    → typewriterType(aiMsg, fullText) 逐字显示
      → speakStream(fullText) 同步 TTS 播报
    → 播报完回到 listening
```

**关键代码**:
```js
// V3.6.16+ 语音交互链路
speechCall.setCallbacks({
  onRecognized: (text) => {
    inputMessage.value = text
    ElMessage.info(`识别: ${text.slice(0, 20)}...`)
  },
})

// V3.6.10+ 打字机 + V3.6.16+ TTS 同步
async function typewriterType(aiMsg, fullText) {
  if (!typewriterMode.value) {
    aiMsg.content = fullText
    if (speechCall.state.value !== 'idle') {
      speechCall.speakStream(fullText)
    }
    return
  }
  // ... 逐字显示 ...
  // 播完触发 TTS
  if (speechCall.state.value !== 'idle') {
    speechCall.speakStream(fullText)
  }
}

// V3.6.16+ 状态机
async function sendMessage() {
  if (speechCall.state.value !== 'idle') speechCall.setProcessing()
  // ... 流式处理 ...
}
```

### 2.3 状态颜色 (V3.6.16+)

```css
.call-dot.state-listening { background: #4ade80; }  /* 绿 - 听 */
.call-dot.state-processing { background: #f59e0b; }  /* 黄 - 处理 */
.call-dot.state-speaking { background: #3b82f6; }   /* 蓝 - 播 */
.call-dot.state-idle { background: #94a3b8; }       /* 灰 - 闲 */
```

UI 显示:
```
🎙️ 听你说 · 01:23
🤖 AI 处理中 · 01:25
🔊 AI 播报中 · 01:26
```

## 3. 验证

| 测试 | 结果 |
|------|------|
| useSpeechCall 状态机 | ✅ idle/listening/processing/speaking 4 态 |
| Vite 启动 | ✅ 1.5s |
| 21 路由 21/21 200 | ✅ |
| ci-check 11/11 | ✅ < 3s |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| frontend-error-check | ✅ 0 错误 0 警告 |
| vite build 0 错 | ✅ 1m 2s |

## 4. 累计 71 个版本 (V3.5.46-V3.6.16)
