/**
 * @file function API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-function
 * 接口数: 10
 *
 *   GET    /api/v1/function/tools
 *   GET    /api/v1/function/tools/category/{category}
 *   GET    /api/v1/function/tools/{id}
 *   GET    /api/v1/function/tools/by-name/{name}
 *   POST   /api/v1/function/tools
 *   PUT    /api/v1/function/tools/{id}
 *   DELETE /api/v1/function/tools/{id}
 *   POST   /api/v1/function/invoke/{name}
 *   ... 共 10 个
 */
// Function Call 工具管理 API (V3.5.5+ 新增, 对应 minimax-function 模块)
import http from './http'

export const functionApi = {
  listTools: (category) => category
    ? http.get(`/api/v1/function/tools/category/${category}`)
    : http.get('/api/v1/function/tools'),
  /**
   * getTool - 查询 
   * @returns GET  的响应 Promise
   */
  getTool: (id) => http.get(`/api/v1/function/tools/${id}`),
  /**
   * getToolByName - 查询 
   * @returns GET  的响应 Promise
   */
  getToolByName: (name) => http.get(`/api/v1/function/tools/by-name/${name}`),
  /**
   * createTool - 创建/更新 /api/v1/function/tools
   * @returns POST /api/v1/function/tools 的响应 Promise
   */
  createTool: (data) => http.post('/api/v1/function/tools', data),
  /**
   * updateTool - 替换 
   * @returns PUT  的响应 Promise
   */
  updateTool: (id, data) => http.put(`/api/v1/function/tools/${id}`, data),
  /**
   * deleteTool - 删除 
   * @returns DELETE  的响应 Promise
   */
  deleteTool: (id) => http.delete(`/api/v1/function/tools/${id}`),
  /**
   * invoke - 创建/更新 
   * @returns POST  的响应 Promise
   */
  invoke: (name, args) => http.post(`/api/v1/function/invoke/${name}`, args),
  /**
   * logs - 查询 /api/v1/function/logs
   * @returns GET /api/v1/function/logs 的响应 Promise
   */
  logs: (params) => http.get('/api/v1/function/logs', { params })
}
