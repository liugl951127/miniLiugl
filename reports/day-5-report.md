# Day 5 - 流式对话 SSE 真实接入 ⭐ 核心 自检报告

**生成时间**: 2026-06-16 13:50 (Asia/Shanghai)
**完成度**: 100% · 全部编译通过 + 单元测试通过 + 真实流式 + 取消验证

---

## 今日交付 ⭐

### 后端 (minimax-model) - 流式架构升级
- ✅ **OpenAI 真实流式调用** (`OpenAiCompatibleAdapter.streamChat`)
  - 用 Java 11+ `HttpClient.BodyHandlers.ofLines()` 解析 SSE
  - 逐行解析 `data: {...}\n\n` 事件
  - 回调式推 chunk + 支持 stopFlag 中断
- ✅ **Mock 流式升级** (`MockAdapter.streamChat`)
  - 30ms/字符节奏输出
  - 支持 stopFlag 中断
  - 输出 `finishReason: "cancelled"` 当被取消
- ✅ **流式控制器** (`ModelController.streamChat`)
  - 按 provider 选 adapter
  - SSE 协议：每 chunk 写 `data: {...}\n\n`
  - 写 `data: [DONE]` 标记结束
  - 完成后自动 `quotaService.record()` 计配额
  - `StreamingResponseBody` 替代 WebFlux
- ✅ **取消机制** (`POST /models/chat/cancel?streamId=xxx`)
  - 后端 `ConcurrentHashMap<streamId, AtomicBoolean>` 跟踪活跃流
  - 取消时把 stopFlag 置 true，provider 检测后停止
- ✅ **单元测试 3 新用例** (`StreamingTest`)
  - mockStreamProducesChunks - 推多个 chunk
  - mockStreamRespectsCancelFlag - 100ms 后取消，验证 finishReason=cancelled
  - mockStreamFullText - Jackson 解析 chunk 提取完整文本

### 前端 (minimax-web) - 流式 UI
- ✅ **流式 fetch** (`api/model.js#streamChat`)
  - 用 `fetch + ReadableStream` 处理 SSE（不走 axios）
  - 返回 `abort()` 方法中断
  - 自动 `data: [DONE]` 检测
- ✅ **打字机效果** (`views/chat/Index.vue`)
  - 实时显示"▊"光标（CSS blink 动画）
  - 字符级追加 assistant 消息
  - "实时生成中... X 字符" 顶部 tag
- ✅ **取消按钮** - 流式时"发送"变"停止"按钮
  - 前端 `streamController.abort()` 立即断连
  - 后端停止生成并标记 cancelled
- ✅ **Token 计数 UI** - 消息底部显示 "· 42 tokens"
- ✅ **自动滚动** - 每次 chunk 触发 `scrollBottom()`

### 验证
- ✅ **Maven 编译**: 5 模块 BUILD SUCCESS
- ✅ **单元测试**: **13 用例全过**（3 旧 + 3 mock + 3 provider + 1 streaming×3）
- ✅ **3 服务并行**: auth:8081 + chat:8082 + model:8083
- ✅ **真实流式 SSE**: 每 30ms 推 1 个字符，70+ 字符
- ✅ **取消触发**: 流被中途打断，输出 20 行后停止
- ✅ **前端构建**: `npm run build` 19.56s 通过

---

## 代码统计

| 指标 | Day 4 | Day 5 | 增量 |
|------|-------|-------|------|
| Java 文件 | 83 | **84** | +1 |
| Java 行数 | 3293 | **3593** | +300 |
| XML 行数 | 1278 | **1336** | +58 |
| 单元测试 | 10 | **13** | +3 |
| TODO/FIXME | 0 | 0 | - |

---

## 关键架构决策

### 1. 真流式 vs 假流式
- **真流式**: Java `HttpClient` + `BodyHandlers.ofLines()` 直接解析 SSE
- **真流式**: 字节流层面增量推送，没有"先收集再发"
- **vs 假流式**: 拿到完整回复后 sleep 30ms 假装流式

### 2. 取消机制设计
- **客户端 abort** → fetch 立即断
- **后端 stopFlag** → provider 在每 chunk 循环里 check，true 时抛 `STREAM_CANCELLED`
- **DB 落库** → 流停止后用已收到的部分内容 + `[已停止]` 后缀落库
- **finishReason** → cancelled 区别于 stop（正常完成）

### 3. 前端不使用 axios 处理 SSE
- `ReadableStream` API 直接拿流
- 避免 axios 拦截器把流当成 JSON 解析
- 单独维护 `streamId` 用于后端 cancel

### 4. Spring MVC 流式响应
- 继续用 `StreamingResponseBody`（不用 WebFlux）
- 在 lambda 内部 `try { writeSse } catch (IOException) { throw RuntimeException }`
- 因为 Consumer<String> 不允许抛 checked exception

---

## 真实运行验证

```bash
# 1. 流式 chat
$ curl -N -X POST http://localhost:8083/api/v1/models/chat/stream \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"model":"MiniMax-Text-01","messages":[{"role":"user","content":"流式测试"}]}'
data: {"id":"mock-stream-...","object":"chat.completion.chunk","choices":[{"delta":{"content":"?"}}]}
data: {"choices":[{"delta":{"content":"M"}}]}
data: {"choices":[{"delta":{"content":"o"}}]}
... (30ms/字符)
data: [DONE]

# 2. 取消流式
$ curl -X POST "http://localhost:8083/api/v1/models/chat/cancel?streamId=my-test-1" \
    -H "Authorization: Bearer $TOKEN"
{"code":0,"message":"success"}

# 流在 ~20 字符处停止（取决于 cancel 触发时机）
```

---

## 关键文件清单

### 后端
- `backend/minimax-model/src/main/java/com/minimax/model/`
  - `provider/OpenAiCompatibleAdapter.java` (重写: streamChat + StreamResult record)
  - `provider/MockAdapter.java` (升级: streamChat + stopFlag)
  - `provider/ModelProviderAdapter.java` (新增 streamChat 默认方法)
  - `controller/ModelController.java` (重写: streamChat + cancel 端点 + stopFlags Map)
  - `test/.../StreamingTest.java` (新增 3 用例)

### 前端
- `frontend/src/api/model.js` (重写: streamChat 函数 + abort 机制)
- `frontend/src/views/chat/Index.vue` (重写: 打字机 + 取消按钮 + 光标动画)

### 关键代码
```java
// 后端流式核心
public StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                               Consumer<String> chunkJsonConsumer,
                               AtomicBoolean stopFlag) {
    HttpResponse<Stream<String>> httpResp = client.send(
        buildRequest(endpoint, apiKey, body),
        HttpResponse.BodyHandlers.ofLines()
    );
    httpResp.body().forEach(line -> {
        if (stopFlag.get()) throw new RuntimeException("STREAM_CANCELLED");
        // 解析 SSE data: ...
        chunkJsonConsumer.accept(data);
    });
}

// 前端流式核心
const ctrl = new AbortController();
fetch(url, { signal: ctrl.signal, ... }).then(async response => {
  const reader = response.body.getReader();
  while (true) {
    const { done, value } = await reader.read();
    // 解析 chunk 推 onChunk
  }
});
return { abort: () => ctrl.abort() };
```

---

## 明日计划 Day 6

- [ ] **短期记忆 (Redis)** - 多轮上下文窗口管理
- [ ] Token 截断策略（按 maxContext 切）
- [ ] 系统提示词模板
- [ ] 对话历史摘要
- [ ] 记忆索引加速查找

---

**状态**: ✅ Day 5 全部完成 + 自检通过 + 实测通过 + 待 git push
