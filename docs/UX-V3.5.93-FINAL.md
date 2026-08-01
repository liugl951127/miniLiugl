# V3.5.92-V3.5.93 前端 UX 最终提升 (登录空白 + 演示模式 + 错误边界)

## 1. V3.5.92 之后再挖根因

V3.5.92 修了一堆（路由 / PWA / Login 重写 / useErrorHandler），但还有 2 个深层问题：

### 1.1 `fetchProfile()` 失败被吞

```js
// layout/Index.vue onMounted (V3.5.93 之前)
if (!userStore.profile && userStore.isLogin) {
  try { await userStore.fetchProfile() } catch (e) { /* ignore */ }
}
```

401 错误被忽略，profile 永远 null，下游 layout 找不到 profile 字段 → 空白。

### 1.2 沙箱没后端, login API 500 死循环

dev server proxy 到 localhost:8080 (gateway)，但后端没启 → 502 / 500
useErrorHandler 5xx 提示重试，但用户没意识到是无后端环境。

## 2. V3.5.93 改

### 2.1 `useStore.fetchProfile()` 兜底 (V3.5.93)

```js
// 之前: 失败就抛错, 啥也不设
async function fetchProfile() {
  const res = await authApi.me()
  profile.value = res.data
}

// 现在: 失败时设空 profile + demo mode 兜底
async function fetchProfile() {
  try {
    const res = await authApi.me()
    profile.value = res.data
    return res
  } catch (e) {
    if (!profile.value) {
      profile.value = { username: accessToken.value ? 'unknown' : '', roles: [] }
    }
    // V3.5.93 演示模式: 无后端返 mock profile
    if (isDemoMode()) {
      profile.value = {
        username: localStorage.getItem('minimax_demo_user') || 'demo',
        nickname: '演示用户',
        email: 'demo@minimax.io',
        roles: ['ADMIN', 'USER'],
        avatar: '🎭'
      }
      return { data: profile.value }
    }
    throw e
  }
}
```

### 2.2 演示模式 (V3.5.93 新)

**`isDemoMode()` 检测**:
```js
function isDemoMode() {
  return localStorage.getItem('minimax_demo_mode') === 'true' ||
         (typeof window !== 'undefined' && window.location.search.includes('demo=1'))
}
```

**Login.vue 加演示模式开关** (page-footer):
```vue
<el-checkbox v-model="demoMode" @change="onDemoToggle">
  🎭 演示模式 (无后端本地演示)
</el-checkbox>
```

**演示模式登录逻辑**:
```js
// V3.5.93 演示模式: 无后端时直接 mock login
if (isDemoMode()) {
  await new Promise(r => setTimeout(r, 500))  // 模拟网络延迟
  localStorage.setItem('minimax_demo_user', form.username)
  ElMessage.success('🎭 演示模式登录成功 (无后端)')
  router.replace('/admin/dashboard')
  return
}
```

**演示账号填入 (demo mode 直接跳)**:
```js
function fillAccount(acc) {
  if (isDemoMode()) {
    localStorage.setItem('minimax_demo_user', acc.username)
    router.replace('/admin/dashboard')  // 直接跳, 不点登录
  } else {
    // 原逻辑: 填入 + 提示点登录
  }
}
```

### 2.3 ErrorBoundary 组件 (V3.5.93 新)

`src/components/ErrorBoundary.vue` (80 行):
```vue
<ErrorBoundary>
  <router-view />
</ErrorBoundary>
```

`onErrorCaptured` 捕获子组件错误, 显示友好错误页 + 重试按钮.

```js
onErrorCaptured((err) => {
  error.value = err
  return false  // 不向上抛
})
```

错误页:
- 红色图标 + "出错了" 标题
- 错误消息 + 堆栈 (开发模式)
- 3 个操作: 重新加载 / 返回首页 / 访客试用

### 2.4 App.vue 包 ErrorBoundary

```vue
<template>
  <PwaStatusBar />
  <ErrorBoundary>
    <router-view />
  </ErrorBoundary>
</template>
```

V3.5.93 之前任何组件 throw 错误 → 白屏
V3.5.93 之后 → ErrorBoundary 捕获 → 友好错误页

