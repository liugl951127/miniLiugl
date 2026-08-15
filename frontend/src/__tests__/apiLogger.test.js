import { describe, it, expect } from 'vitest'
import apiLog from '@/api/logger'

describe('API Logger (V6.2+)', () => {
  it('应该导出所有方法', () => {
    expect(typeof apiLog.request).toBe('function')
    expect(typeof apiLog.response).toBe('function')
    expect(typeof apiLog.error).toBe('function')
    expect(typeof apiLog.businessError).toBe('function')
    expect(typeof apiLog.warn).toBe('function')
    expect(typeof apiLog.debug).toBe('function')
  })

  it('request 应该不抛异常', () => {
    expect(() => apiLog.request('GET', '/test', { foo: 'bar' })).not.toThrow()
  })

  it('response 应该不抛异常', () => {
    expect(() => apiLog.response('GET', '/test', 200, { ok: true }, 123)).not.toThrow()
  })

  it('error 应该处理 axios 错误', () => {
    const axiosError = {
      message: 'Network Error',
      response: {
        status: 500,
        data: { code: 1001, message: 'Server Error' },
        headers: { 'x-trace-id': 'fe-abc' }
      },
      config: { url: '/test', method: 'get' }
    }
    expect(() => apiLog.error('GET', '/test', axiosError)).not.toThrow()
  })

  it('businessError 应该处理 Result', () => {
    const result = { code: 1, message: '业务失败', data: null }
    expect(() => apiLog.businessError('POST', '/test', result)).not.toThrow()
  })

  it('error 应该处理无 response 的网络错误', () => {
    const netError = { message: 'timeout', code: 'ECONNABORTED' }
    expect(() => apiLog.error('GET', '/test', netError)).not.toThrow()
  })
})
