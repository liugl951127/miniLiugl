# V3.5.92+ 前端 UX 全面提升 (登录空白 + 页面重构)

## 1. 问题

**用户反馈**: 登录后页面跳转空白

**根因分析**:
1. **路由重复** (V3.5.86 加 dashboard 时没注意): `path: ''` 跟 `path: 'dashboard'` 同一 `name: 'AdminDashboard'`, Vue Router 匹配混乱
2. **PWA sw.js 版本不匹配**: V3.5.79 usePwa 还在 register `sw.js?v=3.5.79`, 但实际 sw.js 已 V3.5.89
3. **fetchProfile 失败被吞**: layout onMounted 调 `userStore.fetchProfile().catch(() => {})`, 401 错误被忽略, dashboard 跳过去但没数据 → 空白
4. **错误处理不统一**: 各 view 自己 try/catch 写 ElMessage.error, 不一致
5. **无骨架屏**: loading 状态显示空白, 用户不知道在加载

## 2. 改

### 2.1 路由修复 (V3.5.92 关键)

`src/router/index.js` admin 容器:
- ❌ 删 `path: '' + name: 'AdminDashboard'` (跟 path: 'dashboard' 重复)
- ✅ 加 `redirect: '/admin/dashboard'` (容器默认跳 dashboard)
- ✅ 保留 `path: 'dashboard' + name: 'AdminDashboard'` (跟 menu / Login / super/Index 跳的一致)

### 2.2 PWA 版本对齐 (V3.5.92)

`src/composables/usePwa.js`:
```js
// V3.5.79: sw.js?v=3.5.79 (旧)
// V3.5.92: sw.js?v=3.5.92 (新, 跟 sw.js 实际版本一致)
registration = await navigator.serviceWorker.register('/sw.js?v=3.5.92', { scope: '/' })
```

### 2.3 Login.vue 全面重写 (V3.5.92)

`src/views/auth/Login.vue` 362 → ~520 行, 5 段样板 + UX 增强:

| 改进 | 描述 |
|------|------|
| 骨架屏 | loading 状态显示骨架, 不空白 |
| 错误抖动 | shake 动画 (0.4s) 视觉反馈 |
| 演示账号 | 5 卡片可视化, 一键填入 + 自动登录 |
| 密码可见 | show-password 切换 |
| 实时校验 | username/password 长度检查 |
| 错误分类 | 401 / 403 / 404 / 500 / 网络错 / 业务错 |
| 自动跳转 | token 持久化, 二次访问自动登 |
| 动效 | 渐入 / hover / focus ring |
| 渐变背景 | 品牌色 #6366f1 → #a855f7 → #ec4899 |
| 移动端适配 | 响应式 (isMobile ref) |

### 2.4 useErrorHandler composable (新增)

`src/composables/useErrorHandler.js` (130 行):

```js
// 统一错误处理
export function handleError(err, options) {
  // 1. 401 → 清 token + 跳登录 + redirect
  // 2. 403 → 提示 + 跳首页
  // 3. 404 → 友好提示
  // 4. 500+ → 提示重试
  // 5. 超时 / 网络错 → 离线提示
  // 6. 业务错 → BFF message
}

// 401 风暴防护 (5s 内只处理一次)
let last401At = 0
```

### 2.5 PageSkeleton 组件 (新增)

`src/components/PageSkeleton.vue`:
```vue
<PageSkeleton :rows="5" :show-header="true" />
```

### 2.6 usePageSkeleton composable (新增)

`src/composables/usePageSkeleton.js`:
```js
const { isLoading, loadingText, startLoading, stopLoading } = usePageSkeleton()
```

### 2.7 chat/Index UX 增强

`src/views/chat/Index.vue`:
- ✅ 加快捷键: `Ctrl+K` 新对话
- ✅ 加离线监听: online / offline 事件
- ✅ sendMessage 已有的错误处理保留

### 2.8 http.js 集成 useErrorHandler

`src/api/http.js`:
```js
import { handleError } from '@/composables/useErrorHandler'
// 统一处理 401/403/404/500/网络错
```

## 3. 端到端登录流程 (V3.5.92)

```
用户输入用户名密码 → 点登录
  ↓
Login.vue onSubmit
  ├─ formRef.validate()  → 失败 triggerShake()
  ├─ loading = true
  ├─ userStore.login()
  │   ├─ POST /api/v1/auth/login
  │   ├─ 成功 → 存 token + remember
  │   └─ 失败 → handleError 分类
  │       ├─ 401 → 错误提示
  │       ├─ 500 → 重试提示
  │       └─ 网络错 → 离线提示
  ├─ router.replace(redirect || '/admin/dashboard')
  │   ↓ (立即跳, 不等 fetchProfile)
  └─ userStore.fetchProfile() 异步
      ├─ 成功 → profile 更新
      └─ 失败 → 不阻塞跳转 (layout 显示)
  ↓
/admin/dashboard 加载
  ├─ 路由: redirect → /admin/dashboard (V3.5.92 修)
  ├─ Dashboard.vue 5 段样板渲染
  │   ├─ page-header
  │   ├─ 服务健康
  │   ├─ 4 KPI
  │   ├─ 趋势图
  │   └─ 最近审计
  └─ 异步 loadAll() (loading 状态显示骨架)
```

## 4. 验证

| 测试 | 结果 |
|------|------|
| router/index.js 修 (删 path: '', 加 redirect) | ✅ |
| Login.vue 完整重写 (520 行) | ✅ |
| useErrorHandler composable | ✅ |
| PageSkeleton 组件 | ✅ |
| usePageSkeleton composable | ✅ |
| chat/Index 加快捷键 Ctrl+K | ✅ |
| http.js 集成 useErrorHandler | ✅ |
| PWA sw.js 版本对齐 v3.5.92 | ✅ |
| useBrowserCompat.js 修 let i (Vite 严格模式) | ✅ |
| vite build 0 错 | ✅ 52s |
| ci-check 7/7 PASS | ✅ |
| Round 6 90 轮 100% pass | ✅ |
| Round 7 5 browser 全 pass | ✅ |
| Round 8 5 browser trace 全 pass | ✅ |
| 路由表校验 22 menu 全匹配 | ✅ |

## 5. UX 改进对比 (V3.5.80 vs V3.5.92)

| 维度 | V3.5.80 | V3.5.92 |
|------|---------|---------|
| 登录页 | 280 行 | 520 行 (+86%) |
| 演示账号 | 文字列表 | 5 卡片可视化 |
| 错误处理 | ElMessage 单一 | 分类 401/403/404/500/网络 |
| 抖动动画 | 无 | shake 0.4s |
| 自动登录 | 无 | token 持久化 + 自动登 |
| 骨架屏 | 无 | PageSkeleton 组件 |
| 路由跳转 | router.push | router.replace (不留 history) |
| 移动端 | 基础 | 完整响应式 + 渐变背景 |
| 品牌 | 简单 | 渐变 logo + 品牌色系 |

## 6. 累计 47 个版本 (V3.5.46-V3.5.92)
