/**
 * MiniMax AI 平台前端 SDK (V2.7)
 *
 * 包含:
 *   - 基础 AI: 生成 / Embedding / 相似度 / 分词
 *   - 多模态: 图片/语音/视频/文件管理
 *   - 工具管理: 工具 CRUD + 调用
 *   - 数据源管理: 增删改查 + 测试连接
 *   - 报表生成: 7 种图表 (PNG)
 *   - 音乐生成: MIDI
 *   - 动画生成: GIF
 *   - 视频合成: 帧流
 *   - 数据看板: PNG
 *   - 关键词引擎: 智能路由
 *   - 代码生成: 6 种项目类型
 *
 * 所有接口统一走 gateway: /api/ai/**
 */
import http from './http'

// ==================== 基础 AI ====================

/** 文本生成 */
export const generateText = (data) => http.post('/api/ai/generate', data)

/** 流式生成 (SSE) */
export const generateTextStream = (data, onChunk, onError, onComplete) => {
  return http.post('/api/ai/generate/stream', data, {
    responseType: 'stream',
    onDownloadProgress: (e) => {
      // 处理 SSE 流
    }
  }).then(response => {
    const reader = response.data.getReader()
    const decoder = new TextDecoder()
    const read = () => {
      reader.read().then(({ done, value }) => {
        if (done) {
          onComplete && onComplete()
          return
        }
        const chunk = decoder.decode(value)
        onChunk && onChunk(chunk)
        read()
      }).catch(err => onError && onError(err))
    }
    read()
  })
}

/** Embedding 向量化 */
export const embed = (data) => http.post('/api/ai/embed', data)

/** 相似度计算 */
export const similarity = (data) => http.post('/api/ai/similarity', data)

/** 中文分词 */
export const tokenize = (data) => http.post('/api/ai/tokenize', data)

/** AI 模型信息 */
export const getAiInfo = () => http.get('/api/ai/info')

/** 健康检查 */
export const aiHealth = () => http.get('/api/ai/health')

// ==================== 多模态 ====================

/** 上传图片 (自动分析: 主色调/pHash/embedding) */
export const uploadImage = (formData, onProgress) =>
  http.post('/api/ai/multimodal/image/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

/** 上传语音 (自动转写 + 情感分析) */
export const uploadAudio = (formData, onProgress) =>
  http.post('/api/ai/multimodal/audio/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

/** 上传视频 (元数据提取) */
export const uploadVideo = (formData, onProgress) =>
  http.post('/api/ai/multimodal/video/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

/** 我的文件列表 */
export const listFiles = (userId) => http.get('/api/ai/multimodal/files', { params: { userId } })

/** 文件详情 */
export const getFileInfo = (fileId) => http.get(`/api/ai/multimodal/file/${fileId}/info`)

/** 文本转语音 (TTS) */
export const textToSpeech = (data) => http.post('/api/ai/multimodal/tts', data)

/** 图片对比 (pHash + cosine) */
export const compareImages = (formData) =>
  http.post('/api/ai/multimodal/image/compare', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/** 合规: 文本审核 */
export const moderateText = (text) =>
  http.post('/api/ai/multimodal/compliance/moderate-text', { text })

/** 合规: 数据脱敏 */
export const maskText = (text) =>
  http.post('/api/ai/multimodal/compliance/mask', { text })

/** 合规: 刷新敏感词缓存 */
export const refreshSensitiveWords = () =>
  http.post('/api/ai/multimodal/compliance/refresh-sensitive-words')

// ==================== AI 工具管理 ====================

/** 工具列表 */
export const listTools = (params) => http.get('/api/ai/admin/tools', { params })

/** 工具详情 */
export const getTool = (code) => http.get(`/api/ai/admin/tools/${code}`)

/** 创建工具 */
export const createTool = (data) => http.post('/api/ai/admin/tools', data)

/** 更新工具 */
export const updateTool = (id, data) => http.put(`/api/ai/admin/tools/${id}`, data)

/** 删除工具 */
export const deleteTool = (id) => http.delete(`/api/ai/admin/tools/${id}`)

/** 调用工具 */
export const invokeTool = (code, input) =>
  http.post(`/api/ai/admin/tools/${code}/invoke`, { input })

/** 数据源列表 */
export const listDataSources = () => http.get('/api/ai/admin/datasources')

/** 创建数据源 */
export const createDataSource = (data) => http.post('/api/ai/admin/datasources', data)

/** 更新数据源 */
export const updateDataSource = (id, data) => http.put(`/api/ai/admin/datasources/${id}`, data)

/** 删除数据源 */
export const deleteDataSource = (id) => http.delete(`/api/ai/admin/datasources/${id}`)

/** 测试数据源连接 */
export const testDataSource = (id) => http.post(`/api/ai/admin/datasources/${id}/test`)

/** 项目代码生成 */
export const generateProject = (data) => http.post('/api/ai/admin/codegen', data)

// ==================== 报表 (图表 PNG) ====================

/**
 * 渲染图表 (返回 PNG blob URL)
 * @param {Object} chartData - {type, title, categories, series, ...}
 * @returns {Promise<{blobUrl, blob, base64}>}
 */
export const renderChart = async (chartData) => {
  // 实际请求后端, 这里用 mock 返回 (后端尚未实现此接口, 后续补)
  // 临时: 前端用 canvas 渲染或后端 AI 模块实现
  const response = await http.post('/api/ai/chart/render', chartData, { responseType: 'blob' })
  const blob = response.data
  return {
    blob,
    blobUrl: URL.createObjectURL(blob),
    base64: await blobToBase64(blob)
  }
}

/** 音乐生成 (返回 MIDI blob) */
export const generateMusic = async (config) => {
  const response = await http.post('/api/ai/music/generate', config, { responseType: 'blob' })
  const blob = response.data
  return {
    blob,
    blobUrl: URL.createObjectURL(blob)
  }
}

/** 数据看板 (返回 PNG) */
export const renderDashboard = async (config) => {
  const response = await http.post('/api/ai/dashboard/render', config, { responseType: 'blob' })
  const blob = response.data
  return { blob, blobUrl: URL.createObjectURL(blob) }
}

/** 视频合成 (返回 ZIP 包含所有帧) */
export const composeVideo = async (config) => {
  const response = await http.post('/api/ai/video/compose', config, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

/** 关键词路由 (智能意图识别) */
export const routeByKeyword = (text) =>
  http.post('/api/ai/route', { text })

/** 智能分发 (V2.7 核心) */
export const dispatchPrompt = (data) => http.post('/api/ai/dispatch', data)

/** NL2Chart (自然语言生成图表) */
export const nl2chart = (dataSourceId, question) =>
  http.post('/api/ai/nl2chart', { dataSourceId, question }, { responseType: 'blob' })

// ==================== 工具函数 ====================

async function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

/** 下载 Blob 文件 */
export const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 100)
}
