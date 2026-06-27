// 训练任务 API (Day 23)
import http from './http'

export const trainingApi = {
  /** 可训练模型列表 */
  listModels: () => http.get('/training/models'),

  /** 创建训练任务 */
  createTask: (data) => http.post('/training/tasks', data),

  /** 我的训练任务列表 */
  listTasks: () => http.get('/training/tasks'),

  /** 查询任务详情 */
  getTask: (id) => http.get(`/training/tasks/${id}`),

  /** 取消训练任务 */
  cancelTask: (id) => http.post(`/training/tasks/${id}/cancel`),
}
