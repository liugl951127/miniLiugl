/**
 * @file pipeline API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-pipeline
 * 接口数: 8
 *
 *   GET    /api/v1/pipeline/runs/{runId}
 *   GET    /api/v1/pipeline/runs/{runId}/result
 *   GET    /api/v1/pipeline/workflows/{id}
 *   PUT    /api/v1/pipeline/workflows/{id}
 *   DELETE /api/v1/pipeline/workflows/{id}
 *   POST   /api/v1/pipeline/workflows/{id}/run
 *   GET    /api/v1/pipeline/workflows/{id}/runs
 *   POST   /api/v1/pipeline/workflows/validate
 */
// V5.32 画布工作流 API
import http from './http'

// ===== 工作流 CRUD =====
export const listWorkflows = (params) => http.get('/pipeline/workflows', { params })
export const getWorkflow = (id) => http.get(`/pipeline/workflows/${id}`)
export const createWorkflow = (data) => {
  console.log('%c[PIPELINE API] createWorkflow', 'color: #409eff', data)
  return http.post('/pipeline/workflows', data);
}
export const updateWorkflow = (id, data) => http.put(`/pipeline/workflows/${id}`, data)
export const deleteWorkflow = (id) => http.delete(`/pipeline/workflows/${id}`)
export const validateWorkflow = (data) => {
  console.log('%c[PIPELINE API] validateWorkflow', 'color: #409eff', data)
  return http.post('/pipeline/workflows/validate', data);
}

// ===== 工作流执行 =====
export const runWorkflow = (id, params) =>
  http.post(`/pipeline/workflows/${id}/run`, params || {})
export const listWorkflowRuns = (id, params) =>
  id ? http.get(`/pipeline/workflows/${id}/runs`, { params }) : Promise.reject(new Error("workflowId required"))

// ===== 运行详情 =====
export const getRun = (runId) => http.get(`/pipeline/runs/${runId}`)
export const getRunResult = (runId) => http.get(`/pipeline/runs/${runId}/result`)
