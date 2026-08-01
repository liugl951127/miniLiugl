/**
 * V3.6.9+ 语音通话 composable
 * WebRTC MediaRecorder + Web Speech API 双向流
 * - 录音 getUserMedia
 * - 流式 STT (Web Speech API, 实时识别)
 * - 收到 AI 回复后流式 TTS (SpeechSynthesis)
 * - 可视化 (AudioContext + AnalyserNode)
 */
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

export function useSpeechCall() {
  // 状态
  const isCallActive = ref(false)
  const isRecording = ref(false)
  const isMuted = ref(false)
  const callDuration = ref(0)
  const volume = ref(0)
  const interimText = ref('')
  const finalText = ref('')
  const error = ref(null)

  // 内部
  let mediaRecorder = null
  let mediaStream = null
  let audioContext = null
  let analyser = null
  let recognition = null
  let synth = window.speechSynthesis
  let durationTimer = null
  let volumeTimer = null
  let callStartTime = 0

  const isSupported = computed(() => {
    return !!(
      navigator.mediaDevices?.getUserMedia &&
      (window.SpeechRecognition || window.webkitSpeechRecognition) &&
      window.speechSynthesis
    )
  })

  // 启动
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

      // 3. 录音器 (暂存, 流式 STT 不需要录音)
      mediaRecorder = new MediaRecorder(mediaStream, {
        mimeType: 'audio/webm',
      })
      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          // 流式 STT 已用 recognition, 这里只存档
          // TODO: 上传到后端
        }
      }

      // 4. STT (Web Speech API)
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
        }
      }

      recognition.onerror = (event) => {
        error.value = `识别错误: ${event.error}`
        if (event.error === 'no-speech') {
          // 静默, 不打扰
          error.value = null
        } else if (event.error === 'not-allowed') {
          ElMessage.error('麦克风权限被拒绝')
          stop()
        }
      }

      recognition.onend = () => {
        // 浏览器自动结束 (10s 静默), 重启
        if (isCallActive.value && !isMuted.value) {
          try { recognition.start() } catch (e) { /* already started */ }
        }
      }

      // 5. 启动
      isCallActive.value = true
      callStartTime = Date.now()
      recognition.start()
      startTimers()
      ElMessage.success('🎙️ 语音通话已启动')
      return true
    } catch (e) {
      error.value = e.message
      ElMessage.error(`启动失败: ${e.message}`)
      cleanup()
      return false
    }
  }

  // 停止
  function stop() {
    isCallActive.value = false
    isRecording.value = false
    recognition?.stop()
    synth?.cancel()
    cleanup()
    ElMessage.info('📞 通话结束')
  }

  // 静音切换
  function toggleMute() {
    isMuted.value = !isMuted.value
    if (mediaStream) {
      mediaStream.getAudioTracks().forEach((t) => (t.enabled = !isMuted.value))
    }
    if (isMuted.value) {
      recognition?.stop()
      interimText.value = ''
    } else if (isCallActive.value) {
      try { recognition?.start() } catch (e) { /* */ }
    }
  }

  // 流式 TTS (收到 AI 回复时)
  function speakChunk(text) {
    if (!synth || !isCallActive.value) return
    // 清理 markdown
    const clean = text
      .replace(/[*_`#>]/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/!\[([^\]]*)\]\([^)]+\)/g, '')
      .trim()
    if (!clean) return
    const u = new SpeechSynthesisUtterance(clean)
    u.lang = 'zh-CN'
    u.rate = 1.0
    u.pitch = 1.0
    u.onend = () => {
      // 续接下一段
    }
    synth.speak(u)
  }

  // 定时器
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

  // 清理
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
  }

  // 格式化
  const callDurationFormatted = computed(() => {
    const m = Math.floor(callDuration.value / 60)
    const s = callDuration.value % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  })

  onUnmounted(() => {
    stop()
  })

  return {
    isSupported,
    isCallActive,
    isRecording,
    isMuted,
    callDuration,
    callDurationFormatted,
    volume,
    interimText,
    finalText,
    error,
    start,
    stop,
    toggleMute,
    speakChunk,
  }
}
