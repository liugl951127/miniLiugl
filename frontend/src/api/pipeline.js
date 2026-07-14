// V5.32 画布工作流 API
import http from './http'

// ===== 工作流 CRUD =====
export const listWorkflows = (params) => http.get('/api/v1/pipeline/workflows', { params })
export const getWorkflow = (id) => http.get(`/api/v1/pipeline/workflows/${id}`)
export const createWorkflow = (data) => http.post('/api/v1/pipeline/workflows', data)
export const updateWorkflow = (id, data) => http.put(`/api/v1/pipeline/workflows/${id}`, data)
export const deleteWorkflow = (id) => http.delete(`/api/v1/pipeline/workflows/${id}`)
export const validateWorkflow = (data) => http.post('/api/v1/pipeline/workflows/validate', data)

// ===== 工作流执行 =====
export const runWorkflow = (id, params) =>
  http.post(`/api/v1/pipeline/workflows/${id}/run`, params || {})
export const listWorkflowRuns = (id, params) =>
  http.get(`/api/v1/pipeline/workflows/${id}/runs`, { params })

// ===== 运行详情 =====
export const getRun = (runId) => http.get(`/api/v1/pipeline/runs/${runId}`)
export const getRunResult = (runId) => http.get(`/api/v1/pipeline/runs/${runId}/result`)
