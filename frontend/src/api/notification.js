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

/** 清空通知（清空所有） */
export const clearNotifications = () =>
  http.delete('/auth/notifications')

/** 删除单条通知 */
export const deleteNotification = (id) =>
  http.delete(`/auth/notifications/${id}`)

/**
 * 通知设置 API (T1-mock-fix) - 后端 /api/v1/notification/settings
 *
 * 后端模型:
 *   channels: csv (email/sms/dingtalk/webhook/push)
 *   events:   csv (login/error/alert/system)
 *   quietStart, quietEnd: HH:mm
 *
 * 前端表单 (views/notification/Index.vue):
 *   system/task/message/warning (events) + email/sound (channels)
 *
 * 为避免破坏后端校验, 在前端做映射:
 *   events:   system->system, task->alert, message->login, warning->error
 *   channels: email->email, sound->push
 */
export const notificationApi = {
  /** 取当前用户通知设置 (无则返回默认) */
  getSettings: () => http.get('/notification/settings'),
  /** 保存通知设置 (upsert) - body: { channels, events, quietStart, quietEnd } */
  updateSettings: (form) => {
    const events = []
    if (form.system) events.push('system')
    if (form.task) events.push('alert')
    if (form.message) events.push('login')
    if (form.warning) events.push('error')
    const channels = []
    if (form.email) channels.push('email')
    if (form.sound) channels.push('push')
    return http.put('/notification/settings', {
      events: events.join(',') || 'system',
      channels: channels.join(',') || 'email',
      quietStart: '22:00',
      quietEnd: '08:00',
    })
  },
  /** 把后端返回的 settings 解析回前端 form 格式 */
  parseToForm: (settings) => {
    if (!settings) {
      return { system: true, task: true, message: true, warning: true, email: true, sound: false }
    }
    const events = String(settings.events || '').split(',').map(s => s.trim())
    const channels = String(settings.channels || '').split(',').map(s => s.trim())
    return {
      system: events.includes('system'),
      task: events.includes('alert'),
      message: events.includes('login'),
      warning: events.includes('error'),
      email: channels.includes('email'),
      sound: channels.includes('push'),
    }
  },
}
