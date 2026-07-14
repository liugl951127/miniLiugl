// Function Call 工具管理 API (V3.5.5+ 新增, 对应 minimax-function 模块)
import http from './http'

export const functionApi = {
  listTools: (category) => category
    ? http.get(`/api/v1/function/tools/category/${category}`)
    : http.get('/api/v1/function/tools'),
  getTool: (id) => http.get(`/api/v1/function/tools/${id}`),
  getToolByName: (name) => http.get(`/api/v1/function/tools/by-name/${name}`),
  createTool: (data) => http.post('/api/v1/function/tools', data),
  updateTool: (id, data) => http.put(`/api/v1/function/tools/${id}`, data),
  deleteTool: (id) => http.delete(`/api/v1/function/tools/${id}`),
  invoke: (name, args) => http.post(`/api/v1/function/invoke/${name}`, args),
  logs: (params) => http.get('/api/v1/function/logs', { params })
}
