import http from './http'

/** 会话管理 */
export const sessionApi = {
  list: (params) => http.get('/api/v1/sessions', { params }),
  create: (data) => http.post('/api/v1/sessions', data),
  detail: (id) => http.get(`/api/v1/sessions/${id}`),
  update: (id, data) => http.put(`/api/v1/sessions/${id}`, data),
  remove: (id) => http.delete(`/api/v1/sessions/${id}`)
}

/** 消息管理 */
export const messageApi = {
  list: (sessionId, params) => http.get(`/api/v1/sessions/${sessionId}/messages`, { params }),
  append: (sessionId, data) => http.post(`/api/v1/sessions/${sessionId}/messages`, data)
}
