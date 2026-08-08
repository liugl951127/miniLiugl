/**
 * V6.3+ autofill LLM 增强测试 (mock 版)
 * 不真发请求, 验证前端调用逻辑
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// mock http - 在 import autofill 之前
vi.mock('@/api/http', () => ({
  default: {
    post: vi.fn((url, body) => Promise.resolve({
      data: {
        code: 200,
        data: {
          llmIntent: 'form_user',
          confidence: 0.85,
          source: 'llm+heuristic',
          recommendations: {
            username: body?.context?.name?.toLowerCase() || 'new_user',
            nickname: body?.context?.name || '新用户',
            role: 'user'
          }
        }
      }
    })),
    get: vi.fn((url) => Promise.resolve({
      data: { code: 200, data: { mock: true, path: url } }
    }))
  }
}))

import http from '@/api/http'
import { autofill, previewForm, recommendField } from '@/api/autofill'

describe('autofill LLM 增强 (V6.3+ mock)', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('✓ autofill 返回 LLM 增强响应', async () => {
    const resp = await autofill('user', { name: '张三' })
    expect(resp.data.code).toBe(200)
    expect(resp.data.data.llmIntent).toBe('form_user')
    expect(resp.data.data.confidence).toBeGreaterThan(0)
    expect(resp.data.data.source).toMatch(/llm/)
  })

  it('✓ recommendations 含 username', async () => {
    const resp = await autofill('user', { name: '李四' })
    expect(resp.data.data.recommendations.username).toBe('李四')
  })

  it('✓ 5 类表单都能填', async () => {
    for (const t of ['user', 'apiKey', 'dataSource', 'pipeline', 'workflow']) {
      const r = await autofill(t, {})
      expect(r.data.code).toBe(200)
      expect(r.data.data.recommendations).toBeDefined()
    }
  })

  it('✓ previewForm 调用 GET', async () => {
    await previewForm('user')
    expect(http.get).toHaveBeenCalled()
  })

  it('✓ recommendField 调用 GET', async () => {
    await recommendField('user', 'role')
    expect(http.get).toHaveBeenCalled()
  })
})
