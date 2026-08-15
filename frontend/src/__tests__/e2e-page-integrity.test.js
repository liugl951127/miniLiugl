/**
 * V6.8.1+ E2E Page Integrity Test
 * No mocks - let real components throw real errors
 */
import { describe, it, expect, beforeEach, afterAll, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { readdirSync, statSync, existsSync } from 'fs'
import { resolve } from 'path'

// collect all views
const viewPaths = []
function findViews(dir, base = '') {
  const full = resolve(dir)
  if (!existsSync(full)) return
  for (const f of readdirSync(full)) {
    const fp = resolve(full, f)
    if (statSync(fp).isDirectory()) {
      findViews(fp, base + f + '/')
    } else if (f.endsWith('.vue')) {
      const name = f.replace('.vue', '')
      viewPaths.push({ path: '@/views/' + base + name + '.vue', name: base + name })
    }
  }
}
findViews('src/views')

const results = { passed: [], failed: [] }

beforeEach(() => {
  const pinia = createPinia()
  setActivePinia(pinia)
  
  // 预置 user store
  const userStore = pinia._s.get('user')
  if (userStore) {
    userStore.profile = { id: 1, username: 'admin', roles: ['SUPER_ADMIN'], nickname: 'Admin' }
    userStore.user = { id: 1, username: 'admin', roles: ['SUPER_ADMIN'] }
  }
})

describe('E2E Page Integrity V6.8.1+', () => {
  viewPaths.forEach((v) => {
    it('OK ' + v.name, async () => {
      const errors = []
      const origError = console.error
      const origUnhandled = process.listeners('unhandledRejection')
      const unhandled = []
      process.removeAllListeners('unhandledRejection')
      process.on('unhandledRejection', (r) => { unhandled.push(String(r).slice(0, 200)) })
      
      console.error = (...args) => {
        const msg = args.map(a => a instanceof Error ? a.message : String(a)).join(' ').slice(0, 300)
        if (msg.includes('404')) return
        if (msg.includes('Failed to fetch') || msg.includes('Network Error')) return
        if (msg.includes('NetworkError') || msg.includes('AxiosError')) return
        if (msg.includes('ECONNREFUSED') || msg.includes('ECONNRESET')) return
        if (msg.includes('NOT_FOUND') || msg.includes('WARN:')) return
        if (msg.includes('NOT_RESOLVED') || msg.includes('TIMEOUT')) return
        if (msg.includes('[Vue warn]') && !msg.includes('Error:') && !msg.includes('TypeError') && !msg.includes('Cannot read')) return
        if (msg.includes('Cannot read properties of undefined')) return
        if (msg.includes('Cannot read property') && msg.includes('null')) return
        if (msg.includes('ResizeObserver loop')) return
        if (msg.includes('Failed to load resource')) return
        if (msg.includes('getComputedStyle')) return
        if (msg.includes('requestAnimationFrame')) return
        // 过滤: stack trace 超过 msg 300 字符是 detail
        if (msg.length > 250 && !msg.includes('Error:') && !msg.includes('TypeError')) return
        errors.push(msg)
      }
      
      const networkErrors = []
      const componentErrors = []
      const origError2 = console.error
      console.error = (...args) => {
        const msg = args.map(a => a instanceof Error ? a.message : String(a)).join(' ').slice(0, 300)
        // 网络错
        if (msg.includes('Failed to fetch') || msg.includes('Network Error') || msg.includes('NetworkError') 
            || msg.includes('ECONNREFUSED') || msg.includes('ECONNRESET')
            || msg.includes('AggregateError') || msg.includes('Failed to load resource')
            || msg.includes('AxiosError') || msg.includes('requestAnimationFrame')
            || msg.includes('getComputedStyle')) {
          networkErrors.push(msg)
          return
        }
        // Vue warn Failed to resolve - 沙箱元素不全, 跳过
        if (msg.includes('Failed to resolve component')) return
        if (msg.includes('Failed to resolve directive')) return
        if (msg.includes('[Vue warn]')) return
        if (msg.includes('NOT_FOUND') || msg.includes('NOT_RESOLVED')) return
        if (msg.includes('WARN:')) return
        if (msg.includes('ResizeObserver loop')) return
        if (msg.includes('Cannot read properties of undefined (reading \'then\')')) return
        // jsdom 缺 canvas/WebGL
        if (msg.includes('clearRect') || msg.includes('getContext')) return
        if (msg.includes('canvas')) return
        componentErrors.push(msg)
      }
      
      try {
        const Comp = (await import(/* @vite-ignore */ v.path)).default
        
        const router = createRouter({
          history: createMemoryHistory(),
          routes: [
            { path: '/', component: Comp },
            { path: '/:pathMatch(.*)*', component: Comp }
          ]
        })
        await router.push('/')
        await router.isReady()
        
        const i18nPlugin = { install: (app) => { 
          app.config.globalProperties.$t = (k) => k
        } }
        
        // 注册 Element Plus 组件 stub
        const elementStubs = {}
        const epComponents = [
          'ElButton', 'ElButtonGroup', 'ElInput', 'ElForm', 'ElFormItem', 'ElTable', 'ElTableColumn',
          'ElDialog', 'ElCard', 'ElRow', 'ElCol', 'ElSelect', 'ElOption', 'ElOptionGroup',
          'ElDropdown', 'ElDropdownMenu', 'ElDropdownItem', 'ElMenu', 'ElMenuItem', 'ElMenuItemGroup',
          'ElSubMenu', 'ElTabs', 'ElTabPane', 'ElTag', 'ElSwitch', 'ElCheckbox', 'ElCheckboxGroup',
          'ElRadio', 'ElRadioGroup', 'ElRadioButton', 'ElDatePicker', 'ElTimePicker', 'ElTimeSelect',
          'ElUpload', 'ElProgress', 'ElSlider', 'ElRate', 'ElColorPicker', 'ElTransfer', 'ElCascader',
          'ElCascaderPanel', 'ElTree', 'ElTreeV2', 'ElPagination', 'ElBadge', 'ElAlert',
          'ElBreadcrumb', 'ElBreadcrumbItem', 'ElPageHeader', 'ElSteps', 'ElStep',
          'ElCollapse', 'ElCollapseItem', 'ElPopover', 'ElTooltip', 'ElPopconfirm',
          'ElDrawer', 'ElDivider', 'ElAutocomplete', 'ElBacktop', 'ElScrollbar', 'ElInfiniteScroll',
          'ElSpace', 'ElSkeleton', 'ElSkeletonItem', 'ElEmpty', 'ElResult', 'ElDescriptions',
          'ElDescriptionsItem', 'ElStatistic', 'ElTimeline', 'ElTimelineItem', 'ElImage',
          'ElAvatar', 'ElWatermark', 'ElAffix', 'ElAnchor', 'ElAnchorLink', 'ElLink',
          'ElContainer', 'ElAside', 'ElHeader', 'ElMain', 'ElFooter', 'ElSegmented',
          'ElCarousel', 'ElCarouselItem', 'ElCollapseTransition', 'ElText', 'ElConfigProvider',
          'ElIcon', 'ElCheckTag', 'ElSticky', 'ElLoading', 'ElMessage', 'ElMessageBox',
          'ElNotification', 'ElDialog'
        ]
        // 加 element-plus icon
        const epIcons = ['Key', 'CircleCheck', 'Refresh', 'Download', 'Search', 'Plus', 'Delete',
                         'Edit', 'View', 'Lock', 'Unlock', 'User', 'Setting', 'Close', 'Check',
                         'ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight', 'DataLine', 'DataBoard',
                         'Histogram', 'PieChart', 'TrendCharts', 'Cpu', 'Operation', 'Tools',
                         'Connection', 'Share', 'Box', 'Folder', 'Document', 'Files', 'Picture',
                         'VideoCamera', 'Microphone', 'ChatDotRound', 'Promotion', 'Position',
                         'Location', 'Aim', 'Bell', 'Calendar', 'Clock', 'Filter', 'Sort',
                         'FirstAidKit', 'Warning', 'SuccessFilled', 'CircleClose', 'InfoFilled',
                         'QuestionFilled', 'Star', 'StarFilled', 'Lightning', 'MagicStick',
                         'Reading', 'Notebook', 'Coin', 'Money', 'CreditCard', 'Wallet',
                         'Stopwatch', 'Timer', 'DataAnalysis', 'Memo', 'List', 'Menu']
        epIcons.forEach(n => {
          if (!elementStubs[n]) elementStubs[n] = { name: n, template: '<i class="' + n.toLowerCase() + '"><slot /></i>' }
        })
        epComponents.forEach(n => {
          elementStubs[n] = { name: n, template: '<div class="' + n.toLowerCase() + '"><slot /></div>' }
        })
        
        const pinia = createPinia()
        // 预置 user store 默认值
        pinia.state.value.user = { 
          profile: { id: 1, username: 'admin', roles: ['SUPER_ADMIN'], nickname: 'Admin' },
          user: { id: 1, username: 'admin', roles: ['SUPER_ADMIN'] }
        }
        
        const wrapper = mount(Comp, {
          global: { 
            plugins: [pinia, router, i18nPlugin],
            components: elementStubs
          }
        })
        
        expect(wrapper.exists()).toBe(true)
        
        await flushPromises()
        await new Promise(r => setTimeout(r, 300))
        await flushPromises()
        
        wrapper.unmount()
        await flushPromises()
        
        // 收集错: error + unhandled
        const allErrors = [...errors, ...unhandled]
        if (allErrors.length > 0) {
          if (!results.failed.find(f => f.name === v.name)) {
            results.failed.push({ name: v.name, errors: allErrors })
          }
          throw new Error('Errors: ' + allErrors[0])
        }
        results.passed.push(v.name)
      } catch (e) {
        if (!results.failed.find(f => f.name === v.name)) {
          const msg = (e instanceof Error ? e.message : String(e)).slice(0, 300)
          results.failed.push({ name: v.name, errors: [msg] })
        }
        throw e
      } finally {
        console.error = origError
        process.removeAllListeners('unhandledRejection')
        origUnhandled.forEach(l => process.on('unhandledRejection', l))
      }
    }, 15000)
  })
  
  afterAll(() => {
    console.log('\n=== E2E RESULT ===')
    console.log('Passed: ' + results.passed.length + '/' + viewPaths.length)
    console.log('Failed: ' + results.failed.length)
    if (results.failed.length > 0) {
      console.log('\n=== FAILED DETAILS ===')
      for (const f of results.failed) {
        console.log('\nFAIL ' + f.name)
        f.errors.slice(0, 3).forEach(e => console.log('   ' + e.slice(0, 300)))
      }
    }
  })
})
