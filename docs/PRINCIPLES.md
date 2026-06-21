# 🏗️ MiniMax Platform — 实现原理 + 架构图

> 14 天构建 + V2 (4 大新功能) + V3 (3 大新功能) 的完整技术沉淀

---

## 🎯 1. 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          🎨 Frontend (Vue 3 + Element Plus + Vant)            │
│   ┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐          │
│   │  对话    │  知识库  │  记忆    │  Agent   │  知识图谱 │  管理    │          │
│   │  /chat   │  /kg     │  /memory │  /agent  │  /kg     │  /admin  │          │
│   └──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘          │
│   ┌──────────┬──────────┬──────────┬──────────┐                                 │
│   │ 协作     │  插件    │  超级管理 │ 移动端  │                                 │
│   │ /collab  │ /plugins │ /super   │  /m/*   │                                 │
│   └──────────┴──────────┴──────────┴──────────┘                                 │
│         │ axios + JWT (Bearer)  │ SSE (ReadableStream)  │ WebSocket            │
└─────────┼────────────────────────┼─────────────────────┼─────────────────────┘
          │                        │                     │
          ▼                        ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     🔧 Nginx Reverse Proxy (生产环境)                         │
│   :5173 → frontend (static)                                                   │
│   :80  → /api/v1/* → 各微服务                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ⚙️ Backend (Spring Boot 3 + 12 microservices)          │
│                                                                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐  │
│  │  auth   │  │  chat   │  │  model  │  │ memory  │  │   rag   │  │function│  │
│  │  8081   │  │  8082   │  │  8083   │  │  8084   │  │  8085   │  │  8086  │  │
│  │ JWT双token│  │会话+消息│  │多Provider│  │4维记忆  │  │3文档解析│  │4工具  │  │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └───┬────┘  │
│       │            │            │            │            │            │        │
│  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐  │
│  │  admin  │  │  mult.  │  │ monitor │  │  agent  │  │ gateway │  │ common │  │
│  │  8087   │  │  8088   │  │  8089   │  │  8090   │  │  8080   │  │  shared │  │
│  │跨服务HTTP│  │Vision   │  │Prometheus│  │ReAct/KG │  │ (未来)  │  │  JWT   │  │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘  │限流/缓存│  │
│                                                                  └────┬───┘  │
└──────────────────────────────────────────────────────────────────────┼──────┘
                                                                       │
          ┌────────────────────────────────────────────────────────────┼────┐
          │                  💾 Data Layer                                │    │
          │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐│
          │  │ MySQL 8 │  │  Redis  │  │ Vector  │  │Prometheus│  │  File    ││
          │  │ 31 表   │  │ 短记忆  │  │ (内存)  │  │  metrics │  │ RAG 文档 ││
          │  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └──────────┘│
          └────────────────────────────────────────────────────────────────────┘
```

---

## 🔐 2. 核心设计原则

### 2.1 微服务拆分原则 (Single Responsibility)

```
common   →  共享: Result / JwtFilter / RateLimiter / Cache / 异常
auth     →  只管身份 (谁登录, 谁能做什么)
chat     →  只管会话和消息 (会话/历史)
model    →  只管模型调用 (6 个 provider)
memory   →  只管记忆 (短/长/偏好/摘要)
rag      →  只管知识库 (上传/检索/引用)
function →  只管工具 (4 个内置 + LLM 工具循环)
admin    →  跨服务聚合 (代理调用其他服务)
multimodal → 只管视觉 (图片理解)
monitor  →  只管指标 (Prometheus + 告警)
agent    →  自主任务 (ReAct + KG + 协作 + 插件)
```

**好处**:
- 独立部署, 故障隔离
- 团队按服务分配
- 单服务可水平扩展
- 升级不影响全局

### 2.2 通信原则

```
同步 (HTTP)  →  内部服务间调用 (admin 聚合其他服务)
异步 (SSE)   →  流式聊天 (HttpClient.BodyHandlers.ofLines)
双向 (WS)    →  实时协作 (CollabHandler)
```

**绝不在前端直连数据库**:
- 前端 → gateway/auth → 各微服务 → MySQL
- 一律 HTTP + JWT

### 2.3 数据隔离原则

```
sys_user.tenant_id  →  多租户隔离 (V3.1)
adminLiugl.tenant_id = 0  →  跨租户超级管理员
查询过滤: WHERE tenant_id = ?
```

**前端**:
- localStorage 存 token + user
- 不存业务数据

### 2.4 安全原则

```
认证: JWT 双 token (access 30min + refresh 7d)
授权: RBAC (USER / ADMIN / SUPER_ADMIN)
跨租户: 超级管理员可见, 其他按 tenant_id 隔离
密码: BCrypt 编码 (不存明文)
SQL:  MyBatis-Plus 防注入
审计:  admin_audit_log 表记录所有操作
限流:  Bucket4j (IP+User+Global 三层)
```

### 2.5 性能原则

```
缓存:  Caffeine (本地) + Redis (分布式) 双层
限流:  Bucket4j 8.10.1 (token bucket 算法)
异步:  @Async + CompletableFuture (V1.3 优化)
连接池: HikariCP max 30 / min 10
JVM:  -Xms512m -Xmx2g -XX:+UseG1GC
```

---

## 🧩 3. 关键模块实现原理

### 3.1 JWT 双 Token 鉴权

```java
// 1. 登录发 2 个 token
String access = jwt.issueAccessToken(userId, username, roles);
String refresh = jwt.issueRefreshToken();
refreshDB.store(refreshHash, userId, expiresAt=+7d);

// 2. access 过期用 refresh 续期
Claims c = parse(refresh);
if (refreshDB.exists(hash) && !expired) {
    String newAccess = issueAccessToken(...);
    return new LoginResponse;
}

// 3. 每次请求 JwtAuthenticationFilter
String h = req.getHeader("Authorization");
if (h.startsWith("Bearer ")) {
    Claims claims = Jwts.parser()
        .verifyWith(key())
        .build()
        .parseSignedClaims(token);
    SecurityContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(userId, username),
            null,
            roles → ROLE_*
        )
    );
}
```

**关键设计**:
- access 30 min 短命, 减少被盗风险
- refresh 7 天长命, 存 DB 可吊销
- 解析失败不清 SecurityContext (让 401 走统一处理)
- roles 写入 token, 后续不需要查 DB

### 3.2 流式聊天 (SSE)

```java
// 后端 chat 8082
@GetMapping(value = "/sessions/{id}/messages/stream", produces = "text/event-stream")
public SseEmitter stream(@PathVariable Long id, ...) {
    SseEmitter emitter = new SseEmitter(60_000L);
    
    executor.execute(() -> {
        try {
            for (String chunk : modelService.streamChat(prompt)) {
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(chunk));  // 每个 chunk 是 "data: {...}\n\n"
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    return emitter;
}
```

```javascript
// 前端
const resp = await fetch(url, { headers: { Authorization: `Bearer ${token}` }});
const reader = resp.body.getReader();
const decoder = new TextDecoder();
let buffer = '';
while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';
    for (const line of lines) {
        if (line.startsWith('data: ')) {
            const payload = line.substring(6);
            const json = JSON.parse(payload);
            streamContent.value += json.choices[0].delta.content;
        }
    }
}
```

**关键设计**:
- 后端用 `SseEmitter` (Spring MVC 内置, 不需 WebFlux)
- 前端用 `ReadableStream` (浏览器原生)
- 心跳保持: 30s 一条空数据
- 错误恢复: 自动重连

### 3.3 Agent ReAct 循环

```python
# 伪代码
def agent_run(goal, tools):
    messages = [
        {role: "system", content: SYSTEM_PROMPT},
        {role: "user", content: f"目标: {goal}"}
    ]
    
    for round in 1..8:
        resp = llm_call(messages, tools_schema)
        
        # 检测 Final Answer
        if "<final>...</final>" in resp.content:
            return extract(resp.content)
        
        # 检测 tool_calls
        if resp.tool_calls:
            for tc in resp.tool_calls:
                # 调本地工具
                result = tool_executor.execute(tc.function.name, tc.function.arguments)
                messages.append({role: "tool", content: result})
        else:
            # 没 final 又没 tool → stalled
            break
    
    return fail("达到 maxRounds")
```

**关键设计**:
- LLM 通过 XML `<final>...</final>` 标记最终答案
- 否则必须 `tool_calls` (强制结构化输出)
- 工具执行在 LLM 控制下, 不是程序预设
- 失败: 异常 → 继续, 8 轮用完 → fail

### 3.4 RAG 三级降级

```python
def retrieve(query, kb_id):
    # L1: 向量检索 (最佳)
    results = vector_search(query, top_k=10, threshold=0.5)
    if len(results) >= 3:
        return results
    
    # L2: 关键词检索 (MySQL FULLTEXT)
    results = fulltext_search(query, top_k=10)
    if len(results) >= 1:
        return results
    
    # L3: 全量 (兜底)
    return scan_all_chunks(query, top_k=5)
```

**关键设计**:
- L1 最精准但需要 embedding
- L2 中文分词 + 全文索引, 兜底
- L3 暴力扫, 永远能返回
- 实际产品: 同时跑 L1+L2, 去重合并

### 3.5 长期记忆向量召回

```sql
-- memory_long_term 表
CREATE TABLE memory_long_term (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  content TEXT,
  embedding BLOB,        -- 768 维 float[] 序列化为 bytes
  importance INT,
  created_at DATETIME
);

-- 召回 (用 user_id 过滤 + 余弦相似度)
SELECT * FROM memory_long_term
WHERE user_id = ?
ORDER BY COSINE_SIMILARITY(embedding, ?) DESC
LIMIT 5;
```

**关键设计**:
- 向量存 MySQL BLOB (省 ES / Milvus)
- 计算余弦相似度: 768 维 × float = 3KB/条
- 召回 5 条, 注入 prompt

### 3.6 Bucket4j 限流

```java
// 1. 三层限流配置
RateLimitService {
    ipBucket = Bucket.builder()
        .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
        .build();
    
    userBucket = ...  // 60/60s
    globalBucket = ...  // 1000/60s
}

// 2. 拦截器
boolean allow = rateLimit.tryConsume(ip, userId);
// 三层 AND: 都通过才放行
```

**关键设计**:
- 令牌桶算法 (Bucket4j 8.10.1)
- 三层叠加: IP 100/60s + User 60/60s + Global 1000/60s
- 内存存储 (Caffeine 60s TTL)
- 拒绝: 429 + Retry-After

### 3.7 多租户 (V3.1)

```java
// TenantContext (ThreadLocal)
public static Long currentTenantId() { return CURRENT.get(); }
public static boolean isSuperAdmin() { return currentTenantId() == 0L; }

// TenantInterceptor
public boolean preHandle(req, resp, handler) {
    AuthenticatedUser u = (AuthenticatedUser) auth.getPrincipal();
    TenantInfo info = resolver.resolve(u.id());
    TenantContext.set(info.tenantId(), info.tenantCode());
    return true;
}

// Service 层使用
List<SysUser> list() {
    Long tid = TenantContext.currentTenantId();
    if (TenantContext.isSuperAdmin()) {
        return userMapper.selectList(null);  // 全量
    }
    return userMapper.selectList(
        new LambdaQueryWrapper<SysUser>().eq(SysUser::getTenantId, tid)
    );
}

// 清理
public void afterCompletion(...) {
    TenantContext.clear();
}
```

**关键设计**:
- ThreadLocal 自动跟请求生命周期
- 拦截器统一处理, Service 不感知
- adminLiugl 强制 tenantId=0 跨租户
- 普通用户严格隔离

### 3.8 OpenAI 兼容网关 (V3.3)

```java
@PostMapping("/api/v1/openai/chat/completions")
public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
    // 1. 找模型
    ModelConfig m = modelConfigMapper.selectByCode((String) body.get("model"));
    if (m == null) return error("model_not_found");
    
    // 2. 拿 baseUrl / apiKey
    ModelProvider p = providerMapper.selectById(m.getProviderId());
    
    // 3. 转内部协议
    Map<String, Object> upstream = new HashMap<>();
    upstream.put("model", m.getModelCode());
    upstream.put("messages", body.get("messages"));
    upstream.put("stream", body.get("stream"));
    
    // 4. 调用上游
    if (stream) {
        return ResponseEntity.ok((StreamingResponseBody) out -> {
            streamProxy(upstream, p.getBaseUrl(), p.getApiKey(), out);
        });
    } else {
        return ok(callNonStream(upstream, p.getBaseUrl(), p.getApiKey()));
    }
}
```

**关键设计**:
- 接受 OpenAI 格式
- 内部转标准协议
- 流式包装成 OpenAI chunk
- 任何 OpenAI SDK 直接可用

### 3.9 Prometheus 监控 (V0.12)

```java
// 5 类业务指标 (Counter / Gauge / Timer)
Counter chatCall = Counter.builder("minimax_chat_total").register(registry);
chatCall.increment();

Counter tokenUsed = Counter.builder("minimax_tokens_total").tag("model", "...").register(registry);

// 5 条告警规则
ruleEngine.check("HighErrorRate", () -> {
    return errorRate() > 0.05;
});
// 触发 → alert_event 表 + 冷却 15min
```

**关键设计**:
- Micrometer 抽象 (Prometheus 适配)
- 5 类业务 + 4 类网关 + 2 类技术
- 5 默认规则 + 冷却 + 恢复
- 60s 落库快照

---

## 📊 4. 数据流图

### 4.1 用户登录

```
[浏览器]                     [auth 8081]                [MySQL]
   │                              │                       │
   │ POST /auth/login             │                       │
   │ {user, pass}                 │                       │
   ├─────────────────────────────▶│                       │
   │                              │ SELECT * FROM sys_user │
   │                              ├──────────────────────▶│
   │                              │◀───────  user row   │
   │                              │                       │
   │                              │ BCrypt.matches(pwd)  │
   │                              │  ✓ OK                │
   │                              │                       │
   │                              │ SELECT role codes    │
   │                              ├──────────────────────▶│
   │                              │◀─── ['SUPER_ADMIN']  │
   │                              │                       │
   │                              │ INSERT refresh token │
   │                              ├──────────────────────▶│
   │                              │                       │
   │                              │ JWT.issueAccess      │
   │                              │  (30min)              │
   │◀───── {access, refresh, user}                     │
   │                              │                       │
   │ localStorage.set             │                       │
```

### 4.2 流式对话

```
[浏览器]              [chat 8082]              [model 8083]              [OpenAI]
   │                     │                        │                        │
   │ POST /stream        │                        │                        │
   ├────────────────────▶│                        │                        │
   │                     │                        │                        │
   │                     │ 转发 chat request      │                        │
   │                     ├───────────────────────▶│                        │
   │                     │                        │ POST /v1/chat          │
   │                     │                        ├───────────────────────▶│
   │                     │                        │                        │
   │                     │                        │ SSE stream: chunk 1    │
   │                     │                        │◀───────────────────────│
   │                     │                        │   chunk 2              │
   │                     │                        │◀───────────────────────│
   │                     │◀── SSE stream (转格式)  │                        │
   │                     │                        │                        │
   │◀──── SSE stream ── │                        │                        │
   │                     │                        │                        │
   │ render chunk 1      │                        │                        │
   │ render chunk 2      │                        │                        │
   │ ...                 │                        │                        │
```

### 4.3 Agent 自主任务

```
[用户]              [agent 8090]              [model 8083]              [function 8086]
   │                    │                        │                         │
   │ POST /agent/run   │                        │                         │
   │ {goal}             │                        │                         │
   ├───────────────────▶│                        │                         │
   │                    │                        │                         │
   │                    │ Round 1: LLM call     │                         │
   │                    ├───────────────────────▶│                         │
   │                    │                        │                         │
   │                    │ 响应: tool_call       │                         │
   │                    │ {name: "calculator",  │                         │
   │                    │  args: "123*456"}      │                         │
   │                    │                        │                         │
   │                    │ 执行工具: calculator                                 │
   │                    ├─────────────────────────────────────────────────────▶│
   │                    │◀─────────  result: 56088  ──────────────────────────│
   │                    │                        │                         │
   │                    │ Round 2: LLM + 工具结果                              │
   │                    ├───────────────────────▶│                         │
   │                    │                        │                         │
   │                    │ 响应: <final>56088</final>                          │
   │                    │                        │                         │
   │◀─ {answer, steps, rounds: 2}                │                         │
```

### 4.4 RAG 检索 (三级降级)

```
[用户]                 [rag 8085]                  [MySQL]
   │                       │                         │
   │ POST /rag/retrieve    │                         │
   │ {query, kb_id}        │                         │
   ├──────────────────────▶│                         │
   │                       │                         │
   │                       │ L1: 向量检索            │
   │                       │  (内存余弦, top 10)     │
   │                       │                         │
   │                       │ if results < 3:         │
   │                       │   L2: FULLTEXT 搜索     │
   │                       │   (中文分词)           │
   │                       ├────────────────────────▶│
   │                       │◀──── chunks ──────────│
   │                       │                         │
   │                       │ if results < 1:         │
   │                       │   L3: 全表扫描         │
   │                       │   (top 5)              │
   │                       │                         │
   │◀── {chunks, sources}  │                         │
```

---

## 🔄 5. 部署架构 (生产)

```
                            ┌─────────────┐
                            │   Nginx     │
                            │   :80/443   │
                            └──────┬──────┘
                                   │
            ┌──────────────────────┼──────────────────────┐
            │                      │                      │
       ┌────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
       │ Frontend │          │ /api/v1/* │          │ /ws/*     │
       │  (HTML)  │          │  Gateway  │          │ WebSocket │
       └──────────┘          └─────┬─────┘          └───────────┘
                                   │
            ┌──────────────────────┼──────────────────────┐
            │                      │                      │
       ┌────▼────┐  ┌────┴────┐  ┌──▼────┐  ┌────┴────┐  ┌──▼────┐
       │  auth   │  │  chat   │  │ model │  │ memory │  │  ...  │
       │  8081   │  │  8082   │  │ 8083  │  │  8084  │  │       │
       └─────────┘  └─────────┘  └───────┘  └────────┘  └───────┘
            │              │             │            │
            └──────────────┴─────────────┴────────────┘
                                   │
                            ┌──────▼──────┐
                            │   MySQL 8   │
                            │   31 表     │
                            └─────────────┘
```

### 5.1 容器化 (Docker)

```yaml
# deploy/docker-compose.yml
version: '3.8'
services:
  auth:
    image: minimax-auth:1.0.0
    ports: ["8081:8081"]
    depends_on: [mysql]
  chat:
    image: minimax-chat:1.0.0
    ports: ["8082:8082"]
  ...  # 10 个服务
  nginx:
    image: nginx:alpine
    ports: ["80:80"]
    volumes: ["./nginx.conf:/etc/nginx/nginx.conf"]
```

### 5.2 K8s

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: minimax-auth }
spec:
  replicas: 2
  selector: { matchLabels: { app: auth } }
  template:
    spec:
      containers:
      - name: auth
        image: minimax-auth:1.0.0
        ports: [{ containerPort: 8081 }]
```

---

## 🎯 6. 设计权衡

| 决策 | 优点 | 缺点 |
|------|------|------|
| **微服务** (12 个) | 独立部署/扩展/技术异构 | 运维复杂, 网络延迟 |
| **MySQL 存向量** (BLOB) | 省 ES/Milvus, 一致性 | 大规模慢 (>100万向量) |
| **HttpClient 自实现** | 无 Feign 依赖, 控制细 | 要写更多 boilerplate |
| **JWT 双 token** | 无状态, 可扩展 | refresh DB 查 |
| **SseEmitter** (非 WebFlux) | 兼容 MVC, 简单 | 阻塞线程 |
| **OpenAI 兼容网关** | 任何 OpenAI SDK 可用 | 内部协议要维护 |
| **ThreadLocal 多租户** | 透明, Service 不感知 | 异步场景要手动传 |
| **Bucket4j** (内存限流) | 无 Redis 依赖 | 多实例不共享 |

---

## 📐 7. 关键设计模式

### 7.1 Adapter 模式 (Model Provider)
```java
interface ModelAdapter {
    List<ModelInfo> listModels();
    String chat(List<Message> messages);
    void streamChat(List<Message> messages, Consumer<String> onChunk);
}
class OpenAiCompatibleAdapter implements ModelAdapter { ... }
class MockAdapter implements ModelAdapter { ... }
```

### 7.2 Strategy 模式 (限流)
```java
interface RateLimitStrategy {
    boolean allow(String key);
}
class IpRateLimit implements RateLimitStrategy { ... }
class UserRateLimit implements RateLimitStrategy { ... }
class GlobalRateLimit implements RateLimitStrategy { ... }
```

### 7.3 Builder 模式 (Bucket4j)
```java
Bucket.builder()
    .addLimit(Bandwidth.classic(capacity, Refill.greedy(refill, period)))
    .build();
```

### 7.4 Chain of Responsibility (拦截器)
```java
JwtAuthenticationFilter → TenantInterceptor → RateLimitFilter → Controller
```

### 7.5 Repository 模式 (MyBatis-Plus)
```java
List<SysUser> findByTenant(Long tid) {
    return userMapper.selectList(
        new LambdaQueryWrapper<SysUser>().eq(SysUser::getTenantId, tid)
    );
}
```

---

## 🎓 8. 学到什么 (Lesson Learned)

1. **沙箱的 bash exit 会杀子进程** → 用 `disown` 脱离控制组
2. **沙箱的 `/tmp` 不能写** → logs 写到 workspace
3. **沙箱 MySQL socket 认证** → 建 `minimax@127.0.0.1` 用户
4. **沙箱 JAR 绝对路径找不到** → cd 到 target 用相对路径
5. **Spring bean 冲突** → 用 `@Configuration("name")` 加前缀
6. **MyBatis Plus 缺 jar** → `dependency:build-classpath -DincludeScope=runtime`
7. **lambda 引用 final** → `final Map` 替代
8. **Vant 没用** → 按需导入组件
9. **前端 vite 5173 启动慢** → 用 `nohup ... &`
10. **JSON 解析在 Python 失败** → `json.load` 而非 `eval`

---

## 🚀 9. 未来优化

| 优先级 | 项目 | 价值 |
|--------|------|------|
| P0 | 向量数据库 (Milvus/Qdrant) | 大规模 RAG |
| P0 | Redis 集群 | 分布式限流 |
| P1 | K8s Helm | 生产部署 |
| P1 | GitHub Actions CI | 自动化 |
| P2 | gRPC 替代 HTTP | 性能 |
| P2 | Service Mesh (Istio) | 流量管理 |
| P3 | 实时协作 (CRDT) | 高级功能 |
| P3 | 多语言 SDK | 生态 |
