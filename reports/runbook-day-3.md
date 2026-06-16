# Day 3 Runbook - 会话模块 (CRUD + 侧边栏)

**目标**: 落地多会话（chat session）的持久化、增删改查、消息存储 + 前端侧边栏 UI

---

## 后端交付（minimax-chat 模块）

### 1. SQL 脚本 `sql/03_chat.sql`
- `chat_session` 表：id / user_id / title / model / system_prompt / status (1正常 0归档) / created_at / updated_at
- `chat_message` 表：id / session_id / role (user/assistant/system) / content / tokens / created_at
- 索引：user_id+updated_at、session_id+created_at

### 2. 实体
- `ChatSession` (字段对齐表)
- `ChatMessage` (字段对齐表)
- 枚举：`MessageRole` (USER / ASSISTANT / SYSTEM)

### 3. Mapper
- `ChatSessionMapper` (BaseMapper + 自定义 selectByUserId)
- `ChatMessageMapper` (BaseMapper + 自定义 selectBySessionId 分页)
- XML: ChatSessionMapper.xml / ChatMessageMapper.xml

### 4. DTO/VO
- `CreateSessionRequest` (title, model, systemPrompt)
- `UpdateSessionRequest` (title / status)
- `SessionVO` (id, title, model, status, messageCount, lastMessageAt, createdAt)
- `MessageVO` (id, role, content, createdAt)

### 5. Service
- `ChatSessionService` 接口 + Impl
  - create(userId, req)
  - listByUserId(userId, page, size)
  - getById(id, userId)  // 校验归属
  - update(id, userId, req)
  - remove(id, userId)    // 软删
- `ChatMessageService` 接口 + Impl
  - listBySessionId(sessionId, page, size)
  - append(sessionId, role, content)

### 6. Controller
- `SessionController` `/api/v1/sessions`
  - GET    `/`          list
  - POST   `/`          create
  - GET    `/{id}`      detail
  - PUT    `/{id}`      update
  - DELETE `/{id}`      remove
- `MessageController` `/api/v1/sessions/{id}/messages`
  - GET    `/`          list（分页）
  - POST   `/`          append（Day 5 前临时用）

### 7. 配置
- 复用 `minimax-auth` 的 SecurityConfig 加 permitAll 或新增
- `minimax-chat` 模块 `application.yml` (端口 8082)
- 前端代理 `/api/v1/sessions` → 8082

### 8. 自检
- 跑 `mvn -pl minimax-chat -am compile`
- 跑 `JwtTokenProviderTest` 验证未回归
- Java 静态体检

## 前端交付

### 1. API 层 `frontend/src/api/chat.js`
- sessionApi: list/create/detail/update/remove
- messageApi: list/append

### 2. Pinia store `frontend/src/store/session.js`
- currentSessionId, sessions[], messages[]
- loadSessions / selectSession / createSession / deleteSession / appendMessage

### 3. UI 改造 `frontend/src/views/chat/Index.vue`
- 左侧栏：会话列表 + 新建按钮 + 搜索
- 右侧：消息流（Day 5 加流式）+ 输入框
- 接入真实 API，移除 Day 1 占位

### 4. 前端构建
- `npm run build` 必须过

## 自检 + 报告

- `scripts/daily-build.sh 3`
- `scripts/java-static-check.sh`
- 写 `reports/day-3-report.md`
- 更新 `PROGRESS.md`

## 打包 + 邮件

- 打包到 `/workspace/minimax-platform-day3.tar.gz`
- 加密 zip：`/workspace/minimax-platform-day3.zip` 密码 `MinMax2026!`
- SMTP 发邮件到 `liugeliang951127@gmail.com`
- 主题: `[MiniMax] Day 3 - Sessions & Sidebar`

---

**完成时间目标**: 触发后 30 分钟内（实际进度视复杂程度）
**质量门槛**: 前端构建过、Java 体检过、0 个新增 TODO
