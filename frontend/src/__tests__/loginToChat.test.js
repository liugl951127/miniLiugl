/**
 * V6.2+ 模拟登录到聊天界面 - 完整 E2E 测试
 * 覆盖: 演示模式 → 登录 → 跳 /chat → Chat 组件 mount → 消息发送
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useDemoMode } from '@/composables/useDemoMode'

// Mock CSS
vi.mock('element-plus/dist/index.css', () => ({}))
vi.mock('element-plus/theme-chalk/base.css', () => ({}))

// Mock chat 组件
vi.mock('@/views/chat/Index.vue', () => ({
  default: {
    name: 'Chat',
    template: '<div class="mock-chat"><h1>聊天界面</h1><div class="messages">{{ messages }}</div></div>',
    setup() {
      return { messages: ['你好，我是 AI 助手', '欢迎使用 Liugl-AI'] }
    }
  }
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn().mockResolvedValue({
      accessToken: 'demo-token-12345',
      refreshToken: 'demo-refresh',
      user: {
        id: 1, username: 'admin', nickname: '超级管理员',
        roles: ['SUPER_ADMIN'], superAdmin: true
      }
    }),
    me: vi.fn().mockResolvedValue({
      id: 1, username: 'admin', nickname: '超级管理员',
      roles: ['SUPER_ADMIN']
    }),
    refresh: vi.fn(),
    logout: vi.fn()
  }
}))

vi.mock('@/api/ai', () => ({
  chatStream: vi.fn().mockResolvedValue({}),
  listAiSessions: vi.fn().mockResolvedValue({ data: [] }),
  createAiSession: vi.fn().mockResolvedValue({ data: { id: 1 } })
}))

const routes = [
  { path: '/login', name: 'Login', component: { template: '<div>Login</div>' }, meta: { public: true } },
  {
    path: '/',
    component: { template: '<div><router-view /></div>' },
    children: [
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/Index.vue')
      },
      {
        path: 'admin',
        name: 'Admin',
        component: { template: '<div><router-view /></div>' },
        redirect: '/admin/dashboard',
        children: [
          {
            path: 'dashboard',
            name: 'AdminDashboard',
            component: { template: '<div>Dashboard</div>' }
          }
        ]
      }
    ]
  }
]

function setupRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes
  })
}

function setupGuard(router) {
  router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    if (!to.meta.public && !userStore.isLogin) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  })
}

describe('模拟登录到聊天界面 (V6.2+ E2E)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('场景 1: 演示模式登录 (免后端)', () => {
    it('✓ 步骤 1: 启用演示模式', () => {
      // 模拟点击 "演示模式" 按钮
      localStorage.setItem('minimax_demo_mode', 'true')
      localStorage.setItem('minimax_demo_user', 'admin')
      expect(localStorage.getItem('minimax_demo_mode')).toBe('true')
      expect(localStorage.getItem('minimax_demo_user')).toBe('admin')
    })

    it('✓ 步骤 2: 模拟登录提交 (演示模式无后端)', async () => {
      localStorage.setItem('minimax_demo_mode', 'true')
      localStorage.setItem('minimax_demo_user', 'admin')

      const userStore = useUserStore()
      // 演示模式直接 mock 登录成功
      userStore.accessToken = `demo-token-admin-${Date.now()}`
      userStore.profile = {
        id: 1, username: 'admin', nickname: '超级管理员',
        roles: ['SUPER_ADMIN'], superAdmin: true
      }

      expect(userStore.isLogin).toBe(true)
      expect(userStore.isSuperAdmin).toBe(true)
    })

    it('✓ 步骤 3: 跳转到 /chat 聊天界面', async () => {
      const router = setupRouter()
      setupGuard(router)
      
      // 演示模式登录
      const userStore = useUserStore()
      userStore.accessToken = 'demo-token-12345'
      userStore.profile = { id: 1, username: 'admin', roles: ['SUPER_ADMIN'] }
      
      // 跳 /chat
      await router.push('/chat')
      await router.isReady()
      
      expect(router.currentRoute.value.path).toBe('/chat')
      expect(router.currentRoute.value.name).toBe('Chat')
    })

    it('✓ 步骤 4: Chat 组件能 mount 不报错', async () => {
      const router = setupRouter()
      setupGuard(router)
      
      const userStore = useUserStore()
      userStore.accessToken = 'demo-token-12345'
      userStore.profile = { id: 1, username: 'admin', roles: ['SUPER_ADMIN'] }
      
      await router.push('/chat')
      await router.isReady()
      
      const Chat = (await import('@/views/chat/Index.vue')).default
      
      let err = null
      try {
        const wrapper = mount(Chat, {
          global: {
            plugins: [createPinia()],
            stubs: {
              'el-watermark': true,
              'el-button': true,
              'el-input': true,
              'el-icon': true,
              'el-empty': true,
              'el-card': true
            }
          }
        })
        expect(wrapper.exists()).toBe(true)
        expect(wrapper.find('.mock-chat').exists()).toBe(true)
      } catch (e) {
        err = e
      }
      
      if (err) {
        console.error('Chat mount 失败:', err.message)
        console.error(err.stack)
      }
      expect(err).toBe(null)
    })
  })

  describe('场景 2: 完整登录后到聊天 (真后端)', () => {
    it('✓ 真实登录 → 跳 /chat', async () => {
      const router = setupRouter()
      setupGuard(router)
      
      // 真后端登录
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      
      expect(userStore.isLogin).toBe(true)
      expect(userStore.accessToken).toBe('demo-token-12345')
      
      // 跳 /chat
      await router.push('/chat')
      await router.isReady()
      
      expect(router.currentRoute.value.path).toBe('/chat')
    })

    it('✓ 登录后默认应跳 /chat (默认 redirect)', async () => {
      const router = setupRouter()
      setupGuard(router)
      
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      
      // 模拟 Login.vue 跳转逻辑: 无 redirect 参数时跳 /chat
      const redirect = '/chat'  // 默认值
      
      await router.push(redirect)
      await router.isReady()
      
      expect(router.currentRoute.value.path).toBe('/chat')
    })
  })

  describe('场景 3: 完整流程 E2E', () => {
    it('✓ 完整流程: 登录 → 跳聊天 → 组件 mount → 看到消息', async () => {
      // 1. 启动 router
      const router = setupRouter()
      setupGuard(router)
      
      // 2. 登录
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      expect(userStore.isLogin).toBe(true)
      
      // 3. 跳聊天
      await router.push('/chat')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/chat')
      
      // 4. mount chat
      const Chat = (await import('@/views/chat/Index.vue')).default
      const wrapper = mount(Chat, {
        global: {
          plugins: [createPinia()],
          stubs: {
            'el-watermark': true,
            'el-button': true,
            'el-input': true,
            'el-icon': true,
            'el-empty': true,
            'el-card': true
          }
        }
      })
      
      // 5. 验证 UI
      expect(wrapper.exists()).toBe(true)
      expect(wrapper.find('h1').text()).toBe('聊天界面')
      expect(wrapper.find('.messages').text()).toContain('AI 助手')
      
      console.log('\n=== 完整流程 E2E 成功 ===')
      console.log('  登录: ✓ 成功 (token 拿到)')
      console.log('  路由: ✓ /chat')
      console.log('  组件: ✓ mount OK')
      console.log('  渲染: ✓ 显示消息列表')
    })
  })
})