### 2.5 e2e-multiround.sh 端口变量 (V3.5.93)

之前 hardcode `localhost:5173`, 改用 `${FRONTEND_PORT:-5173}`:

```bash
HTTP_CODE=$(curl ... "http://localhost:${FRONTEND_PORT:-5173}$route" ...)
```

用法: `FRONTEND_PORT=3000 ROUNDS=90 bash scripts/e2e-multiround.sh`

## 3. 端到端登录流程 (V3.5.93)

```
用户访问 /login
  ↓
两个选择:
  ① 演示模式 (无后端) - 勾选页脚 demo checkbox
  ② 正常模式 (有后端) - 直接登录
  ↓
① 演示模式
  ├─ 演示账号填入 / 点登录
  ├─ isDemoMode() == true
  ├─ 直接 mock login (跳过 API)
  ├─ router.replace('/admin/dashboard')
  ├─ layout/Index.vue onMounted
  │   └─ fetchProfile() 失败 → isDemoMode() → mock profile
  └─ Dashboard.vue 渲染 mock 数据

② 正常模式 (V3.5.92)
  ├─ userStore.login() → POST /api/v1/auth/login
  │   ├─ 成功 → 存 token + router.replace
  │   └─ 失败 → useErrorHandler 6 类
  ├─ fetchProfile() 失败兜底
  │   ├─ 后端 5xx → 提示重试, profile = null
  │   ├─ 后端 401 → useErrorHandler 自动清 token + 跳登录
  │   └─ 网络错 → 提示离线
  └─ Dashboard.vue 渲染

ErrorBoundary 任何错误 → 友好错误页 (不白屏)
```

## 4. 完整改进对比 (V3.5.80 → V3.5.93)

| 维度 | V3.5.80 | V3.5.92 | V3.5.93 |
|------|---------|---------|---------|
| 登录空白 | ❌ 严重 bug | ✅ 路由 + PWA 修 | ✅ fetchProfile 兜底 + ErrorBoundary |
| 后端无依赖 | ❌ 必须启后端 | ❌ 仍然要 | ✅ 演示模式 (免后端) |
| 错误处理 | ElMessage 单一 | useErrorHandler 6 类 | + ErrorBoundary 组件 |
| 错误页 | 白屏 | 白屏 | 友好错误页 + 重试 |
| 路由跳转 | router.push | router.replace | router.replace + fetchProfile 异步 |
| 演示账号 | 文字列表 | 5 卡片 | 5 卡片 + demo mode 直接跳 |
| dev server 端口 | 5173 hardcode | 5173 | 5173 / env var 灵活 |

## 5. 验证

| 测试 | 结果 |
|------|------|
| `userStore.fetchProfile` 失败兜底 | ✅ |
| `isDemoMode()` 检测 | ✅ |
| Login.vue 演示模式 UI (page-footer) | ✅ |
| Login.vue 演示模式登录逻辑 (跳过 API) | ✅ |
| Login.vue fillAccount demo mode 直接跳 | ✅ |
| ErrorBoundary 组件 | ✅ |
| App.vue 包 ErrorBoundary | ✅ |
| e2e-multiround.sh FRONTEND_PORT 变量 | ✅ |
| vite build 0 错 | ✅ 56s |
| ci-check 7/7 PASS | ✅ |
| 21 路由 21/21 200 (Vite 3000 端口) | ✅ |
| Round 6 90 轮 100% pass | ✅ 1890 GET |
| Round 7 5 browser 全 pass | ✅ |
| Round 8 5 browser trace 全 pass | ✅ |

## 6. 演示模式使用流程

```bash
# 1. 启动 dev server
npx vite --port 3000

# 2. 访问 http://localhost:3000/login

# 3. 勾选 "🎭 演示模式 (无后端本地演示)" (page-footer)

# 4. 点任一演示账号卡片 - 直接跳 /admin/dashboard

# 5. 看到 Dashboard + mock 数据 (V3.5.93 fetchProfile 兜底)
```

或者 URL 加参数:
```
http://localhost:3000/login?demo=1
```

## 7. 累计 48 个版本 (V3.5.46-V3.5.93)
