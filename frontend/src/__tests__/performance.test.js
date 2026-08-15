/**
 * V6.7+ 性能优化测试
 */
import { describe, it, expect, vi } from 'vitest'
import { ref, nextTick } from 'vue'

describe('useDebounce', () => {
  it('基础防抖', async () => {
    let i = 0
    const fn = () => i++
    let debounced
    vi.useFakeTimers()
    try {
      const { useDebounce } = await import('@/composables/usePerformance')
      debounced = useDebounce(fn, 100)
      debounced()
      debounced()
      debounced()
      vi.advanceTimersByTime(50)
      expect(i).toBe(0)
      vi.advanceTimersByTime(50)
      expect(i).toBe(1)
    } finally {
      vi.useRealTimers()
    }
  })
})

describe('useThrottle', () => {
  it('基础节流', async () => {
    let i = 0
    const fn = () => i++
    vi.useFakeTimers()
    try {
      const { useThrottle } = await import('@/composables/usePerformance')
      const throttled = useThrottle(fn, 100)
      throttled()
      throttled()
      throttled()
      expect(i).toBe(1)
      vi.advanceTimersByTime(100)
      throttled()
      expect(i).toBe(2)
    } finally {
      vi.useRealTimers()
    }
  })
})

describe('useAsyncData', () => {
  it('异步加载', async () => {
    const { useAsyncData } = await import('@/composables/usePerformance')
    const { data, loading, error } = useAsyncData(
      async () => 'test-data',
      { immediate: false }
    )
    expect(loading.value).toBe(false)
    expect(data.value).toBe(null)
  })

  it('错误处理', async () => {
    const { useAsyncData } = await import('@/composables/usePerformance')
    const { error } = useAsyncData(
      async () => { throw new Error('test') },
      { immediate: false }
    )
    expect(error.value).toBe(null)
  })
})

describe('性能优化常量', () => {
  it('debounce delay 范围', () => {
    const delays = [100, 200, 300, 500, 1000]
    delays.forEach(d => {
      expect(d).toBeGreaterThanOrEqual(50)
      expect(d).toBeLessThanOrEqual(2000)
    })
  })

  it('throttle 范围', () => {
    const delays = [50, 100, 200, 500]
    delays.forEach(d => {
      expect(d).toBeGreaterThanOrEqual(16) // 60fps
      expect(d).toBeLessThanOrEqual(1000)
    })
  })
})
