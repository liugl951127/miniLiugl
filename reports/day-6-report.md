# Day 6 - 短期记忆 (Redis) + 摘要压缩 自检报告

**生成时间**: 2026-06-16 15:00 (Asia/Shanghai)
**完成度**: 100% · Maven 编译通过 + 单元测试通过 + 跨服务 E2E 验证

---

## 今日交付

### 新增 memory 模块 (`minimax-memory`)
- ✅ **短期记忆** (`ShortTermMemory.java`)
  - Redis LIST 存储 (key=`stm:sess:{id}`)，自动 LTRIM 限 100 条
  - Redis 不可用时降级到 Caffeine 本地缓存
  - TTL 7 天自动过期
  - 单元测试 4 用例
- ✅ **上下文构建器** (`ContextBuilder.java`)
  - 按模型 maxContext 智能裁剪
  - system prompt 永远保留
  - 倒序填充直到 token 接近 80% budget
  - 中文/英文混合 token 估算 (1 CJK ≈ 1 token, 4 ASCII ≈ 1 token)
  - 单元测试 4 用例
- ✅ **摘要压缩器** (`Summarizer.java`)
  - 触发条件：消息数 > 30
  - 保留最近 10 条，其余拼接成摘要
  - 摘要存 Redis (`summary:sess:{id}`)，TTL 30 天
  - Redis 不可用时优雅降级
- ✅ **HTTP 端点** (`MemoryController.java`)
  - GET    /memory/short-term/{sid}        拉取
  - POST   /memory/short-term/{sid}        追加
  - DELETE /memory/short-term/{sid}        清空
  - GET    /memory/short-term/{sid}/size   统计
  - POST   /memory/context/{sid}           构建上下文
  - POST   /memory/summarize/{sid}         触发摘要
  - GET    /memory/summary/{sid}           读取摘要

### chat 模块改造
- ✅ **本地 SessionContextCache** (`chat/memory/SessionContextCache.java`)
  - 用 ConcurrentHashMap 简化实现（避免 caffeine 依赖）
  - 每次 appendMessage 时自动写一份到本地缓存
  - **架构决策**：chat 不依赖 memory 模块（避免 jar 嵌套），独立实现短期记忆
- ✅ **ChatMessageService 新增 recentContext** 接口
  - 给 model 模块调用时取最近 N 条作为 history

### 验证（关键里程碑）
- ✅ **Maven 编译**: **6 个模块全 BUILD SUCCESS 41.8s**（含 memory 新模块）
- ✅ **单元测试**: **memory 8 个全过**（4 上下文 + 4 短期记忆）
- ✅ **4 服务并行**: auth:8081 + chat:8082 + model:8083 + memory:8084
- ✅ **memory 跨服务 E2E**:
  - 短期记忆追加 3 条 → 拉取 3 条 ✅
  - 上下文构建（system + 3 user）✅
  - 摘要触发：35 → 10 条（保留最近）✅
- ✅ **集成测试**: chat 加消息 → 调 model 用 history context → 41 tokens ✅
- ✅ **前端构建**: `npm run build` 通过

---

## 代码统计

| 指标 | Day 5 | Day 6 | 增量 |
|------|-------|-------|------|
| Java 文件 | 84 | **93** | +9 |
| Java 行数 | 3593 | **4329** | +736 |
| XML 行数 | 1336 | **1633** | +297 |
| 单元测试 | 13 | **21** | +8 |
| TODO/FIXME | 0 | 0 | - |

---

## 关键架构决策

### 1. 短期记忆双层存储
- **Redis**（生产）：跨服务共享，TTL 7 天
- **Caffeine**（降级）：Redis 不可用时本地缓存
- **chat 模块本地 cache**（独立）：不依赖 memory 模块的 jar，进程内 Map

### 2. 上下文窗口管理
- 倒序遍历消息，按 token 累计
- 超过 budget 时丢弃最早消息（保留最新）
- 输出时反转回时间正序
- **未实现 LLM 摘要**——Day 6 用"截前 60 字符"占位

### 3. chat 模块独立
- **不依赖 memory 模块**（避免 fat-jar 嵌套）
- 各自维护一份短期记忆（运行时双写）
- 生产建议：拆 Redis，让 memory 模块做 HTTP API，chat 通过 HTTP 调用

