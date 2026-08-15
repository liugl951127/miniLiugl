/**
 * @file training API 调用层 (V3.5.12+)
 *
 */
// 训练任务 API (Day 23)
import http from './http'

export const trainingApi = {
  listModels: () => http.get('/training/models'),
  createTask: (data) => http.post('/training/tasks', data),
  listTasks: () => http.get('/training/tasks'),
  getTask: (id) => http.get(`/training/tasks/${id}`),
  cancelTask: (id) => http.post(`/training/tasks/${id}/cancel`),
  getHistory: (id) => http.get(`/training/tasks/${id}/history`),
  /** 训练日志：返回 List<Map>，fallback 为空数组 */
  getLogs: (id) => http.get(`/training/tasks/${id}/logs`).catch(() => ({ data: [] })),
  /** V7.0 Flow④: 启用训练好的模型 */
  enableModel: (taskId) => http.post(`/training-impl/tasks/${taskId}/enable`),
  /** V7.1: Dashboard 总览数据 */
  dashboardOverview: () => http.get('/training-impl/dashboard/overview'),
}
