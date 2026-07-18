/**
 * @file notification API 调用层 (V3.5.12+)
 *
 */
import http from './http'

/** 通知列表（分页） */
// V1.9.1: 后端 NotificationController 在 auth 模块, 路径改为 /auth/notifications
// http.js 拦截器自动补 /api/v1 前缀
export const listNotifications = (params) =>
  http.get('/auth/notifications', { params })

/** 未读数量 */
export const unreadCount = () =>
  http.get('/auth/notifications/unread-count')

/** 标记单条已读 */
export const markRead = (id) =>
  http.put(`/auth/notifications/${id}/read`)

/** 全部已读 */
export const markAllRead = () =>
  http.put('/auth/notifications/read-all')

/** 清空通知 */
export const clearNotifications = () =>
  http.delete('/auth/notifications')