# MiniMax Platform — API 文档

> 完整的 HTTP API 端点参考。所有请求需 `Authorization: Bearer <accessToken>` 除公开端点。
>
> Base URL: `http://localhost:8080` (经 Gateway) 或 `http://localhost:808X` (直连)

---

## 通用约定

### 响应格式
```json
{
  "code": 0,
  "message": "OK",
  "data": { ... },
  "timestamp": 1234567890
}
```

### 错误码
- `0` 成功
- `1000` 业务异常
- `1001` 参数校验失败
- `1002` 未登录或登录已过期
- `1003` 无权限
- `1500` 服务降级
- `1500+` 限流触发

### 限流头
- `X-Trace-Id`: 链路追踪 ID
- `X-RateLimit-Remaining`: 剩余配额
- `X-RateLimit-Reset`: 重置时间

---

## 1. Auth 模块 `:8081/api/v1`

### 1.1 公开端点

#### `POST /auth/register`
注册新用户。
```json
// Request
{
  "username": "alice",
  "password": "alice@123",
  "nickname": "Alice",
  "email": "alice@example.com"
}
// Response
{
  "code": 0,
  "data": {
    "id": 2,
    "username": "alice",
    "nickname": "Alice"
  }
}
```

#### `POST /auth/login`
登录获取双 token。
```json
// Request
{"username": "admin", "password": "admin@123"}
// Response
{
  "code": 0,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 1800,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "roles": ["ADMIN"]
    }
  }
}
```

#### `POST /auth/refresh`
用 refresh_token 续期。
```json
{"refreshToken": "eyJ..."}
```

#### `GET /auth/health`
健康检查（公开）。

### 1.2 鉴权端点

#### `GET /auth/me`
当前用户信息。

#### `GET /auth/users`
用户列表（admin）。

#### `PUT /auth/users/{id}`
更新用户。

#### `POST /auth/logout`
登出（使 token 失效）。

---

## 2. Chat 模块 `:8082/api/v1`

#### `GET /sessions`
列当前用户会话。

#### `POST /sessions`
创建会话。
```json
{"title": "新对话", "modelCode": "gpt-4o"}
```

#### `GET /sessions/{id}`
会话详情。

#### `DELETE /sessions/{id}`
删除会话。

#### `GET /sessions/{id}/messages`
会话的所有消息。

#### `POST /sessions/{id}/messages`
非流式发消息。
```json
{"role": "user", "content": "你好"}
```

#### `POST /sessions/{id}/messages/stream?streamId=xxx`
**流式发消息** (SSE)。
```json
// Request
{
  "role": "user",
  "content": "你好",
  "modelCode": "gpt-4o"
}
// Response: text/event-stream
data: {"type":"chunk","content":"你"}
data: {"type":"chunk","content":"好"}
data: {"type":"done"}
```

#### `POST /sessions/stop-stream`
停止流式。
```json
{"streamId": "stream-123"}
```

---

## 3. Model 模块 `:8083/api/v1`

#### `GET /models`
列出可用模型。

#### `GET /models/providers`
列出 Provider。

#### `GET /models/{code}`
模型详情。

#### `POST /models/chat`
OpenAI 兼容 chat 端点。
```json
{
  "model": "gpt-4o",
  "messages": [
    {"role": "user", "content": "你好"}
  ],
  "temperature": 0.7
}
```

#### `POST /models/chat/stream?streamId=xxx`
流式 chat (SSE)。

#### `POST /models/chat/cancel?streamId=xxx`
取消流式。

---

## 4. Memory 模块 `:8084/api/v1`

### 短期记忆
- `GET /memory/short-term/{sid}` 拉取
- `POST /memory/short-term/{sid}` 追加
- `DELETE /memory/short-term/{sid}` 清空
- `GET /memory/short-term/{sid}/size` 统计

### 上下文
- `POST /memory/cross-context` 跨会话 context
  ```json
  {
    "userId": 1,
    "sessionId": 100,
    "systemPrompt": "你是助手",
    "maxContext": 4096,
    "recallTopK": 5
  }
  ```

### 摘要
- `POST /memory/summarize/{sid}` 触发摘要
- `GET /memory/summary/{sid}` 读摘要

### 长期记忆
- `POST /memory/long-term` 存储
  ```json
  {
    "userId": 1,
    "sessionId": 100,
    "role": "user",
    "content": "我喜欢川菜",
    "tags": "food"
  }
  ```
- `POST /memory/long-term/recall` 召回
  ```json
  {"userId": 1, "query": "宠物", "topK": 5}
  ```
- `GET /memory/long-term/recent?userId=1&limit=20` 列最近
- `DELETE /memory/long-term/{id}?userId=1` 删除

### 偏好
- `PUT /memory/pref/{key}?userId=1` 设置
- `GET /memory/pref/{key}?userId=1` 读取
- `GET /memory/pref?userId=1` 列出
- `DELETE /memory/pref/{key}?userId=1` 删除

---

## 5. RAG 模块 `:8085/api/v1`

