/**
 * @file webhook API 调用层 (V3.5.12+)
 *
 */
// V2.9.1 Webhook 集成 SDK
import http from './http'

export const list = (ownerId) => http.get('/ai/webhooks', { params: { ownerId } })
export const detail = (id) => http.get(`/ai/webhooks/${id}`)
export const create = (data) => {
  console.log('%c[WEBHOOK API] create', 'color: #409eff', data)
  return http.post('/ai/webhooks', data);
}
export const update = (id, data) => http.put(`/ai/webhooks/${id}`, data)
export const delete_ = (id) => http.delete(`/ai/webhooks/${id}`)
export const test = (id) => http.post(`/ai/webhooks/${id}/test`)
export const deliveries = (id, limit = 50) => http.get(`/ai/webhooks/${id}/deliveries`, { params: { limit } })
export const recentDeliveries = (limit = 50) => http.get('/ai/webhooks/deliveries', { params: { limit } })
export const eventTypes = () => {
  console.log('%c[WEBHOOK API] eventTypes', 'color: #409eff', '')
  return http.get('/ai/webhooks/events');
}
export const stats = () => {
  console.log('%c[WEBHOOK API] stats', 'color: #409eff', '')
  return http.get('/ai/webhooks/stats');
}
export const publish = (eventType, payload) => http.post('/ai/webhooks/publish', { eventType, payload })

export const webhookApi = {
  list, detail, create, update, delete: delete_, test, deliveries,
  recentDeliveries, eventTypes, stats, publish
}
