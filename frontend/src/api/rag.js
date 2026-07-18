/**
 * @file rag API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-rag
 * 接口数: 13
 *
 *   POST   /api/v1/rag/kb
 *   GET    /api/v1/rag/kb
 *   GET    /api/v1/rag/kb/public
 *   GET    /api/v1/rag/kb/{id}
 *   DELETE /api/v1/rag/kb/{id}
 *   PUT    /api/v1/rag/kb/{id}
 *   POST   /api/v1/rag/doc/upload
 *   GET    /api/v1/rag/doc
 *   ... 共 13 个
 */
// RAG 知识库 API (V5.24 + V5.22 进度 + 可取消)
import http from './http'

// 知识库 (KB)
export const createKb = (ownerId, body) =>
  http.post(`/rag/kb?ownerId=${ownerId}`, body)

export const listMyKbs = (ownerId) =>
  http.get(`/rag/kb?ownerId=${ownerId}`)

export const listPublicKbs = () =>
  http.get('/rag/kb/public')

export const getKb = (id, ownerId) =>
  http.get(`/rag/kb/${id}?ownerId=${ownerId}`)

export const deleteKb = (id, ownerId) =>
  http.delete(`/rag/kb/${id}?ownerId=${ownerId}`)

// V5.33 Day 23: 更新知识库（元数据编辑）
export const updateKb = (id, ownerId, patch) =>
  http.put(`/rag/kb/${id}?ownerId=${ownerId}`, patch)

// V5.33 Day 23: 重命名文档
export const renameDoc = (id, ownerId, title) =>
  http.put(`/rag/doc/${id}?ownerId=${ownerId}`, { title })

// 文档 (Document)
// V5.22: uploadDoc 返回 { promise, cancel }
// opts: { title, sourceType, tags, onProgress(pct, loaded, total) }
export const uploadDoc = (ownerId, kbId, file, opts = {}) => {
  const form = new FormData()
  form.append('file', file)
  const params = new URLSearchParams({ ownerId, kbId })
  if (opts.title) params.append('title', opts.title)
  if (opts.sourceType) params.append('sourceType', opts.sourceType)
  if (opts.tags) params.append('tags', opts.tags)

  const cfg = {
    headers: { 'Content-Type': 'multipart/form-data' },
  }

  // V5.22: 支持进度回调
  if (typeof opts.onProgress === 'function') {
    cfg.onUploadProgress = (e) => {
      const pct = e.total > 0 ? Math.round((e.loaded / e.total) * 100) : 0
      opts.onProgress(pct, e.loaded, e.total)
    }
  }

  const promise = http.post(`/rag/doc/upload?${params}`, form, cfg)
  return { promise, cancel: null } // cancel 由调用方通过 AbortController 管理
}

// 使用 AbortController 的上传版本
export const uploadDocWithCancel = (ownerId, kbId, file, opts = {}) => {
  const controller = new AbortController()
  const form = new FormData()
  form.append('file', file)
  const params = new URLSearchParams({ ownerId, kbId })
  if (opts.title) params.append('title', opts.title)
  if (opts.sourceType) params.append('sourceType', opts.sourceType)
  if (opts.tags) params.append('tags', opts.tags)

  const cfg = {
    headers: { 'Content-Type': 'multipart/form-data' },
    signal: controller.signal,
  }

  if (typeof opts.onProgress === 'function') {
    cfg.onUploadProgress = (e) => {
      const pct = e.total > 0 ? Math.round((e.loaded / e.total) * 100) : 0
      opts.onProgress(pct, e.loaded, e.total)
    }
  }

  return {
    promise: http.post(`/rag/doc/upload?${params}`, form, cfg),
    cancel: () => controller.abort(),
  }
}

export const listDocs = (kbId, limit = 50) =>
  http.get(`/rag/doc?kbId=${kbId}&limit=${limit}`)

export const listChunks = (docId) =>
  http.get(`/rag/doc/${docId}/chunks`)

export const deleteDoc = (id, ownerId) =>
  http.delete(`/rag/doc/${id}?ownerId=${ownerId}`)

// 检索 + 问答
export const retrieve = (body) => http.post('/rag/retrieve', body)

export const ask = (body) => http.post('/rag/ask', body)
