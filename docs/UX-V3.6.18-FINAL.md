# V3.6.18 ErrorBoundary 循环更新修复

## 1. V3.6.17 之后

V3.6.17 修了 vueuse computedEager 升级。V3.6.18 继续修 ErrorBoundary 循环更新:
- **ErrorState 简化** (不调 useErrorHandler + useUserStore, 避免循环)
- **ErrorBoundary 改 computed → function** (errorType 计算不进入响应式追踪)
- **onErrorCaptured 加 guard** (已有 error 不再设值)

## 2. V3.6.18 改

### 2.1 循环更新根因

```
chat:1 Uncaught (in promise) Maximum recursive updates exceeded in component <ErrorBoundary>
```

**触发链 (V3.6.12+ 集成 ErrorState 后)**:
```
ErrorBoundary (computed errorType)
  → ErrorState (computed errorClassify(props.error))
    → useErrorHandler.errorClassify() ❌ 函数不存在
    → useUserStore.enterDemoMode() (useUserStore reactive)
      → userStore 变化
        → ErrorBoundary 重渲染
          → computed errorType 重新计算
            → ErrorState 重渲染
              → useUserStore 重新读
                → ...
                  → Maximum recursive updates exceeded
```

### 2.2 ErrorState 简化 (V3.6.18+, 140 → 137 行)

**移除**:
- ❌ `import { useErrorHandler }` - 调不存在的 errorClassify
- ❌ `import { useUserStore }` - 调 enterDemoMode 触发 reactive
- ❌ `const classified = computed(...)` - 内部 derived 引发循环

**简化**:
- 所有 `emoji` / `title` / `description` / `detail` 用 props 派生 (computed)
- 不调任何 composable, 完全静态
- `showDemo` 看 `props.isDemo` 而非 userStore

### 2.3 ErrorBoundary 改 computed → function (V3.6.18+)

```js
// V3.6.12+ (computed)
const errorType = computed(() => {
  const e = error.value
  if (e?.response?.status === 401) return 'auth'
  ...
})

// V3.6.18+ (function, 不进响应式)
function getErrorType(err) {
  if (!err) return 'unknown'
  if (err?.response?.status === 401) return 'auth'
  ...
}
```

**模板用法**:
```vue
<!-- V3.6.12+ (computed 响应式) -->
<ErrorState :error-type="errorType" />

<!-- V3.6.18+ (function, 每次 render 调用) -->
<ErrorState :error-type="getErrorType(error)" />
```

### 2.4 onErrorCaptured 加 guard (V3.6.18+)

```js
onErrorCaptured((err) => {
  // V3.6.18+ 防止循环: error 已有值时不再设
  if (error.value) return false
  error.value = err
  return false
})
```

## 3. 验证

| 测试 | 结果 |
|------|------|
| ErrorState 简化 | ✅ 0 composable 依赖 |
| ErrorBoundary function 替代 computed | ✅ 不进响应式 |
| onErrorCaptured guard | ✅ 防止重复设 error |
| 21 路由 21/21 200 | ✅ |
| ci-check 11/11 | ✅ < 3s |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| frontend-error-check | ✅ 0 错误 0 警告 |
| vite build 0 错 | ✅ 1m |

## 4. 关键经验

1. **集成组件 = 共享响应式 = 循环风险** — ErrorBoundary 跟 ErrorState 看似独立, 实际通过 useUserStore 共享状态
2. **computed vs function 选择** — 模板里要的是"值", 但值若来自其他 reactive, 会进追踪链
3. **composable 谨慎调用** — useErrorHandler / useUserStore 在跨组件使用时, 每个调用点都加新依赖
4. **onErrorCaptured 必须 idempotent** — 同一错误不能重复设 error.value
5. **Maximum recursive updates 是 Vue 3 经典坑** — 解决方案: 1) function 而非 computed 2) 减少 composable 依赖 3) 加 guard

## 5. 累计 73 个版本 (V3.5.46-V3.6.18)
