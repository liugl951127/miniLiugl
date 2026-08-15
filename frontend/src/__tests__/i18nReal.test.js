/**
 * V6.3+ i18n 真实翻译测试
 * 验证 t('key') 返回中文, 不是 key
 */
import { describe, it, expect } from 'vitest'
import { t, useI18n } from '@/i18n'

describe('i18n 真实翻译 (V6.3+)', () => {
  it('✓ 顶层 key', () => {
    expect(t('app.name')).toBe('Liugl-AI 大模型平台')
    expect(t('app.tagline')).toBe('企业级 LLM 应用平台')
  })

  it('✓ 嵌套 key', () => {
    expect(t('login.title')).toBe('登录')
    expect(t('login.subtitle')).toContain('企业级 AI 平台')
    expect(t('login.tab.account')).toBe('账号登录')
    expect(t('login.placeholder.username')).toBe('请输入用户名')
  })

  it('✓ admin 嵌套', () => {
    expect(t('admin.title')).toBe('管理后台')
    expect(t('admin.menu.dashboard')).toBe('指标仪表盘')
    expect(t('admin.health.up')).toBe('正常')
    expect(t('admin.group.core')).toBe('核心管理')
  })

  it('✓ nav 顶层', () => {
    expect(t('nav.chat')).toBe('智能对话')
    expect(t('nav.kg')).toBe('知识图谱')
  })

  it('✓ common 通用', () => {
    expect(t('common.confirm')).toBe('确定')
    expect(t('common.cancel')).toBe('取消')
    expect(t('common.save')).toBe('保存')
  })

  it('✓ 找不到的 key 返回 key 本身', () => {
    expect(t('unknown.key')).toBe('unknown.key')
    expect(t('not.exists')).toBe('not.exists')
  })

  it('✓ useI18n 返回 t 函数', () => {
    const { t: ti, locale } = useI18n()
    expect(typeof ti).toBe('function')
    expect(locale.value).toBe('zh')
    expect(ti('app.name')).toBe('Liugl-AI 大模型平台')
  })
})
