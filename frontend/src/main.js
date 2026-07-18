/**
 * @file main.js - Vue 应用入口 (V3.5.12+)
 *
 * 启动顺序:
 *   1. 浏览器兼容层 (initBrowserCompat)
 *   2. Capacitor 移动端 (initCapacitor)
 *   3. Pinia + 持久化插件
 *   4. Vue Router
 *   5. Element Plus (中文 locale)
 *   6. Vue I18n
 *   7. 自定义指令 + 全局错误处理
 *   8. 挂载到 #app
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/global.scss'
import { i18n } from './i18n'
import { initBrowserCompat } from '@/composables/useBrowserCompat'
import { initCapacitor } from '@/composables/useCapacitor'

// V3.0.0: 浏览器兼容层初始化 (检测 + polyfill)
initBrowserCompat()

// V3.1.1: 移动端 Capacitor 初始化 (状态栏/启动屏)
initCapacitor()

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default' })
app.use(i18n)

// V2.7.9: 注册权限指令
import permission from '@/directives/permission'
app.directive('permission', permission)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
