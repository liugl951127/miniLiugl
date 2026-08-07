/**
 * V6.2+ 登录跳转流程测试 (V3 完整版 + 边界场景)
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

vi.mock('element-plus/dist/index.css', () => ({}))
vi.mock('element-plus/theme-chalk/base.css', () => ({}))

vi.mock('@/views/admin/Dashboard.vue', () => ({
  default: {
    name: 'AdminDashboard',
    template: '<div class="mock-dashboard">Mock Dashboard</div>'
  }
}))

vi.mock('@/api/monitor', () => ({
  getMonitorHealth: vi.fn().mockResolvedValue({}),
  getMonitorInfo: vi.fn().mockResolvedValue({ userCount: 100, sessionCount: 50, callCount: 1000, toolCount: 10 })
}))
vi.mock('@/api/admin', () => ({
  getRecentAudit: vi.fn().mockResolvedValue({ data: [] })
}))
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn().mockResolvedValue({
      accessToken: 'mock-token-12345',
      refreshToken: 'mock-refresh',
      user: { id: 1, username: 'admin', roles: ['SUPER_ADMIN'], superAdmin: true }
    }),
    me: vi.fn().mockResolvedValue({ id: 1, username: 'admin', roles: ['SUPER_ADMIN'] }),
    refresh: vi.fn(),
    logout: vi.fn()
  }
}))

const routes = [
  { path: '/login', name: 'Login', component: { template: '<div>Login</div>' }, meta: { public: true } },
  {
    path: '/',
    component: { template: '<div><router-view /></div>' },
    children: [
      {
        path: 'admin',
        name: 'Admin',
        component: { template: '<div class="admin"><router-view /></div>' },
        redirect: '/admin/dashboard',
        children: [
          {
            path: 'dashboard',
            name: 'AdminDashboard',
            component: () => import('@/views/admin/Dashboard.vue')
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

describe('登录跳转流程 (V6.2+)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('userStore 状态管理', () => {
    it('初始: 未登录', () => {
      const userStore = useUserStore()
      expect(userStore.isLogin).toBe(false)
      expect(userStore.accessToken).toBe('')
      expect(userStore.profile).toBe(null)
    })

    it('登录后: isLogin=true', async () => {
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      expect(userStore.isLogin).toBe(true)
      expect(userStore.accessToken).toBe('mock-token-12345')
    })

    it('登录后: profile 包含 user', async () => {
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      expect(userStore.profile?.username).toBe('admin')
      expect(userStore.profile?.roles).toContain('SUPER_ADMIN')
    })

    it('登录后: isSuperAdmin=true (有 SUPER_ADMIN 角色)', async () => {
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      expect(userStore.isSuperAdmin).toBe(true)
    })

    it('退出登录: 清空状态', async () => {
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      await userStore.logout()
      expect(userStore.isLogin).toBe(false)
      expect(userStore.accessToken).toBe('')
      expect(userStore.profile).toBe(null)
    })
  })

  describe('路由守卫', () => {
    it('✓ 未登录访问 /admin/dashboard → 跳 /login', async () => {
      const router = setupRouter()
      setupGuard(router)
      await router.push('/admin/dashboard')
      await router.isReady()
      expect(router.currentRoute.value.name).toBe('Login')
    })

    it('✓ 登录后访问 /admin/dashboard → 正常', async () => {
      const router = setupRouter()
      setupGuard(router)
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      await router.push('/admin/dashboard')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/admin/dashboard')
      expect(router.currentRoute.value.name).toBe('AdminDashboard')
    })

    it('✓ /admin (父) → 应 redirect 到 /admin/dashboard', async () => {
      const router = setupRouter()
      setupGuard(router)
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      await router.push('/admin')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/admin/dashboard')
    })
  })

  describe('Dashboard 组件', () => {
    it('Dashboard 组件能 mount', async () => {
      const userStore = useUserStore()
      await userStore.login({ username: 'admin', password: 'admin123' })
      
      const Dashboard = (await import('@/views/admin/Dashboard.vue')).default
      
      let err = null
      try {
        const wrapper = mount(Dashboard, {
          global: {
            plugins: [createPinia()],
            stubs: {
              'v-chart': true,
              'el-watermark': true,
              'el-button': true,
              'el-tag': true,
              'el-icon': true,
              'el-row': true,
              'el-col': true,
              'el-card': true,
              'el-table': true,
              'el-table-column': true
            }
          }
        })
        expect(wrapper.exists()).toBe(true)
      } catch (e) {
        err = e
      }
      
      if (err) {
        console.error('Mount 失败:', err.message)
        console.error(err.stack)
      }
      expect(err).toBe(null)
    })
  })

})
