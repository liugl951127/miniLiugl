# MiniMax AI Platform — API 文档

> **版本**: 5.9.0 | **Base**: http://localhost:7080
> **认证**: `Authorization: Bearer <accessToken>`（公开端点除外）

## 统一响应格式

```json
{
  "code": 0,
  "message": "OK",
  "data": { ... },
  "timestamp": 1234567890
}
```

## 错误码

| code | 说明 |
|------|------|
| 0 | 成功 |
| 1000 | 业务异常 |
| 1001 | 参数校验失败 |
| 1002 | 未登录 |
| 1003 | 无权限 |
| 1500 | 服务降级/限流 |

---

## /api

### GET /api/v1/admin/apikey/stats

> 标签: Admin

API Key 配额统计

### GET /api/v1/admin/audit/by-actor/{id}

> 标签: Admin

按操作者查审计日志

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `limit` | query | integer | ❌ | - |

### GET /api/v1/admin/audit/recent

> 标签: Admin

最近审计日志

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `limit` | query | integer | ❌ | - |

### GET /api/v1/admin/health

> 标签: Admin

跨服务健康检查

### GET /api/v1/admin/models

> 标签: Admin

管理端列出模型

### GET /api/v1/admin/models/providers

> 标签: Admin

管理端列出 Provider

### PUT /api/v1/admin/models/{code}/rate-limit

> 标签: Admin

修改模型限流

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `code` | path | string | ✅ | - |
| `actorId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `rateLimit` | integer | - |

### GET /api/v1/admin/ping

> 标签: Admin

快速 ping 检查

### GET /api/v1/admin/stats/dashboard

> 标签: Admin

Dashboard 数据

### GET /api/v1/admin/stats/ops

> 标签: Admin

运营统计概览

### GET /api/v1/admin/users

> 标签: Admin

用户管理（分页）

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `page` | query | integer | ❌ | - |
| `size` | query | integer | ❌ | - |
| `keyword` | query | string | ❌ | - |

### POST /api/v1/admin/users

> 标签: Admin

创建用户（管理员）

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `actorId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | string | - |
| `password` | string | - |
| `nickname` | string | - |
| `email` | string | - |

### POST /api/v1/admin/users/{id}/reset-password

> 标签: Admin

重置用户密码

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `actorId` | query | integer(int64) | ✅ | - |

### PUT /api/v1/admin/users/{id}/status

> 标签: Admin

启用/禁用用户

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `actorId` | query | integer(int64) | ✅ | - |
| `enabled` | query | boolean | ✅ | - |

### GET /api/v1/auth/health

> 标签: Auth

Auth 服务健康检查（公开）

**响应** (200 健康):

### POST /api/v1/auth/login

> 标签: Auth

用户登录（获取双 Token）

**请求体**:

Content-Type: `application/json`

**响应** (200 登录成功):

```json
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
      "roles": [
        "ADMIN"
      ]
    }
  }
}
```

### POST /api/v1/auth/logout

> 标签: Auth

登出（使 Token 失效）

**响应** (200 登出成功):

### GET /api/v1/auth/me

> 标签: Auth

获取当前用户信息

**响应** (200 用户信息):

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "roles": [
      "ADMIN"
    ]
  }
}
```

### POST /api/v1/auth/refresh

> 标签: Auth

用 Refresh Token 续期

**请求体**:

Content-Type: `application/json`

**响应** (200 续期成功):

### POST /api/v1/auth/register

> 标签: Auth

注册新用户

**请求体**:

Content-Type: `application/json`

```json
{
  "username": "alice",
  "password": "alice@123",
  "nickname": "Alice",
  "email": "alice@example.com"
}
```

**响应** (200 注册成功):

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 2,
    "username": "alice",
    "nickname": "Alice"
  }
}
```

### GET /api/v1/auth/users

> 标签: Auth

用户列表（管理员）

**响应** (200 分页用户列表):

### POST /api/v1/auth/users

> 标签: Auth

创建用户（管理员）

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | string | - |
| `password` | string | - |
| `nickname` | string | - |
| `email` | string | - |

### PUT /api/v1/auth/users/{id}

> 标签: Auth

更新用户信息

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

### POST /api/v1/function/chat

> 标签: Function

Chat + 工具循环调用

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | integer | - |
| `message` | string | - |
| `enableTools` | boolean | - |
| `toolNames` | array | - |

### POST /api/v1/function/invoke/{name}

> 标签: Function

直接调用工具

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `name` | path | string | ✅ | - |
| `userId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

### GET /api/v1/function/logs

> 标签: Function

调用历史

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `userId` | query | integer(int64) | ✅ | - |

### GET /api/v1/function/tools

> 标签: Function

列出所有可用工具

### POST /api/v1/function/tools

> 标签: Function

注册新工具

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `ownerId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

### GET /api/v1/function/tools/{id}

> 标签: Function

工具详情

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

