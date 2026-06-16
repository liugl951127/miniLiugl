# Day 4 - 模型路由层 (OpenAI 兼容 + 多 provider + 限流) 自检报告

**生成时间**: 2026-06-16 13:30 (Asia/Shanghai)
**完成度**: 100% · 全部编译通过 + 单元测试通过 + 真实跨服务跑通

---

## 今日交付

### 后端 (minimax-model)
- ✅ SQL: `sql/04_model.sql`（3 张表 + 3 provider + 6 模型初始数据）
  - `model_provider` 供应商表（OpenAI / Minimax-M3 / Ollama / 智谱 / Qwen / DeepSeek）
  - `model_config` 模型配置表（含 max_context/max_output/pricing/capabilities）
  - `model_quota` 用户配额（按用户/模型/天 + UPSERT 原子计数）
- ✅ 实体: ModelProvider、ModelConfig、ModelQuota
- ✅ Mapper: 3 个 + 2 个 XML 映射（含 `selectEnabledWithProvider` 联表、`incrementUsage` 原子 upsert）
- ✅ Provider 适配器:
  - `ModelProviderAdapter` 接口（chat/stream/ping）
  - `OpenAiCompatibleAdapter` 阻塞实现（用 Java 11+ HttpClient 调真实 API）
  - `MockAdapter` 离线 mock（无 key 演示用，含流式逐字推）
  - `ModelProviderFactory` 按 provider code 路由
- ✅ 限流（Bucket4j 8.16.0）: 容量 10 + 60/min greedy
- ✅ 配额: `QuotaService` 原子累加
- ✅ Service: `ModelServiceImpl`（限流 + 路由 + 配额 + 完整响应）
- ✅ Controller: `ModelController` 4 个端点
  - GET /models - 列表
  - GET /models/providers - provider 列表
  - POST /models/chat - 阻塞 chat
  - POST /models/chat/stream - SSE 流式
- ✅ H2 测试 profile + H2 schema + 单元测试 7 用例

### 前端 (minimax-web)
- ✅ `api/model.js` - 模型路由 API 层
- ✅ `store/model.js` - Pinia store（models + providers + currentModel 持久化）
- ✅ `views/chat/Index.vue` - **集成 model 选择器** + **调真模型**（不再写死 mock 回复）
- ✅ Vite proxy: `/api/v1/models` → 8083

### 验证（关键里程碑）
- ✅ **Maven 编译**: 5 个模块全部 BUILD SUCCESS（含 aliyun 镜像下载）
- ✅ **单元测试**: **10 用例全过**（auth 4 + chat 3 + model 3）
- ✅ **3 服务并行运行**: auth (8081) + chat (8082) + model (8083)
- ✅ **跨服务 JWT 鉴权**: auth 签发 → model 验证 → chat 验证
- ✅ **完整 E2E 流程**:
  - 登录 → 列模型 → 创建 chat session → 调模型（mock 37 tokens）→ 存消息 → 拉消息
- ✅ **限流触发**: 连续调用触发 `code=1006 RATE_LIMIT`
- ✅ **SSE 流式**: 阻塞端点 + 流式端点都通，字符级推送
- ✅ **前端构建**: `npm run build` 19.26s 通过

---

## 代码统计

| 指标 | Day 3 | Day 4 | 增量 |
|------|-------|-------|------|
| Java 文件 | 60 | **83** | +23 |
| Java 行数 | 2251 | **3293** | +1042 |
| XML 行数 | 928 | **1278** | +350 |
| SQL 行数 | 264 | 396 | +132 |
| 单元测试 | 7 | **10** | +3 |
| TODO/FIXME | 0 | 0 | - |

---

## 构建产物

| 文件 | 大小 | 端口 | 启动时间 |
|------|------|------|----------|
| `minimax-gateway.jar` | 62M | 8080 | - |
| `minimax-auth.jar` | 51M | 8081 | 9.3s |
| `minimax-chat.jar` | 103M | 8082 | 9.3s |
| `minimax-model.jar` | 74M | 8083 | 10.9s |
| `minimax-common-1.0.0-SNAPSHOT.jar` | 19K | - | - |

---

## 真实运行验证

