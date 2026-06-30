# MiniMax 平台 API 参考文档

> 由 `openapi.yaml` 自动生成 · 请勿手动修改

**版本:** 5.9.0  

**描述:** MiniMax 大模型平台完整 REST API 规范（V5.9）。

## Base URL
- Gateway: `http://localhost:7080`
- Auth: `http://localhost:8081`
- Chat: `http://localhost:8082`
- Model: `http://localhost:8083`
- Memory: `http://localhost:8084`
- RAG: `http://localhost:8085`
- Function: `http://localhost:8086`
- Admin: `http://localhost:8087`
- Multimodal: `http://localhost:8088`
- Monitor: `http://localhost:8089`

## 认证
除公开端点外，所有请求需携带 `Authorization: Bearer <accessToken>`。

## 统一响应格式
```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "timestamp": 1234567890
}
```

## 错误码
- `0` 成功
- `1000` 业务异常
- `1001` 参数校验失败
- `1002` 未登录
- `1003` 无权限
- `1500` 服务降级 / 限流
  

**Base URL:** `http://localhost:7080`  


---

## 目录

- [Auth](#auth)
  - [POST /api/v1/auth/register](#post-api-v1-auth-register)
  - [POST /api/v1/auth/login](#post-api-v1-auth-login)
  - [POST /api/v1/auth/refresh](#post-api-v1-auth-refresh)
  - [POST /api/v1/auth/logout](#post-api-v1-auth-logout)
  - [GET /api/v1/auth/me](#get-api-v1-auth-me)
  - [GET /api/v1/auth/users](#get-api-v1-auth-users)
  - [POST /api/v1/auth/users](#post-api-v1-auth-users)
  - [PUT /api/v1/auth/users/{id}](#put-api-v1-auth-users-{id})
  - [GET /api/v1/auth/health](#get-api-v1-auth-health)
- [Chat](#chat)
  - [GET /api/v1/sessions](#get-api-v1-sessions)
  - [POST /api/v1/sessions](#post-api-v1-sessions)
  - [GET /api/v1/sessions/{id}](#get-api-v1-sessions-{id})
  - [DELETE /api/v1/sessions/{id}](#delete-api-v1-sessions-{id})
  - [GET /api/v1/sessions/{id}/messages](#get-api-v1-sessions-{id}-messages)
  - [POST /api/v1/sessions/{id}/messages](#post-api-v1-sessions-{id}-messages)
  - [POST /api/v1/sessions/{id}/messages/stream](#post-api-v1-sessions-{id}-messages-stream)
  - [POST /api/v1/sessions/stop-stream](#post-api-v1-sessions-stop-stream)
- [Model](#model)
  - [GET /api/v1/models](#get-api-v1-models)
  - [POST /api/v1/models](#post-api-v1-models)
  - [GET /api/v1/models/providers](#get-api-v1-models-providers)
  - [GET /api/v1/models/{code}](#get-api-v1-models-{code})
  - [POST /api/v1/models/chat/stream](#post-api-v1-models-chat-stream)
  - [POST /api/v1/models/chat/cancel](#post-api-v1-models-chat-cancel)
  - [POST /api/v1/models/imagegen](#post-api-v1-models-imagegen)
- [Memory](#memory)
  - [GET /api/v1/memory/short-term/{sid}](#get-api-v1-memory-short-term-{sid})
  - [POST /api/v1/memory/short-term/{sid}](#post-api-v1-memory-short-term-{sid})
  - [DELETE /api/v1/memory/short-term/{sid}](#delete-api-v1-memory-short-term-{sid})
  - [GET /api/v1/memory/short-term/{sid}/size](#get-api-v1-memory-short-term-{sid}-size)
  - [POST /api/v1/memory/cross-context](#post-api-v1-memory-cross-context)
  - [POST /api/v1/memory/summarize/{sid}](#post-api-v1-memory-summarize-{sid})
  - [GET /api/v1/memory/summary/{sid}](#get-api-v1-memory-summary-{sid})
  - [POST /api/v1/memory/long-term](#post-api-v1-memory-long-term)
  - [GET /api/v1/memory/long-term](#get-api-v1-memory-long-term)
  - [POST /api/v1/memory/long-term/recall](#post-api-v1-memory-long-term-recall)
  - [DELETE /api/v1/memory/long-term/{id}](#delete-api-v1-memory-long-term-{id})
  - [GET /api/v1/memory/pref/{key}](#get-api-v1-memory-pref-{key})
  - [PUT /api/v1/memory/pref/{key}](#put-api-v1-memory-pref-{key})
  - [DELETE /api/v1/memory/pref/{key}](#delete-api-v1-memory-pref-{key})
- [RAG](#rag)
  - [POST /api/v1/rag/kb](#post-api-v1-rag-kb)
  - [GET /api/v1/rag/kb](#get-api-v1-rag-kb)
  - [GET /api/v1/rag/kb/public](#get-api-v1-rag-kb-public)
  - [GET /api/v1/rag/kb/{id}](#get-api-v1-rag-kb-{id})
  - [DELETE /api/v1/rag/kb/{id}](#delete-api-v1-rag-kb-{id})
  - [GET /api/v1/rag/doc](#get-api-v1-rag-doc)
  - [POST /api/v1/rag/doc/upload](#post-api-v1-rag-doc-upload)
  - [GET /api/v1/rag/doc/{id}/chunks](#get-api-v1-rag-doc-{id}-chunks)
  - [DELETE /api/v1/rag/doc/{id}](#delete-api-v1-rag-doc-{id})
  - [POST /api/v1/rag/retrieve](#post-api-v1-rag-retrieve)
  - [POST /api/v1/rag/ask](#post-api-v1-rag-ask)
- [Function](#function)
  - [GET /api/v1/function/tools](#get-api-v1-function-tools)
  - [POST /api/v1/function/tools](#post-api-v1-function-tools)
  - [GET /api/v1/function/tools/{id}](#get-api-v1-function-tools-{id})
  - [PUT /api/v1/function/tools/{id}](#put-api-v1-function-tools-{id})
  - [DELETE /api/v1/function/tools/{id}](#delete-api-v1-function-tools-{id})
  - [POST /api/v1/function/invoke/{name}](#post-api-v1-function-invoke-{name})
  - [GET /api/v1/function/logs](#get-api-v1-function-logs)
  - [POST /api/v1/function/chat](#post-api-v1-function-chat)
- [Admin](#admin)
  - [GET /api/v1/admin/users](#get-api-v1-admin-users)
  - [POST /api/v1/admin/users](#post-api-v1-admin-users)
  - [POST /api/v1/admin/users/{id}/reset-password](#post-api-v1-admin-users-{id}-reset-password)
  - [PUT /api/v1/admin/users/{id}/status](#put-api-v1-admin-users-{id}-status)
  - [GET /api/v1/admin/stats/ops](#get-api-v1-admin-stats-ops)
  - [GET /api/v1/admin/stats/dashboard](#get-api-v1-admin-stats-dashboard)
  - [GET /api/v1/admin/health](#get-api-v1-admin-health)
  - [GET /api/v1/admin/ping](#get-api-v1-admin-ping)
  - [GET /api/v1/admin/audit/recent](#get-api-v1-admin-audit-recent)
  - [GET /api/v1/admin/audit/by-actor/{id}](#get-api-v1-admin-audit-by-actor-{id})
  - [GET /api/v1/admin/apikey/stats](#get-api-v1-admin-apikey-stats)
  - [GET /api/v1/admin/models/providers](#get-api-v1-admin-models-providers)
  - [PUT /api/v1/admin/models/{code}/rate-limit](#put-api-v1-admin-models-{code}-rate-limit)
- [Multimodal](#multimodal)
  - [POST /api/v1/multimodal/upload](#post-api-v1-multimodal-upload)
  - [POST /api/v1/multimodal/describe](#post-api-v1-multimodal-describe)
  - [GET /api/v1/multimodal/info](#get-api-v1-multimodal-info)
- [Monitor](#monitor)
  - [GET /api/v1/monitor/health](#get-api-v1-monitor-health)
  - [GET /api/v1/monitor/health/database](#get-api-v1-monitor-health-database)
  - [GET /api/v1/monitor/health/jvm](#get-api-v1-monitor-health-jvm)
  - [GET /api/v1/monitor/health/disk](#get-api-v1-monitor-health-disk)
  - [GET /api/v1/monitor/metrics](#get-api-v1-monitor-metrics)
  - [GET /api/v1/monitor/metrics/snapshot](#get-api-v1-monitor-metrics-snapshot)
  - [GET /api/v1/monitor/metrics/trend](#get-api-v1-monitor-metrics-trend)
  - [POST /api/v1/monitor/metrics/inc](#post-api-v1-monitor-metrics-inc)
  - [GET /api/v1/monitor/alerts](#get-api-v1-monitor-alerts)
  - [GET /api/v1/monitor/alerts/firing](#get-api-v1-monitor-alerts-firing)
  - [GET /api/v1/monitor/alerts/rules](#get-api-v1-monitor-alerts-rules)
  - [GET /api/v1/monitor/alerts/summary](#get-api-v1-monitor-alerts-summary)
  - [GET /actuator/health](#get-actuator-health)
  - [GET /actuator/prometheus](#get-actuator-prometheus)

---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/actuator/health`

**Spring Actuator 健康检查**


**响应**

- `200` Actuator 健康


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/actuator/prometheus`

**Prometheus 指标端点**


**响应**

- `200` Prometheus metrics


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/apikey/stats`

**API Key 配额统计**


**响应**

- `200` 统计概览


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/audit/by-actor/{id}`

**按操作者查审计日志**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `limit` | integer | 否 |  |


**响应**

- `200` 审计日志


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/audit/recent`

**最近审计日志**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `limit` | integer | 否 |  |


**响应**

- `200` 审计日志


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/health`

**跨服务健康检查**


**响应**

- `200` 各服务健康状态


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/models/providers`

**管理端列出模型**


**响应**

- `200` 模型列表


---

### <span style="background:#FF9800;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">PUT</span> `/api/v1/admin/models/{code}/rate-limit`

**修改模型限流**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `code` | string | 是 |  |

| `actorId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| rateLimit | integer |  |


**响应**

- `200` 更新成功


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/ping`

**快速 ping 检查**


**响应**

- `200` pong


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/stats/dashboard`

**Dashboard 数据**


**响应**

- `200` Dashboard 指标


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/stats/ops`

**运营统计概览**


**响应**

- `200` 统计数据


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/admin/users`

**用户管理（分页）**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `page` | integer | 否 |  |

| `size` | integer | 否 |  |

| `keyword` | string | 否 |  |


**响应**

- `200` 分页用户列表


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/admin/users`

**创建用户（管理员）**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `actorId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| username | string |  |
| password | string |  |
| nickname | string |  |
| email | string |  |


**响应**

- `200` 创建成功


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/admin/users/{id}/reset-password`

**重置用户密码**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `actorId` | integer | 是 |  |


**响应**

- `200` 重置成功


---

### <span style="background:#FF9800;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">PUT</span> `/api/v1/admin/users/{id}/status`

**启用/禁用用户**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `actorId` | integer | 是 |  |

| `enabled` | boolean | 是 |  |


**响应**

- `200` 更新成功


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/auth/health`

**Auth 服务健康检查（公开）**


**响应**

- `200` 健康 → `HealthStatus`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/auth/login`

**用户登录（获取双 Token）**


**请求体 (Request Body)**

**Content-Type:** `application/json`

`LoginRequest`


**响应**

- `200` 登录成功 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/auth/logout`

**登出（使 Token 失效）**


**响应**

- `200` 登出成功 → `Result`


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/auth/me`

**获取当前用户信息**


**响应**

- `200` 用户信息 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/auth/refresh`

**用 Refresh Token 续期**


**请求体 (Request Body)**

**Content-Type:** `application/json`

`RefreshRequest`


**响应**

- `200` 续期成功 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/auth/register`

**注册新用户**


**请求体 (Request Body)**

**Content-Type:** `application/json`

`RegisterRequest`


**响应**

- `200` 注册成功 → `Result`


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/auth/users`

**用户列表（管理员）**


**响应**

- `200` 分页用户列表 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/auth/users`

**创建用户（管理员）**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| username | string |  |
| password | string |  |
| nickname | string |  |
| email | string |  |


**响应**

- `200` 创建成功


---

### <span style="background:#FF9800;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">PUT</span> `/api/v1/auth/users/{id}`

**更新用户信息**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

object


**响应**

- `200` 更新成功


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/function/chat`

**Chat + 工具循环调用**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | integer |  |
| message | string |  |
| enableTools | boolean |  |
| toolNames | array |  |


**响应**

- `200` 带工具调用的回复


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/function/invoke/{name}`

**直接调用工具**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `name` | string | 是 |  |

| `userId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

object


**响应**

- `200` 调用结果


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/function/logs`

**调用历史**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `userId` | integer | 是 |  |


**响应**

- `200` 调用日志


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/function/tools`

**列出所有可用工具**


**响应**

- `200` 工具列表


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/function/tools`

**注册新工具**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `ownerId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

`FunctionTool`


**响应**

- `200` 注册成功


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/function/tools/{id}`

**删除工具**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `ownerId` | integer | 是 |  |


**响应**

- `200` 已删除


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/function/tools/{id}`

**工具详情**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**响应**

- `200` 工具详情


---

### <span style="background:#FF9800;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">PUT</span> `/api/v1/function/tools/{id}`

**更新工具**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `ownerId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

object


**响应**

- `200` 更新成功


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/memory/cross-context`

**跨会话上下文整合**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | integer |  |
| sessionId | integer |  |
| systemPrompt | string |  |
| maxContext | integer |  |
| recallTopK | integer |  |


**响应**

- `200` 整合后的上下文


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/memory/long-term`

**列出最近长期记忆**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `userId` | integer | 是 |  |

| `limit` | integer | 否 |  |


**响应**

- `200` 记忆列表


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/memory/long-term`

**存储长期记忆**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | integer |  |
| sessionId | integer |  |
| role | string |  |
| content | string |  |
| tags | string |  |


**响应**

- `200` 存储成功


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/memory/long-term/recall`

**召回长期记忆**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| userId | integer |  |
| query | string |  |
| topK | integer |  |


**响应**

- `200` 召回结果


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/memory/long-term/{id}`

**删除长期记忆**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `userId` | integer | 是 |  |


**响应**

- `200` 已删除


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/memory/pref/{key}`

**删除用户偏好**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `key` | string | 是 |  |

| `userId` | integer | 是 |  |


**响应**

- `200` 已删除


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/memory/pref/{key}`

**读取用户偏好**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `key` | string | 是 |  |

| `userId` | integer | 是 |  |


**响应**

- `200` 偏好值


---

### <span style="background:#FF9800;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">PUT</span> `/api/v1/memory/pref/{key}`

**设置用户偏好**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `key` | string | 是 |  |

| `userId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| value | string |  |


**响应**

- `200` 设置成功


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/memory/short-term/{sid}`

**清空短期记忆**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `sid` | string | 是 |  |


**响应**

- `200` 已清空


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/memory/short-term/{sid}`

**拉取短期记忆**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `sid` | string | 是 |  |


**响应**

- `200` 记忆内容


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/memory/short-term/{sid}`

**追加短期记忆**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `sid` | string | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| content | string |  |


**响应**

- `200` 追加成功


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/memory/short-term/{sid}/size`

**短期记忆容量统计**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `sid` | string | 是 |  |


**响应**

- `200` 容量统计


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/memory/summarize/{sid}`

**触发会话摘要**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `sid` | string | 是 |  |


**响应**

- `200` 摘要生成中


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/memory/summary/{sid}`

**读取会话摘要**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `sid` | string | 是 |  |


**响应**

- `200` 摘要内容


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/models`

**列出所有可用模型**


**响应**

- `200` 模型列表 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/models`

**OpenAI 兼容 Chat 接口**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| model | string |  |
| messages | array |  |
| temperature | number |  |
| maxTokens | integer |  |


**响应**

- `200` Chat 响应


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/models/chat/cancel`

**取消流式响应**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `streamId` | string | 是 |  |


**响应**

- `200` 已取消


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/models/chat/stream`

**流式 Chat（SSE）**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `streamId` | string | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| model | string |  |
| messages | array |  |
| temperature | number |  |


**响应**

- `200` SSE 流


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/models/imagegen`

**图片生成**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| model | string |  |
| prompt | string |  |
| n | integer |  |
| size | string |  |


**响应**

- `200` 图片生成结果


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/models/providers`

**列出所有模型 Provider**


**响应**

- `200` Provider 列表


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/models/{code}`

**获取模型详情**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `code` | string | 是 |  |


**响应**

- `200` 模型详情


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/alerts`

**最近告警**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `limit` | integer | 否 |  |


**响应**

- `200` 告警列表


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/alerts/firing`

**正在 firing 的告警**


**响应**

- `200` firing 告警


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/alerts/rules`

**告警规则列表**


**响应**

- `200` 规则列表


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/alerts/summary`

**告警摘要**


**响应**

- `200` 告警摘要


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/health`

**深度健康检查**


**响应**

- `200` 健康状态


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/health/database`

**数据库健康**


**响应**

- `200` DB 健康


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/health/disk`

**磁盘健康**


**响应**

- `200` 磁盘使用情况


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/health/jvm`

**JVM 健康**


**响应**

- `200` JVM 健康


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/metrics`

**业务指标**


**响应**

- `200` 指标快照


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/monitor/metrics/inc`

**自助计数**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| metric | string |  |
| delta | integer |  |


**响应**

- `200` 计数成功


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/metrics/snapshot`

**指标快照**


**响应**

- `200` 快照数据


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/monitor/metrics/trend`

**指标趋势**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `metric` | string | 否 |  |

| `from` | string | 否 |  |

| `to` | string | 否 |  |


**响应**

- `200` 趋势数据


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/multimodal/describe`

**图片理解（Vision）**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| imageBase64 | string |  |
| mimeType | string |  |
| prompt | string | 可选提示词 |


**响应**

- `200` 图片描述


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/multimodal/info`

**多模态模型信息**


**响应**

- `200` 模型信息


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/multimodal/upload`

**上传图片**


**请求体 (Request Body)**

**Content-Type:** `multipart/form-data`

| 参数 | 类型 | 说明 |
|---|---|---|
| file | string |  |


**响应**

- `200` 上传成功


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/rag/ask`

**RAG 问答**


**请求体 (Request Body)**

**Content-Type:** `application/json`

`RAGAskRequest`


**响应**

- `200` RAG 问答结果 → `Result`


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/rag/doc`

**列出知识库中的文档**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `kbId` | integer | 是 |  |


**响应**

- `200` 文档列表


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/rag/doc/upload`

**上传文档到知识库**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `ownerId` | integer | 是 |  |

| `kbId` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `multipart/form-data`

| 参数 | 类型 | 说明 |
|---|---|---|
| file | string |  |


**响应**

- `200` 上传成功


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/rag/doc/{id}`

**删除文档**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `ownerId` | integer | 是 |  |


**响应**

- `200` 已删除


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/rag/doc/{id}/chunks`

**获取文档切片**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**响应**

- `200` 切片列表


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/rag/kb`

**列出我的知识库**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `ownerId` | integer | 是 |  |


**响应**

- `200` 知识库列表


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/rag/kb`

**创建知识库**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| name | string |  |
| description | string |  |
| isPublic | boolean |  |


**响应**

- `200` 创建成功


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/rag/kb/public`

**列出公开知识库**


**响应**

- `200` 公开知识库列表


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/rag/kb/{id}`

**删除知识库**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `ownerId` | integer | 是 |  |


**响应**

- `200` 已删除


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/rag/kb/{id}`

**知识库详情**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `ownerId` | integer | 是 |  |


**响应**

- `200` 知识库详情


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/rag/retrieve`

**纯检索（无生成）**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| kbId | integer |  |
| query | string |  |
| topK | integer |  |


**响应**

- `200` 检索结果


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/sessions`

**列出当前用户的会话列表**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `page` | integer | 否 |  |

| `size` | integer | 否 |  |


**响应**

- `200` 会话列表 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/sessions`

**创建新会话**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| title | string |  |
| modelCode | string |  |


**响应**

- `200` 创建成功 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/sessions/stop-stream`

**停止流式响应**


**请求体 (Request Body)**

**Content-Type:** `application/json`

| 参数 | 类型 | 说明 |
|---|---|---|
| streamId | string |  |


**响应**

- `200` 已停止


---

### <span style="background:#F44336;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">DELETE</span> `/api/v1/sessions/{id}`

**删除会话**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**响应**

- `200` 删除成功


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/sessions/{id}`

**获取会话详情**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**响应**

- `200` 会话详情 → `Result`


---

### <span style="background:#4CAF50;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">GET</span> `/api/v1/sessions/{id}/messages`

**获取会话的所有消息**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**响应**

- `200` 消息列表 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/sessions/{id}/messages`

**非流式发消息**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |


**请求体 (Request Body)**

**Content-Type:** `application/json`

`ChatRequest`


**响应**

- `200` 消息响应 → `Result`


---

### <span style="background:#2196F3;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">POST</span> `/api/v1/sessions/{id}/messages/stream`

**流式发消息（SSE）**


**Query / Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|

| `id` | integer | 是 |  |

| `streamId` | string | 是 | 流 ID（客户端生成，用于停止） |


**请求体 (Request Body)**

**Content-Type:** `application/json`

`StreamChatRequest`


**响应**

- `200` SSE 流 → string


---
