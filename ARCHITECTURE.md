# MiniMax Platform — 架构设计文档

> 详细的系统设计、模块关系、数据流、技术选型说明

---

## 1. 设计原则

| 原则 | 落地方式 |
|------|----------|
| **微服务** | 11 个独立模块, 各自 fat-jar, 独立部署 |
| **无状态** | 业务数据全在 DB/Redis, 服务可任意水平扩 |
| **可降级** | Redis/Caffeine/LLM/Embedding 任意一层失败都有 fallback |
| **零外部依赖** | H2 + Mock 模式可本地一键跑通, 不需 ES/Redis |
| **可观测** | Micrometer + Prometheus + traceId + 告警 |
| **安全** | JWT + BCrypt + SSRF 防护 + 字符白名单 + 审计 |

---

## 2. 模块依赖

```
┌─────────────────────────────────────────────────────────────┐
│                       Common (无依赖)                        │
│  Result/异常/JWT/限流/缓存/异步/请求日志                      │
└────────────┬────────────────────────────────────────────────┘
             │ 所有业务模块依赖
   ┌─────────┼─────────┬─────────┬─────────┬─────────┐
   ▼         ▼         ▼         ▼         ▼         ▼
 auth     chat      memory    model      rag    function
   │         │         │         │         │         │
   └────┬────┴────┬────┴────┬────┴────┬────┴────┬────┘
        │         │         │         │         │
        ▼         ▼         ▼         ▼         ▼
        └────────┴────────┴────────┴────────┴────┐
                          │                     │
                    admin  multimodal  monitor  │
                          │  (调用所有)         │
                          └─────────────────────┘
```

**核心原则**:
- common 是不依赖任何业务模块的"基础库"
- 业务模块之间**不互相依赖** (避免 fat-jar 嵌套)
- admin / multimodal / monitor 是"消费方", 调其他服务

---

## 3. 数据模型

### 3.1 ER 概览

```
auth (用户体系)
  ↓
  user ─┬─ user_role ─ role
        ├─ refresh_token
        └─ login_log

chat (会话消息)
  ↓
  session → message

memory (记忆)
  ↓
  short_term_cache  (Redis/Caffeine)
  long_term  (向量 + 原文 + tags + importance)
  user_pref  (KV)

model (模型)
  ↓
  provider → config → quota

rag (知识库)
  ↓
  knowledge_base → document → document_chunk (向量)

function (工具)
  ↓
  function_tool  (注册表)
  function_call_log  (审计)

admin / monitor / multimodal
  ↓
  admin_audit_log / metric_snapshot / alert_rule / alert_event / image_storage
```

### 3.2 关键表

| 表 | 用途 | 关键字段 |
|----|------|----------|
| sys_user | 用户 | username / password (BCrypt) / status |
| sys_role | 角色 | code (ADMIN/USER) |
| chat_session | 会话 | user_id / title / model_code |
| chat_message | 消息 | session_id / role / content |
| memory_long_term | 长期记忆 | user_id / embedding (LONGBLOB) / dim |
| memory_user_pref | 偏好 | user_id / pref_key / pref_value |
| model_provider | 模型供应商 | code / type (openai/minimax/...) |
| model_config | 模型配置 | code / model_code / api_key / base_url |
| model_quota | 配额 | user_id / model_code / used |
| knowledge_base | 知识库 | owner_id / visibility |
| document | 文档 | kb_id / source_type / content / checksum |
| document_chunk | 切片 | doc_id / embedding (LONGBLOB) / chunk_index |
| function_tool | 工具 | name / parameters (JSON Schema) / endpoint |
| function_call_log | 工具调用 | tool_name / args / result / status |
| admin_audit_log | 审计 | actor / action / resource_type / detail |
| metric_snapshot | 指标 | service / metric_name / value |
| alert_rule | 告警规则 | metric / operator / threshold / severity |
| alert_event | 告警事件 | rule_id / value / status (firing/resolved) |

---

## 4. 关键流程

### 4.1 用户登录流程

