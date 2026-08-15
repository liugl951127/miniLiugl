// V6.8.1+ logger 测试
import { describe, it, expect, beforeEach, vi } from 'vitest'
import logger, { downloadLogs, useLogger } from '@/utils/logger'

describe('Logger (V6.8.1+)', () => {
  beforeEach(() => {
    logger.clearHistory()
  })

  it('✓ 4 级日志都可用', () => {
    expect(() => logger.debug('Test', 'msg')).not.toThrow()
    expect(() => logger.info('Test', 'msg')).not.toThrow()
    expect(() => logger.warn('Test', 'msg')).not.toThrow()
    expect(() => logger.error('Test', 'msg')).not.toThrow()
    expect(() => logger.success('Test', 'msg')).not.toThrow()
  })

  it('✓ history 记录日志', () => {
    logger.info('Test', 'hello')
    const h = logger.getHistory()
    expect(h.length).toBe(1)
    expect(h[0].level).toBe('info')
    expect(h[0].module).toBe('Test')
    expect(h[0].args).toEqual(['hello'])
  })

  it('✓ level 过滤', () => {
    logger.debug('Test', 'd')
    logger.info('Test', 'i')
    logger.warn('Test', 'w')
    logger.error('Test', 'e')
    
    const all = logger.getHistory()
    expect(all.length).toBe(4)

    const errors = logger.getHistory({ level: 'error' })
    expect(errors.length).toBe(1)
  })

  it('✓ module 过滤', () => {
    logger.info('A', 'a1')
    logger.info('B', 'b1')
    logger.info('A', 'a2')
    
    const aLogs = logger.getHistory({ module: 'A' })
    expect(aLogs.length).toBe(2)
  })

  it('✓ since 时间过滤', () => {
    logger.clearHistory()
    const t1 = Date.now() - 1  // 确保 t1 < 任何后续 ts
    logger.info('Test', 'old')
    const t2 = Date.now() + 1
    // since < t2 应该包含 old (ts < t2)
    const beforeT2 = logger.getHistory({ since: t1 })
    expect(beforeT2.length).toBe(1)
    // since > t2 应排除 old
    const afterT2 = logger.getHistory({ since: t2 })
    expect(afterT2.length).toBe(0)
  })

  it('✓ Error 对象序列化', () => {
    const err = new Error('boom')
    logger.error('Test', err)
    const h = logger.getHistory()
    expect(h[0].args[0].name).toBe('Error')
    expect(h[0].args[0].message).toBe('boom')
  })

  it('✓ setLevel/getLevel', () => {
    logger.setLevel('warn')
    expect(logger.getLevel()).toBe('warn')
    logger.setLevel('debug')
  })

  it('✓ useLogger composable', () => {
    const log = useLogger()
    expect(log).toBe(logger)
  })

  it('✓ clearHistory', () => {
    logger.info('Test', 'x')
    expect(logger.getHistory().length).toBe(1)
    logger.clearHistory()
    expect(logger.getHistory().length).toBe(0)
  })

  it('✓ downloadLogs 创建 Blob', () => {
    logger.info('Test', 'file-content')
    
    // mock 全套
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    const origCreateEl = document.createElement.bind(document)
    document.createElement = (tag) => {
      if (tag === 'a') return mockAnchor
      return origCreateEl(tag)
    }
    const origAppend = document.body.appendChild.bind(document.body)
    const origRemove = document.body.removeChild.bind(document.body)
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()
    
    let blobContent = null
    const origCreate = URL.createObjectURL
    URL.createObjectURL = (blob) => { blobContent = blob; return 'blob:mock' }
    const origRevoke = URL.revokeObjectURL
    URL.revokeObjectURL = vi.fn()
    
    downloadLogs()
    
    expect(blobContent).toBeTruthy()
    expect(blobContent.type).toContain('text/plain')
    expect(mockAnchor.click).toHaveBeenCalled()
    
    // 还原
    document.createElement = origCreateEl
    document.body.appendChild = origAppend
    document.body.removeChild = origRemove
    URL.createObjectURL = origCreate
    URL.revokeObjectURL = origRevoke
  })

  it('✓ console 集成 (4 级都打印)', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {})
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const errSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    
    logger.debug('Test', 'd')
    logger.info('Test', 'i')
    logger.warn('Test', 'w')
    logger.error('Test', 'e')
    
    expect(debugSpy).toHaveBeenCalled()
    expect(logSpy).toHaveBeenCalled()
    expect(warnSpy).toHaveBeenCalled()
    expect(errSpy).toHaveBeenCalled()
    
    logSpy.mockRestore()
    warnSpy.mockRestore()
    errSpy.mockRestore()
    debugSpy.mockRestore()
  })
})
