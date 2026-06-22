// V5.31 数据智能分析 API
import http from './http'

// ===== 数据源管理 =====
export const listDataSources = () => http.get('/analytics/datasources')
export const getDataSource = (id) => http.get(`/analytics/datasources/${id}`)
export const createDataSource = (data) => http.post('/analytics/datasources', data)
export const updateDataSource = (id, data) => http.put(`/analytics/datasources/${id}`, data)
export const deleteDataSource = (id) => http.delete(`/analytics/datasources/${id}`)
export const testDataSource = (data) => http.post('/analytics/datasources/test', data)

// ===== Schema 浏览 =====
export const listDatabases = (dsId) => http.get(`/analytics/datasources/${dsId}/databases`)
export const listTables = (dsId, db) => http.get(`/analytics/datasources/${dsId}/databases/${db}/tables`)
export const describeTable = (dsId, db, table) =>
  http.get(`/analytics/datasources/${dsId}/databases/${db}/tables/${table}`)
export const profileTable = (dsId, db, table) =>
  http.get(`/analytics/datasources/${dsId}/databases/${db}/tables/${table}/profile`)

// ===== 文件导入 =====
export const uploadIngestFile = (formData, onUploadProgress) =>
  http.post('/analytics/ingest/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
export const getIngestTask = (taskId) => http.get(`/analytics/ingest/tasks/${taskId}`)
export const getIngestQuality = (taskId) => http.get(`/analytics/ingest/tasks/${taskId}/quality`)

// ===== NL2SQL 实验室 =====
export const nl2sqlAsk = (data) => http.post('/analytics/nlsql/ask', data)
export const nl2sqlExplain = (sql) => http.post('/analytics/nlsql/explain', { sql })
export const nl2qlFeedback = (data) => http.post('/analytics/nlsql/feedback', data)
export const nl2sqlHistory = (params) => http.get('/analytics/nlsql/history', { params })

// ===== 查询执行 (SQL 沙盒) =====
export const executeQuery = (data) => http.post('/analytics/query/execute', data)
export const dryRunQuery = (data) => http.post('/analytics/query/dry-run', data)

// ===== 报告生成 =====
export const generateReport = (data) => http.post('/analytics/reports/generate', data)
export const getReport = (reportId) => http.get(`/analytics/reports/${reportId}`)
