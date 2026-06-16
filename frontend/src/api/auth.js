import http from './http'

export const authApi = {
  login: (data) => http.post('/api/v1/auth/login', data),
  register: (data) => http.post('/api/v1/auth/register', data),
  me: () => http.get('/api/v1/auth/me'),
  refresh: (refreshToken) => http.post('/api/v1/auth/refresh', { refreshToken }),
  logout: (refreshToken) => http.post('/api/v1/auth/logout', { refreshToken })
}
