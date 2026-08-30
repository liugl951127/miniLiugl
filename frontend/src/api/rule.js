/**
 * @file rule API 调用层 (T1-mock-fix)
 *
 * 后端: /api/v1/rule (minimax-pipeline module)
 * 端点: POST/GET/PUT/DELETE
 *
 * 前端对接: views/rule/Index.vue saveRule() / deleteRule()
 */
import http from './http'

export const ruleApi = {
  /** 规则列表 (分页) - GET /api/v1/rule */
  list: (params = {}) => http.get('/rule', { params }),
  /** 规则详情 - GET /api/v1/rule/{id} */
  get: (id) => http.get(`/rule/${id}`),
  /** 创建规则 - POST /api/v1/rule */
  create: (data) => http.post('/rule', data),
  /** 更新规则 - PUT /api/v1/rule/{id} */
  update: (id, data) => http.put(`/rule/${id}`, data),
  /** 删除规则 (软删) - DELETE /api/v1/rule/{id} */
  remove: (id) => http.delete(`/rule/${id}`),
  /** 别名: delete 同 remove */
  delete: (id) => http.delete(`/rule/${id}`),
  /** V9.1: AI 生成规则 (自然语言 → JSON DSL) - POST /api/v1/rule/ai-generate */
  aiGenerate: (text) => http.post('/rule/ai-generate', { text }),
}
