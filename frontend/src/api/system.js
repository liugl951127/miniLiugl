import http from './http'

export const systemApi = {
  health: () => http.get('/api/v1/health'),
  intro: () => http.get('/api/v1/intro')
}

export const authApi = {
  login: (data) => http.post('/api/v1/auth/login', data),
  register: (data) => http.post('/api/v1/auth/register', data),
  me: () => http.get('/api/v1/auth/me'),
  refresh: () => http.post('/api/v1/auth/refresh'),
  logout: () => http.post('/api/v1/auth/logout')
}

export const sessionApi = {
  list: (params) => http.get('/api/v1/sessions', { params }),
  create: (data) => http.post('/api/v1/sessions', data),
  detail: (id) => http.get(`/api/v1/sessions/${id}`),
  update: (id, data) => http.put(`/api/v1/sessions/${id}`, data),
  remove: (id) => http.delete(`/api/v1/sessions/${id}`),
  messages: (id, params) => http.get(`/api/v1/sessions/${id}/messages`, { params })
}

export const chatApi = {
  send: (data) => http.post('/api/v1/chat/send', data),
  // 流式 - 直接用 EventSource / fetch SSE, 见 views/chat/Index.vue
  streamUrl: '/api/v1/chat/stream'
}

export const modelApi = {
  list: () => http.get('/api/v1/models'),
  providers: () => http.get('/api/v1/models/providers')
}

export const kbApi = {
  list: (params) => http.get('/api/v1/knowledge-bases', { params }),
  create: (data) => http.post('/api/v1/knowledge-bases', data),
  remove: (id) => http.delete(`/api/v1/knowledge-bases/${id}`),
  documents: (id, params) => http.get(`/api/v1/knowledge-bases/${id}/documents`, { params }),
  upload: (id, formData) => http.post(`/api/v1/knowledge-bases/${id}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
