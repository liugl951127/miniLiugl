/**
 * V3.6.16+ 完整语音交互 composable
 * 链路: STT (说话) → AI 处理 → 打字机 (逐字显示) → TTS (同步播报)
 *
 * 5 大能力:
 * 1. MediaRecorder (WebRTC 录音)
 * 2. Web Speech API STT (流式识别)
 * 3. AudioContext + AnalyserNode (音量可视化)
 * 4. 打字机集成 (typewriterType 函数回调)
 * 5. Web Speech API TTS (流式播报)
 *
 * 状态机:
 * idle → listening (STT) → processing (AI) → speaking (TTS) → idle
 */
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

export function useSpeechCall() {
  // === 状态 ===
  const state = ref('idle')  // idle | listening | processing | speaking
  const isMuted = ref(false)
  const callDuration = ref(0)
  const volume = ref(0)
  const interimText = ref('')
  const finalText = ref('')
  const error = ref(null)

  // === 内部 ===
  let mediaStream = null
  let audioContext = null
  let analyser = null
  let recognition = null
  let synth = window.speechSynthesis
  let mediaRecorder = null
  let durationTimer = null
  let volumeTimer = null
  let callStartTime = 0

  // === 回调 (由 chat/Index 注入) ===
  let onRecognizedCallback = null  // STT 完成时调
  let onTypewriterCallback = null  // 打字机显示调
  let onSpeakCallback = null       // TTS 播报调

  function setCallbacks({ onRecognized, onTypewriter, onSpeak } = {}) {
    onRecognizedCallback = onRecognized
    onTypewriterCallback = onTypewriter
    onSpeakCallback = onSpeak
  }

  // === 浏览器支持检测 ===
  const isSupported = computed(() => {
    return !!(
      navigator.mediaDevices?.getUserMedia &&
      (window.SpeechRecognition || window.webkitSpeechRecognition) &&
      window.speechSynthesis
    )
  })

  // === 启动通话 ===
  async function start() {
    if (!isSupported.value) {
      ElMessage.warning('当前浏览器不支持语音通话 (需 Chrome/Edge/Safari)')
      return false
    }
    try {
      error.value = null

      // 1. 麦克风
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          sampleRate: 16000,
        },
      })

      // 2. 音频分析 (音量可视化)
      audioContext = new (window.AudioContext || window.webkitAudioContext)()
      const source = audioContext.createMediaStreamSource(mediaStream)
      analyser = audioContext.createAnalyser()
      analyser.fftSize = 256
      source.connect(analyser)

      // 3. STT (Web Speech API)
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition
      recognition = new SR()
      recognition.continuous = true
      recognition.interimResults = true
      recognition.lang = 'zh-CN'

      recognition.onresult = (event) => {
        let interim = ''
        let final = ''
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const r = event.results[i]
          if (r.isFinal) {
            final += r[0].transcript
          } else {
            interim += r[0].transcript
          }
        }
        if (interim) interimText.value = interim
        if (final) {
          finalText.value += final
          interimText.value = ''
          // V3.6.16+ 回调: 通知 chat 发送
          if (onRecognizedCallback) {
            onRecognizedCallback(final)
          }
        }
      }

      recognition.onerror = (event) => {
        error.value = `识别错误: ${event.error}`
        if (event.error === 'no-speech') {
          error.value = null
        } else if (event.error === 'not-allowed') {
          ElMessage.error('麦克风权限被拒绝')
          stop()
        }
      }

      recognition.onend = () => {
        if (state.value === 'listening' && !isMuted.value) {
          try { recognition.start() } catch (e) { /* already started */ }
        }
      }

      // 4. 状态切换
      state.value = 'listening'
      callStartTime = Date.now()
      recognition.start()
      startTimers()
      ElMessage.success('🎙️ 语音通话已启动 (STT 识别中)')
      return true
    } catch (e) {
      error.value = e.message
      ElMessage.error(`启动失败: ${e.message}`)
      cleanup()
      return false
    }
  }

  // === 停止通话 ===
  function stop() {
    state.value = 'idle'
    isMuted.value = false
    recognition?.stop()
    synth?.cancel()
    cleanup()
    console.log('[SpeechCall] 已停止监听')  // V3.6.21+ 不弹 toast, 避免误判
  }

  // === 静音切换 ===
  function toggleMute() {
    isMuted.value = !isMuted.value
    if (mediaStream) {
      mediaStream.getAudioTracks().forEach((t) => (t.enabled = !isMuted.value))
    }
    if (isMuted.value) {
      recognition?.stop()
      interimText.value = ''
    } else if (state.value === 'listening') {
      try { recognition?.start() } catch (e) { /* */ }
    }
  }

  // === V3.6.16+ 流式 TTS (边打字边播报) ===
  function speakStream(text, { onStart, onEnd } = {}) {
    if (!synth) return
    if (state.value === 'idle') return

    // 清理 markdown
    const clean = text
      .replace(/[*_`#>]/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/!\[([^\]]*)\]\([^)]+\)/g, '')
      .trim()
    if (!clean) return

    state.value = 'speaking'
    const u = new SpeechSynthesisUtterance(clean)
    u.lang = 'zh-CN'
    u.rate = 1.1
    u.pitch = 1.0
    u.volume = 0.9
    u.onstart = () => {
      if (onStart) onStart()
    }
    u.onend = () => {
      if (onEnd) onEnd()
      // 播完回到 listening
      if (state.value === 'speaking') {
        state.value = 'listening'
      }
    }
    u.onerror = (e) => {
      console.warn('[TTS] Error:', e)
      state.value = 'listening'
    }
    synth.speak(u)
  }

  // === 取消 TTS ===
  function cancelTTS() {
    synth?.cancel()
    if (state.value === 'speaking') {
      state.value = 'listening'
    }
  }

  // === 停止当前 AI 回复的 TTS 播报（用户主动打断）===
  function stopSpeaking() {
    cancelTTS()
  }

  // === 状态机: processing (AI 在生成) ===
  function setProcessing() {
    state.value = 'processing'
  }

  // === 状态机: listening (回到 STT 监听) ===
  function setListening() {
    if (state.value !== 'idle') {
      state.value = 'listening'
    }
  }

  // === 定时器 ===
  function startTimers() {
    durationTimer = setInterval(() => {
      callDuration.value = Math.floor((Date.now() - callStartTime) / 1000)
    }, 1000)
    volumeTimer = setInterval(() => {
      if (!analyser || isMuted.value) {
        volume.value = 0
        return
      }
      const data = new Uint8Array(analyser.frequencyBinCount)
      analyser.getByteFrequencyData(data)
      const sum = data.reduce((a, b) => a + b, 0)
      volume.value = Math.min(100, Math.floor((sum / data.length) * 1.5))
    }, 100)
  }

  // === 清理 ===
  function cleanup() {
    if (durationTimer) clearInterval(durationTimer)
    if (volumeTimer) clearInterval(volumeTimer)
    durationTimer = null
    volumeTimer = null
    mediaStream?.getTracks().forEach((t) => t.stop())
    mediaStream = null
    if (audioContext?.state !== 'closed') {
      audioContext?.close()
    }
    audioContext = null
    analyser = null
    mediaRecorder = null
    recognition = null
    callDuration.value = 0
    volume.value = 0
    interimText.value = ''
    finalText.value = ''
    state.value = 'idle'
  }

  // === 格式化 ===
  const callDurationFormatted = computed(() => {
    const m = Math.floor(callDuration.value / 60)
    const s = callDuration.value % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  })

  // === 状态 label ===
  const stateLabel = computed(() => {
    return {
      idle: '空闲',
      listening: '🎙️ 听你说',
      processing: '🤖 AI 处理中',
      speaking: '🔊 AI 播报中',
    }[state.value]
  })

  onUnmounted(() => {
    stop()
  })

  return {
    isSupported,
    state,
    stateLabel,
    isMuted,
    callDuration,
    callDurationFormatted,
    volume,
    interimText,
    finalText,
    error,
    setCallbacks,
    start,
    stop,
    toggleMute,
    speakStream,
    cancelTTS,
    stopSpeaking,
    setProcessing,
    setListening,
  }
}
