// 监控 + 告警 + 审计 API (V2.7.1)
import http from './http'

// ==================== 监控基础 ====================

export const getMonitorInfo = () => http.get('/monitor/info')
export const getMonitorHealth = () => http.get('/monitor/health')
export const getJvmHealth = () => http.get('/monitor/health/jvm')
export const getDbHealth = () => http.get('/monitor/health/database')
export const getDiskHealth = () => http.get('/monitor/health/disk')

export const getMetrics = () => http.get('/monitor/metrics')
export const getMetricsSnapshot = () => http.get('/monitor/metrics/snapshot')

// ==================== 告警 (V2.7.1 新增) ====================

// 别名 (兼容 monitor/Index.vue 旧 API)
export const getMonitorAlertRules = listAlertRules
export const createMonitorAlertRule = createAlertRule
export const updateMonitorAlertRule = updateAlertRule
export const deleteMonitorAlertRule = deleteAlertRule

export const getAlertChannels = listAlertChannels
export const updateAlertChannel = (id, ch) => http.put(`/monitor/alerts/channels/${id}`, ch)

/** 触发中的告警 */
export const getFiringAlerts = () => http.get('/monitor/alerts/firing')

/** 告警摘要 */
export const getAlertSummary = () => http.get('/monitor/alerts/summary')

/** 告警规则列表 */
export const listAlertRules = () => http.get('/monitor/alerts/rules')

/** 创建告警规则 */
export const createAlertRule = (rule) => http.post('/monitor/alerts/rules', rule)

/** 更新告警规则 */
export const updateAlertRule = (id, rule) => http.put(`/monitor/alerts/rules/${id}`, rule)

/** 删除告警规则 */
export const deleteAlertRule = (id) => http.delete(`/monitor/alerts/rules/${id}`)

/** 启用/禁用告警规则 */
export const toggleAlertRule = (id, enabled) =>
  http.post(`/monitor/alerts/rules/${id}/toggle`, { enabled })

/** 确认告警 */
export const acknowledgeAlert = (id) => http.post(`/monitor/alerts/${id}/ack`)

/** 通知渠道列表 */
export const listAlertChannels = () => http.get('/monitor/alerts/channels')

/** 创建通知渠道 */
export const createAlertChannel = (channel) => http.post('/monitor/alerts/channels', channel)

/** 删除通知渠道 */
export const deleteAlertChannel = (id) => http.delete(`/monitor/alerts/channels/${id}`)

/** 测试通知渠道 */
export const testAlertChannel = (id) => http.post(`/monitor/alerts/channels/${id}/test`)

/** 告警历史 */
export const getAlertHistory = (params) => http.get('/monitor/alerts/history', { params })

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
