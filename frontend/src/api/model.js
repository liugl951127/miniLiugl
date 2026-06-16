import http from './http'

/** 模型路由 API */
export const modelApi = {
  list: () => http.get('/api/v1/models'),
  providers: () => http.get('/api/v1/models/providers'),
  chat: (data) => http.post('/api/v1/models/chat', data),
  /** 流式端点 - 不走 http.js 拦截器，自己用 fetch 处理 */
  streamUrl: '/api/v1/models/chat/stream'
}