### PUT /api/v1/function/tools/{id}

> 标签: Function

更新工具

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `ownerId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

### DELETE /api/v1/function/tools/{id}

> 标签: Function

删除工具

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `ownerId` | query | integer(int64) | ✅ | - |

### POST /api/v1/memory/cross-context

> 标签: Memory

跨会话上下文整合

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | integer | - |
| `sessionId` | integer | - |
| `systemPrompt` | string | - |
| `maxContext` | integer | - |
| `recallTopK` | integer | - |

### POST /api/v1/memory/long-term

> 标签: Memory

存储长期记忆

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | integer | - |
| `sessionId` | integer | - |
| `role` | string | - |
| `content` | string | - |
| `tags` | string | - |

### GET /api/v1/memory/long-term

> 标签: Memory

列出最近长期记忆

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `userId` | query | integer(int64) | ✅ | - |
| `limit` | query | integer | ❌ | - |

### POST /api/v1/memory/long-term/recall

> 标签: Memory

召回长期记忆

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | integer | - |
| `query` | string | - |
| `topK` | integer | - |

### DELETE /api/v1/memory/long-term/{id}

> 标签: Memory

删除长期记忆

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `userId` | query | integer(int64) | ✅ | - |

### GET /api/v1/memory/pref/{key}

> 标签: Memory

读取用户偏好

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `key` | path | string | ✅ | - |
| `userId` | query | integer(int64) | ✅ | - |

### PUT /api/v1/memory/pref/{key}

> 标签: Memory

设置用户偏好

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `key` | path | string | ✅ | - |
| `userId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `value` | string | - |

### DELETE /api/v1/memory/pref/{key}

> 标签: Memory

删除用户偏好

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `key` | path | string | ✅ | - |
| `userId` | query | integer(int64) | ✅ | - |

### GET /api/v1/memory/short-term/{sid}

> 标签: Memory

拉取短期记忆

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `sid` | path | string | ✅ | - |

### POST /api/v1/memory/short-term/{sid}

> 标签: Memory

追加短期记忆

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `sid` | path | string | ✅ | - |

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `content` | string | - |

### DELETE /api/v1/memory/short-term/{sid}

> 标签: Memory

清空短期记忆

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `sid` | path | string | ✅ | - |

### GET /api/v1/memory/short-term/{sid}/size

> 标签: Memory

短期记忆容量统计

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `sid` | path | string | ✅ | - |

### POST /api/v1/memory/summarize/{sid}

> 标签: Memory

触发会话摘要

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `sid` | path | string | ✅ | - |

### GET /api/v1/memory/summary/{sid}

> 标签: Memory

读取会话摘要

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `sid` | path | string | ✅ | - |

### GET /api/v1/models

> 标签: Model

列出所有可用模型

**响应** (200 模型列表):

### POST /api/v1/models

> 标签: Model

OpenAI 兼容 Chat 接口

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `model` | string | - |
| `messages` | array | - |
| `temperature` | number | - |
| `maxTokens` | integer | - |

### POST /api/v1/models/chat/cancel

> 标签: Model

取消流式响应

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `streamId` | query | string | ✅ | - |

### POST /api/v1/models/chat/stream

> 标签: Model

流式 Chat（SSE）

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `streamId` | query | string | ✅ | - |

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `model` | string | - |
| `messages` | array | - |
| `temperature` | number | - |

### POST /api/v1/models/imagegen

> 标签: Model

图片生成

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `model` | string | - |
| `prompt` | string | - |
| `n` | integer | - |
| `size` | string | - |

### GET /api/v1/models/providers

> 标签: Model

列出所有模型 Provider

### GET /api/v1/models/{code}

> 标签: Model

获取模型详情

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `code` | path | string | ✅ | - |

### GET /api/v1/monitor/alerts

> 标签: Monitor

最近告警

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `limit` | query | integer | ❌ | - |

### GET /api/v1/monitor/alerts/firing

> 标签: Monitor

正在 firing 的告警

### GET /api/v1/monitor/alerts/rules

> 标签: Monitor

告警规则列表

### GET /api/v1/monitor/alerts/summary

> 标签: Monitor

告警摘要

### GET /api/v1/monitor/health

> 标签: Monitor

深度健康检查

### GET /api/v1/monitor/health/database

> 标签: Monitor

数据库健康

### GET /api/v1/monitor/health/disk

> 标签: Monitor

磁盘健康

### GET /api/v1/monitor/health/jvm

> 标签: Monitor

JVM 健康

### GET /api/v1/monitor/metrics

> 标签: Monitor

业务指标

### POST /api/v1/monitor/metrics/inc

> 标签: Monitor

自助计数

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `metric` | string | - |
| `delta` | integer | - |

### GET /api/v1/monitor/metrics/snapshot

> 标签: Monitor

指标快照

### GET /api/v1/monitor/metrics/trend

> 标签: Monitor

