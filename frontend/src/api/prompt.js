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