### 知识库
- `POST /rag/kb` 创建
- `GET /rag/kb?ownerId=1` 列我的
- `GET /rag/kb/public` 列公开
- `GET /rag/kb/{id}?ownerId=1` 详情
- `DELETE /rag/kb/{id}?ownerId=1` 删除

### 文档
- `POST /rag/doc/upload?ownerId=1&kbId=1` 上传 (multipart)
- `GET /rag/doc?kbId=1` 列文档
- `GET /rag/doc/{id}/chunks` 切片
- `DELETE /rag/doc/{id}?ownerId=1` 删除

### 检索
- `POST /rag/retrieve` 纯检索
- `POST /rag/ask` RAG 问答
  ```json
  {
    "kbId": 1,
    "question": "支付方式有哪些?",
    "topK": 5
  }
  // Response
  {
    "data": {
      "answer": "支持微信、支付宝...",
      "sources": [
        {"chunkId": 10, "docTitle": "产品手册", "score": 0.92, "snippet": "..."}
      ]
    }
  }
  ```

---

## 6. Function 模块 `:8086/api/v1`

### 工具 CRUD
- `GET /function/tools` 列出
- `GET /function/tools/{id}` 详情
- `POST /function/tools?ownerId=1` 注册
  ```json
  {
    "name": "my_tool",
    "displayName": "我的工具",
    "description": "...",
    "parameters": "{\"type\":\"object\",\"properties\":{...}}",
    "endpoint": "http://my-service/tool",
    "httpMethod": "POST"
  }
  ```
- `PUT /function/tools/{id}?ownerId=1` 更新
- `DELETE /function/tools/{id}?ownerId=1` 删除

### 调用
- `POST /function/invoke/{name}?userId=1` 直接调用
- `GET /function/logs?userId=1` 调用历史

### Chat
- `POST /function/chat` chat + 工具循环
  ```json
  {
    "userId": 1,
    "message": "上海现在几点?",
    "enableTools": true,
    "toolNames": ["get_current_time"]
  }
  ```

---

## 7. Admin 模块 `:8087/api/v1`

### 用户管理
- `GET /admin/users?page=1&size=20`
- `POST /admin/users?actorId=1`
- `POST /admin/users/{id}/reset-password?actorId=1`
- `PUT /admin/users/{id}/status?actorId=1&enabled=true`

### 模型管理
- `GET /admin/models/providers`
- `GET /admin/models`
- `PUT /admin/models/{code}/rate-limit?actorId=1`

### 统计
- `GET /admin/stats/ops`
- `GET /admin/stats/dashboard`

### 监控
- `GET /admin/health` 跨服务 health
- `GET /admin/ping`

### 审计
- `GET /admin/audit/recent?limit=50`
- `GET /admin/audit/by-actor/{id}?limit=20`

---

## 8. Multimodal 模块 `:8088/api/v1`

- `POST /multimodal/upload` 上传图片
- `POST /multimodal/describe` 图片理解
  ```json
  {
    "imageBase64": "...",
    "mimeType": "image/png",
    "prompt": "描述这张图"
  }
  ```
- `GET /multimodal/info` 模型信息

---

## 9. Monitor 模块 `:8089/api/v1`

### 健康
- `GET /monitor/health` 深度健康
- `GET /monitor/health/database` DB
- `GET /monitor/health/jvm` JVM
- `GET /monitor/health/disk` 磁盘

### 指标
- `GET /monitor/metrics` 业务指标
- `GET /monitor/metrics/snapshot` 快照
- `GET /monitor/metrics/trend` 趋势
- `POST /monitor/metrics/inc` 自助计数

### 告警
- `GET /monitor/alerts` 最近
- `GET /monitor/alerts/firing` firing
- `GET /monitor/alerts/rules` 规则
- `GET /monitor/alerts/summary` 摘要

### Prometheus
- `GET /actuator/prometheus`

---

## 10. 错误响应示例

### 401 未登录
```json
{
  "code": 1002,
  "message": "未登录或登录已过期",
  "timestamp": 1781594520220
}
```

### 403 无权限
```json
{
  "code": 1003,
  "message": "无权限访问此资源"
}
```

### 429 限流
```json
{
  "code": 1500,
  "message": "Rate limit exceeded, please retry later",
  "headers": {
    "X-RateLimit-Remaining": "0",
    "X-RateLimit-Reset": "60"
  }
}
```

### 500 服务异常
```json
{
  "code": 1500,
  "message": "[function unavailable] ...",
  "traceId": "abc123def456"
}
```

---

## 11. 速率限制

| Scope | 默认容量 | 补充周期 |
|-------|----------|----------|
| IP | 100 | 60s |
| User | 60 | 60s |
| Global | 1000 | 60s |
| 登录端点 (IP) | 10 | 60s |
| 聊天端点 (IP) | 60 | 60s |
| RAG 上传 (IP) | 20 | 60s |

配置: `application.yml` 的 `minimax.ratelimit.*`

---

## 12. 完整端点数

| 模块 | 端点数 |
|------|--------|
| auth | 8 |
| chat | 8 |
| memory | 16 |
| model | 6 |
| rag | 11 |
| function | 10 |
| admin | 14 |
| multimodal | 3 |
| monitor | 15 + /actuator/* |
| **合计** | **92+** |
