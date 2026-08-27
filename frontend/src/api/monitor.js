/**
 * @file monitor API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-monitor
 * 接口数: 27
 *
 *   GET    /monitor/api-docs
 *   GET    /api/v1/monitor/health
 *   GET    /api/v1/monitor/health/database
 *   GET    /api/v1/monitor/health/jvm
 *   GET    /api/v1/monitor/health/disk
 *   GET    /api/v1/monitor/metrics
 *   GET    /api/v1/monitor/metrics/snapshot
 *   GET    /api/v1/monitor/metrics/trend
 *   ... 共 27 个
 */
// 监控 + 告警 + 审计 API (V2.7.1)
import http from './http'

// ==================== 监控基础 ====================

export const getMonitorInfo = () => {
  return http.get('/monitor/info');
}
export const getMonitorHealth = () => {
  return http.get('/monitor/health');
}
export const getJvmHealth = () => {
  return http.get('/monitor/health/jvm');
}
export const getDbHealth = () => {
  return http.get('/monitor/health/database');
}
export const getDiskHealth = () => {
  return http.get('/monitor/health/disk');
}

export const getMetrics = () => {
  return http.get('/monitor/metrics');
}
export const getMetricsSnapshot = () => {
  return http.get('/monitor/metrics/snapshot');
}

// ==================== 告警 (V2.7.1 新增) ====================

/** 触发中的告警 */
export const getFiringAlerts = () => {
  return http.get('/monitor/alerts/firing');
}

/** 告警摘要 */
export const getAlertSummary = () => {
  return http.get('/monitor/alerts/summary');
}

/** 告警规则列表 */
export const listAlertRules = () => {
  return http.get('/monitor/alerts/rules');
}

/** 创建告警规则 */
export const createAlertRule = (rule) => {
  return http.post('/monitor/alerts/rules', rule);
}

/** 更新告警规则 */
export const updateAlertRule = (id, rule) => http.put(`/monitor/alerts/rules/${id}`, rule)

/** 删除告警规则 */
export const deleteAlertRule = (id) => http.delete(`/monitor/alerts/rules/${id}`)

/** 全部告警规则 (含禁用, Day 45) */
export const getAllAlertRules = () => http.get('/monitor/alerts/rules/all')

/** 启用/禁用告警规则 */
export const toggleAlertRule = (id, enabled) =>
  http.post(`/monitor/alerts/rules/${id}/toggle`, { enabled })

/** 确认告警 (Day 34: 支持备注) */
export const acknowledgeAlert = (id, notes = '') =>
  http.post(`/monitor/alerts/${id}/ack`, notes ? { notes } : {})

// ==================== 静默功能 (Day 35) ====================

/**
 * 静默告警事件
 * @param {number} id 告警 ID
 * @param {number} [minutes=60] 静默时长(分钟), 默认 60
 * @param {number} [endTime] 可选: 截止时间戳(ms), 优先级高于 minutes
 */
export const silenceAlert = (id, { minutes = 60, endTime = null } = {}) =>
  http.post(`/monitor/alerts/${id}/silence`, endTime ? { endTime } : { minutes })

/** 取消静默告警事件 */
export const unsilenceAlert = (id) =>
  http.post(`/monitor/alerts/${id}/unsilence`)

/**
 * 静默告警规则
 * @param {number} id 规则 ID
 * @param {number} [minutes=60] 静默时长(分钟), 默认 60
 * @param {number} [endTime] 可选: 截止时间戳(ms)
 */
export const silenceRule = (id, { minutes = 60, endTime = null } = {}) =>
  http.post(`/monitor/alerts/rules/${id}/silence`, endTime ? { endTime } : { minutes })

/** 取消静默告警规则 */
export const unsilenceRule = (id) =>
  http.post(`/monitor/alerts/rules/${id}/unsilence`)

/** 通知渠道列表 */
export const listAlertChannels = () => {
  return http.get('/monitor/alerts/channels');
}

