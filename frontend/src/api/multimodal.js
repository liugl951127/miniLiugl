/**
 * @file multimodal API 调用层 (V7.1 合并)
 *
 * 对应后端:
 *   - minimax-model: ImageGenController (/api/v1/imagegen)
 *                     AudioController       (/api/v1/audio)
 *                     VideoGenController    (/api/v1/video)
 *                     MusicGenController    (/api/v1/music)
 *   - minimax-agent:  DocumentQAController (/api/v1/agent/doc)
 *   - minimax-ai:     MultimodalController  (/api/v1/ai/multimodal)
 *   - minimax-ai:     OnnxMultimodalController  (/api/v1/multimodal) ← V7.1 新增 (本地 ONNX)
 *
 * V6.8.1 fix: FormData 不手动设 Content-Type (Axios 自动加 boundary)
 *             docAsk 改用 URLSearchParams (application/x-www-form-urlencoded)
 */
import http, { http as httpNamed } from './http'

// ==================== 图片生成 ====================

/** 文生图 - 调用 minimax-model ImageGenController */
export const imageGenGenerate = (data) => {
  // data: { prompt, model?, size?, n? }
  return http.post('/imagegen/generate', data)
}

/** 列出可用文生图模型 */
export const imageGenModels = () => http.get('/imagegen/models')

// ==================== 图片理解 ====================

/** 图片理解 - 调用 minimax-ai MultimodalController /vision */
export const visionAnalyze = (imageUrl, prompt, model) =>
  http.post('/ai/multimodal/vision', { imageUrl, prompt, model })

// ==================== TTS / ASR ====================

/** TTS - 调用 minimax-model AudioController */
export const audioTts = (data) => {
  // data: { text, voice?, speed?, pitch? }
  return http.post('/audio/tts/synthesize', data)
}

/** 列出可用 TTS 声音 */
export const listVoices = () => http.get('/audio/voices')

/** 别名: 列出 TTS 声音 (兼容 views/multimodal/Index.vue) */
export const audioTtsVoices = listVoices

/** ASR - 语音转文本 */
export const audioAsr = (formData) => http.post('/audio/asr/recognize', formData)

// ==================== 视频生成 ====================

/** 文生视频 - 调用 minimax-model VideoGenController */
export const videoGenerate = (data) => {
  // data: { prompt, model?, duration? }
  return http.post('/video/generate', data)
}

/** 列出可用视频生成模型 (兼容 views/multimodal/Index.vue) */
export const videoModels = () => http.get('/video/models')

/** 视频理解 - 调用 minimax-ai */
export const videoUnderstand = (videoUrl, prompt) =>
  http.post('/ai/multimodal/video/understand', { videoUrl, prompt })

// ==================== 音乐生成 ====================

/** 音乐生成 - 调用 minimax-model MusicGenController */
export const musicGenerate = (data) => {
  // data: { prompt, lyrics?, model? }
  return http.post('/music/generate', data)
}

/** 列出可用音乐生成模型 (兼容 views/multimodal/Index.vue) */
export const musicModels = () => http.get('/music/models')

// ==================== 文档问答 ====================

/** 上传文档 - minimax-agent DocumentQAController
 *  V6.8.1 fix: 不设 Content-Type */
export const docUpload = (formData) =>
  http.post('/agent/doc/upload', formData)

/** 文档提问 - minimax-agent DocumentQAController
 *  V6.8.1 fix: 用 URLSearchParams 发送 x-www-form-urlencoded */
export const docAsk = (docId, question) => {
  const params = new URLSearchParams({ docId, question })
  return http.post('/agent/doc/ask', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}

/** 上传并提问（单次请求）- minimax-agent DocumentQAController
 *  V6.8.1 fix: 不设 Content-Type */
export const docUploadAndAsk = (formData) =>
  http.post('/agent/doc/upload-and-ask', formData)

// ==================== 通用上传 ====================

/** 上传图片 (自动分析: 主色调/pHash/embedding)
 *  V6.8.1 fix: 不设 Content-Type */
export const uploadImage = (formData, onProgress) =>
  http.post('/ai/multimodal/image/upload', formData, {
    onUploadProgress: onProgress
  })

/** 上传音频 */
export const uploadAudio = (formData, onProgress) =>
  http.post('/ai/multimodal/audio/upload', formData, {
    onUploadProgress: onProgress
  })

/** 上传视频 */
export const uploadVideo = (formData, onProgress) =>
  http.post('/ai/multimodal/video/upload', formData, {
    onUploadProgress: onProgress
  })

/** 我的文件列表 (userId 由网关从 JWT 自动注入 X-User-Id) */
export const listFiles = () => http.get('/ai/multimodal/files')

/** 文件详情 */
export const getFileInfo = (fileId) => http.get(`/ai/multimodal/file/${fileId}/info`)

/** 文本审核 */
export const moderateText = (text) => http.post('/ai/multimodal/compliance/moderate-text', { text })

/** 数据脱敏 */
export const maskText = (text) => http.post('/ai/multimodal/compliance/mask', { text })

// ==================== V7.1 本地 ONNX 多模态 ====================
/**
 * minimax-ai OnnxMultimodalController (/api/v1/multimodal)
 * 模型由 data/models/ 下 Git LFS 管理
 */
export const multimodalApi = {
  /** 3 个模型的就绪状态 */
  status() {
    return http.get('/multimodal/status')
  },

  /** 图片分类 (top-k) */
  classify(file, topK = 5) {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/classify?topK=' + topK, fd)
  },

  /** 目标检测 */
  detect(file) {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/detect', fd)
  },

  /** 图片 embedding */
  encodeImage(file) {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/encode-image', fd)
  },

  /** 文本 embedding */
  encodeText(text) {
    return http.post('/multimodal/encode-text', { text })
  },

  /** 文图相似度 */
  similarity(file, text) {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/text-image-similarity?text=' + encodeURIComponent(text), fd)
  },

  /** 语音转文字 (Whisper-tiny) */
  transcribe(file, lang = 'zh') {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/transcribe?lang=' + lang, fd)
  },

  /** 语音活动检测 (Silero VAD) */
  vad(file) {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/vad', fd)
  },

  /** 视频智能分析 (复用 ResNet50 + Whisper) */
  analyzeVideo(file) {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/multimodal/analyze-video', fd)
  }
}