```
前端 POST /api/v1/auth/login {username, password}
  ↓
AuthController.login()
  ↓
AuthService:
  1) 查 SysUser by username
  2) BCrypt 验密码
  3) 生成 access_token (30min) + refresh_token (7d)
  4) 写 login_log
  ↓
返回 {accessToken, refreshToken, userInfo}

前端存到 localStorage + Pinia
后续请求加 Authorization: Bearer xxx
```

### 4.2 聊天流式流程

```
前端 POST /api/v1/sessions/{id}/messages {role, content}
  ↓
ChatController.sendMessageStream()
  ↓
MessageService:
  1) 短期记忆 append (Redis/Caffeine)
  2) 可能触发: 摘要 (35→10) + 长期记忆 recall
  3) 调 ModelService.chat() 走流式
  ↓
OpenAiCompatibleAdapter:
  - HttpClient + BodyHandlers.ofLines
  - 每行: parse SSE data: {...} → chunk
  - 通过 StreamingResponseBody 写回前端
  ↓
前端 fetch().body.getReader():
  - ReadableStream 解码
  - 累加到 AI 消息 content
  - 打字机效果显示
```

### 4.3 RAG 问答流程

```
用户问题 "我们的产品支持哪些支付方式?"
  ↓
RagController.ask(kbId, question)
  ↓
RagService.ask():
  1) Retriever.retrieve(kbId, query, topK=5)
     - query → embedding (Mock/OpenAI)
     - 拉 KB 内所有 chunks (含向量)
     - cosine 相似度
     - 排序取 topK
  2) 拼 system prompt: "你是基于知识库回答...引用 [来源 N]"
  3) 调 model 服务 /api/v1/models/chat (OpenAI 兼容)
  4) 返回 {answer, sources[]}
  ↓
前端展示: 答案 + 引用卡片 (绿色, 可点击跳转)
```

### 4.4 Function Calling 流程

```
用户 "上海现在几点? 帮我算 (123+456)*789"
  ↓
FunctionCallService.chatWithTools()
  ↓
[Round 1] LLM 调 model 服务, 传 tools 列表
  → LLM 返回 tool_calls: [
      {name: "get_current_time", args: {timezone: "Asia/Shanghai"}},
      {name: "calculator", args: {expression: "(123+456)*789"}}
    ]
  ↓
执行工具:
  - TimeTool.execute({timezone: "Asia/Shanghai"})
    → 返 '{"datetime": "2026-06-16T23:00:00+08:00"}'
  - CalculatorTool.execute({expression: "(123+456)*789"})
    → 自实现求值器 → 返 '{"result": 456897}'
  - 写 function_call_log (审计)
  ↓
[Round 2] 把 tool 结果以 role=tool 回传 LLM
  → LLM 整合 → "上海 23:00, (123+456)*789 = 456897"
  ↓
返回 {answer, toolCalls[]}
```

### 4.5 监控告警流程

```
@Scheduled(60s) SnapshotService.snapshot()
  ↓
读 JVM/Disk/CPU + 业务指标 → 写 metric_snapshot

@Scheduled(30s) AlertEngine.evaluate()
  ↓
对每条 enabled 规则:
  1) 读最新指标值
  2) compare(value, operator, threshold)
  3) 触发 → 写 alert_event (考虑 cooldown)
  4) 恢复 → firing → resolved
  ↓
Prometheus 服务每 15s 抓 /actuator/prometheus
  → Grafana 仪表盘展示
  → AlertManager 触发外部告警 (邮件/Slack)
```

---

## 5. 技术选型

### 后端

| 组件 | 选型 | 理由 |
|------|------|------|
| 框架 | Spring Boot 3.2 | Jakarta EE 9 + Java 17 |
| 安全 | Spring Security 6 | 工业标准 |
| JWT | jjwt 0.12 | 简单稳定 |
| 持久层 | MyBatis-Plus 3.5 | 强大但易用, Lambda API |
| 限流 | Bucket4j 8.10 | 工业级令牌桶 |
| 缓存 | Caffeine 3.1 | 高性能 Java 缓存 |
| 向量 | MySQL BLOB + Java 余弦 | 1000 条内不引外部 |
| 文档解析 | Apache POI + PDFBox | 标准库 |
| 监控 | Micrometer + Prometheus | Spring Boot 生态 |
| 文档生成 | Knife4j | OpenAPI 3 UI |

