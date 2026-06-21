// Monitor API
import http from './http'

export const getMonitorHealth = () => http.get('/monitor/health')
export const getMonitorDb = () => http.get('/monitor/health/database')
export const getMonitorJvm = () => http.get('/monitor/health/jvm')
export const getMonitorDisk = () => http.get('/monitor/health/disk')
export const getMonitorMetrics = () => http.get('/monitor/metrics')
export const getMonitorSnapshot = () => http.get('/monitor/metrics/snapshot')
export const getMonitorTrend = (hours = 24) => http.get(`/monitor/metrics/trend?hours=${hours}`)
export const getMonitorAlerts = () => http.get('/monitor/alerts')
export const getMonitorAlertsFiring = () => http.get('/monitor/alerts/firing')
export const getMonitorInfo = () => http.get('/monitor/info')

// KG API (Agent 模块下的知识图谱)
export const kgSearchEntities = (userId, keyword, limit = 50) =>
  http.get('/agent/kg/entities/search', { params: { userId, keyword, limit } })
export const kgCreateEntity = (body) => http.post('/agent/kg/entities', body)
export const kgGetEntity = (id) => http.get(`/agent/kg/entities/${id}`)
export const kgCreateRelation = (body) => http.post('/agent/kg/relations', body)
export const kgNeighbors = (id) => http.get(`/agent/kg/entities/${id}/neighbors`)
export const kgTwoHop = (id) => http.get(`/agent/kg/entities/${id}/2hop`)
export const kgPath = (userId, fromId, toId) =>
  http.get('/agent/kg/path', { params: { userId, fromId, toId } })
// V5.9: 告警规则 CRUD
export const getMonitorAlertRules = () => http.get('/monitor/alerts/rules')
export const createMonitorAlertRule = (body) => http.post('/monitor/alerts/rules', body)
export const updateMonitorAlertRule = (id, body) => http.put(`/monitor/alerts/rules/${id}`, body)
export const deleteMonitorAlertRule = (id) => http.delete(`/monitor/alerts/rules/${id}`)
export const getMonitorAlertSummary = () => http.get('/monitor/alerts/summary')
