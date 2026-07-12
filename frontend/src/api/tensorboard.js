// V2.8.8 TensorBoard 自托管 API
import http from './http'

// Runs
export const listRuns = () => http.get('/tensorboard/runs')

// Tags
export const listTags = (runId) => http.get(`/tensorboard/runs/${runId}/tags`)

// Scalars
export const readScalar = (runId, tag) => http.get(`/tensorboard/runs/${runId}/scalars/${tag}`)

// Events (WandB 兼容)
export const readEvents = (runId) => http.get(`/tensorboard/runs/${runId}/events`)

// Health
export const health = () => http.get('/tensorboard/health')

// 写入 (供训练回调)
export const writeScalar = (runId, tag, step, value) =>
  http.post(`/tensorboard/runs/${runId}/scalars/${tag}`, { step, value })

export const tensorboardApi = {
  listRuns, listTags, readScalar, readEvents, health, writeScalar
}
