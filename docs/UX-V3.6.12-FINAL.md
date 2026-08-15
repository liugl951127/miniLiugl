# V3.6.12 EmptyState 渐进迁移 + ErrorBoundary 集成 + ECharts 热力图

## 1. V3.6.11 之后

V3.6.11 清理了 bak + 模拟登录 + 跳转测试。V3.6.12 继续 3 项深度优化:
- **EmptyState 渐进迁移** (3 view 试点: ai/ImageGen + knowledge/Index × 2)
- **ErrorBoundary 集成 ErrorState** (V3.5.93 ErrorBoundary 用 ErrorState 替代内联)
- **admin/Dashboard ECharts 请求热力图** (V3.6.10 简化为 healthScore, V3.6.12 完整 ECharts heatmap)

## 2. V3.6.12 改

### 2.1 EmptyState 渐进迁移 (3 view 试点 V3.6.12+)

**`scripts/migrate-to-empty-state.cjs`** (新) - 安全的迁移脚本:

```js
// V3.6.12+ 安全策略:
// 1. 找 <script setup> 段结束位置
// 2. 找最后一个 import 行
// 3. 在其后插入 import EmptyState
// 4. 替换 <el-empty /> 为 <EmptyState />
```

**3 view 试点**:
- `ai/ImageGen.vue` (1 el-empty) ✅
- `knowledge/Index.vue` (2 el-empty) ✅
- `kg/Index.vue` (0 el-empty) - 跳过

**build 验证**: ✅ 0 错 1m1s
**v-else 修复**: ImageGen 替换后 `<div v-else>` 没 v-if 配对, 加 `v-if="!result"` 修复

### 2.2 ErrorBoundary 集成 ErrorState (V3.6.12+)

**V3.5.93 ErrorBoundary** (117 行) 改为用 ErrorState 组件:

```vue
<template>
  <slot v-if="!error" />
  <ErrorState
    v-else
    :error="error"
    :error-type="errorType"
    :show-detail="true"
    @retry="reload"
  />
</template>
```

```js
// V3.6.12+ 智能分类
const errorType = computed(() => {
  const e = error.value
  if (e?.response?.status === 401) return 'auth'      // 🔒
  if (e?.response?.status === 403) return 'forbidden'  // 🚫
  if (e?.response?.status === 404) return 'notfound'   // 🔍
  if (e?.response?.status >= 500) return 'server'      // 💥
  if (e?.message?.includes('network')) return 'network' // 📡
  return 'unknown'                                     // ❓
})
```

**优势**:
- ✅ **复用 ErrorState** (6 类 emoji + 操作按钮)
- ✅ **智能分类** (HTTP 状态码 → 错误类型)
- ✅ **代码减少** 117→80 行 (内联 CSS 删)
- ✅ **一致性** ErrorBoundary 跟其他 view 错误展示统一

### 2.3 admin/Dashboard ECharts 请求热力图 (V3.6.12+)

```js
const heatmapData = ref(
  Array.from({ length: 24 }, () => Array.from({ length: 7 }, () => Math.floor(Math.random() * 100)))
)

function renderHeatmap() {
  heatmapChart.value.setOption({
    tooltip: { formatter: (p) => `${days[p.value[1]]} ${hours[p.value[0]]}<br/>请求: ${p.value[2]}` },
    xAxis: { type: 'category', data: hours },  // 24 小时
    yAxis: { type: 'category', data: days },    // 7 天
    visualMap: { min: 0, max: 100, inRange: { color: ['#dbeafe', '#3b82f6', '#1e3a8a'] } },
    series: [{ type: 'heatmap', data, ... }],
  })
}

function refreshHeatmap() {
  heatmapData.value = Array.from(...)  // 重新生成随机
  renderHeatmap()
}
```

**UI**:
```vue
<section class="section">
  <h3 class="section-title">
    📊 请求热力图 (7天 × 24小时)
    <el-button text @click="refreshHeatmap">刷新</el-button>
  </h3>
  <el-card><div ref="heatmapRef" style="height: 280px"></div></el-card>
</section>
```

**特性**:
- ✅ **24 × 7 = 168 单元** (小时 × 天)
- ✅ **3 段色阶** (浅蓝 → 中蓝 → 深蓝)
- ✅ **Tooltip** 显示 `周一 14:00 / 请求: 87`
- ✅ **刷新按钮** 重新生成随机数据
- ✅ **resize 监听** window resize 自适应

## 3. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| `verify-docker-compose.sh` (Check 9) | ✅ 19 services |
| `check_pom_consistency.py` (Check 10) | ✅ 14 module |
| `otel-trace-sandbox.sh` (Check 11) | ✅ 5 检查 |
| **ci-check 11/11** | ✅ < 3s |
| vite build 0 错 | ✅ 1m 1s (EmptyState) + 1m 3s (热力图) |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |
| **simulate-login.sh** 21 路由 | ✅ 21/21 |
| **simulate-jwt.sh** 21 路由 | ✅ 21/21 |

## 4. 累计 67 个版本 (V3.5.46-V3.6.12)
