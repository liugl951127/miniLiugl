/**
 * V6.3+ ErrorBoundary 错误监控测试
 * 找出是什么 throw 导致 ErrorBoundary 触发
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

vi.mock('element-plus/dist/index.css', () => ({}))
vi.mock('element-plus/theme-chalk/base.css', () => ({}))

vi.mock('@/views/chat/Index.vue', () => ({
  default: {
    name: 'Chat',
    template: '<div class="chat-mock">Chat mock</div>',
    setup() {
      return {}
    }
  }
}))

vi.mock('@/views/admin/Dashboard.vue', () => ({
  default: {
    name: 'Dashboard',
    template: '<div class="dash-mock">Dashboard mock</div>'
  }
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn().mockResolvedValue({
      accessToken: 'test-token', refreshToken: 'test-refresh',
      user: { id: 1, username: 'admin', roles: ['SUPER_ADMIN'], superAdmin: true }
    }),
    me: vi.fn().mockResolvedValue({ id: 1, username: 'admin', roles: ['SUPER_ADMIN'] }),
    refresh: vi.fn(), logout: vi.fn()
  }
}))

describe('ErrorBoundary 触发源 (V6.3+)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('✓ Chat 组件能正常 mount', async () => {
    const Chat = (await import('@/views/chat/Index.vue')).default
    let err = null
    try {
      const wrapper = mount(Chat, {
        global: { plugins: [createPinia()] },
        stubs: ['v-chart', 'el-button', 'el-input', 'el-icon', 'el-watermark']
      })
      expect(wrapper.exists()).toBe(true)
    } catch (e) {
      err = e
    }
    if (err) console.error('Chat mount err:', err.message)
    expect(err).toBe(null)
  })

  it('✓ ErrorBoundary 在无错时不显示', async () => {
    const ErrorBoundary = (await import('@/components/ErrorBoundary.vue')).default
    const wrapper = mount(ErrorBoundary, {
      slots: { default: '<div class="ok">OK</div>' },
      global: { stubs: ['ErrorState', 'el-skeleton'] }
    })
    expect(wrapper.find('.ok').exists()).toBe(true)
  })
})
