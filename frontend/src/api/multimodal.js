// 多模态 API (V2.7) - 重新指向 /api/ai/multimodal
// 保留旧路径用于向后兼容
import http from './http'

// 旧版接口 (V2.6 前) - 标记为 deprecated
export const uploadImageOld = (formData) => http.post('/multimodal/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' },
})

export const describeImageOld = (imageBase64, mimeType, prompt) =>
  http.post('/multimodal/describe', { imageBase64, mimeType, prompt })

export const getMultimodalInfoOld = () => http.get('/multimodal/info')

// 新版接口 (V2.7+) - 走 /api/ai/multimodal (含 7 种图表/AI 分析)
export {
  uploadImage,
  uploadAudio,
  uploadVideo,
  listFiles,
  getFileInfo,
  textToSpeech,
  compareImages,
  moderateText,
  maskText,
  refreshSensitiveWords
} from './ai'
