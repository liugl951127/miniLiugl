/**
 * @file ai API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-ai
 * 接口数: 93
 *
 *   POST   /api/v1/ai/generate
 *   POST   /api/v1/ai/embed
 *   POST   /api/v1/ai/similarity
 *   POST   /api/v1/ai/tokenize
 *   GET    /api/v1/ai/info
 *   GET    /api/v1/ai/health
 *   POST   /api/v1/ai/train
 *   POST   /api/v1/ai/video/compose
 *   ... 共 93 个
 */
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
export const generateText = (data) => {
  return http.post('/ai/generate', data);
}

/** 流式生成 (SSE) */
export const generateTextStream = (data, onChunk, onError, onComplete) => {
  return http.post('/ai/generate/stream', data, {
    responseType: 'stream',
    onDownloadProgress: (e) => {
      // 处理 SSE 流 (e.loaded/total 可用于进度)
      // 暂未使用, 留 hook
      void e
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
export const embed = (data) => {
  return http.post('/ai/embed', data);
}

/** 相似度计算 */
export const similarity = (data) => {
  return http.post('/ai/similarity', data);
}

/** 中文分词 */
export const tokenize = (data) => {
  return http.post('/ai/tokenize', data);
}

/** AI 模型信息 */
export const getAiInfo = () => {
  return http.get('/ai/info');
}

/** 健康检查 */
export const aiHealth = () => {
  return http.get('/ai/health');
}

// ==================== 多模态 ====================

/** 上传图片 (自动分析: 主色调/pHash/embedding)
 *  V6.8.1 fix: 不设 Content-Type，Axios 自动加 boundary */
export const uploadImage = (formData, onProgress) =>
  http.post('/ai/multimodal/image/upload', formData, {
    onUploadProgress: onProgress
  })

/** 上传语音 (自动转写 + 情感分析)
 *  V6.8.1 fix: 不设 Content-Type */
export const uploadAudio = (formData, onProgress) =>
  http.post('/ai/multimodal/audio/upload', formData, {
    onUploadProgress: onProgress
  })

/** 上传视频 (元数据提取)
 *  V6.8.1 fix: 不设 Content-Type */
export const uploadVideo = (formData, onProgress) =>
  http.post('/ai/multimodal/video/upload', formData, {
    onUploadProgress: onProgress
  })

/** 视频内容理解 (V7.3): 提取关键帧 + LLM 视觉分析
 * @param {FormData} formData - 包含 file/prompt/model 字段
 */
export const videoUnderstand = (formData, onProgress) =>
  http.post('/ai/multimodal/video/understand', formData, {
    onUploadProgress: onProgress
  })

/** 人脸分析 (V7.3): 截取摄像头帧 + LLM 人脸/内容分析
 * @param {string} imageBase64 - base64 编码的图片数据
 * @param {string} prompt - 分析提示词
 * @param {string} model - 使用的模型
 */
export const faceAnalyze = (imageBase64, prompt, model) =>
  http.post('/ai/multimodal/vision', { imageBase64, prompt, model })

/** 我的文件列表 (userId 由网关从 JWT 自动注入 X-User-Id) */
export const listFiles = () => http.get('/ai/multimodal/files')

/** 文件详情 */
export const getFileInfo = (fileId) => http.get(`/ai/multimodal/file/${fileId}/info`)

/** 文本转语音 (TTS) */
export const textToSpeech = (data) => {
  return http.post('/ai/multimodal/tts', data);
}

/** 图片对比 (pHash + cosine)
 *  V6.8.1 fix: 不设 Content-Type */
export const compareImages = (formData) =>
  http.post('/ai/multimodal/image/compare', formData)

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
export const createTool = (data) => {
  return http.post('/ai/admin/tools', data);
}

/** 更新工具 */
export const updateTool = (id, data) => http.put(`/ai/admin/tools/${id}`, data)

/** 删除工具 */
export const deleteTool = (id) => http.delete(`/ai/admin/tools/${id}`)

/** 调用工具 */
export const invokeTool = (code, input) =>
  http.post(`/ai/admin/tools/${code}/invoke`, { input })

/** 数据源列表 */
export const listDataSources = () => {
  return http.get('/ai/admin/datasources');
}

/** 创建数据源 */
export const createDataSource = (data) => {
  return http.post('/ai/admin/datasources', data);
}

/** 更新数据源 */
export const updateDataSource = (id, data) => http.put(`/ai/admin/datasources/${id}`, data)

/** 删除数据源 */
export const deleteDataSource = (id) => http.delete(`/ai/admin/datasources/${id}`)

/** 测试数据源连接 */
export const testDataSource = (id) => http.post(`/ai/admin/datasources/${id}/test`)

/** 项目代码生成 */
export const generateProject = (data) => {
  return http.post('/ai/admin/codegen', data);
}

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
export const dispatchPrompt = (data) => {
  return http.post('/ai/dispatch', data);
}

/** NL2Chart (自然语言生成图表) */
export const nl2chart = (dataSourceId, question) =>
  http.post('/ai/nl2chart', { dataSourceId, question }, { responseType: 'blob' })

/** AI 工作流 (DAG) */
export const executeWorkflow = (workflow) => {
  return http.post('/ai/workflow/execute', workflow);
}
export const validateWorkflow = (workflow) => {
  return http.post('/ai/workflow/validate', workflow);
}

/** 训练可视化 */
export const startTraining = (config) => {
  return http.post('/ai/training/start', config);
}
export const demoTraining = () => {
  return http.post('/ai/training/demo');
}
export const listTrainingTasks = () => {
  return http.get('/ai/training/tasks');
}
export const getTrainingTask = (id) => http.get(`/ai/training/tasks/${id}`)
export const getTrainingHistory = (id) => http.get(`/ai/training/tasks/${id}/history`)
export const deleteTrainingTask = (id) => http.delete(`/ai/training/tasks/${id}`)

/** AIGC 图片生成 */
export const generateImage = (req) => {
  return http.post('/ai/image/generate', req);
}
export const listImageTypes = () => {
  return http.post('/ai/image/types');
}
export const inferImageType = (prompt) => http.get('/ai/image/infer', { params: { prompt } })

/** 视频流式生成 (SSE) */
export const listVideoStreams = () => {
  return http.get('/ai/video/stream/list');
}
export const getVideoStream = (id) => http.get(`/ai/video/stream/${id}`)
export const cancelVideoStream = (id) => http.post(`/ai/video/stream/cancel/${id}`)

/** 权限 (V2.7.9) */
export const getMyPermissions = () => {
  return http.get('/ai/permission/me');
}
export const listAllRoles = () => {
  return http.get('/ai/permission/roles');
}
export const checkPermissions = (role, permissions) => http.post('/ai/permission/check', { role, permissions })

/** 音乐流式生成 (V2.8.1) */
export const listMusicStreams = () => {
  return http.get('/ai/music/stream/list');
}
export const getMusicStream = (id) => http.get(`/ai/music/stream/${id}`)
export const cancelMusicStream = (id) => http.post(`/ai/music/stream/cancel/${id}`)

// ============== V2.8.3 新工具 SDK ==============
// 后端端点: POST /api/ai/admin/tools/{code}/invoke

/** 文本分析 (摘要/情感/实体/关键词) */
export const analyzeText = (req) => {
  return http.post('/ai/admin/tools/invoke', req);
}

/** 视觉分析 (颜色/风格/相似度) */
export const analyzeVision = (req) => {
  return http.post('/ai/admin/tools/invoke', req);
}

/** 音频分析 (音量/频谱/情绪) */
export const analyzeAudio = (req) => {
  return http.post('/ai/admin/tools/invoke', req);
}

/** 文件转换 (JSON/YAML/CSV/Base64) */
export const convertFile = (req) => {
  return http.post('/ai/admin/tools/file.convert/invoke', req);
}

/** 相关性分析 (Pearson/Spearman) */
export const analyzeCorrelation = (req) => {
  return http.post('/ai/admin/tools/data.analyze.correlation/invoke', req);
}

/** 线性预测 (回归/移动平均/指数平滑) */
export const predictData = (req) => {
  return http.post('/ai/admin/tools/data.predict.linear/invoke', req);
}

/** 时间工具 (格式/计算/时区) */
export const timeConvert = (req) => {
  return http.post('/ai/admin/tools/time.convert/invoke', req);
}

/** AIGC 图片生成 (via tool) */
export const generateImageTool = (req) => {
  return http.post('/ai/admin/tools/image.generate/invoke', req);
}

/** 图表生成 (via tool) */
export const generateChartTool = (req) => {
  return http.post('/ai/admin/tools/chart.generate/invoke', req);
}

/** 音乐生成 (via tool) */
export const generateMusicTool = (req) => {
  return http.post('/ai/admin/tools/music.generate/invoke', req);
}

// ============== V2.8.4 企业项目生成 ==============

/** Java 企业项目生成 (完整 ZIP, 返回 Base64) */
export const generateJavaProject = (req) => {
  return http.post('/ai/admin/tools/java.project.gen/invoke', req);
}

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
export const createAiSession = (data) => {
  return http.post('/ai/chat/sessions', data);
}
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

// ============================================================
// V3.5.48: 新增 8 大功能 API 函数 (覆盖 100+ 后端端点)
// ============================================================

// ============ 1. PptGen (4 端点) ============
/** 生成 PPT (返回 base64) */
export const pptGenerate = (data) => {
  return http.post('/ai/ppt/generate', data);
}
/** 自动生成 PPT (只给主题) */
export const pptAuto = (data) => {
  return http.post('/ai/ppt/auto', data);
}
/** 列出可用主题 */
export const pptThemes = () => {
  return http.get('/ai/ppt/themes');
}
/** 解析大纲预览 */
export const pptParse = (data) => {
  return http.post('/ai/ppt/parse', data);
}

// ============ 2. ProjectDownload (2 端点) ============
/** 下载生成的项目 ZIP (GET) */
export const projectDownloadGet = (params) => http.get('/ai/project/download', { params })
/** 下载生成的项目 ZIP (POST) */
export const projectDownloadPost = (data) => http.post('/ai/project/download', data, { responseType: 'blob' })

// ============ 3. AutoAgentGroup (3 端点) ============
/** 一句话生成智能体群 */
export const autoAgentGroupGenerate = (data) => {
  // V6.8.1 fix: 对齐 AiAgentGroupAutoRealController → /api/v1/ai/agent-group/auto/generate-auto
  return http.post('/ai/agent-group/auto/generate-auto', data);
}
/** 基于模板生成智能体群 */
export const autoAgentGroupByTemplate = (data) => {
  return http.post('/ai/agent-group/auto/template', data);
}
/** 列出可用模板 */
export const autoAgentGroupTemplates = () => {
  return http.get('/ai/agent-group/auto/templates');
}

// ============ 4. Intent (12 端点) ============
/** 单条意图预测 */
export const intentPredict = (data) => {
  return http.post('/ai/intent/predict', data);
}
/** 批量意图预测 */
export const intentPredictBatch = (data) => {
  return http.post('/ai/intent/predict/batch', data);
}
/** 动态添加关键词 */
export const intentAddKeyword = (data) => {
  return http.post('/ai/intent/keyword', data);
}
/** 动态添加短语 */
export const intentAddPhrase = (data) => {
  return http.post('/ai/intent/phrase', data);
}
/** 列出所有意图 */
export const intentList = () => {
  return http.get('/ai/intent/list');
}
/** 意图统计 */
export const intentStats = () => {
  return http.get('/ai/intent/stats');
}
/** 意图识别基准测试 */
export const intentBenchmark = (data) => {
  return http.post('/ai/intent/benchmark', data);
}
/** 获取意图配置 */
export const intentGetConfig = () => {
  return http.get('/ai/intent/config');
}
/** 更新意图配置 */
export const intentUpdateConfig = (data) => {
  return http.put('/ai/intent/config', data);
}
/** 重置意图配置 */
export const intentResetConfig = () => http.post('/ai/intent/config/reset', {})
/** 清空意图上下文 */
export const intentClearContext = (data) => {
  return http.post('/ai/intent/context/clear', data);
}

// ============ 5. Cluster / Raft (15 端点) ============
/** 列出所有节点 */
export const clusterListNodes = () => {
  return http.get('/ai/cluster/nodes/list');
}
/** 列出 ACTIVE 节点 */
export const clusterActiveNodes = () => {
  return http.get('/ai/cluster/nodes/active');
}
/** 获取节点详情 */
export const clusterNode = (nodeId) => http.get(`/ai/cluster/nodes/${nodeId}`)
/** 当前节点 */
export const clusterMe = () => {
  return http.get('/ai/cluster/me');
}
/** Leader 节点 */
export const clusterLeader = () => {
  return http.get('/ai/cluster/leader');
}
/** 节点路由 */
export const clusterRoute = (data) => {
  return http.post('/ai/cluster/route', data);
}
/** 节点排空 (drain) */
export const clusterDrainNode = (nodeId) => http.post(`/ai/cluster/node/${nodeId}/drain`, {})
/** 集群统计 */
export const clusterStats = () => {
  return http.get('/ai/cluster/stats');
}
/** Raft 启动 */
export const raftStart = () => http.post('/ai/cluster/raft/start', {})
/** Raft 停止 */
export const raftStop = () => http.post('/ai/cluster/raft/stop', {})
/** Raft 状态 */
export const raftState = () => {
  return http.get('/ai/cluster/raft/state');
}
/** Raft Leader */
export const raftLeader = () => {
  return http.get('/ai/cluster/raft/leader');
}
/** Raft 提交 */
export const raftSubmit = (data) => {
  return http.post('/ai/cluster/raft/submit', data);
}
/** Raft 已应用 */
export const raftApplied = () => {
  return http.get('/ai/cluster/raft/applied');
}
/** Raft Append (底层) */
export const raftAppend = (data) => {
  return http.post('/ai/raft/append', data);
}
/** Raft 投票 */
export const raftVote = (data) => {
  return http.post('/ai/raft/vote', data);
}
/** Raft 状态 */
export const raftStatus = () => {
  return http.get('/ai/raft/status');
}
/** Raft 提交日志 */
export const raftLog = (params) => http.get('/ai/raft/log', { params })
/** Raft 触发选举 */
export const raftTriggerElection = () => http.post('/ai/raft/trigger-election', {})

// ============ 6. Push (10 端点) ============
/** 订阅推送 */
export const pushSubscribe = (data) => {
  return http.post('/ai/push/subscribe', data);
}
/** 取消订阅 */
export const pushUnsubscribe = (data) => {
  return http.post('/ai/push/unsubscribe', data);
}
/** 我的订阅列表 */
export const pushSubscriptions = () => {
  return http.get('/ai/push/subscriptions');
}
/** 所有订阅 */
export const pushAllSubscriptions = () => {
  return http.get('/ai/push/subscriptions/all');
}
/** 发送推送给用户 */
export const pushSendToUser = (data) => {
  return http.post('/ai/push/send/user', data);
}
/** 发送推送给平台 */
export const pushSendToPlatform = (data) => {
  return http.post('/ai/push/send/platform', data);
}
/** 广播推送 */
export const pushBroadcast = (data) => {
  return http.post('/ai/push/send/broadcast', data);
}
/** 推送消息历史 */
export const pushMessages = (params) => http.get('/ai/push/messages', { params })
/** 推送统计 */
export const pushStats = () => {
  return http.get('/ai/push/stats');
}
/** Push 集成 - 自动检测 */
export const pushIntegrationAuto = () => http.post('/ai/push/integration/auto', {})
/** Push 集成 - APNs 配置 */
export const pushIntegrationApns = (data) => {
  return http.post('/ai/push/integration/apns', data);
}
/** Push 集成 - FCM 配置 */
export const pushIntegrationFcm = (data) => {
  return http.post('/ai/push/integration/fcm', data);
}
/** Push 集成 - Web Push (VAPID) */
export const pushIntegrationWeb = (data) => {
  return http.post('/ai/push/integration/web', data);
}
/** Push 集成 - 健康检查 */
export const pushIntegrationHealth = () => {
  return http.get('/ai/push/integration/health');
}
/** Push 集成 - 统计 */
export const pushIntegrationStats = () => {
  return http.get('/ai/push/integration/stats');
}
/** Push 集成 - 检测环境 */
export const pushIntegrationDetect = () => {
  return http.get('/ai/push/integration/detect');
}
/** Push 集成 - VAPID 公钥 */
export const pushIntegrationVapidKey = () => {
  return http.get('/ai/push/integration/vapid-public-key');
}

// ============ 7. Document (3 端点) ============
/** 解析文档 */
export const documentParse = (data) => {
  return http.post('/ai/document/parse', data);
}
/** 提取关键词 */
export const documentKeywords = (data) => {
  return http.post('/ai/document/keywords', data);
}
/** 支持的格式 */
export const documentFormats = () => {
  return http.get('/ai/document/formats');
}

// ============ 8. Framework (10 端点) ============
/** 执行 Agent */
export const frameworkAgentExecute = (data) => {
  return http.post('/ai/framework/agents/execute', data);
}
/** 路由 Agent */
export const frameworkAgentRoute = (data) => {
  return http.post('/ai/framework/agents/route', data);
}
/** 列出所有 Agent */
export const frameworkAgentsList = () => {
  return http.get('/ai/framework/agents');
}
/** 权限列表 */
export const frameworkPermissionList = () => {
  return http.get('/ai/framework/permission/list');
}
/** 授予权限 */
export const frameworkPermissionGrant = (data) => {
  return http.post('/ai/framework/permission/grant', data);
}
/** 撤销权限 */
export const frameworkPermissionRevoke = (data) => {
  return http.post('/ai/framework/permission/revoke', data);
}
/** 撤销所有权限 */
export const frameworkPermissionRevokeAll = (data) => {
  return http.post('/ai/framework/permission/revoke-all', data);
}
/** 记忆统计 */
export const frameworkMemoryStats = () => {
  return http.get('/ai/framework/memory/stats');
}
/** 清空记忆 */
export const frameworkMemoryClear = (data) => {
  return http.post('/ai/framework/memory/clear', data);
}
/** 产品搜索 */
export const frameworkProductSearch = (params) => http.get('/ai/framework/products/search', { params })

// ============ 9. KnowledgeBase (11 端点) - AI 知识库 ============
/** KB 搜索 */
export const aiKbSearch = (data) => {
  return http.post('/rag/retrieve', data);
}
/** KB 关键词搜索 */
export const aiKbSearchKeyword = (params) => http.get('/rag/retrieve', { params })
/** KB 公共知识库 */
export const aiKbPublic = () => {
  return http.get('/ai/kb/public');
}
/** KB 上传文档 */
export const aiKbUpload = (data) => {
  return http.post('/rag/doc/upload', data);
}
/** KB 文档详情 */
export const aiKbDoc = (docId) => http.get(`/ai/kb/docs/${docId}`)
/** KB 文档列表 */
export const aiKbDocs = (kbId) => http.get(`/ai/kb/docs/list/${kbId}`)
/** KB 文档分块 */
export const aiKbDocChunks = (docId) => http.get(`/ai/kb/chunks/${docId}`)
/** KB 统计 */
export const aiKbStats = (kbId) => http.get(`/ai/kb/stats/${kbId}`)
/** KB 授权 */
export const aiKbGrant = (data) => {
  return http.post('/ai/kb/permission/grant', data);
}
/** KB 撤销 */
export const aiKbRevoke = (data) => {
  return http.post('/ai/kb/permission/revoke', data);
}

// ============ 10. Dashboard (10 端点) - AI 仪表盘 ============
/** 健康检查 */
export const aiDashboardHealth = () => {
  return http.get('/ai/dashboard/health');
}
/** 指标 */
export const aiDashboardMetrics = () => {
  return http.get('/ai/dashboard/stats');
}
/** 所有指标 */
export const aiDashboardMetricsAll = () => {
  return http.get('/ai/dashboard/recent');
}
/** 指标 by name */
export const aiDashboardMetric = (name) => http.get(`/ai/dashboard/metrics/${name}`)
/** 趋势 by name */
export const aiDashboardTrend = (name) => http.get(`/ai/dashboard/trend/${name}`)
/** 缓存清理 */
export const aiDashboardCacheClear = () => http.post('/ai/dashboard/cache/clear', {})
/** 缓存统计 */
export const aiDashboardCacheStats = () => {
  return http.get('/ai/dashboard/cache/stats');
}
/** 工具 Top */
export const aiDashboardToolsTop = (params) => http.get('/ai/dashboard/tools/top', { params })
/** 工具 Track */
export const aiDashboardToolsTrack = (data) => {
  return http.post('/ai/dashboard/tools/track', data);
}
/** 仪表盘 from-data */
export const aiDashboardFromData = (data) => {
  return http.post('/ai/dashboard/from-data', data);
}

// ============ 11. Distributed (4 端点) ============
/** All-reduce */
export const distributedAllReduce = (data) => {
  return http.post('/ai/distributed/all-reduce', data);
}
/** Shard */
export const distributedShard = (data) => {
  return http.post('/ai/distributed/shard', data);
}
/** Shard Info */
export const distributedShardInfo = () => {
  return http.get('/ai/distributed/shard/info');
}
/** Train Step */
export const distributedTrainStep = (data) => {
  return http.post('/ai/distributed/schedule', data);
}

// ============ 12. ImageGen (3 端点) - AI 图像生成 ============
/** 生成图像 */
export const aiImageGenGenerate = (data) => {
  return http.post('/ai/image/generate', data);
}
/** 推断图像类型 */
export const aiImageInfer = (data) => {
  return http.post('/ai/image/infer', data);
}
/** 列出图像类型 */
export const aiImageTypes = () => {
  return http.get('/ai/image/types');
}

// ============ 13. Animation (2 端点) ============
/** 文字淡入 GIF */
export const animationTextFade = (data) => http.post('/ai/animation/text-fade', data, { responseType: 'blob' })
/** 进度 GIF */
export const animationProgress = (data) => http.post('/ai/animation/progress', data, { responseType: 'blob' })

// ============ 14. AI Pipeline (6 端点) ============
/** AI Pipeline 配置 */
export const aiPipelineConfig = () => {
  return http.get('/ai/pipeline/config');
}
/** AI Pipeline 配置 (compute mode) */
export const aiPipelineConfigComputeMode = (data) => {
  return http.post('/ai/pipeline/config/compute-mode', data);
}
/** AI Pipeline 执行 */
export const aiPipelineExecute = (data) => {
  return http.post('/ai/pipeline/execute', data);
}
/** AI Pipeline Intent Reload */
export const aiPipelineIntentReload = () => http.post('/ai/pipeline/intent/reload', {})
/** AI Pipeline Intent Stats */
export const aiPipelineIntentStats = () => {
  return http.get('/ai/pipeline/intent/stats');
}
/** AI Route Recognize */
export const aiRouteRecognize = (data) => {
  return http.post('/ai/route/recognize', data);
}

// ============ 15. AI Tools / Files / Charts (附加) ============
/** 列出 AI 工具 (通用) */
export const aiListTools = () => {
  return http.get('/ai/tools');
}
/** 调用 AI 工具 (通用) */
export const aiInvokeTool = (code, data) => http.post(`/ai/tools/${code}/invoke`, data)
/** AI 文件列表 */
export const aiListFiles = () => {
  return http.get('/ai/files');
}
/** AI 数据源列表 */
export const aiListDatasources = () => {
  return http.get('/ai/datasources');
}
/** AI 数据源 schema */
export const aiDatasourceSchema = (id) => http.get(`/ai/datasources/${id}/schema`)
/** AI 数据源 test */
export const aiDatasourceTest = (id) => http.post(`/ai/datasources/${id}/test`, {})
/** AI 数据源 query */
export const aiDatasourceQuery = (id, data) => http.post(`/ai/datasources/${id}/query`, data)
/** 渲染图表 (POST) */
export const aiRenderChart = (data) => http.post('/ai/chart/render', data, { responseType: 'blob' })
/** NL2Chart */
export const aiNl2Chart = (data) => http.post('/ai/nl2chart', data, { responseType: 'blob' })
/** 渲染仪表盘 */
export const aiRenderDashboard = (data) => http.post('/ai/dashboard/render', data, { responseType: 'blob' })
/** 视频合成 */
export const aiComposeVideo = (data) => {
  return http.post('/ai/video/compose', data);
}
/** 视频 from-data */
export const aiVideoFromData = (data) => {
  return http.post('/ai/video/from-data', data);
}
/** 训练 */
export const aiTrain = (data) => {
  return http.post('/ai/train', data);
}

// ============ 16. Webhook (11 端点) ============
/** 列出 webhook */
export const webhooksList = () => {
  return http.get('/ai/webhooks');
}
/** 创建 webhook */
export const webhooksCreate = (data) => {
  return http.post('/ai/webhooks', data);
}
/** webhook 详情 */
export const webhookGet = (id) => http.get(`/ai/webhooks/${id}`)
/** 测试 webhook */
export const webhookTest = (id) => http.post(`/ai/webhooks/${id}/test`, {})
/** webhook 投递历史 */
export const webhookDeliveries = (id) => http.get(`/ai/webhooks/${id}/deliveries`)
/** 发送 webhook 事件 */
export const webhookPublish = (data) => {
  return http.post('/ai/webhooks/publish', data);
}
/** webhook 事件 */
export const webhookEvents = () => {
  return http.get('/ai/webhooks/events');
}
/** webhook 统计 */
export const webhookStats = () => {
  return http.get('/ai/webhooks/stats');
}

// ==================== Day 32: 投票对话 API ====================

/** 智能对话（自动投票，低置信度触发多模型）(Day 32) */
export const votingChat = (data) => {
  return http.post('/ai/chat', data);
}

/** 强制多模型投票（绕过置信度预判）(Day 32) */
export const forceVotingChat = (data) => {
  return http.post('/ai/chat/voting', data);
}

/** 查询投票配置信息 (Day 32) */
export const votingInfo = () => {
  return http.get('/ai/chat/voting-info');
}

// ==================== Day 34: 流式聊天 SSE ====================
/**
 * 流式聊天 (SSE via fetch + ReadableStream)
 * @param {object} data { text, model, sessionId }
 * @param {Function} onChunk - 每次收到 SSE data 行时调用，参数为字符串内容
 * @param {Function} onError
 * @param {Function} onComplete
 * @returns {Function} cancel - 调用以取消流
 */
/**
 * 流式聊天 (SSE via fetch + ReadableStream)
 * Day 36: 修复 cancel 作用域 + 自动重连（最多 2 次，指数退避）
 *
 * @param {object} data { text, model, sessionId }
 * @param {Function} onChunk       - 每次收到 SSE data 行时调用，参数为字符串内容
 * @param {Function} onError       - 网络/业务错误时调用
 * @param {Function} onComplete    - 流结束时调用
 * @param {Function} onReconnecting - 重连开始时调用（可选）
 * @returns {{ cancel: Function }} cancel 函数
 */
export const chatStream = (data, onChunk, onError, onComplete, onReconnecting) => {
  // 从 Pinia persist 读取 token
  let token = ''
  try {
    const stored = localStorage.getItem('minimax-user')
    if (stored) token = JSON.parse(stored)?.state?.accessToken || ''
  } catch (_) {}

  const base = import.meta.env.VITE_API_BASE || ''
  const url = `${base}/api/v1/model/chat/stream`

  const MAX_RETRIES = 2
  const BACKOFF_MS = [1000, 2000] // 指数退避: 1s, 2s

  let controller = new AbortController()
  let cancelled = false
  let done = false

  // 核心: 执行一次 fetch + SSE 读取
  function doFetch(retryCount = 0) {
    if (cancelled || done) return

    controller = new AbortController()
    const reqBody = JSON.stringify({
      text: data.text,
      model: data.model || 'gpt-4o-mini',
      sessionId: data.sessionId
    })

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      body: reqBody,
      credentials: 'include',
      signal: controller.signal
    }).then(async (response) => {
      if (!response.ok) {
        // HTTP 错误不重试（业务错误）
        onError && onError(new Error('HTTP ' + response.status))
        return
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = '' // 未完成的行片段

      function pump() {
        if (cancelled || done) return
        reader.read().then(({ done: doneReading, value }) => {
          if (doneReading || cancelled) {
            if (!done) { done = true; onComplete && onComplete() }
            return
          }
          const text = decoder.decode(value, { stream: true })
          // 处理跨 chunk 的行（buffer 拼接）
          const combined = buffer + text
          const lines = combined.split('\n')
          // 最后一行可能是未完成的片段
          buffer = lines.pop() || ''

          for (const rawLine of lines) {
            const trimmed = rawLine.trim()
            if (!trimmed.startsWith('data: ')) continue
            const json = trimmed.slice(6)
            if (json === '[DONE]') {
              done = true
              onComplete && onComplete()
              return
            }
            try {
              const parsed = JSON.parse(json)
              const content = parsed?.choices?.[0]?.delta?.content || ''
              if (content) onChunk(content)
            } catch (_) { /* ignore parse errors */ }
          }
          pump()
        }).catch((err) => {
          if (cancelled) return // 用户主动取消，跳过
          // 网络/IO 错误 → 触发重连
          if (retryCount < MAX_RETRIES) {
            onReconnecting && onReconnecting(retryCount + 1, BACKOFF_MS[retryCount])
            setTimeout(() => doFetch(retryCount + 1), BACKOFF_MS[retryCount])
          } else {
            onError && onError(err)
          }
        })
      }

      pump()
    }).catch((err) => {
      if (cancelled) return
      if (retryCount < MAX_RETRIES) {
        onReconnecting && onReconnecting(retryCount + 1, BACKOFF_MS[retryCount])
        setTimeout(() => doFetch(retryCount + 1), BACKOFF_MS[retryCount])
      } else {
        onError && onError(err)
      }
    })
  }

  doFetch()

  // 返回取消函数（可在任意时机调用）
  return {
    cancel() {
      cancelled = true
      try { controller.abort() } catch (_) {}
    }
  }
}

// ==================== 训练模型推理 API (V6.8+) ====================
// V7.2: 训练模型判断逻辑移至各页面内部，通过 API 动态获取 trainedModels 列表
// 不再硬编码模型代码，避免与后端 DB 不同步

/** 列出可用训练模型 */
export const listTrainedModels = () => http.get('/ai/chat/training/models')

/**
 * 训练模型对话（非流式）
 * @param {object} data { model, message/text }
 */
export const trainingChat = (data) => {
  return http.post('/ai/training/chat', data)
}
