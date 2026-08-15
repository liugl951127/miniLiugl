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
  ownerId ? http.post(`/rag/kb?ownerId=${ownerId}`, body) : http.post('/rag/kb', body)

export const listMyKbs = (ownerId) =>
  ownerId ? http.get(`/rag/kb?ownerId=${ownerId}`) : http.get('/rag/kb')

export const listPublicKbs = () =>
  http.get('/rag/kb/public')

export const getKb = (id, ownerId) =>
  ownerId ? http.get(`/rag/kb/${id}?ownerId=${ownerId}`) : http.get(`/rag/kb/${id}`)

export const deleteKb = (id, ownerId) =>
  ownerId ? http.delete(`/rag/kb/${id}?ownerId=${ownerId}`) : http.delete(`/rag/kb/${id}`)

// V5.33 Day 23: 更新知识库（元数据编辑）
export const updateKb = (id, ownerId, patch) =>
  ownerId ? http.put(`/rag/kb/${id}?ownerId=${ownerId}`, patch) : http.put(`/rag/kb/${id}`, patch)

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

/** 获取文档完整内容 (Day 44) */
export const getDocContent = (docId) =>
  http.get(`/rag/doc/${docId}/content`)

// 检索 + 问答
export const retrieve = (body) => {
  console.log('%c[RAG API] retrieve', 'color: #409eff', body)
  return http.post('/rag/retrieve', body);
}

export const ask = (body) => {
  console.log('%c[RAG API] ask', 'color: #409eff', body)
  return http.post('/rag/ask', body);
}

/**
 * SSE 流式上传 (Day 41) + 自动重试 (Day 42)
 * 使用 fetch + ReadableStream 实现 SSE，EventSource 不支持 POST 文件上传
 * 网络错误 / 502 / 503 / 504 自动重试，最多 MAX_RETRIES 次，指数退避
 *
 * @param {string} ownerId
 * @param {string} kbId
 * @param {File} file
 * @param {{ title?, sourceType?, tags?, onProgress(stage, pct, message, docId)?, onRetry?(attempt, maxRetries, delayMs)? }} opts
 * @returns {{ cancel: function, promise: Promise }}
 */
export const uploadDocStream = (ownerId, kbId, file, opts = {}) => {
  const MAX_RETRIES = 3
  const params = new URLSearchParams({ ownerId, kbId })
  if (opts.title) params.append('title', opts.title)
  if (opts.sourceType) params.append('sourceType', opts.sourceType)
  if (opts.tags) params.append('tags', opts.tags)

  const baseUrl = (window.__API_BASE__ || '') + '/api/v1'
  const url = `${baseUrl}/rag/doc/upload-stream?${params}`

  const form = new FormData()
  form.append('file', file)

  let reader = null
  let aborted = false

  /**
   * 判断错误是否可重试
   * - 网络错误 / 超时 / 502 / 503 / 504 / 429 / 599 均可重试
   * - 业务错误（如解析失败 dedup 等）不重试
   */
  function isRetryable(err, attempt) {
    if (attempt >= MAX_RETRIES) return false
    const msg = (err.message || '').toLowerCase()
    // 明确不可重试：dedup 已存在 / 400 参数错误 / 401 未登录 / 403 无权
    if (msg.includes('已存在') || msg.includes('already exists')) return false
    if (msg.includes('http 400') || msg.includes('bad request')) return false
    if (msg.includes('http 401') || msg.includes('http 403')) return false
    // 可重试：网络类 / 服务端错误 / 超时
    if (msg.includes('failed to fetch') || msg.includes('network') || msg.includes('timeout')) return true
    const m = msg.match(/http (\d+)/)
    if (m) {
      const code = parseInt(m[1])
      return [429, 502, 503, 504, 599].includes(code)
    }
    return true // 默认认为可重试
  }

  function doUpload(attempt) {
    return new Promise((resolve, reject) => {
      let localReader = null
      let localAborted = false

      fetch(url, {
        method: 'POST',
        body: form,
        headers: { 'Accept': 'text/event-stream' }
      }).then(response => {
        if (!response.ok) {
          reject(new Error('HTTP ' + response.status))
          return
        }
        const stream = response.body
        if (!stream) {
          reject(new Error('No stream'))
          return
        }
        localReader = stream.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        function read() {
          if (localAborted || aborted) return
          localReader.read().then(({ done, value }) => {
            if (done || localAborted || aborted) {
              return
            }
            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')
            buffer = lines.pop() || ''
            for (const line of lines) {
              if (line.startsWith('data:')) {
                const json = line.slice(5).trim()
                if (!json) continue
                try {
                  const data = JSON.parse(json)
                  if (data.stage === 'ERROR' || data.error) {
                    reject(new Error(data.error || data.message))
                    localAborted = true
                    aborted = true
                  } else if (opts.onProgress) {
                    opts.onProgress(data)
                  }
                } catch {}
              }
            }
            read()
          }).catch(e => {
            if (!localAborted && !aborted) reject(e)
          })
        }
        read()
      }).catch(e => {
        if (!aborted) reject(e)
      })

      // 外部 cancel 时也取消 reader
      const origCancel = reader ? reader.cancel.bind(reader) : () => {}
      reader = {
        cancel: () => {
          localAborted = true
          if (localReader) localReader.cancel()
        }
      }
    })
  }

  const promise = (async () => {
    let lastError
    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      if (aborted) break
      try {
        await doUpload(attempt)
        return // 成功则 resolve
      } catch (e) {
        lastError = e
        if (!isRetryable(e, attempt) || aborted) break
        const delay = Math.min(1000 * Math.pow(2, attempt), 10000) // 1s, 2s, 4s, max 10s
        if (opts.onRetry) {
          opts.onRetry(attempt + 1, MAX_RETRIES + 1, Math.round(delay))
        }
        await new Promise(r => setTimeout(r, delay))
      }
    }
    throw lastError
  })()

  return {
    promise,
    cancel() {
      aborted = true
      if (reader) reader.cancel()
    }
  }
}
