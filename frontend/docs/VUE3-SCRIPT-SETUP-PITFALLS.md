# Vue 3 `<script setup>` 踩坑总结 (V3.5.46-V3.5.94, 49 版)

> 49 个版本踩过的 Vue 3 `<script setup>` 坑 - 全部 22 view 实测修复

## 0. 概述

Vue 3 `<script setup>` 是 **SFC 最佳实践** - 编译时优化、零运行时、TypeScript 友好。
但它的"自动暴露顶层变量"机制**不等于"自动定义"** - 49 版踩了 7 大类坑。

## 1. 顶层变量未定义 → `.length` TypeError (V3.5.94 经典)

### 1.1 报错

```
TypeError: Cannot read properties of undefined (reading 'length')
    at Proxy._sfc_render (http://localhost:3000/src/views/chat/Index.vue)
```

### 1.2 根因

```vue
<template>
  <section v-if="toolCalls.length" class="section">
    <el-table :data="toolCalls">
</template>

<script setup>
// 顶层没 const toolCalls
// 只有 messages[i].toolCalls (嵌套字段)
const messages = ref([])
const messagesRef = ref(null)
</script>
```

`<script setup>` **不会**自动从 `messages` 里"猜"出 `toolCalls` 顶层变量。

### 1.3 修法

```js
const messages = ref([])
const toolCalls = ref([])  // V3.5.94 加
const sources = ref([])    // V3.5.94 加

// 同步逻辑
onToolCall: (tc) => {
  aiMsg.toolCalls.push(tc)
  toolCalls.value.push(tc)  // 同步到顶层
}
```

### 1.4 教训

- 模板里用 `xxx` 就**必须** `const xxx = ref(...)` 顶层定义
- 嵌套字段 (`messages[i].toolCalls`) 跟顶层 (`toolCalls`) 是**两套** - 别混

## 2. 路由 path 重复 → 跳转混乱 (V3.5.86/V3.5.92)

### 2.1 报错

点登录按钮 → 白屏 / 跳到错的页面 / 跳回登录

### 2.2 根因

```js
// router/index.js 错误
{
  path: '/admin',
  children: [
    { path: '', name: 'Admin', component: Index },         // path: ''
    { path: 'dashboard', name: 'Admin', component: Dashboard },  // 同一 name + 重复 path
  ]
}
```

Vue Router 不会报错 - 同 name 但 path 不同，匹配规则混乱：
- `router.replace('/admin/dashboard')` → 命中 path 跟 name → 跳到 `Index` (path: '' 优先)
- 用户看到 `/admin/dashboard` 但内容是 `Index`

### 2.3 修法 (V3.5.92)

```js
{
  path: '/admin',
  redirect: '/admin/dashboard',  // 父级 redirect
  children: [
    { path: 'dashboard', name: 'AdminDashboard', component: Dashboard },  // 唯一
    { path: '', name: 'Admin', component: Index },  // 仅 fallback
  ]
}
```

### 2.4 教训

- 路由 path 必须**唯一**
- 同 name 不会报错 - 但 match 行为不可预测
- 父级 redirect 比子级 redirect 更清晰

## 3. PWA sw.js 版本不匹配 → 旧 SW 拦截新请求 (V3.5.92)

### 3.1 报错

升级后 `sw.js` 改了，但浏览器还是老 SW。
老 SW cache 拦截 → 老 HTML / 老 JS → 用户看不到新功能 / 看到错。

### 3.2 根因

```js
// usePwa.js (V3.5.92 之前)
navigator.serviceWorker.register('/sw.js')
// 浏览器用老 SW (跟新 sw.js 哈希对不上)
```

### 3.3 修法 (V3.5.92)

```js
// 1. sw.js 加版本常量
const SW_VERSION = '3.5.92'
const CACHE_NAME = `minimax-v${SW_VERSION}`

// 2. usePwa.js 加 ?v= 查询参数 (强制重新拉)
navigator.serviceWorker.register(`/sw.js?v=${SW_VERSION}`)

// 3. activate 事件清掉所有老 cache
self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then(names => 
      Promise.all(names.map(name => 
        name !== CACHE_NAME ? caches.delete(name) : null
      ))
    )
  )
})
```

### 3.4 教训

- sw.js 改了 → 加 `?v=xxx` 强制新 SW
- 不调 `skipWaiting()` / `clients.claim()` (避免老 tab 异常)
- `activate` 清所有老 cache

## 4. 401 fetchProfile 错误被吞 → layout 空白 (V3.5.93)

### 4.1 报错

后端 401 (token 过期) → layout 找不到 profile → 整页空白。

### 4.2 根因

```js
// layout/Index.vue onMounted
if (!userStore.profile && userStore.isLogin) {
  try { await userStore.fetchProfile() } catch (e) { /* ignore */ }
  //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  //                错误被吞, profile 永远 null
}
```

### 4.3 修法 (V3.5.93)