/** 查通知渠道 */
export const getAlertChannel = (id) => http.get(`/monitor/alerts/channels/${id}`)

/** 创建通知渠道 */
export const createAlertChannel = (channel) => {
  return http.post('/monitor/alerts/channels', channel);
}

/** 更新通知渠道 */
export const updateAlertChannel = (id, channel) => http.put(`/monitor/alerts/channels/${id}`, channel)

/** 删除通知渠道 */
export const deleteAlertChannel = (id) => http.delete(`/monitor/alerts/channels/${id}`)

/** 测试通知渠道 */
export const testAlertChannel = (id) => http.post(`/monitor/alerts/channels/${id}/test`)

/** 告警历史 */
export const getAlertHistory = (params) => http.get('/monitor/alerts/history', { params })

/** Day 52: 告警历史高级筛选（severity / metricName / status / startTime / endTime / page / limit） */
export const getAlertHistoryAdvanced = (params) => http.get('/monitor/alerts/history/advanced', { params })

// ==================== 审计日志 (V2.7.1 新增) ====================

/** 审计日志列表 */
export const getAuditLogs = (params) => http.get('/admin/audit/recent', { params })

/** 按用户查询审计 */
export const getAuditByUser = (userId) => http.get(`/admin/audit/by-actor/${userId}`)

/** 按天统计 */
export const getAuditByDay = (params) => http.get('/admin/audit/by-day', { params })

/** 导出审计日志 */
export const exportAuditLogs = (params) =>
  http.get('/admin/audit/export', { params, responseType: 'blob' })


// 别名 (兼容 monitor/Index.vue 旧 API) - 必须在定义后导出
export const getMonitorAlertRules = listAlertRules
export const createMonitorAlertRule = createAlertRule
export const updateMonitorAlertRule = updateAlertRule
export const deleteMonitorAlertRule = deleteAlertRule
export const getAlertChannels = listAlertChannels
export const getMonitorJvm = getJvmHealth
export const getMonitorDisk = getDiskHealth
export const getMonitorDb = getDbHealth
export const getMonitorMetrics = getMetrics
export const getMonitorTrend = (hours) => http.get('/monitor/metrics/trend', { params: { hours } })
export const getMonitorSnapshot = getMetricsSnapshot
export const getMonitorAlerts = getFiringAlerts
export const getMonitorAlertsFiring = getFiringAlerts
export const getMonitorAlertSummary = getAlertSummary

// ==================== 知识图谱 (Agent 模块) ====================

/** 搜索实体 */
export const kgSearchEntities = (userId, keyword, limit = 20) =>
  http.get('/agent/kg/entities/search', { params: { userId, keyword, limit } })

/** 查实体 */
export const kgGetEntity = (id) => http.get(`/agent/kg/entities/${id}`)

/** 邻居 (1跳) */
export const kgNeighbors = (id) => http.get(`/agent/kg/entities/${id}/neighbors`)

/** 2跳 */
export const kgTwoHop = (id) => http.get(`/agent/kg/entities/${id}/2hop`)

/** 路径 */
export const kgPath = (userId, fromId, toId) =>
  http.get('/agent/kg/path', { params: { userId, fromId, toId } })

// ==================== Day 32: RCA 根因分析 + 异常检测 API ====================

/** 告警 RCA 分析 (Day 33 修: 加 /api/v1 前缀) */
export const rcaAnalysis = (alertId, context) => http.post(`/monitor/alerts/${alertId}/rca`, context || {})

/** 手动触发异常检测 (Day 33 修: 加 /api/v1 前缀) */
export const anomalyDetect = (params) => {
  return http.post('/monitor/anomaly/detect', params);
}

/** 异常检测摘要 (Day 33 修: 加 /api/v1 前缀) */
export const anomalySummary = (params) => http.get('/monitor/anomaly/summary', { params })

