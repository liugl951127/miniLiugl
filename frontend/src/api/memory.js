// 记忆管理 API (V5.24)
import http from './http'

// 短期记忆 (按 session)
export const getShortTerm = (sessionId) =>
  http.get(`/memory/short-term/${sessionId}`)

export const appendShortTerm = (sessionId, body) =>
  http.post(`/memory/short-term/${sessionId}`, body)

export const clearShortTerm = (sessionId) =>
  http.delete(`/memory/short-term/${sessionId}`)

export const shortTermSize = (sessionId) =>
  http.get(`/memory/short-term/${sessionId}/size`)

// 上下文构建
export const buildContext = (sessionId, body) =>
  http.post(`/memory/context/${sessionId}`, body)

export const crossContext = (body) =>
  http.post('/memory/cross-context', body)

// 摘要
export const summarize = (sessionId) =>
  http.post(`/memory/summarize/${sessionId}`)

export const getSummary = (sessionId) =>
  http.get(`/memory/summary/${sessionId}`)

// 长期记忆
export const storeLongTerm = (body) =>
  http.post('/memory/long-term', body)

export const recallLongTerm = (body) =>
  http.post('/memory/long-term/recall', body)

export const recentLongTerm = (userId, limit = 50) =>
  http.get(`/memory/long-term/recent?userId=${userId}&limit=${limit}`)

export const deleteLongTerm = (id, userId) =>
  http.delete(`/memory/long-term/${id}?userId=${userId}`)

// 偏好
export const setPref = (userId, key, value, source) =>
  http.put(`/memory/pref/${key}?userId=${userId}`, { value, source })

export const getPref = (userId, key) =>
  http.get(`/memory/pref/${key}?userId=${userId}`)

export const listPref = (userId) =>
  http.get(`/memory/pref?userId=${userId}`)