import http from './http'

/** 通知列表（分页） */
export const listNotifications = (params) =>
  http.get('/api/v1/notifications', { params })

/** 未读数量 */
export const unreadCount = () =>
  http.get('/api/v1/notifications/unread-count')

/** 标记单条已读 */
export const markRead = (id) =>
  http.put(`/api/v1/notifications/${id}/read`)

/** 全部已读 */
export const markAllRead = () =>
  http.put('/api/v1/notifications/read-all')

/** 清空通知 */
export const clearNotifications = () =>
  http.delete('/api/v1/notifications')