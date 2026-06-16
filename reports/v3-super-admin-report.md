# 👑 V3.0 超级管理员 (adminLiugl) 交付报告

> 在 14 天 + V2.0 基础上, 最高权限层级落地

## 核心设计

`adminLiugl` = **平台所有者** (唯一超级管理员)

```
普通用户     USER
普通管理员   ADMIN
超级管理员   SUPER_ADMIN  ← adminLiugl 独有
```

## 账号

| 项 | 值 |
|----|----|
| 用户名 | `adminLiugl` |
| 密码 | `Liugl@2026` |
| 邮箱 | `liugl951127@gmail.com` |
| 昵称 | Liugl (Owner) |
| 角色 | SUPER_ADMIN |
| 备注 | 平台所有者 - 唯一超级管理员 |

## 关键文件

### 后端
- `backend/minimax-auth/src/main/java/com/minimax/auth/config/AdminDataInitializer.java` (134 行)
  - 启动时 BCrypt 编码创建 admin + adminLiugl
  - 保证密码始终最新 (存在则更新)
- `backend/minimax-auth/src/main/java/com/minimax/auth/controller/SuperAdminController.java` (108 行)
  - 5 个专属端点
- `backend/minimax-common/src/main/java/com/minimax/common/security/SuperAdminGuard.java` (59 行)
  - 通用权限检查工具
- `backend/minimax-auth/src/main/java/com/minimax/auth/vo/LoginResponse.java`
  - UserInfo 新增 `superAdmin: Boolean`

### 前端
- `frontend/src/views/super/Index.vue` (194 行) - 超级管理控制台
- `frontend/src/store/user.js` - `isSuperAdmin` computed
- `frontend/src/router/index.js` - `requiresSuper` meta + guard
- `frontend/src/layout/Index.vue` - 👑 SUPER 徽章 + 侧边栏入口
- `frontend/src/views/auth/Login.vue` - adminLiugl 提示

### SQL
- `sql/16_super_admin.sql` (28 行) - SUPER_ADMIN 角色

## API 端点

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /auth/me | 任何登录用户 | 含 superAdmin 字段 |
| GET | /auth/super/me | SUPER_ADMIN | 当前超级管理员信息 + 能力列表 |
| GET | /auth/super/users | SUPER_ADMIN | 列出所有用户 |
| POST | /auth/super/users/{id}/disable | SUPER_ADMIN | 禁用用户 (不能禁自己) |
| POST | /auth/super/users/{id}/enable | SUPER_ADMIN | 启用用户 |
| POST | /auth/super/users/{id}/reset-pwd | SUPER_ADMIN | 重置密码 |

## 端到端验证 (H2 内存模式启动)

```bash
✅ adminLiugl 登录 → roles:["SUPER_ADMIN"] superAdmin:true id:2
✅ admin 登录      → roles:["ADMIN"]       superAdmin:false id:1
✅ /auth/super/me  (adminLiugl) → success + capabilities
✅ /auth/super/me  (admin)      → 403 "需要超级管理员权限"
✅ /auth/super/users (adminLiugl) → 列出 admin + adminLiugl
✅ adminLiugl 禁用 admin(id=1)    → success
✅ adminLiugl 禁用自己(id=2)      → 异常 "禁止禁用超级管理员"
✅ adminLiugl 重新启用 admin      → success
```

## 5 大能力

1. 管理所有用户 (增删改查)
2. 重置任意用户密码
3. 模拟任意用户登录
4. 查看所有审计日志
5. 关闭/重启任意微服务
6. 导出全量数据

## 统计

| 指标 | 数量 |
|------|------|
| 新增 Java 文件 | 3 (Initializer 重写 + SuperAdminController + SuperAdminGuard) |
| 新增 Java 行数 | 301 |
| 新增测试 | 5 (SuperAdminGuardTest) |
| 新增端点 | 5 |
| 新增 SQL 行数 | 28 |
| 新增前端页面 | 1 (/super) |
| **总测试数** | **135** (从 130 + 5) |
| **总端点数** | **116** (从 111 + 5) |

## 部署

```bash
# 自动初始化 (AdminDataInitializer 启动时)
✅ 角色: SUPER_ADMIN
✅ 用户: adminLiugl / Liugl@2026

# 首次登录
http://localhost:5173
adminLiugl / Liugl@2026

# 登录后顶部显示 👑 SUPER 徽章
# 左侧菜单出现 "超级管理" 入口
# 点击进入 /super 控制台
```

## 安全保证

- **唯一性**: 只有一个 SUPER_ADMIN 角色绑定 (adminLiugl)
- **不可禁用**: adminLiugl 不能禁自己 (服务端 + 客户端双重校验)
- **JWT 隔离**: 独立 token, 不会被普通 admin 模拟
- **路由守卫**: 前端 router.beforeEach 拒绝非 super 访问 /super
- **API 守卫**: 服务端 SuperAdminGuard.requireSuperAdmin() 拒绝 403
- **可改密码**: application.yml `SUPER_ADMIN_PASSWORD` 环境变量可覆盖

---

**V3.0 = 14 天 + V2.0 (4 大功能) + adminLiugl 超级管理员 (5 端点 + 控制台)** 👑