```js
// userStore.fetchProfile
async function fetchProfile() {
  try {
    const res = await authApi.me()
    profile.value = res.data
    return res
  } catch (e) {
    // V3.5.93 兜底: 设空 profile 而不是吞错误
    if (!profile.value) {
      profile.value = { username: accessToken.value ? 'unknown' : '', roles: [] }
    }
    // V3.5.93 演示模式: mock profile
    if (isDemoMode()) {
      profile.value = { username: 'demo', nickname: '演示用户', roles: ['ADMIN', 'USER'] }
      return { data: profile.value }
    }
    throw e  // 还是抛, 让上层 useErrorHandler 处理
  }
}
```

### 4.4 教训

- 永远**不要** `catch (e) {}` 吞错
- 兜底值必须有 (`profile.value = { ... }`)
- 演示模式 vs 正常模式分两条路

## 5. el-* prop 表达式错 (V3.5.94 顺手修)

### 5.1 报错

```vue
<el-empty description="{{ t('chat.empty.history') }}" />
```

`t('chat.empty.history')` 显示成原文 (没翻译)。

### 5.2 根因

`description` 是 prop，不是 v-text。`{{ }}` 在 prop 字符串里**不会**解析。

### 5.3 修法

```vue
<el-empty :description="t('chat.empty.history')" />
<!--  ^ 加冒号, JS 表达式 -->
```

### 5.4 教训

- 所有 el-* prop 用 JS 表达式 → 加 `:` 前缀
- `description="静态文字"` OK
- `description="{{ t(...) }}"` ❌ (Vue 文本插值不会触发)
- `:description="t(...)"` ✅

## 6. ErrorBoundary 缺 → 单组件 throw 整页白屏 (V3.5.93 加)

### 6.1 报错

子组件 throw TypeError → 整页白屏 (SPA 经典)

### 6.2 根因

Vue 3 默认不挂 ErrorBoundary - React 16+ 内置, Vue 3 需手写。

### 6.3 修法 (V3.5.93)

```vue
<!-- components/ErrorBoundary.vue (80 行) -->
<template>
  <div v-if="error" class="error-boundary">
    <h1>出错了</h1>
    <p>{{ error.message }}</p>
    <el-button @click="reload">重新加载</el-button>
    <el-button @click="goHome">返回首页</el-button>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
const error = ref(null)
onErrorCaptured((err) => {
  error.value = err
  return false  // 不向上抛
})
</script>
```

```vue
<!-- App.vue 包 ErrorBoundary -->
<ErrorBoundary>
  <router-view />
</ErrorBoundary>
```

### 6.4 教训

- SPA 必须有 ErrorBoundary (顶层)
- `onErrorCaptured` + `return false` 阻止向上抛
- 错误页: 重试 / 首页 / 访客试用 三个动作

## 7. 演示模式缺 → 无后端时 login API 500 (V3.5.93 加)

### 7.1 报错

沙箱无后端 → login POST 500 → 用户卡在登录页

### 7.2 根因

dev server proxy 到 `localhost:8080` (gateway)，但后端没启 → 502/500
`useErrorHandler` 5xx 提示重试，但用户没意识到是无后端环境。

### 7.3 修法 (V3.5.93)

```js
// 1. 检测函数
function isDemoMode() {
  return localStorage.getItem('minimax_demo_mode') === 'true' ||
         window.location.search.includes('demo=1')
}

// 2. login 兜底
if (isDemoMode()) {
  await new Promise(r => setTimeout(r, 500))  // 模拟延迟
  localStorage.setItem('minimax_demo_user', form.username)
  ElMessage.success('🎭 演示模式登录成功 (无后端)')
  router.replace('/admin/dashboard')
  return
}

// 3. fetchProfile 兜底
if (isDemoMode()) {
  profile.value = { username: 'demo', nickname: '演示用户', roles: ['ADMIN', 'USER'] }
}
```

### 7.4 教训

- 演示模式是 SPA 必备 - 后端没起也能演示
- `?demo=1` URL 参数 + localStorage flag 双轨
- 演示账号填入 → 一键跳 (不点登录)

## 8. 22 view 排查结果 (V3.5.95)

