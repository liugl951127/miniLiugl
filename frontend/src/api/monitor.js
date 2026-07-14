// 监控 + 告警 + 审计 API (V2.7.1)
import http from './http'

// ==================== 监控基础 ====================

export const getMonitorInfo = () => http.get('/api/v1/monitor/info')
export const getMonitorHealth = () => http.get('/api/v1/monitor/health')
export const getJvmHealth = () => http.get('/api/v1/monitor/health/jvm')
export const getDbHealth = () => http.get('/api/v1/monitor/health/database')
export const getDiskHealth = () => http.get('/api/v1/monitor/health/disk')

export const getMetrics = () => http.get('/api/v1/monitor/metrics')
export const getMetricsSnapshot = () => http.get('/api/v1/monitor/metrics/snapshot')

// ==================== 告警 (V2.7.1 新增) ====================

/** 触发中的告警 */
export const getFiringAlerts = () => http.get('/api/v1/monitor/alerts/firing')

/** 告警摘要 */
export const getAlertSummary = () => http.get('/api/v1/monitor/alerts/summary')

/** 告警规则列表 */
export const listAlertRules = () => http.get('/api/v1/monitor/alerts/rules')

/** 创建告警规则 */
export const createAlertRule = (rule) => http.post('/api/v1/monitor/alerts/rules', rule)

/** 更新告警规则 */
export const updateAlertRule = (id, rule) => http.put(`/api/v1/monitor/alerts/rules/${id}`, rule)

/** 删除告警规则 */
export const deleteAlertRule = (id) => http.delete(`/api/v1/monitor/alerts/rules/${id}`)

/** 启用/禁用告警规则 */
export const toggleAlertRule = (id, enabled) =>
  http.post(`/api/v1/monitor/alerts/rules/${id}/toggle`, { enabled })

/** 确认告警 */
export const acknowledgeAlert = (id) => http.post(`/api/v1/monitor/alerts/${id}/ack`)

/** 通知渠道列表 */
export const listAlertChannels = () => http.get('/api/v1/monitor/alerts/channels')

/** 查通知渠道 */
export const getAlertChannel = (id) => http.get(`/api/v1/monitor/alerts/channels/${id}`)

/** 创建通知渠道 */
export const createAlertChannel = (channel) => http.post('/api/v1/monitor/alerts/channels', channel)

/** 更新通知渠道 */
export const updateAlertChannel = (id, channel) => http.put(`/api/v1/monitor/alerts/channels/${id}`, channel)

/** 删除通知渠道 */
export const deleteAlertChannel = (id) => http.delete(`/api/v1/monitor/alerts/channels/${id}`)

/** 测试通知渠道 */
export const testAlertChannel = (id) => http.post(`/api/v1/monitor/alerts/channels/${id}/test`)

/** 告警历史 */
export const getAlertHistory = (params) => http.get('/api/v1/monitor/alerts/history', { params })

// ==================== 审计日志 (V2.7.1 新增) ====================

/** 审计日志列表 */
export const getAuditLogs = (params) => http.get('/api/v1/admin/audit/recent', { params })

/** 按用户查询审计 */
export const getAuditByUser = (userId) => http.get(`/api/v1/admin/audit/by-actor/${userId}`)

/** 按天统计 */
export const getAuditByDay = (params) => http.get('/api/v1/admin/audit/by-day', { params })

/** 导出审计日志 */
export const exportAuditLogs = (params) =>
  http.get('/api/v1/admin/audit/export', { params, responseType: 'blob' })

// 默认导出 (兼容 import monitorApi)
const monitorApi = {
  getMonitorInfo, getMonitorHealth, getJvmHealth, getDbHealth, getDiskHealth,
  getMetrics, getMetricsSnapshot,
  getFiringAlerts, getAlertSummary,
  listAlertRules, createAlertRule, updateAlertRule, deleteAlertRule, toggleAlertRule,
  acknowledgeAlert, listAlertChannels, createAlertChannel, deleteAlertChannel,
  testAlertChannel, getAlertHistory,
  getAuditLogs, getAuditByUser, getAuditByDay, exportAuditLogs
}
export default monitorApi
export { monitorApi }

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
export const getMonitorTrend = (hours) => http.get('/api/v1/monitor/metrics/trend', { params: { hours } })
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
