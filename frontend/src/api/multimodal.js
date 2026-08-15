/**
 * @file multimodal API 调用层 (V6.8.1)
 *
 * 对应后端:
 *   - minimax-model: ImageGenController (/api/v1/imagegen)
 *                     AudioController       (/api/v1/audio)
 *                     VideoGenController    (/api/v1/video)
 *                     MusicGenController    (/api/v1/music)
 *   - minimax-agent:  DocumentQAController (/api/v1/agent/doc)
 *   - minimax-ai:     MultimodalController  (/api/v1/ai/multimodal)
 *
 * V6.8.1 fix: FormData 不手动设 Content-Type (Axios 自动加 boundary)
 *             docAsk 改用 URLSearchParams (application/x-www-form-urlencoded)
 */
import http from './http'

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

/** 列出可用 TTS 音色 */
export const audioTtsVoices = () => http.get('/audio/tts/voices')

/** ASR - 调用 minimax-model AudioController
 *  V6.8.1 fix: 不设 Content-Type，Axios 自动用 multipart/form-data + boundary */
export const audioAsr = (formData) =>
  http.post('/audio/asr/transcribe', formData)

/** 列出可用 ASR 模型 */
export const audioAsrModels = () => http.get('/audio/asr/models')

// ==================== 视频生成 ====================

/** 列出可用视频模型 */
export const videoModels = () => http.get('/video/models')

/** 文生视频 - minimax-model VideoGenController
 *  V6.8.1 fix: 不设 Content-Type */
export const videoGenerate = (formData) =>
  http.post('/video/generate', formData)

/** 图生视频 - minimax-model VideoGenController
 *  V6.8.1 fix: 不设 Content-Type */
export const videoI2V = (formData) =>
  http.post('/video/i2v', formData)

// ==================== 音乐生成 ====================

/** 列出可用音乐模型 */
export const musicModels = () => http.get('/music/models')

/** 文生音乐 - minimax-model MusicGenController */
export const musicGenerate = (data) => {
  // data: { prompt, lyrics?, model? }
  return http.post('/music/generate', data)
}

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

/** 上传音频
 *  V6.8.1 fix: 不设 Content-Type */
export const uploadAudio = (formData, onProgress) =>
  http.post('/ai/multimodal/audio/upload', formData, {
    onUploadProgress: onProgress
  })

/** 上传视频
 *  V6.8.1 fix: 不设 Content-Type */
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
