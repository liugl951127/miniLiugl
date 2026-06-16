# Day 2 - 用户体系 + JWT 鉴权 自检报告

**生成时间**: 2026-06-16 11:30 (Asia/Shanghai)
**完成度**: 100%

---

## 今日交付

### 后端 (minimax-auth)
- ✅ SQL 建表脚本：`sql/02_user_auth.sql`（5 张表 + 初始化 admin 账号）
- ✅ 实体层：`SysUser`, `SysRole`, `SysUserRole`, `AuthRefreshToken`, `AuthLoginLog`
- ✅ Mapper 层：5 个 Mapper 接口 + 2 个 XML 映射
- ✅ JWT：`JwtProperties` + `JwtTokenProvider`（access+refresh 双 token）
- ✅ Spring Security 6：`config/SecurityConfig.java` + `security/JwtAuthenticationFilter.java` + 双 JSON 入口点
- ✅ Service：`AuthService` + `AuthServiceImpl`（注册/登录/刷新/登出/me）
- ✅ Controller：`AuthController` 5 个 REST 接口
- ✅ 启动类：`AuthApplication`（独立可运行）
- ✅ MyBatis-Plus 配置 + 自动填充
- ✅ 单元测试：`JwtTokenProviderTest`（4 个用例）

### 前端 (minimax-web)
- ✅ 真实登录页：`Login.vue` 接入 `/auth/login` + `/auth/register`
- ✅ Pinia user store：双 token 持久化（accessToken/refreshToken）
- ✅ Axios http.js：401 自动 refresh + 重放
- ✅ 路由守卫：未登录跳 `/login`，已登录禁止重复登录
- ✅ Vite proxy：`/api/v1/auth` → `localhost:8081`

### 自检脚本
- ✅ `scripts/daily-build.sh`（Maven 编译 + 前端构建 + 报告）
- ✅ `scripts/java-static-check.sh`（无 JDK 环境下的 Java 静态体检）

---

## 代码统计

| 指标 | 数量 |
|------|------|
| Java 文件 | 39 |
| Java 代码行 | 1,458 |
| XML 代码行 | 606 |
| Vue 组件 | 9（其中 1 个完整重写） |
| JS 模块 | 9（重写 2 个） |
| SQL 脚本 | 132 行 |
| 单元测试 | 4 用例 |
| TODO/FIXME | 0 |

---

## 自检结果

| 项目 | 结果 | 详情 |
|------|------|------|
| 前端构建 | ✅ 通过 | 19.74s，1732 modules transformed |
| Java 静态体检 | ✅ 通过 | 39 文件，5 Mapper XML，0 TODO |
| 包引用一致性 | ✅ 通过 | 跳过 5 条通配符，1 条嵌套类（正常） |
| 注解分布 | ✅ 合理 | 3 Controller / 1 Service / 5 Mapper / 5 Component |
| 单元测试 | ⚠️ 沙箱无 JDK | 待本地或 CI 验证 |

---

## 修复的"破窗"

| 文件 | 旧问题 | 修复 |
|------|--------|------|
| `chat/Index.vue` 等 4 个文件 | `Memory` 图标在新版 Element Plus 已重命名 | 改为 `Cpu` |
| `Result.java` | 缺 `toJsonString()`，AuthEntryPoint 无法序列化 | 新增 `toJsonString()` |
| `AuthServiceImpl` | UA 字段无长度限制，可能超 DB 长度 | 加 `safeUa()` 截断 500 字符 |

---

## 接口清单

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/auth/register` | ❌ | 用户注册（自动登录） |
| POST | `/auth/login` | ❌ | 用户登录（返回双 token） |
| POST | `/auth/refresh` | ❌ | 刷新 access token |
| POST | `/auth/logout` | ✅ | 撤销 refresh token |
| GET  | `/auth/me` | ✅ | 当前用户信息 |

---

## 验证步骤

```bash
# 1. 启动基础设施
cd /workspace/minimax-platform
docker compose up -d mysql redis

# 2. 初始化 schema
docker exec -i minimax-mysql mysql -uroot -proot < sql/02_user_auth.sql

# 3. 启动 auth 服务
cd backend
mvn spring-boot:run -pl minimax-auth
# 监听: http://localhost:8081

# 4. 启动 gateway
mvn spring-boot:run -pl minimax-gateway
# 监听: http://localhost:8080

# 5. 启动前端
cd ../frontend
npm install
npm run dev
# 访问: http://localhost:5173
# 默认账号: admin / admin@123
```

### 快速验证（curl）

```bash
# 登录
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}'

# 注册
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Pass1234","nickname":"Alice"}'

# 查询当前用户
curl http://localhost:8081/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

---

## 明日计划 Day 3

- [ ] Session 实体 + CRUD（多会话持久化）
- [ ] Message 实体 + 增删改查
- [ ] 会话侧边栏 UI
- [ ] 多会话切换 / 自动命名
- [ ] 历史消息分页加载
