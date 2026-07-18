/**
 * @file prompt API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-prompt
 * 接口数: 6
 *
 *   GET    /api/v1/prompts/{id}
 *   GET    /api/v1/prompts/categories
 *   PUT    /api/v1/prompts/{id}
 *   DELETE /api/v1/prompts/{id}
 *   POST   /api/v1/prompts/{id}/use
 *   POST   /api/v1/prompts/resolve
 */
import http from './http'

const BASE = '/api/v1/prompts'

/** 分页列表 */
export const promptApi = {
  list(params) {
    return http.get(BASE, { params })
  },
  get(id) {
    return http.get(`${BASE}/${id}`)
  },
  create(data) {
    return http.post(BASE, data)
  },
  update(id, data) {
    return http.put(`${BASE}/${id}`, data)
  },
  remove(id) {
    return http.delete(`${BASE}/${id}`)
  },
  use(id) {
    return http.post(`${BASE}/${id}/use`)
  },
  categories() {
    return http.get(`${BASE}/categories`)
  },
  resolve(data) {
    return http.post(`${BASE}/resolve`, data)
  }
}