| 路径 | view | `.length` 风险 | 状态 |
|------|------|---------------|------|
| / | showcase/Index | 已修 | ✅ |
| /login | auth/Login | 已加 demo mode | ✅ |
| /h5login | auth/H5Login | 已修 | ✅ |
| /chat | chat/Index | **V3.5.94 修 toolCalls** | ✅ |
| /chat/stream | chat/Stream | 已修 | ✅ |
| /ai/chat | ai/AiChat | 已修 | ✅ |
| /ai/workflow | ai/Workflow | 已修 | ✅ |
| /ai/image-gen | ai/ImageGen | 已修 | ✅ |
| /ai/tool-admin | ai/AiToolAdmin | 已修 | ✅ |
| /ai/marketplace | ai/Marketplace | 已修 | ✅ |
| /admin | admin/Index | 已加 dashboard | ✅ |
| /admin/dashboard | admin/Dashboard | 已修 | ✅ |
| /admin/metrics | admin/Metrics | 已修 | ✅ |
| /admin/audit | admin/Audit | 已修 | ✅ |
| /admin/alerts | admin/Alerts | 已修 | ✅ |
| /admin/cluster | admin/Cluster | 已修 | ✅ |
| /admin/traces | admin/Traces | 已修 | ✅ |
| /admin/provider | admin/Provider | 已修 | ✅ |
| /monitor | monitor/Index | 已修 | ✅ |
| /kg | kg/Index | 已修 | ✅ |
| /agent | agent/Index | 已修 | ✅ |
| /agent/multi | agent/Multi | 已修 | ✅ |
| /admin/framework | admin/Framework | 已修 | ✅ |

## 9. 防御性自检脚本 (V3.5.95 提案)

```js
// scripts/check-setup-var.cjs
const fs = require('fs')
const path = require('path')

const viewsDir = 'frontend/src/views'
const files = fs.readdirSync(viewsDir, { recursive: true })
  .filter(f => f.endsWith('.vue') && !f.includes('.bak'))
  .map(f => path.join(viewsDir, f))

let errors = 0
for (const f of files) {
  const content = fs.readFileSync(f, 'utf-8')
  // 1. 模板里 .xxx 访问 -> 找 xxx
  const tmplMatch = content.match(/<template>([\s\S]*?)<\/template>/)?.[1] || ''
  const varInTmpl = new Set()
  ;(tmplMatch.match(/\b([a-z]\w*)\.\w+/g) || []).forEach(m => {
    const v = m.split('.')[0]
    // 排除 element-plus 内置: el, ElMessage
    if (v !== 'el' && !v.startsWith('El') && !['Math', 'JSON', 'Date', 'Object', 'Array', 'String', 'Number', 'Boolean', 'Promise'].includes(v)) {
      varInTmpl.add(v)
    }
  })
  // 2. script setup 里定义
  const scriptMatch = content.match(/<script setup>([\s\S]*?)<\/script>/)?.[1] || ''
  for (const v of varInTmpl) {
    const defined = new RegExp(`\\b(const|let|var)\\s+${v}\\s*=`).test(scriptMatch)
    if (!defined) {
      console.error(`❌ ${f}: 模板用 \`${v}.xxx\` 但 setup 没定义`)
      errors++
    }
  }
}

console.log(`\n${errors === 0 ? '✅ ALL PASS' : `❌ ${errors} errors`}`)
process.exit(errors)
```

## 10. 49 版踩坑时间线

| 版本 | 坑 | 修法 |
|------|----|----|
| V3.5.46 | Vue 3 TDZ `let i` 模板引用 | 改 const + 函数封装 |
| V3.5.74 | Element Plus 2.4 部分 prop 错 | 升级 EP + 文档化 |
| V3.5.80 | 5 段样板不统一 | 强制 5 段样板 |
| V3.5.81 | i18n key 漏 | 写 key 清单 + 集中 i18n |
| V3.5.84 | sw.js 业务 cache 太重 | 改 network-only |
| V3.5.86 | router `/admin` 没 dashboard | 加子路由 |
| V3.5.89 | sw.js 没加 traceparent | W3C traceparent 标准 |
| V3.5.92 | router path '' 跟 'dashboard' 重复 | 删 path '' 加 redirect |
| V3.5.92 | sw.js 版本不匹配 | sw.js?v=3.5.92 |
| V3.5.92 | fetchProfile 错误被吞 | useErrorHandler 兜底 |
| V3.5.92 | 无骨架屏 | PageSkeleton 组件 |
| V3.5.93 | fetchProfile 401 → layout 空白 | 设空 profile 兜底 |
| V3.5.93 | 无 ErrorBoundary → 子组件 throw 白屏 | ErrorBoundary 80 行 |
| V3.5.93 | 沙箱无后端 → login 500 | 演示模式 |
| V3.5.94 | **chat/Index toolCalls 未定义** | 加 const toolCalls = ref([]) |
| V3.5.94 | el-empty description="{{ t(...) }}" 错 | :description="t(...)" |

## 11. 总结 - 7 大坑类

1. **顶层变量未定义** (V3.5.94) - 加 `const xxx = ref(...)`
2. **路由 path 重复** (V3.5.86/V3.5.92) - 父级 redirect + 唯一 path
3. **PWA sw.js 版本不匹配** (V3.5.92) - `?v=xxx` + activate 清 cache
4. **fetchProfile 错误被吞** (V3.5.93) - 设空 profile 兜底
5. **el-* prop 表达式** (V3.5.94) - 加 `:` 前缀
6. **缺 ErrorBoundary** (V3.5.93) - `<ErrorBoundary>` 包 `<router-view>`
7. **缺演示模式** (V3.5.93) - `?demo=1` + localStorage flag

