/**
 * @file CrudTable.test.js - V6.8.2+ 业务组件 / composable / util 单元测试
 */

import { describe, it, expect, vi } from 'vitest'

// 1. 测试 composable
describe('业务 composable - 函数', () => {
  it('useTable / useCrud / useConfirm / usePageSetup 都是函数', async () => {
    const { useTable } = await import('@/composables/useTable')
    const { useCrud } = await import('@/composables/useCrud')
    const { useConfirm } = await import('@/composables/useConfirm')
    const { usePageSetup } = await import('@/composables/usePageSetup')
    expect(typeof useTable).toBe('function')
    expect(typeof useCrud).toBe('function')
    expect(typeof useConfirm).toBe('function')
    expect(typeof usePageSetup).toBe('function')
  })
})

// 2. 测试 utils/format
describe('utils/format', () => {
  it('formatNumber 千分位', async () => {
    const { formatNumber } = await import('@/utils/format')
    expect(formatNumber(1234567)).toBe('1,234,567')
    expect(formatNumber(0)).toBe('0')
    expect(formatNumber(1234.567, 2)).toBe('1,234.57')
    expect(formatNumber(null)).toBe('-')
    expect(formatNumber(undefined)).toBe('-')
    expect(formatNumber('')).toBe('-')
    expect(formatNumber('abc')).toBe('abc')
  })

  it('formatFileSize', async () => {
    const { formatFileSize } = await import('@/utils/format')
    expect(formatFileSize(0)).toBe('0 B')
    expect(formatFileSize(1024)).toBe('1.0 KB')
    expect(formatFileSize(1024 * 1024)).toBe('1.0 MB')
    expect(formatFileSize(1024 * 1024 * 1024)).toBe('1.0 GB')
  })

  it('truncate', async () => {
    const { truncate } = await import('@/utils/format')
    expect(truncate('hello world', 5)).toBe('hello...')
    expect(truncate('hi', 10)).toBe('hi')
    expect(truncate(null)).toBe('')
  })

  it('formatDate', async () => {
    const { formatDate } = await import('@/utils/format')
    expect(formatDate(null)).toBe('-')
    expect(formatDate('2026-08-10T10:30:45')).toMatch(/2026-08-10/)
    expect(formatDate('2026-08-10T10:30:45', 'YYYY-MM-DD HH:mm:ss')).toMatch(/2026-08-10 10:30:45/)
  })

  it('formatRelativeTime', async () => {
    const { formatRelativeTime } = await import('@/utils/format')
    expect(formatRelativeTime(null)).toBe('-')
    const now = new Date().toISOString()
    expect(formatRelativeTime(now)).toBe('0 秒前')
  })
})

// 3. 验证 useToast 导出
describe('useToast', () => {
  it('导出 success / error / warning / info', async () => {
    const { useToast } = await import('@/composables/useToast')
    expect(typeof useToast).toBe('function')
    const t = useToast()
    expect(t).toHaveProperty('success')
    expect(t).toHaveProperty('error')
    expect(t).toHaveProperty('warning')
    expect(t).toHaveProperty('info')
  })
})

// 4. 验证 store 命名一致性
describe('store export names', () => {
  it('useUserStore 是函数', async () => {
    const { useUserStore } = await import('@/store/user')
    expect(typeof useUserStore).toBe('function')
  })

  it('useSessionStore 是函数', async () => {
    const { useSessionStore } = await import('@/store/session')
    expect(typeof useSessionStore).toBe('function')
  })

  it('useModelStore 是函数', async () => {
    const { useModelStore } = await import('@/store/model')
    expect(typeof useModelStore).toBe('function')
  })

  it('useNotificationStore 是函数', async () => {
    const { useNotificationStore } = await import('@/store/notification')
    expect(typeof useNotificationStore).toBe('function')
  })

  it('useTenantStore 是函数', async () => {
    const { useTenantStore } = await import('@/store/tenant')
    expect(typeof useTenantStore).toBe('function')
  })
})
