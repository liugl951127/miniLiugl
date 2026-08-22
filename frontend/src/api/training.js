/**
 * @file training API 调用层 (V3.5.12+)
 *
 * 训练任务 API (Day 23)
 * 训练模型 API (T1-mock-fix) - 后端 /api/v1/training/models (minimax-ai)
 */
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

/**
 * 训练好的模型 API (T1-mock-fix)
 * 端点: /api/v1/training/models
 *  - GET    /api/v1/training/models
 *  - POST   /api/v1/training/models
 *  - GET    /api/v1/training/models/{id}
 *  - DELETE /api/v1/training/models/{id}
 *  - PUT    /api/v1/training/models/{id}/status
 *  - POST   /api/v1/training/models/{id}/publish
 *  - POST   /api/v1/training/models/{id}/test
 */
export const trainedModelApi = {
  /** 列表 (分页 + status 过滤) */
  list: (params = {}) => http.get('/training/models', { params }),
  /** 详情 */
  get: (id) => http.get(`/training/models/${id}`),
  /** 创建 - body: { code, name, accuracy, status } */
  create: (data) => http.post('/training/models', data),
  /** 删除 */
  remove: (id) => http.delete(`/training/models/${id}`),
  /** 别名 */
  delete: (id) => http.delete(`/training/models/${id}`),
  /** 启停 - body: { status: 'ENABLED' | 'DISABLED' | 'DRAFT' } */
  changeStatus: (id, status) => http.put(`/training/models/${id}/status`, { status }),
  /** 发布 (设置 publishedAt) */
  publish: (id) => http.post(`/training/models/${id}/publish`),
  /** 模型测试 (返回 { accuracy, latencyMs, sampleOutput }) */
  test: (id) => http.post(`/training/models/${id}/test`),
}
