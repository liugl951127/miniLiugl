# Day 16 Runbook — 多租户前端 + SQL 补全

**日期**: 2026-06-20
**目标**: V3.1 — 多租户管理系统前端 UI + SQL 补全

---

## 背景

- 后端租户模块已存在: `TenantController` (7 端点) + `TenantService` + `sql/17_tenant.sql`
- 前端完全缺失: 无 API / 无 Store / 无 UI / 无路由 / 无菜单
- 本次任务: **补全租户管理前端闭环**

---

## 任务清单

### 1. SQL 补全
- [ ] 确认 `sql/17_tenant.sql` 与 `sql/init-minimax.sql` 同步
- [ ] 确保 init-sql.sh 包含 17_tenant.sql

### 2. 前端 API
- [ ] `src/api/tenant.js`: listTenants / createTenant / setTenantStatus / updateQuota / deleteTenant / listTenantUsers / myTenant

### 3. Pinia Store
- [ ] `src/store/tenant.js`: useTenantStore (fetchTenants / createTenant / toggleStatus / setQuota / removeTenant / fetchTenantUsers)

### 4. 租户管理 UI
- [ ] `src/views/tenant/Index.vue`:
  - KPI 卡片 (租户总数/正常/用户数/停用)
  - 租户列表表格 (ID/代码/名称/套餐/状态/配额进度/QPS/联系人)
  - 启/停 按钮
  - 配额调整弹窗
  - 创建租户表单弹窗
  - 用户列表弹窗
  - 删除租户 (禁止删 default)

### 5. 路由 + 布局
- [ ] `src/router/index.js`: 新增 `/tenant` 路由 (requiresSuper: true)
- [ ] `src/layout/Index.vue`: 侧边栏超级管理员区块新增 "🏢 租户管理" 菜单
- [ ] `src/layout/Index.vue`: 右上角下拉菜单新增 "🏢 租户管理" 入口
- [ ] `src/layout/Index.vue`: onCommand handler 支持 tenant 命令
- [ ] `src/layout/Index.vue`: activeMenu computed 支持 `/tenant` 路径

### 6. 自检
- [ ] `scripts/self-check.sh` ≥ 78/78
- [ ] `scripts/java-static-check.sh` 0 错误
- [ ] `npm run build` 通过

---

## 预期文件变更

| 文件 | 操作 |
|------|------|
| `frontend/src/api/tenant.js` | 新增 |
| `frontend/src/store/tenant.js` | 新增 |
| `frontend/src/views/tenant/Index.vue` | 新增 |
| `frontend/src/router/index.js` | 编辑 |
| `frontend/src/layout/Index.vue` | 编辑 |

---

## 验证方法

1. 用 adminLiugl 登录 → 侧边栏出现 "🏢 租户管理"
2. 点击进入 → 看到 2 个预置租户 (default / demo)
3. 点击 "新建租户" → 填写表单 → 租户出现在列表
4. 点击 "停用" → 状态变为红色
5. 点击 "启用" → 状态变绿
6. 点击 "用户" → 看到该租户下的用户列表
7. 点击 "配额" → 调整配额 → 进度条更新
8. 非 adminLiugl 账号访问 `/tenant` → 302 跳转首页

---

## 关键数据

- 新增前端文件: 3 (api + store + vue)
- 编辑前端文件: 2 (router + layout)
- 无需新增后端 Java 文件 (后端已完整)
- 预计代码行: ~400 行 Vue / ~80 行 JS
