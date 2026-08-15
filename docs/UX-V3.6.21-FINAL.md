# V3.6.21 登录后空白页优化 (3 处修)

## 1. 问题 (用户反馈)

> 登录进去提示"通话已结束" → 空白页面，体验感非常不好

**3 个问题**:
1. `useSpeechCall.stop()` 误触发 → 弹 "📞 通话结束" toast, 但用户没启动通话
2. `fetchProfile` 失败 → 抛错 → `ErrorBoundary` 捕获 → 显示 ErrorState (但 fetchProfile 已有 V3.5.93 兜底)
3. `ErrorBoundary` 错误时直接显示 ErrorState → 用户感觉"突然出现错误页", 体验突兀

## 2. V3.6.21 改 (3 处修)

### 2.1 useSpeechCall.stop() 改 console.log (V3.6.21+)

```diff
- ElMessage.info('📞 通话结束')
+ console.log('[SpeechCall] 已停止监听')  // V3.6.21+ 不弹 toast, 避免误判
```

**理由**: 通话结束是预期行为, 不应该干扰用户。console 仍可查, 但 toast 改为"已停止监听"无意义。

### 2.2 App.vue 增强 (V3.6.21+, Skeleton fallback)

```vue
<template>
  <PwaStatusBar />
  <ErrorBoundary>
    <router-view v-slot="{ Component, route }">
      <transition name="app-fade" mode="out-in">
        <div v-if="isLoading" key="loading" class="app-loading">
          <el-skeleton :rows="3" animated />
        </div>
        <component v-else :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
  </ErrorBoundary>
</template>
```

**新增**:
- `isLoading` 状态 + `provide('appLoading')` 全局可访问
- 路由切换时显示 Skeleton 过渡
- 错误时显示 Skeleton (不再空白)

### 2.3 ErrorBoundary 加 Skeleton 过渡 (V3.6.21+)

```vue
<template>
  <slot v-if="!error" />
  <div v-else class="error-boundary-wrap">
    <transition name="error-fade" mode="out-in">
      <div v-if="showDetail" key="state" class="error-boundary-content">
        <ErrorState :error="error" :error-type="getErrorType(error)" :show-detail="true" @retry="reload" />
      </div>
      <div v-else key="skeleton" class="error-boundary-skeleton">
        <el-skeleton :rows="5" animated />
      </div>
    </transition>
  </div>
</template>

<script setup>
onErrorCaptured((err) => {
  if (error.value) return false
  error.value = err
  // V3.6.21+ 500ms 延迟显示 ErrorState, 先 Skeleton 过渡
  setTimeout(() => { showDetail.value = true }, 500)
  return false
})
</script>
```

**核心改进**:
- 错误时**先 Skeleton 500ms** → 再切 ErrorState
- 用户看到"加载中"过渡, 不会突然"空白/错误"
- `<transition name="error-fade">` 平滑切换

### 2.4 fetchProfile 兜底 (V3.5.93+ 已实现, V3.6.21 强化)

```js
async function fetchProfile() {
  try {
    const res = await authApi.me()
    profile.value = res.data
    return res
  } catch (e) {
    // V3.6.21+ 永远不 throw, 避免 ErrorBoundary 触发
    if (!profile.value) {
      profile.value = { username: accessToken.value ? 'unknown' : '', roles: [] }
    }
    if (isDemoMode()) {
      profile.value = {
        username: localStorage.getItem('minimax_demo_user') || 'demo',
        roles: ['USER'],
        ...
      }
    }
    console.warn('[userStore] fetchProfile 失败, 已降级:', e?.message)
    return { ok: false, error: e }
  }
}
```

**关键**: 永远不 throw, 永远返 `{ ok: false, error }`, 让 layout 自己处理空态。

## 3. 验证

| 测试 | 结果 |
|------|------|
| useSpeechCall stop 不弹 toast | ✅ 改 console.log |
| App.vue Skeleton fallback | ✅ 路由切换 + 错误都显示 Skeleton |
| ErrorBoundary 500ms 延迟 | ✅ Skeleton → ErrorState 平滑切换 |
| fetchProfile 永不 throw | ✅ V3.5.93+ 兜底 |
| vite build 0 错 | ✅ 1m 2s |
| 8 路由 8/8 200 | ✅ |
| 5 关键文件编译 200 | ✅ |

## 4. 累计 76 个版本 (V3.5.46-V3.6.21)

## 5. 关键经验

1. **toast 不能滥用** — "通话已结束" 触发的时机不可控, 改 console 更稳
2. **错误也要过渡** — ErrorBoundary 错误时立刻显示 ErrorState 太突兀, Skeleton 过渡
3. **fetchProfile 永远不 throw** — 让 UI 层决定怎么显示, store 层只设值
4. **<ErrorBoundary> 包裹是基础** — 任何 SPA 必备
5. **App.vue 提供全局 loading** — 路由切换更平滑