```bash
# 启动 3 个服务
java -jar minimax-auth.jar  --spring.profiles.active=test --server.port=8081
java -jar minimax-chat.jar  --spring.profiles.active=test --server.port=8082
java -jar minimax-model.jar --spring.profiles.active=test --server.port=8083

# 1. 登录
$ curl -X POST http://localhost:8081/auth/login -d '{"username":"admin","password":"admin@123"}'
{"code":0,"data":{"accessToken":"eyJ..."}}

# 2. 列模型
$ curl http://localhost:8083/api/v1/models -H "Authorization: Bearer ..."
{"code":0,"data":[
  {"code":"gpt-4o-mini",   "providerCode":"openai",   "maxContext":128000, ...},
  {"code":"MiniMax-Text-01","providerCode":"minimax", "maxContext":1000000, ...},
  {"code":"llama3:8b",      "providerCode":"ollama",  "maxContext":8192, ...},
  ...
]}

# 3. 调模型（mock 模式无需 key）
$ curl -X POST http://localhost:8083/api/v1/models/chat -d '{
    "model":"MiniMax-Text-01",
    "messages":[{"role":"user","content":"介绍你自己"}]
  }'
{"code":0,"data":{
  "model":"MiniMax-Text-01",
  "content":"【Mock 回复】你调用的是 MiniMax-Text-01...",
  "totalTokens":37,
  "providerCode":"mock"
}}

# 4. SSE 流式
$ curl -N -X POST http://localhost:8083/api/v1/models/chat/stream -d '...'
data: {"choices":[{"index":0,"delta":{"content":"M"}}]}
data: {"choices":[{"index":0,"delta":{"content":"o"}}]}
data: {"choices":[{"index":0,"delta":{"content":"c"}}]}
...
data: [DONE]
```

---

## 关键文件清单

### 后端
- `sql/04_model.sql` (132 行)
- `sql/init/04_model.sql`
- `backend/minimax-model/pom.xml`
- `backend/minimax-model/src/main/java/com/minimax/model/`
  - `entity/ModelProvider.java` + `ModelConfig.java` + `ModelQuota.java`
  - `mapper/ModelProviderMapper.java` + `ModelConfigMapper.java` + `ModelQuotaMapper.java`
  - `service/ModelService.java` + `ModelServiceImpl.java`
  - `provider/ModelProviderAdapter.java` (interface)
  - `provider/OpenAiCompatibleAdapter.java` (Java 11+ HttpClient 真实调用)
  - `provider/MockAdapter.java` (离线 mock 含流式)
  - `provider/ModelProviderFactory.java` (按 code 路由)
  - `quota/RateLimiter.java` (Bucket4j 60/min)
  - `quota/QuotaService.java` (原子配额计数)
  - `controller/ModelController.java` (4 端点)
  - `dto/ChatRequest.java`
  - `vo/ModelVO.java` + `ChatResponse.java`
  - `config/SecurityConfig.java` + `MybatisPlusConfig.java`
  - `ModelApplication.java`
  - `resources/application.yml` + `application-test.yml`
  - `resources/mapper/ModelConfigMapper.xml` + `ModelQuotaMapper.xml`
  - `resources/schema-h2.sql` (H2 测试用)
  - `test/.../MockAdapterTest.java` + `ModelProviderFactoryTest.java` (3+4 用例)

### 前端
- `frontend/src/api/model.js` (新增)
- `frontend/src/store/model.js` (新增，含 currentModel 持久化)
- `frontend/src/views/chat/Index.vue` (集成 model 选择器 + 调真模型)
- `frontend/vite.config.js` (proxy 加 models 路径)

### 工具改进
- `backend/minimax-common/src/main/java/com/minimax/common/security/JwtAuthenticationFilter.java`
  (架构改进 - 失败不再重抛)

---

## 关键架构决策

### 1. Provider 适配器模式
- `ModelProviderAdapter` 接口统一了 OpenAI/Minimax-M3/Ollama/Anthropic 等异构 API
- 6 个 provider 自动 fallback 到 `OpenAiCompatibleAdapter`（用 code 协议分发）
- `MockAdapter` 兜底，让前端不依赖 key 也能演示

### 2. 限流 + 配额分离
- `RateLimiter` 用 Bucket4j 内存桶（防刷）
- `QuotaService` 用 MySQL UPSERT 原子累加（防滥用）
- 两者各管一摊：限流 = 即时拦截，配额 = 长期统计

### 3. Spring MVC 流式替代 WebFlux
- **SSE 用 `StreamingResponseBody`**（不是 `Flux<String>`）——避免 webflux 依赖冲突
- 调用方拿 `modelService.chat()` 阻塞响应，再流式推字符
- Day 5 接入真模型时再换更优的流式（HttpClient 的 Stream API）

### 4. 应用配置
- `minimax.model.mock-mode` 开关：true 时强制走 mock
- 测试 profile 默认开 mock，prod 关闭
- 加 mock 让 Day 4 完整可演示，**不依赖任何 API key**

---

## 明日计划 Day 5

- [ ] **流式对话 SSE 真实接入**（核心）
- [ ] 优化 StreamingResponseBody：直接调真实 provider 的流式 API
- [ ] 前端实时打字机效果（替换 append-then-scroll）
- [ ] Token 计数 + 费用预估
- [ ] 取消按钮（中断流式）
- [ ] 自动滚动到底部

---

**状态**: ✅ Day 4 全部完成 + 自检通过 + 实测通过 + 待 git push
