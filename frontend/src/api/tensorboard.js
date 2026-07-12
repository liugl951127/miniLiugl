// V2.8.9 TensorBoard 自托管 API (含统计分析)
import http from './http'

// Runs
export const listRuns = () => http.get('/tensorboard/runs')

// Tags
export const listTags = (runId) => http.get(`/tensorboard/runs/${runId}/tags`)

// Scalars
export const readScalar = (runId, tag) => http.get(`/tensorboard/runs/${runId}/scalars/${tag}`)

// Events (WandB 兼容)
export const readEvents = (runId) => http.get(`/tensorboard/runs/${runId}/events`)

// V2.8.9: 统计分析
export const readStats = (runId, tag) => http.get(`/tensorboard/runs/${runId}/stats/${tag}`)

// V2.8.9: 直方图
export const readHistogram = (runId, tag, bins = 20) =>
  http.get(`/tensorboard/runs/${runId}/histogram/${tag}`, { params: { bins } })

// V2.8.9: 多 run 对比
export const compareRuns = (runIds, tag) =>
  http.post('/tensorboard/runs/compare', { runIds, tag })

// Health
export const health = () => http.get('/tensorboard/health')

// 写入 (供训练回调)
export const writeScalar = (runId, tag, step, value) =>
  http.post(`/tensorboard/runs/${runId}/scalars/${tag}`, { step, value })

export const tensorboardApi = {
  listRuns, listTags, readScalar, readEvents, readStats, readHistogram, compareRuns, health, writeScalar
}
