/**
 * Liugl-AI AI 平台前端 SDK (V2.7)
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
export const generateText = (data) => http.post('/ai/generate', data)

/** 流式生成 (SSE) */
export const generateTextStream = (data, onChunk, onError, onComplete) => {
  return http.post('/ai/generate/stream', data, {
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
export const embed = (data) => http.post('/ai/embed', data)

/** 相似度计算 */
export const similarity = (data) => http.post('/ai/similarity', data)

/** 中文分词 */
export const tokenize = (data) => http.post('/ai/tokenize', data)

/** AI 模型信息 */
export const getAiInfo = () => http.get('/ai/info')

/** 健康检查 */
export const aiHealth = () => http.get('/ai/health')

// ==================== 多模态 ====================

/** 上传图片 (自动分析: 主色调/pHash/embedding) */
export const uploadImage = (formData, onProgress) =>
  http.post('/ai/multimodal/image/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

/** 上传语音 (自动转写 + 情感分析) */
export const uploadAudio = (formData, onProgress) =>
  http.post('/ai/multimodal/audio/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

/** 上传视频 (元数据提取) */
export const uploadVideo = (formData, onProgress) =>
  http.post('/ai/multimodal/video/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

/** 我的文件列表 */
export const listFiles = (userId) => http.get('/ai/multimodal/files', { params: { userId } })

/** 文件详情 */
export const getFileInfo = (fileId) => http.get(`/ai/multimodal/file/${fileId}/info`)

/** 文本转语音 (TTS) */
export const textToSpeech = (data) => http.post('/ai/multimodal/tts', data)

/** 图片对比 (pHash + cosine) */
export const compareImages = (formData) =>
  http.post('/ai/multimodal/image/compare', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/** 合规: 文本审核 */
export const moderateText = (text) =>
  http.post('/ai/multimodal/compliance/moderate-text', { text })

/** 合规: 数据脱敏 */
export const maskText = (text) =>
  http.post('/ai/multimodal/compliance/mask', { text })

/** 合规: 刷新敏感词缓存 */
export const refreshSensitiveWords = () =>
  http.post('/ai/multimodal/compliance/refresh-sensitive-words')

// ==================== AI 工具管理 ====================

/** 工具列表 */
export const listTools = (params) => http.get('/ai/admin/tools', { params })

/** 工具详情 */
export const getTool = (code) => http.get(`/ai/admin/tools/${code}`)

/** 创建工具 */
export const createTool = (data) => http.post('/ai/admin/tools', data)

/** 更新工具 */
export const updateTool = (id, data) => http.put(`/ai/admin/tools/${id}`, data)

/** 删除工具 */
export const deleteTool = (id) => http.delete(`/ai/admin/tools/${id}`)

/** 调用工具 */
export const invokeTool = (code, input) =>
  http.post(`/ai/admin/tools/${code}/invoke`, { input })

/** 数据源列表 */
export const listDataSources = () => http.get('/ai/admin/datasources')

/** 创建数据源 */
export const createDataSource = (data) => http.post('/ai/admin/datasources', data)

/** 更新数据源 */
export const updateDataSource = (id, data) => http.put(`/ai/admin/datasources/${id}`, data)

/** 删除数据源 */
export const deleteDataSource = (id) => http.delete(`/ai/admin/datasources/${id}`)

/** 测试数据源连接 */
export const testDataSource = (id) => http.post(`/ai/admin/datasources/${id}/test`)

/** 项目代码生成 */
export const generateProject = (data) => http.post('/ai/admin/codegen', data)

// ==================== 报表 (图表 PNG) ====================

/**
 * 渲染图表 (返回 PNG blob URL)
 * @param {Object} chartData - {type, title, categories, series, ...}
 * @returns {Promise<{blobUrl, blob, base64}>}
 */
export const renderChart = async (chartData) => {
  // 实际请求后端, 这里用 mock 返回 (后端尚未实现此接口, 后续补)
  // 临时: 前端用 canvas 渲染或后端 AI 模块实现
  const response = await http.post('/ai/chart/render', chartData, { responseType: 'blob' })
  const blob = response.data
  return {
    blob,
    blobUrl: URL.createObjectURL(blob),
    base64: await blobToBase64(blob)
  }
}

/** 音乐生成 (返回 MIDI blob) */
export const generateMusic = async (config) => {
  const response = await http.post('/ai/music/generate', config, { responseType: 'blob' })
  const blob = response.data
  return {
    blob,
    blobUrl: URL.createObjectURL(blob)
  }
}

/** 数据看板 (返回 PNG) */
export const renderDashboard = async (config) => {
  const response = await http.post('/ai/dashboard/render', config, { responseType: 'blob' })
  const blob = response.data
  return { blob, blobUrl: URL.createObjectURL(blob) }
}

/** 视频合成 (返回 ZIP 包含所有帧) */
export const composeVideo = async (config) => {
  const response = await http.post('/ai/video/compose', config, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

/** 关键词路由 (智能意图识别) */
export const routeByKeyword = (text) =>
  http.post('/ai/route', { text })

/** 智能分发 (V2.7 核心) */
export const dispatchPrompt = (data) => http.post('/ai/dispatch', data)

/** NL2Chart (自然语言生成图表) */
export const nl2chart = (dataSourceId, question) =>
  http.post('/ai/nl2chart', { dataSourceId, question }, { responseType: 'blob' })

/** AI 工作流 (DAG) */
export const executeWorkflow = (workflow) => http.post('/ai/workflow/execute', workflow)
export const validateWorkflow = (workflow) => http.post('/ai/workflow/validate', workflow)

/** 训练可视化 */
export const startTraining = (config) => http.post('/ai/training/start', config)
export const demoTraining = () => http.post('/ai/training/demo')
export const listTrainingTasks = () => http.get('/ai/training/tasks')
export const getTrainingTask = (id) => http.get(`/ai/training/tasks/${id}`)
export const getTrainingHistory = (id) => http.get(`/ai/training/tasks/${id}/history`)
export const deleteTrainingTask = (id) => http.delete(`/ai/training/tasks/${id}`)

/** AIGC 图片生成 */
export const generateImage = (req) => http.post('/ai/image/generate', req)
export const listImageTypes = () => http.post('/ai/image/types')
export const inferImageType = (prompt) => http.get('/ai/image/infer', { params: { prompt } })

/** 视频流式生成 (SSE) */
export const listVideoStreams = () => http.get('/ai/video/stream/list')
export const getVideoStream = (id) => http.get(`/ai/video/stream/${id}`)
export const cancelVideoStream = (id) => http.post(`/ai/video/stream/cancel/${id}`)

/** 权限 (V2.7.9) */
export const getMyPermissions = () => http.get('/ai/permission/me')
export const listAllRoles = () => http.get('/ai/permission/roles')
export const checkPermissions = (role, permissions) => http.post('/ai/permission/check', { role, permissions })

/** 音乐流式生成 (V2.8.1) */
export const listMusicStreams = () => http.get('/ai/music/stream/list')
export const getMusicStream = (id) => http.get(`/ai/music/stream/${id}`)
export const cancelMusicStream = (id) => http.post(`/ai/music/stream/cancel/${id}`)

// ============== V2.8.3 新工具 SDK ==============
// 后端端点: POST /api/ai/admin/tools/{code}/invoke

/** 文本分析 (摘要/情感/实体/关键词) */
export const analyzeText = (req) => http.post('/ai/admin/tools/text.analyze/invoke', req)

/** 视觉分析 (颜色/风格/相似度) */
export const analyzeVision = (req) => http.post('/ai/admin/tools/vision.analyze/invoke', req)

/** 音频分析 (音量/频谱/情绪) */
export const analyzeAudio = (req) => http.post('/ai/admin/tools/audio.analyze/invoke', req)

/** 文件转换 (JSON/YAML/CSV/Base64) */
export const convertFile = (req) => http.post('/ai/admin/tools/file.convert/invoke', req)

/** 相关性分析 (Pearson/Spearman) */
export const analyzeCorrelation = (req) => http.post('/ai/admin/tools/data.analyze.correlation/invoke', req)

/** 线性预测 (回归/移动平均/指数平滑) */
export const predictData = (req) => http.post('/ai/admin/tools/data.predict.linear/invoke', req)

/** 时间工具 (格式/计算/时区) */
export const timeConvert = (req) => http.post('/ai/admin/tools/time.convert/invoke', req)

/** AIGC 图片生成 (via tool) */
export const generateImageTool = (req) => http.post('/ai/admin/tools/image.generate/invoke', req)

/** 图表生成 (via tool) */
export const generateChartTool = (req) => http.post('/ai/admin/tools/chart.generate/invoke', req)

/** 音乐生成 (via tool) */
export const generateMusicTool = (req) => http.post('/ai/admin/tools/music.generate/invoke', req)

// ============== V2.8.4 企业项目生成 ==============

/** Java 企业项目生成 (完整 ZIP, 返回 Base64) */
export const generateJavaProject = (req) => http.post('/ai/admin/tools/java.project.gen/invoke', req)

/** 直接下载项目 ZIP (浏览器) */
export const downloadJavaProject = (projectName = 'minimax-app', version = '1.0.0', type = 'spring-boot', packageName = '', database = 'mysql') => {
  const params = new URLSearchParams({ projectName, version, type, database })
  if (packageName) params.append('packageName', packageName)
  return `/ai/project/download?${params.toString()}`
}

/** 解码 Base64 ZIP 并下载 (JSON 接口) */
export const downloadJavaProjectFromBase64 = (base64, filename) => {
  const bytes = Uint8Array.from(atob(base64), c => c.charCodeAt(0))
  const blob = new Blob([bytes], { type: 'application/zip' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename || 'minimax-app.zip'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

/** 智能对话 (上下文感知) */
export const aiChatWithContext = (data) => http.post('/ai/dispatch', { ...data, withContext: true })

/** AI 会话 (V2.8.2) */
export const listAiSessions = (userId) => http.get('/ai/chat/sessions', { params: { userId } })
export const getAiSession = (id) => http.get(`/ai/chat/sessions/${id}`)
export const createAiSession = (data) => http.post('/ai/chat/sessions', data)
export const deleteAiSession = (id) => http.delete(`/ai/chat/sessions/${id}`)

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
