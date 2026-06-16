# MiniMax 平台 - 14 天推进记录

> 每天 20:00 自动构建一次。每天结束时这里会追加当日产出 + 明日计划。

## Day 1 - 2026-06-15 ✅ 项目骨架

**今日完成：**
- [x] 顶层目录结构（backend / frontend / sql / scripts / deploy）
- [x] Spring Boot 3 多模块 Maven 工程（7 个子模块）
- [x] 统一响应包装 `Result<T>` + 业务异常体系 + 全局异常处理
- [x] 前端 Vue 3 + Vite + Element Plus + Pinia 骨架
- [x] 前端路由（登录/对话/知识库/记忆/管理后台/关于）
- [x] 布局：左侧菜单 + 顶部导航 + 用户下拉
- [x] `docker-compose.yml`：MySQL 8 + Redis 7 + ES 8 + MinIO + 监控
- [x] 网关健康检查 + 平台介绍 API
- [x] 每日构建脚本 `scripts/daily-build.sh`（自检 + 打包）
- [x] 定时任务安装脚本 `scripts/setup-cron.sh`

**关键文件数：** ~40 个源文件 + 1 个 docker-compose + 完整文档
**压缩包大小：** ~60KB（不含 node_modules/target）

**明日计划 Day 2：**
- [ ] User 实体 + MyBatis-Plus 持久层
- [ ] JWT 工具类（生成/解析/刷新）
- [ ] Spring Security 6 配置（无状态 + 自定义过滤器）
- [ ] AuthController：`/auth/register` `/auth/login` `/auth/me` `/auth/refresh` `/auth/logout`
- [ ] 前端：真实登录页 + Token 持久化 + 路由守卫完善
- [ ] 接入 MySQL 真实建表

## Day 2 - 2026-06-16 ✅ 用户体系 + JWT 鉴权

**今日完成：**
- [x] SQL 建表脚本（sys_user/role/refresh_token/login_log）
- [x] User/Role/UserRole/RefreshToken/LoginLog 5 个实体
- [x] MyBatis-Plus 5 个 Mapper + 2 个 XML
- [x] JWT 工具（access 30min + refresh 7d 双 token + SHA-256 哈希刷新）
- [x] Spring Security 6 + JwtAuthenticationFilter + 双 JSON 入口点
- [x] AuthService（注册/登录/刷新/登出/me）
- [x] AuthController 5 个 REST 接口
- [x] 启动类 AuthApplication（独立可跑）
- [x] 前端真实登录页（登录/注册切换）
- [x] Pinia user store 双 token 持久化
- [x] Axios http.js 401 自动 refresh + 重放
- [x] 路由守卫完整版
- [x] Vite proxy /api/v1/auth → 8081
- [x] 单元测试 JwtTokenProviderTest（4 用例）
- [x] 自检脚本 daily-build.sh + java-static-check.sh
- [x] 修复 Day 1 破窗：Element Plus 图标改名 (Memory→Cpu) / Result.toJsonString / UA 长度限制

**关键文件数：** 39 Java + 9 Vue + 9 JS + 5 SQL = 62 个源文件
**代码量：** Java 1458 行 + XML 606 行 + Vue 609 行 + JS 291 行 + SQL 132 行 = 3096 行
**前端构建：** 19.74s ✅
**Java 静态体检：** ✅（含 5 通配符 import / 1 嵌套类 / 0 TODO）

**明日计划 Day 3：**
- [ ] Session 实体 + CRUD
- [ ] Message 实体 + 增删改查
- [ ] 会话侧边栏 UI
- [ ] 多会话切换
- [ ] 历史消息分页加载

## Day 3 - 待开始

> ⏰ 明日 20:00 由 cron 触发推进。Runbook: `reports/runbook-day-3.md`
> Cron 任务 ID: `409475887587539` (Mavis / minimax-daily-pipeline)
> 下次执行: 2026-06-16 20:00 (Asia/Shanghai)

## Day 4 - 待开始
