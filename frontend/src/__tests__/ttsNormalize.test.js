import { describe, it, expect } from 'vitest'
import { ttsNormalize, hasTtsContent } from '@/utils/ttsNormalize'

describe('ttsNormalize', () => {
  it('金额 ¥1,234.56 → 一千二百三十四元 5 角 6 分', () => {
    const r = ttsNormalize('共 ¥1,234.56')
    expect(r).toContain('一千二百三十四')
    expect(r).toContain('元')
  })
  
  it('金额 100元 → 一百元', () => {
    const r = ttsNormalize('需要 100元')
    expect(r).toContain('一百元')
  })
  
  it('金额 $1,234 → 一千二百三十四美元', () => {
    const r = ttsNormalize('价格 $1,234')
    expect(r).toContain('美元')
    expect(r).toContain('一千二百三十四')
  })
  
  it('百分比 50% → 百分之五十', () => {
    const r = ttsNormalize('利率 50%')
    expect(r).toContain('百分之五十')
  })
  
  it('百分比 12.5% → 百分之十二点五', () => {
    const r = ttsNormalize('增长 12.5%')
    expect(r).toContain('百分之十二')
    expect(r).toContain('点五')
  })
  
  it('日期 2026-08-03 → 2026 年 8 月 3 日', () => {
    const r = ttsNormalize('日期 2026-08-03')
    expect(r).toContain('年')
    expect(r).toContain('月')
    expect(r).toContain('日')
  })
  
  it('时间 14:30 → 14 点 30 分', () => {
    const r = ttsNormalize('时间 14:30')
    expect(r).toContain('点')
    expect(r).toContain('分')
  })
  
  it('时间 9:00 → 9 点', () => {
    const r = ttsNormalize('9:00 开会')
    expect(r).toContain('点')
    expect(r).not.toContain('分')
  })
  
  it('单位 5kg → 5 千克', () => {
    const r = ttsNormalize('重量 5kg')
    expect(r).toContain('千克')
  })
  
  it('单位 100℃ → 100 摄氏度', () => {
    const r = ttsNormalize('温度 100℃')
    expect(r).toContain('摄氏度')
  })
  
  it('大数字 12345678 → 1234 万 5678', () => {
    const r = ttsNormalize('总人口 12345678')
    expect(r).toContain('万')
  })
  
  it('手机号 18812345678 → 188 1234 5678', () => {
    const r = ttsNormalize('联系 18812345678')
    expect(r).toContain('188')
    expect(r).toContain('1234')
    expect(r).toContain('5678')
  })
  
  it('邮箱 a@b.com → a at b dot com', () => {
    const r = ttsNormalize('邮箱 a@b.com')
    expect(r).toContain('at')
    expect(r).toContain('dot')
  })
  
  it('URL → 字母化', () => {
    const r = ttsNormalize('访问 https://example.com')
    expect(r).not.toContain('https')
  })
  
  it('hasTtsContent 检测', () => {
    expect(hasTtsContent('hello 123')).toBe(true)
    expect(hasTtsContent('hello')).toBe(false)
    expect(hasTtsContent('价格 ¥100')).toBe(true)
  })
  
  it('空文本', () => {
    expect(ttsNormalize('')).toBe('')
    expect(ttsNormalize(null)).toBe('')
  })
  
  it('纯中文文本 (无数字)', () => {
    const r = ttsNormalize('你好世界')
    expect(r).toBe('你好世界')
  })
  
  it('混合文本', () => {
    const r = ttsNormalize('在 2026-08-03，温度 25℃，价格 ¥1,234.56')
    expect(r).toContain('年')
    expect(r).toContain('摄氏度')
    expect(r).toContain('元')
  })
})