/** 活跃异常检测指标 (Day 33 修: 加 /api/v1 前缀) */
export const activeAnomalyMetrics = () => {
  return http.get('/monitor/anomaly/active-metrics');
}

// ==================== Day 53: 告警趋势预测 API ====================

/** 告警趋势预测 (EWMA + 线性回归) */
export const getAlertPredict = (params) => {
  return http.get('/monitor/alerts/predict', { params })
}

/** 按级别预测 (CRITICAL / WARNING / INFO) */
export const getAlertPredictBySeverity = (params) => {
  return http.get('/monitor/alerts/predict/by-severity', { params })
}

// ==================== Day 54: 告警根因知识库 API ====================

/**
 * 查询告警知识库：同类历史告警处理经验
 * @param {object} params metricName?, historyDays?, limit?
 */
export const getAlertRcaKnowledge = (params) =>
  http.get('/monitor/alerts/rca/knowledge', { params })

/**
 * 根据告警ID查找同类历史告警处理经验
 * @param {number} alertId 当前告警ID
 * @param {number} [historyDays=30] 历史窗口
 * @param {number} [limit] 最大返回数
 */
export const getAlertRcaSimilar = (alertId, historyDays, limit) =>
  http.get('/monitor/alerts/rca/similar', {
    params: { alertId, historyDays, limit }
  })

/**
 * 告警知识摘要（高频级别/平均恢复时长/常见原因）
 * @param {string} [metricName] 指标名（可选）
 * @param {number} [historyDays=30] 历史窗口
 */
export const getAlertRcaSummary = (metricName, historyDays) =>
  http.get('/monitor/alerts/rca/summary', {
    params: { metricName, historyDays }
  })

// ==================== Day 43: SLA 统计 API ====================
/**
 * 告警 SLA 统计
 * @param {number} [windowDays] 统计窗口(天)，默认 30
 */
export const getAlertSla = (windowDays) => {
  return http.get('/monitor/alerts/sla', { params: { windowDays } })
}

/** 告警趋势（按天聚合）Day 44 */
export const getAlertTrend = (params) => {
  return http.get('/monitor/alerts/trend', { params })
}

/** 告警统计概览（总数/级别/活跃/Top规则）Day 47 */
export const getAlertStatistics = (days = 30) => {
  return http.get('/monitor/alerts/statistics', { params: { days } })
}

/** 告警时间序列（按日聚合，ECharts 趋势图用）Day 48 */
export const getAlertTimeSeries = (days = 30) => {
  return http.get('/monitor/alerts/timeseries', { params: { days } })
}

// ==================== V8.0.2 默认导出 (lazy function 初始化) ====================
// 修复: getAlertSla/getAlertTrend 较晚才声明, 之前 monitorApi 对象直接构造会 TDZ
// 修复方法: 用函数封装, 对象只在函数调用时才构造 (此时所有 const 已完成)
// 即使 minifier 重排代码, 函数体内的引用都在调用时才求值, 不会 TDZ
function createMonitorApi() {
  return {
    getMonitorInfo, getMonitorHealth, getJvmHealth, getDbHealth, getDiskHealth,
    getMetrics, getMetricsSnapshot,
    getFiringAlerts, getAlertSummary,
    listAlertRules, createAlertRule, updateAlertRule, deleteAlertRule, toggleAlertRule,
    acknowledgeAlert, silenceAlert, unsilenceAlert,
    silenceRule, unsilenceRule,
    listAlertChannels, createAlertChannel, deleteAlertChannel,
    testAlertChannel, getAlertHistory,
    getAuditLogs, getAuditByUser, getAuditByDay, exportAuditLogs,
    getAlertSla, getAlertTrend,
    // Day 53: 告警趋势预测
    getAlertPredict, getAlertPredictBySeverity,
    // Day 54: 告警根因知识库
    getAlertRcaKnowledge, getAlertRcaSimilar, getAlertRcaSummary
  }
}
const monitorApi = createMonitorApi()
export default monitorApi
export { monitorApi }
