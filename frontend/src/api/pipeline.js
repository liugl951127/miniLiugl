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
  return http.post('/pipeline/workflows', data);
}
export const updateWorkflow = (id, data) => http.put(`/pipeline/workflows/${id}`, data)
export const deleteWorkflow = (id) => http.delete(`/pipeline/workflows/${id}`)
export const validateWorkflow = (data) => {
  return http.post('/pipeline/workflows/validate', data);
}

// ===== 工作流执行 =====
export const runWorkflow = (id, params) =>
  http.post(`/pipeline/workflows/${id}/run`, params || {})
export const listWorkflowRuns = (id, params) =>
  id ? http.get(`/pipeline/workflows/${id}/runs`, { params }) : Promise.reject(new Error("workflowId required"))

// ===== 运行详情 =====
export const getRun = (runId) => http.get(`/pipeline/runs/${runId}`)
export const getRunResult = (runId, outputNodeId) =>
  http.get(`/pipeline/runs/${runId}/result`, { params: { outputNodeId } })

/**
 * 获取最近 N 次运行 (聚合多个工作流的运行历史)
 * 后端无全局 "list all runs" 接口, 因此:
 *   1. 先拉取最近 N 个工作流
 *   2. 对每个工作流拉取其运行历史
 *   3. 合并, 按 startTime 倒序, 取最近 N 条
 *
 * @param {number} [limit=5] 返回条数
 * @param {number} [workflowLimit=5] 遍历的工作流数
 * @returns {Promise<Array<{runId, workflowId, workflowName, status, startTime, duration}>>}
 */
export const listRecentRuns = async (limit = 5, workflowLimit = 5) => {
  try {
    const wfResp = await listWorkflows({ limit: workflowLimit })
    const workflows = wfResp?.data?.list || wfResp?.data || []
    if (!Array.isArray(workflows) || workflows.length === 0) return []

    const wfMap = new Map(workflows.map(wf => [wf.id, wf]))

    // 并行拉取每个工作流的运行历史
    const allRunLists = await Promise.all(
      workflows.map(async (wf) => {
        try {
          const r = await listWorkflowRuns(wf.id, { limit: Math.max(limit, 5) })
          const runs = r?.data || []
          return runs.map(run => ({
            ...run,
            workflowId: wf.id,
            workflowName: wf.name || wf.id
          }))
        } catch (_) {
          return []
        }
      })
    )

    const merged = allRunLists.flat()
    // 按 startTime 倒序
    merged.sort((a, b) => {
      const ta = new Date(a.startTime || a.createdAt || 0).getTime()
      const tb = new Date(b.startTime || b.createdAt || 0).getTime()
      return tb - ta
    })
    return merged.slice(0, limit)
  } catch (e) {
    return []
  }
}