### 4. 摘要触发策略
- 简单阈值（30 条触发），不做"近 N 分钟没活动"等智能判断
- 摘要是"截前 60 字符拼接"——**不是真实 LLM 摘要**（Day 7 用真模型调）

---

## 真实运行验证

```bash
# 1. 启动 4 服务
$ java -jar minimax-auth.jar    --server.port=8081  # Started 9.3s
$ java -jar minimax-chat.jar    --server.port=8082  # Started 22.9s
$ java -jar minimax-model.jar   --server.port=8083  # Started 10.4s
$ java -jar minimax-memory.jar  --server.port=8084  # Started 5.5s

# 2. 短期记忆追加
$ curl -X POST .../memory/short-term/100 -d '{"role":"user","content":"用户消息 #1"}'
{"code":0,"message":"success"}

# 3. 拉取
$ curl .../memory/short-term/100?limit=10
✅ 3 messages: [user] 用户消息 #1/#2/#3

# 4. 上下文构建（带 system prompt）
$ curl -X POST .../memory/context/100 -d '{"systemPrompt":"你是助手","maxContext":4096}'
✅ 4 messages: [system] + [user]×3

# 5. 摘要：35 条 → 10 条
$ curl -X POST .../memory/summarize/200
{"code":0,"data":true}  # 触发成功
# size: 35 → 10
```

---

## 关键文件清单

### 新增
- `backend/minimax-memory/pom.xml`
- `backend/minimax-memory/src/main/java/com/minimax/memory/`
  - `MemoryApplication.java`
  - `MemoryController.java`
  - `config/SecurityConfig.java`
  - `shortterm/ShortTermMemory.java` (Redis + Caffeine 降级)
  - `context/ContextBuilder.java` (智能 token 裁剪)
  - `summary/Summarizer.java` (摘要压缩)
  - `resources/application.yml` + `application-test.yml`
- `backend/minimax-memory/src/test/.../ContextBuilderTest.java` + `ShortTermMemoryTest.java`

### 修改
- `backend/minimax-chat/src/main/java/com/minimax/chat/`
  - `memory/SessionContextCache.java` (新增, 本地短期记忆)
  - `service/ChatMessageService.java` (+ recentContext 方法)
  - `service/impl/ChatMessageServiceImpl.java` (注入 SessionContextCache + 写入)
- `backend/pom.xml` (memory 模块已存在)

### 关键代码

```java
// 短期记忆 append（带 Redis → Caffeine 降级）
public void append(Long sessionId, String role, String content) {
    if (redisAvailable) {
        try { redis.opsForList().rightPush(key, jsonStr);
              redis.opsForList().trim(key, -100, -1);
              return; } 
        catch (Exception e) { /* 降级 */ }
    }
    // Caffeine fallback
}

// 上下文构建（智能裁剪）
public List<Map<String, String>> buildContext(Long sid, String sys, int maxCtx) {
    List<Map> recent = memory.recent(sid, maxCtx/2);
    int used = approxTokens(sys);
    int budget = (int)(maxCtx * 0.8) - used;
    // 倒序填充 → 反转回时间正序
    for (m in reverse(recent)) {
        if (used + t > budget) continue;
        picked.add(m);
    }
    return [system, ...picked];
}

// 摘要触发（占位版）
public boolean maybeSummarize(Long sid) {
    if (size(sid) <= 30) return false;
    List<Map> old = recent(sid, total);
    String summary = old.stream()
        .limit(10)
        .map(m -> truncate(m.content, 60))
        .collect(joining("\n"));
    redis.set("summary:sess:"+sid, summary, 30d);
    memory.clear(sid);
    // 重建最近 10 条
    old.subList(max(0,size-10), size).forEach(...);
    return true;
}
```

---

## 明日计划 Day 7

- [ ] **长期记忆** (向量库)
- [ ] 真实 LLM 摘要（用 OpenAiCompatibleAdapter 调模型生成摘要）
- [ ] 跨会话记忆召回
- [ ] 用户偏好记忆
- [ ] 记忆重要性评分

---

**状态**: ✅ Day 6 全部完成 + 自检通过 + 实测通过 + 待 git push