指标趋势

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `metric` | query | string | ❌ | - |
| `from` | query | string(date-time) | ❌ | - |
| `to` | query | string(date-time) | ❌ | - |

### POST /api/v1/multimodal/describe

> 标签: Multimodal

图片理解（Vision）

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `imageBase64` | string | - |
| `mimeType` | string | - |
| `prompt` | string | 可选提示词 |

### GET /api/v1/multimodal/info

> 标签: Multimodal

多模态模型信息

### POST /api/v1/multimodal/upload

> 标签: Multimodal

上传图片

**请求体**:

Content-Type: `multipart/form-data`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `file` | string | - |

### POST /api/v1/rag/ask

> 标签: RAG

RAG 问答

**请求体**:

Content-Type: `application/json`

**响应** (200 RAG 问答结果):

```json
{
  "code": 0,
  "data": {
    "answer": "支持微信、支付宝...",
    "sources": [
      {
        "chunkId": 10,
        "docTitle": "产品手册",
        "score": 0.92,
        "snippet": "支付方式包括..."
      }
    ]
  }
}
```

### GET /api/v1/rag/doc

> 标签: RAG

列出知识库中的文档

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `kbId` | query | integer(int64) | ✅ | - |

### POST /api/v1/rag/doc/upload

> 标签: RAG

上传文档到知识库

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `ownerId` | query | integer(int64) | ✅ | - |
| `kbId` | query | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `multipart/form-data`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `file` | string | - |

### DELETE /api/v1/rag/doc/{id}

> 标签: RAG

删除文档

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `ownerId` | query | integer(int64) | ✅ | - |

### GET /api/v1/rag/doc/{id}/chunks

> 标签: RAG

获取文档切片

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

### POST /api/v1/rag/kb

> 标签: RAG

创建知识库

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | - |
| `description` | string | - |
| `isPublic` | boolean | - |

### GET /api/v1/rag/kb

> 标签: RAG

列出我的知识库

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `ownerId` | query | integer(int64) | ✅ | - |

### GET /api/v1/rag/kb/public

> 标签: RAG

列出公开知识库

### GET /api/v1/rag/kb/{id}

> 标签: RAG

知识库详情

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `ownerId` | query | integer(int64) | ✅ | - |

### DELETE /api/v1/rag/kb/{id}

> 标签: RAG

删除知识库

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `ownerId` | query | integer(int64) | ✅ | - |

### POST /api/v1/rag/retrieve

> 标签: RAG

纯检索（无生成）

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `kbId` | integer | - |
| `query` | string | - |
| `topK` | integer | - |

### GET /api/v1/sessions

> 标签: Chat

列出当前用户的会话列表

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `page` | query | integer | ❌ | - |
| `size` | query | integer | ❌ | - |

**响应** (200 会话列表):

### POST /api/v1/sessions

> 标签: Chat

创建新会话

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | string | - |
| `modelCode` | string | - |

**响应** (200 创建成功):

### POST /api/v1/sessions/stop-stream

> 标签: Chat

停止流式响应

**请求体**:

Content-Type: `application/json`

**字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `streamId` | string | - |

### GET /api/v1/sessions/{id}

> 标签: Chat

获取会话详情

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

**响应** (200 会话详情):

### DELETE /api/v1/sessions/{id}

> 标签: Chat

删除会话

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

### GET /api/v1/sessions/{id}/messages

> 标签: Chat

获取会话的所有消息

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

**响应** (200 消息列表):

### POST /api/v1/sessions/{id}/messages

> 标签: Chat

非流式发消息

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |

**请求体**:

Content-Type: `application/json`

**响应** (200 消息响应):

### POST /api/v1/sessions/{id}/messages/stream

> 标签: Chat

流式发消息（SSE）

**Query/Path 参数**:

| 名称 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | path | integer(int64) | ✅ | - |
| `streamId` | query | string | ✅ | 流 ID（客户端生成，用于停止） |

**请求体**:

Content-Type: `application/json`

## /actuator

### GET /actuator/health

> 标签: Monitor

Spring Actuator 健康检查

### GET /actuator/prometheus

> 标签: Monitor

Prometheus 指标端点

---

## 认证方式

使用 **JWT Bearer Token**:

```bash
curl -H "Authorization: Bearer <accessToken>" http://localhost:7080/api/v1/...
```

## 服务 Base URL

| 服务 | Port | Base |
|------|------|------|
| API Gateway | 7080 | /api/v1 |
| Auth | 8081 | / |
| Chat | 8082 | / |
| Model | 8083 | / |
| Memory | 8084 | / |
| RAG | 8085 | / |
| Function | 8086 | / |
| Admin | 8087 | / |
| Monitor | 8089 | / |
| Multimodal | 8088 | / |
| Agent | 8090 | / |
| Prompt | 8091 | / |

---

*由 openapi.yaml 自动生成 | 2026-07-02*
