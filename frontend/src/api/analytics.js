/**
 * @file analytics API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-analytics
 * 接口数: 19
 *
 *   POST   /api/v1/analytics/datasources
 *   GET    /api/v1/analytics/datasources
 *   GET    /api/v1/analytics/datasources/{id}
 *   POST   /api/v1/analytics/datasources/test
 *   GET    /api/v1/analytics/datasources/{dsId}/databases
 *   GET    /api/v1/analytics/datasources/{dsId}/databases/{db}/tables
 *   GET    /api/v1/analytics/datasources/{dsId}/databases/{db}/tables/{table}
 *   GET    /api/v1/analytics/datasources/{dsId}/databases/{db}/tables/{table}/profile
 *   ... 共 19 个
 */
// V5.31 数据智能分析 API
import http from './http'

// ===== 数据源管理 =====
export const listDataSources = () => {
  console.log('%c[ANALYTICS API] listDataSources', 'color: #409eff', '')
  return http.get('/analytics/datasources');
}
export const getDataSource = (id) => http.get(`/analytics/datasources/${id}`)
export const createDataSource = (data) => {
  console.log('%c[ANALYTICS API] createDataSource', 'color: #409eff', data)
  return http.post('/analytics/datasources', data);
}
export const updateDataSource = (id, data) => http.put(`/analytics/datasources/${id}`, data)
export const deleteDataSource = (id) => http.delete(`/analytics/datasources/${id}`)
export const testDataSource = (data) => {
  console.log('%c[ANALYTICS API] testDataSource', 'color: #409eff', data)
  return http.post('/analytics/datasources/test', data);
}

// ===== Schema 浏览 =====
export const listDatabases = (dsId) => http.get(`/analytics/datasources/${dsId}/databases`)
export const listTables = (dsId, db) => http.get(`/analytics/datasources/${dsId}/databases/${db}/tables`)
export const describeTable = (dsId, db, table) =>
  http.get(`/analytics/datasources/${dsId}/databases/${db}/tables/${table}`)
export const profileTable = (dsId, db, table) =>
  http.get(`/analytics/datasources/${dsId}/databases/${db}/tables/${table}/profile`)

// ===== 文件导入 =====
// V5.22: 返回 { promise, cancel } 支持可取消上传
export const uploadIngestFile = (formData, opts = {}) => {
  const controller = new AbortController()
  const cfg = {
    headers: { 'Content-Type': 'multipart/form-data' },
  }
  if (typeof opts?.onProgress === 'function') {
    cfg.onUploadProgress = opts.onProgress
  }
  if (opts?.signal) {
    cfg.signal = opts.signal
  }
  return {
    promise: http.post('/analytics/ingest/upload', formData, cfg),
    cancel: () => controller.abort(),
    signal: controller.signal,
  }
}
export const getIngestTask = (taskId) => http.get(`/analytics/ingest/tasks/${taskId}`)
export const getIngestQuality = (taskId) => http.get(`/analytics/ingest/tasks/${taskId}/quality`)

// ===== NL2SQL 实验室 =====
export const nl2sqlAsk = (data) => {
  console.log('%c[ANALYTICS API] nl2sqlAsk', 'color: #409eff', data)
  return http.post('/analytics/nlsql/ask', data);
}
export const nl2sqlExplain = (sql) => http.post('/analytics/nlsql/explain', { sql })
export const nl2qlFeedback = (data) => {
  console.log('%c[ANALYTICS API] nl2qlFeedback', 'color: #409eff', data)
  return http.post('/analytics/nlsql/feedback', data);
}
export const nl2sqlHistory = (params) => http.get('/analytics/nlsql/history', { params })

// ===== 查询执行 (SQL 沙盒) =====
export const executeQuery = (data) => {
  console.log('%c[ANALYTICS API] executeQuery', 'color: #409eff', data)
  return http.post('/analytics/query/execute', data);
}
export const dryRunQuery = (data) => {
  console.log('%c[ANALYTICS API] dryRunQuery', 'color: #409eff', data)
  return http.post('/analytics/query/dry-run', data);
}

// ===== 报告生成 =====
export const generateReport = (data) => {
  console.log('%c[ANALYTICS API] generateReport', 'color: #409eff', data)
  return http.post('/analytics/reports/generate', data);
}
export const getReport = (reportId) => http.get(`/analytics/reports/${reportId}`)

// ===== Day 37: 投票一致率统计 =====
/**
 * 投票统计摘要
 * GET /api/v1/ai/voting/stats
 * 后端可提供: { totalVotes, avgAgreement, avgModelCount, avgLatencyMs }
 */
export const getVoteStatsSummary = () => {
  console.log('%c[ANALYTICS API] getVoteStatsSummary', 'color: #409eff', '')
  return http.get('/ai/voting/stats')
}

/**
 * 一致率趋势数据
 * GET /api/v1/ai/voting/stats/trend?strategy=all&bucket=day
 */
export const getVoteTrend = (params) => {
  console.log('%c[ANALYTICS API] getVoteTrend', 'color: #409eff', params)
  return http.get('/ai/voting/stats/trend', { params })
}

/**
 * 投票记录分页列表
 * GET /api/v1/ai/voting/records?page=1&size=10
 */
export const getVoteRecords = (params) => {
  console.log('%c[ANALYTICS API] getVoteRecords', 'color: #409eff', params)
  return http.get('/ai/voting/records', { params })
}
