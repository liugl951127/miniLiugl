// RAG 知识库 API (V5.24)
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

// 文档 (Document)
export const uploadDoc = (ownerId, kbId, file, opts = {}) => {
  const form = new FormData()
  form.append('file', file)
  const params = new URLSearchParams({ ownerId, kbId })
  if (opts.title) params.append('title', opts.title)
  if (opts.sourceType) params.append('sourceType', opts.sourceType)
  if (opts.tags) params.append('tags', opts.tags)
  return http.post(`/rag/doc/upload?${params}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
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