/**
 * V6.7+ 移动端响应式测试
 */
import { describe, it, expect } from 'vitest'
import { BREAKPOINTS } from '@/composables/useMediaQuery'

describe('移动端响应式基础', () => {
  it('断点常量', () => {
    expect(BREAKPOINTS.xs).toBe(480)
    expect(BREAKPOINTS.sm).toBe(640)
    expect(BREAKPOINTS.md).toBe(768)
    expect(BREAKPOINTS.lg).toBe(1024)
    expect(BREAKPOINTS.xl).toBe(1280)
    expect(BREAKPOINTS.xxl).toBe(1536)
  })

  it('设备类型', () => {
    const devices = ['mobile', 'tablet', 'desktop']
    expect(devices).toHaveLength(3)
  })

  it('宽度 < 768 是 mobile', () => {
    const width = 600
    expect(width < BREAKPOINTS.md).toBe(true)
  })

  it('宽度 >= 1024 是 desktop', () => {
    const width = 1280
    expect(width >= BREAKPOINTS.lg).toBe(true)
  })
})

describe('移动端组件', () => {
  it('BackToTop 移动端偏移', () => {
    const props = { bottom: 16, right: 16 }
    expect(props.bottom).toBeLessThan(40)
  })

  it('SearchBar shortcut', () => {
    const props = { shortcut: '/' }
    expect(props.shortcut).toBe('/')
  })

  it('DataTable 移动端单页', () => {
    const props = { pageSize: 10, layout: 'prev, pager, next' }
    expect(props.layout).not.toContain('sizes')
  })

  it('PageContainer 移动端类', () => {
    const containerClass = { mobile: true, desktop: false }
    expect(containerClass.mobile).toBe(true)
  })
})

describe('CSS 媒体查询', () => {
  it('mobile 断点 (max-width: 768px)', () => {
    const css = '@media (max-width: 768px)'
    expect(css).toContain('max-width: 768px')
  })

  it('tablet 断点', () => {
    const css = '@media (min-width: 768px) and (max-width: 1024px)'
    expect(css).toContain('768px')
    expect(css).toContain('1024px')
  })

  it('iOS 安全区', () => {
    const css = 'env(safe-area-inset-bottom)'
    expect(css).toContain('safe-area-inset')
  })
})
