# V3.5.96 GitHub Actions 接 Check 8/9 + admin/Index.vue 侧边栏重写

## 1. V3.5.95 之后

V3.5.95 加了 `check-setup-var.cjs` (Check 8) + `verify-docker-compose.sh` (Check 9),
但没接到 GitHub Actions — V3.5.96 接上 + 重写 admin/Index.vue 侧边栏。

## 2. V3.5.96 改

### 2.1 `.github/workflows/defensive-ci.yml` (V3.5.96 新)

独立 workflow, 触发白名单:

```yaml
on:
  push:
    branches: [main, develop]
    paths:
      # Check 8: <script setup>
      - 'frontend/src/views/**/*.vue'
      - 'frontend/src/components/**/*.vue'
      # Check 9: docker-compose
      - 'docker-compose.yml'
      - 'deploy/otel-collector-config.yaml'
      # CI 脚本
      - 'scripts/check-setup-var.cjs'
      - 'scripts/verify-docker-compose.sh'
```

7 step:
1. Checkout
2. Set up Node 18
3. Set up Python 3.11
4. Run ci-check.sh (Check 1-7) - id: ci-check
5. Check 8 - `<script setup>` 防御性自检 - id: check8
6. Check 9 - docker-compose 静态验证 - id: check9
7. Defensive CI Summary (汇总 GitHub Step Summary)

### 2.2 `.github/workflows/ci.yml` 加 Check 8/9 step (V3.5.96)

```yaml
ci-check:
  name: CI 9 项硬性检查 (V3.5.95+)
  steps:
    - name: Set up Node.js 18
    - name: Set up Python 3.11
    - name: Run ci-check.sh (Check 1-9)
    - name: Check 8 - <script setup> 防御性自检
      run: node scripts/check-setup-var.cjs
    - name: Check 9 - docker-compose 静态验证
      run: bash scripts/verify-docker-compose.sh
    - name: Run check_entity_schema.py
```

### 2.3 `admin/Index.vue` V3.5.96 重写 (156 → 436 行)

V3.5.96 新增功能:
1. **侧边栏分组** - 4 组: core / observability / ai / system
2. **折叠按钮** - 顶栏 `Fold` 图标 + `localStorage` 持久化
3. **顶部面包屑** - el-breadcrumb 路径 / 首页 / 后台 / 当前页
4. **健康状态标签** - `health: up/down/unknown` 绿/红/灰
5. **告警 Badge** - el-badge 红色 `alertCount`
6. **用户信息** - 头像 + 昵称 (来自 userStore.profile)
7. **快捷入口卡片** - 8 链接: chat/kg/agent/ai/marketplace/monitor/user/profile
8. **V3.5.96 i18n** - admin.menu.* 12 键 + admin.group.* 4 键 + admin.quick.* 9 键 = 25 键

### 2.4 i18n 键 (V3.5.96, 25 keys)

```js
// zh.js / en.js
admin: {
  audit: '审计' / 'Audit',
  alerts: '告警' / 'Alerts',
  collapse: '折叠侧边栏' / 'Collapse Sidebar',
  home: '首页' / 'Home',
  health: { up: '系统正常', down: '异常', unknown: '未知' },
  group: {
    core: '核心' / 'Core',
    observability: '可观测性' / 'Observability',
    ai: 'AI 模型' / 'AI Models',
    system: '系统' / 'System',
  },
  menu: {
    dashboard / metrics / audit / alerts / traces / cluster / monitor /
    provider / leaderboard / apikey / framework / governance / document /
    push / wechat / wechatUnionid,  // 16 键
  },
  quick: {
    title / chat / kg / agent / ai / marketplace / monitor / user / profile,  // 9 键
  },
}
```

## 3. admin/Index.vue V3.5.96 改进对比

| 维度 | V3.5.95 (156 行) | V3.5.96 (436 行) |
|------|-----------------|------------------|
| 侧边栏分组 | ❌ 9 个 menu 1 段 | ✅ 4 组 (core/observability/ai/system) |
| 折叠按钮 | ❌ | ✅ Fold 按钮 + localStorage |
| 面包屑 | ❌ 只有标题 | ✅ 完整路径 (Home/Admin/Current) |
| 健康状态 | ❌ | ✅ Tag + 3 状态色 |
| 告警 Badge | ❌ | ✅ el-badge 红色数字 |
| 用户信息 | ❌ | ✅ 头像 + 昵称 |
| 快捷入口 | 4 链接 (alert) | 8 链接 (quick-card) |
| i18n | 部分 (admin.title / admin.menu.7 键) | 完整 (admin.* 25 键) |
| 移动端 | ❌ | ✅ < 768px 自动折叠 64px |
| 动画 | 页面切换 fade | 同 + 侧边栏折叠过渡 |

## 4. 触发白名单 (V3.5.96)

defensive-ci.yml 只在以下文件改动时跑:
- `frontend/src/views/**/*.vue` (改 22 view)
- `frontend/src/components/**/*.vue` (改 component)
- `docker-compose.yml` (改 compose)
- `scripts/check-setup-var.cjs` (改 check 脚本)
- `scripts/verify-docker-compose.sh` (改 check 脚本)

避免: 改文档/UI 微调触发 5min CI 浪费。

## 5. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 文件 0 错误 |
| `verify-docker-compose.sh` (Check 9) | ✅ 19 services 干净 |
| **ci-check 9/9** | ✅ |
| **defensive-ci.yml** yaml 解析 | ✅ (jobs.defensive 7 step) |
| vite build 0 错 | ✅ 53s |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| Round 6 90 轮 100% pass | ✅ 1890 GET |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 6. 累计 51 个版本 (V3.5.46-V3.5.96)
