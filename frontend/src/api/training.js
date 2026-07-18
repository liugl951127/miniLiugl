/**
 * @file training API 调用层 (V3.5.12+)
 *
 */
// 训练任务 API (Day 23)
import http from './http'

export const trainingApi = {
  /** 可训练模型列表 */
  /**
   * listModels - 查询 /training/models
   * @returns GET /training/models 的响应 Promise
   */
  listModels: () => http.get('/training/models'),

  /** 创建训练任务 */
  /**
   * createTask - 创建/更新 /training/tasks
   * @returns POST /training/tasks 的响应 Promise
   */
  createTask: (data) => http.post('/training/tasks', data),

  /** 我的训练任务列表 */
  /**
   * listTasks - 查询 /training/tasks
   * @returns GET /training/tasks 的响应 Promise
   */
  listTasks: () => http.get('/training/tasks'),

  /** 查询任务详情 */
  /**
   * getTask - 查询 
   * @returns GET  的响应 Promise
   */
  getTask: (id) => http.get(`/training/tasks/${id}`),

  /** 取消训练任务 */
  /**
   * cancelTask - 创建/更新 
   * @returns POST  的响应 Promise
   */
  cancelTask: (id) => http.post(`/training/tasks/${id}/cancel`),
}
