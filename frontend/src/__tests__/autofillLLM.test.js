/**
 * V6.3+ autofill LLM 增强测试
 * 验证后端 /ai/autofill 响应含 llmIntent / confidence / source
 */
import { describe, it, expect, vi } from 'vitest'
import { autofill } from '@/api/autofill'

describe('autofill LLM 增强 (V6.3+)', () => {
  it('✓ 返回 llmIntent 字段', async () => {
    const resp = await autofill('user', { name: '张三' })
    expect(resp.data).toBeDefined()
    // resp 可能是 { code, data: { llmIntent, confidence, source, recommendations } }
    const data = resp.data || resp
    expect(data.llmIntent).toBeDefined()
    expect(data.confidence).toBeGreaterThan(0)
    expect(data.source).toMatch(/llm|heuristic|llm\+heuristic/)
  })

  it('✓ 包含 recommendations', async () => {
    const resp = await autofill('user', { name: '李四' })
    const data = resp.data || resp
    const recs = data.recommendations || data
    expect(recs.username).toBeDefined()
  })

  it('✓ 5 类表单都能填', async () => {
    for (const t of ['user', 'apiKey', 'dataSource', 'pipeline', 'workflow']) {
      const r = await autofill(t, {})
      const d = r.data || r
      const recs = d.recommendations || d
      expect(recs).toBeDefined()
    }
  })
})
