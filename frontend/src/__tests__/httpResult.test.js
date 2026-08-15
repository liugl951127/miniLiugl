/**
 * V3.7.34+ http.js Result 包装单元测试 (镜像)
 *
 * 覆盖 3 场景:
 * 1. 成功 (code=0): 自动剥 data, 挂 __result
 * 2. 失败 (code!==0): 拒绝, 挂 err.__result
 * 3. 非 Result 包装: 原样返回
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'

// mock axios
const mockAxios = {
  create: vi.fn(() => mockAxios),
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  },
  get: vi.fn(),
  post: vi.fn(),
}

vi.mock('axios', () => ({ default: mockAxios }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))

// 模拟 http.js 拦截器逻辑
function makeResponseInterceptor(resp) {
  let data = resp.data
  // V3.7.22+ 业务码处理
  if (data && typeof data === 'object' && 'code' in data && data.code !== 0) {
    return { error: true, data }
  }
  // V3.7.22+ 自动剥 Result.data (支持双层嵌套)
  if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
    const original = data
    data = data.data
    // 兼容双层嵌套
    if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
      data = data.data
    }
    if (data && typeof data === 'object') {
      data.__result = original
    }
    return { data }
  }
  return { data }
}

describe('http.js Result 包装自动剥', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('1. 成功 (code=0): 自动剥 data, 挂 __result', () => {
    const result = makeResponseInterceptor({
      data: {
        code: 0,
        message: 'success',
        data: { id: 1, name: 'admin' },
        timestamp: 1722566400000
      }
    })
    
    expect(result.data.id).toBe(1)
    expect(result.data.name).toBe('admin')
    expect(result.data.__result.code).toBe(0)
    expect(result.data.__result.message).toBe('success')
    expect(result.data.__result.timestamp).toBe(1722566400000)
  })

  it('2. 失败 (code=1): 拒绝, 挂 err.__result', () => {
    const result = makeResponseInterceptor({
      data: {
        code: 1,
        message: 'user not found',
        data: null,
        timestamp: 1722566400000
      }
    })
    
    expect(result.error).toBe(true)
    expect(result.data.code).toBe(1)
    expect(result.data.message).toBe('user not found')
  })

  it('3. 非 Result 包装: 原样返回', () => {
    const result = makeResponseInterceptor({
      data: { id: 1, name: 'admin' }  // 没 code 字段
    })
    
    expect(result.data.id).toBe(1)
    expect(result.data.name).toBe('admin')
    expect(result.data.__result).toBeUndefined()
  })

  it('4. 双层嵌套: 自动剥 2 次', () => {
    const result = makeResponseInterceptor({
      data: {
        code: 0,
        data: {
          code: 0,
          data: { id: 1 }
        }
      }
    })
    
    expect(result.data.id).toBe(1)
  })

  it('5. __result 字段给业务需要时访问', () => {
    const result = makeResponseInterceptor({
      data: {
        code: 0,
        message: 'success',
        data: { items: [] },
        timestamp: 1234
      }
    })
    
    expect(result.data.__result.code).toBe(0)
    expect(result.data.__result.message).toBe('success')
    expect(result.data.__result.timestamp).toBe(1234)
  })

  it('6. data 是 null: 返回 null (不抛错)', () => {
    const result = makeResponseInterceptor({
      data: {
        code: 0,
        message: 'success',
        data: null,
        timestamp: 1234
      }
    })
    
    expect(result.data).toBe(null)
  })
})