### 前端

| 组件 | 选型 | 理由 |
|------|------|------|
| 框架 | Vue 3.4 | Composition API |
| UI | Element Plus 2.6 | 桌面端最佳 |
| 状态 | Pinia 2.1 | Vue 3 官方推荐 |
| HTTP | Axios 1.6 | 拦截器 + 取消 |
| 图表 | ECharts 5.5 | 强大 |
| Markdown | markdown-it | 灵活 |
| 代码高亮 | highlight.js | 100+ 语言 |
| 构建 | Vite 5 | 极快 |
| 路由 | vue-router 4 | 标准 |

### 基础设施

| 组件 | 选型 |
|------|------|
| 容器 | Docker 20.10+ |
| 编排 | Docker Compose / K8s 1.24+ |
| 反向代理 | Nginx Ingress |
| 数据库 | MySQL 8.0 (生产) / H2 (测试) |
| 缓存 | Redis 7 (生产) / Caffeine (单机) |
| 监控 | Prometheus + Grafana |
| 日志 | ELK / Loki |
| CI/CD | GitHub Actions |

---

## 6. 安全架构

### 6.1 认证

```
┌────────────────────────────────────────────────────────┐
│ 1. 用户登录 → JWT 双 token                              │
│    - access_token (30min, 短命)                          │
│    - refresh_token (7d, 长命)                            │
│ 2. 前端 access_token 过期自动用 refresh 续期            │
│ 3. refresh 也过期 → 重新登录                            │
└────────────────────────────────────────────────────────┘
```

### 6.2 授权

```
┌────────────────────────────────────────────────────────┐
│ RBAC 简化版:                                            │
│   - ADMIN: 所有权限                                      │
│   - USER:  自身资源                                      │
│                                                        │
│ 资源级:                                                │
│   - 知识库: 私有仅 owner / 公开所有人                   │
│   - 工具: 内置只读 / 自定义仅 owner                      │
│   - 会话: 仅创建者                                       │
└────────────────────────────────────────────────────────┘
```

### 6.3 防护

| 风险 | 防护 |
|------|------|
| SQL 注入 | MyBatis-Plus 参数化 |
| XSS | Vue 自动转义 |
| CSRF | JWT 无 Cookie |
| SSRF | HttpGetTool 阻止内网 |
| 注入 | Calculator 字符白名单 + 自实现求值 |
| 暴力破解 | 限流 (IP 100/60s, login 10/60s) |
| 密码泄漏 | BCrypt (cost 10) |
| 重放攻击 | JWT exp + 短寿命 |

---

## 7. 性能指标 (参考)

| 场景 | QPS | P99 | 资源 |
|------|-----|-----|------|
| Auth 健康检查 | 5,000+ | <50ms | 256m |
| Chat 流式首字 | 200+ | <800ms | 768m |
| RAG 检索 (1000 文档) | 500+ | <100ms | 768m |
| Function 调用 | 1,000+ | <200ms | 512m |

---

## 8. 演进路线

| 阶段 | 方向 |
|------|------|
| V1 (当前) | 11 微服务 + 基础 AI 能力 |
| V2 | 多租户 + 配额 + 计费 |
| V3 | Agent 自主规划 + 多步推理 |
| V4 | 私有模型微调 + LoRA |
| V5 | 多语言 + i18n |
| V6 | 移动端 SDK + 小程序 |

---

## 9. 已知限制

- **向量库自实现**: 1000+ 文档建议改用专业向量库 (Milvus/Qdrant)
- **LLM 全部用 OpenAI 协议**: 需模型支持 function calling
- **Mock embedding**: 64 维, 召回精度有限, 生产建议 1536 维
- **无分布式追踪**: 仅 traceId, 建议接 OpenTelemetry / Jaeger
- **HikariCP 未分库**: 大规模建议读写分离 + 分库

---

## 10. 联系

- GitHub: https://github.com/liugl951127/miniLiugl
- 邮箱: liugl951127@gmail.com
- License: MIT
