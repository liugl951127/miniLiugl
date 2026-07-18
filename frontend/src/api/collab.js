/**
 * @file collab API 调用层 (V3.5.12+)
 *
 */
// V2.8.7 实时协作 API
import http from './http'

// ===== 房间 CRUD =====
export const createRoom = (data) => http.post('/collab/rooms', data)
export const getRoom = (roomId) => http.get(`/collab/rooms/${roomId}`)
export const listPublicRooms = (limit = 50) => http.get('/collab/rooms/public', { params: { limit } })
export const closeRoom = (roomId, userId) => http.delete(`/collab/rooms/${roomId}`, { params: { userId } })

// ===== 参与者 / 消息 =====
export const getParticipants = (roomId, onlineOnly = true) =>
  http.get(`/collab/rooms/${roomId}/participants`, { params: { onlineOnly } })
export const getMessages = (roomId, limit = 50) =>
  http.get(`/collab/rooms/${roomId}/messages`, { params: { limit } })

// ===== WebSocket 连接 =====
export const WS_COLLAB_PATH = '/ws/collab'

/**
 * 构建协作 WebSocket URL
 * @param {string} roomId 房间 ID
 * @param {object} user   { id, username, nickname, avatar }
 * @returns {string} 完整 WebSocket URL
 */
export function buildCollabWsUrl(roomId, user) {
  const base = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host
  const params = new URLSearchParams({
    roomId,
    userId: user.id,
    username: user.username || user.nickname || 'user',
    nickname: user.nickname || user.username || 'User',
    avatar: user.avatar || ''
  })
  return `${base}${WS_COLLAB_PATH}?${params.toString()}`
}
