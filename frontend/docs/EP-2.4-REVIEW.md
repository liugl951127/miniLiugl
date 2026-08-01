# V3.5.81+ Element Plus 2.4 文档 Review

## 1. 覆盖范围

5 P0 view + V3.5.80 重写后, 检查 Element Plus 2.4 组件使用是否规范.

## 2. P0 view 用到的 Element Plus 2.4 组件清单

| 组件 | 数量 | view |
|------|------|------|
| `el-container / el-aside / el-header` | 3 | layout/Index.vue |
| `el-row / el-col` | 15+ | 全部 5 view |
| `el-card` | 30+ | 全部 5 view |
| `el-button` | 40+ | 全部 5 view |
| `el-icon` | 50+ | 全部 5 view (Vue 3 命名) |
| `el-input / el-textarea` | 8+ | Login / H5Login / chat / AiChat / Stream |
| `el-form / el-form-item` | 5+ | Login / chat / AiChat |
| `el-table / el-table-column` | 6+ | Audit / Metrics / Alerts / Monitor |
| `el-tabs / el-tab-pane` | 3+ | Login / Alerts / H5Login |
| `el-tag` | 40+ | 全部 5 view |
| `el-select / el-option` | 8+ | Metrics / Audit / H5Login |
| `el-checkbox` | 5+ | chat (3 工具栏) / Login (记住) |
| `el-switch` | 3+ | Metrics (自动刷新) / Alerts (启用) |
| `el-progress` | 3+ | Monitor (JVM / 磁盘 / Metrics 错误率) |
| `el-rate` | 1 | Stream (评分反馈) |
| `el-pagination` | 1 | Audit |
| `el-empty` | 5+ | 全部 5 view |
| `el-skeleton` | 2+ | Monitor (JVM / 磁盘 加载) |
| `el-statistic` | 5+ | Dashboard (KPI) / Metrics (KPI) |
| `el-collapse / el-collapse-item` | 1 | chat (工具调用折叠) |
| `el-drawer` | 2 | layout (mobile) / chat (历史) |
| `el-dialog` | 2+ | Alerts (规则 / 渠道) |
| `el-notification` | 1+ | SW.js 通知 |
| `el-tooltip` | 1+ | Audit (overflow) |

## 3. Element Plus 2.4 新组件 (已用)

- ✅ `el-statistic` - Dashboard 4 KPI / Metrics 4 KPI (V3.5.80 推广)
- ✅ `el-skeleton` - Monitor 加载态
- ✅ `el-empty` - 全部 5 view 空态
- ✅ `el-rate` - Stream 评分反馈 (V5.19+)
- ✅ `el-progress :color` 函数 - Monitor 阈值变色

## 4. 命名规范 (V3.5.81 review)

### ✅ 正确用法

```vue
<!-- el-icon 包裹图标组件 -->
<el-icon><User /></el-icon>

<!-- el-icon 动态颜色 -->
<el-icon :size="20" :color="var(--liugl-primary)"><component :is="icon" /></el-icon>

<!-- el-table-column 用 template #default slot -->
<el-table-column label="操作">
  <template #default="{ row }">
    <el-button @click="onEdit(row)">编辑</el-button>
  </template>
</el-table-column>

<!-- el-card shadow="hover" 统一 -->
<el-card shadow="hover" class="kpi-card">
  <el-statistic :value="value" :title="label" />
</el-card>

<!-- el-tabs v-model + @tab-change -->
<el-tabs v-model="tab" @tab-change="onTabChange">
  <el-tab-pane label="标签" name="key" />
</el-tabs>
```

### ❌ 错用法 (V3.5.80 修过)

```vue
<!-- 错: el-icon:: 非法语法 -->
<el-icon::User />

<!-- 错: 用不存在的图标名 -->
<el-icon><ChatLine /></el-icon>  <!-- ChatLine 不存在, 用 ChatLineRound / ChatDotRound -->
```

## 5. 数据驱动 (V3.5.80 推广)

```vue
<!-- V3.5.80: 演示账号数据驱动 -->
<el-col v-for="acc in demoAccounts" :key="acc.username" :xs="12" :sm="8">
  <el-card shadow="hover" class="demo-card" @click="fillAccount(acc.username, acc.password)">
    <div class="demo-role">{{ acc.role }}</div>
    ...
  </el-card>
</el-col>
```

```js
// script setup
const demoAccounts = [
  { role: '👑 超级管理员', username: 'adminLiugl', password: 'admin123456', desc: '平台所有者' },
  ...
]
```

## 6. CSS variable (V3.5.80 统一)

```vue
<style lang="scss" scoped>
.page-monitor {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
  background: var(--liugl-bg);
}

.kpi-value {
  color: var(--liugl-text);
}

.detail-row .k { color: var(--liugl-text-secondary); }
</style>
```

## 7. 5 view 一致性矩阵 (V3.5.80)

| view | page-header | section | el-card | el-table | el-statistic | i18n $t() |
|------|:-:|:-:|:-:|:-:|:-:|:-:|
| auth/Login | ✓ | ✓ | ✓ | - | - | ✓ 5 处 |
| auth/H5Login | ✓ | ✓ | ✓ | - | - | - |
| chat/Index | ✓ | ✓ | ✓ | - | - | - |
| ai/AiChat | ✓ | ✓ | ✓ | - | - | - |
| monitor/Index | ✓ | ✓ | ✓ | ✓ | - | - |
| admin/Dashboard (样板) | ✓ | ✓ | ✓ | - | ✓ | - |
| admin/Audit (P1) | ✓ | ✓ | ✓ | ✓ | - | - |
| admin/Metrics (P1) | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| admin/Alerts (P1) | ✓ | ✓ | ✓ | ✓ | - | - |
| chat/Stream (P1) | ✓ | ✓ | ✓ | - | - | - |

## 8. 已知 EP 2.4 限制 + 替代

| 限制 | 替代方案 |
|------|---------|
| `el-table` 不支持虚拟滚动 (1万+行) | 用 `el-table-v2` (V2.4.4+) 或分页 (V3.5.80 Audit 用分页) |
| `el-tree` 大数据慢 | 懒加载 `lazy` + `:load` |
| `el-select` 多选 1000+ 性能差 | 用 `el-select-v2` 虚拟滚动 |
| `el-dialog` 不能嵌套 `el-drawer` | 反过来 (drawer 套 dialog) |
| `el-tooltip` disabled 状态不显示 | 改用 `el-popover` 或 title 属性 |

## 9. 累计 10 view 全部符合 EP 2.4 规范

V3.5.80 重写: Dashboard / Login / H5Login / Chat / AiChat / Monitor (5 P0)
V3.5.81 重写: Audit / Metrics / Alerts / Stream / admin/Index (5 P1)
