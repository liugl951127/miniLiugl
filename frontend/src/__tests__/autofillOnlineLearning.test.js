/**
 * V6.3+ autofill 在线学习测试
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/http', () => ({
  default: {
    post: vi.fn((url, body) => {
      if (url === '/ai/autofill/feedback') {
        return Promise.resolve({
          data: {
            code: 200,
            data: {
              ok: true,
              formType: body.formType,
              feedback: body.feedback,
              modelWeights: { TF: 0.5, NGRAM: 0.3, SYNONYM: 0.1, CONTEXT: 0.1 }
            }
          }
        })
      }
      return Promise.resolve({
        data: { code: 200, data: { mock: true, recommendations: {} } }
      })
    }),
    get: vi.fn((url) => Promise.resolve({
      data: { code: 200, data: { mock: true, path: url } }
    }))
  }
}))

import http from '@/api/http'
import { feedback, getStats } from '@/api/autofill'

describe('autofill 在线学习 (V6.3+ mock)', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('✓ feedback accept 调 POST', async () => {
    const r = await feedback('user', 'accept', { formId: 'f1', context: { name: '张三' } })
    expect(r.data.code).toBe(200)
    expect(r.data.data.ok).toBe(true)
    expect(r.data.data.feedback).toBe('accept')
  })

  it('✓ feedback correct 带 correctedIntent', async () => {
    const r = await feedback('user', 'correct', { formId: 'f1', correctedIntent: 'form_user' })
    expect(r.data.data.feedback).toBe('correct')
  })

  it('✓ feedback reject 调 POST', async () => {
    const r = await feedback('user', 'reject', { formId: 'f1' })
    expect(r.data.data.feedback).toBe('reject')
  })

  it('✓ getStats 调 GET', async () => {
    await getStats()
    expect(http.get).toHaveBeenCalledWith('/ai/autofill/stats')
  })
})
