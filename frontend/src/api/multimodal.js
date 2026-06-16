// 多模态 API
import http from './http'

export const uploadImage = (formData) => http.post('/multimodal/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' },
})

export const describeImage = (imageBase64, mimeType, prompt) =>
  http.post('/multimodal/describe', { imageBase64, mimeType, prompt })

export const getMultimodalInfo = () => http.get('/multimodal/info')
