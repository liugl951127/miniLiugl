# MiniMax LLM Provider 接入指南 (V5.18)

> V5.18 从 mock 走向生产 — 5 个真实 LLM Provider 全支持 + 多 Key 轮询 + 自动 fallback

## 1. 支持的 Provider

| Provider | 协议 | 端点 | 认证方式 | 适用模型 |
|----------|------|------|---------|----------|
| **OpenAI** | OpenAI Chat Completions | `https://api.openai.com/v1` | Bearer | gpt-4o, gpt-4o-mini, o1-preview |
| **Minimax-M3** | OpenAI 兼容 | `https://api.minimax.chat/v1` | Bearer | MiniMax-Text-01, MiniMax-VL |
| **DeepSeek** | OpenAI 兼容 | `https://api.deepseek.com/v1` | Bearer | deepseek-chat, deepseek-coder |
| **Ollama** | OpenAI 兼容 | `http://localhost:11434/v1` | (无 key) | llama3, qwen2, mistral |
| **Anthropic** | Anthropic Messages | `https://api.anthropic.com` | x-api-key header | claude-3-5-sonnet, claude-3-opus, claude-3-haiku |
| **Google Gemini** | Generative Language | `https://generativelanguage.googleapis.com` | URL ?key= | gemini-1.5-pro, gemini-1.5-flash |

## 2. 配置 API Key

### 2.1 方式 A: 环境变量 (推荐)

```bash
# OpenAI (逗号分隔支持多 Key 轮询)
export OPENAI_API_KEY=sk-proj-xxxxx,sk-proj-yyyyy,sk-proj-zzzzz

# Anthropic
export ANTHROPIC_API_KEY=sk-ant-xxxxx

# Gemini
export GEMINI_API_KEY=AIzaSy-xxxxx

# 或 .env 文件
cat >> .env <<EOF
OPENAI_API_KEY=sk-proj-xxxxx
ANTHROPIC_API_KEY=sk-ant-xxxxx
GEMINI_API_KEY=AIzaSy-xxxxx
EOF
```

### 2.2 方式 B: 管理后台 (加密存储)

路径: 管理员后台 → 模型管理 → Provider 列表 → 编辑 → 填 api_key

DB 存储在 `model_provider.api_key`, 加密 (V5.18 TODO: AES 加密)

### 2.3 优先级

```
环境变量 (高) > DB api_key (兜底) > mock (fallback)
```

## 3. 多 Key 轮询 (V5.18 新增)

### 3.1 用法

```bash
# 设置 3 个 OpenAI Key (逗号分隔)
export OPENAI_API_KEY=sk-A,sk-B,sk-C
```

每次请求自动 Round-Robin 取下一个:
- 请求 1: sk-A
- 请求 2: sk-B
- 请求 3: sk-C
- 请求 4: sk-A (循环)

### 3.2 失败熔断

- 单 Key 失败计数 >= 3, 自动跳过 5 分钟
- 请求成功后清零计数
- 所有 Key 失败 → reset + 用第一个 (作为最后兜底)

## 4. 协议转换

### 4.1 OpenAI 兼容 (openai / minimax / deepseek / ollama)

```java
ModelProviderAdapter adapter = providerFactory.get("openai");
ChatResponse resp = adapter.chat("https://api.openai.com/v1", apiKey, req);
```

### 4.2 Anthropic Claude (V5.18 新)

```java
ModelProviderAdapter adapter = providerFactory.get("anthropic");
ChatResponse resp = adapter.chat("https://api.anthropic.com", apiKey, req);
```

**协议差异** (AnthropicAdapter 处理):
- Header: `x-api-key` + `anthropic-version: 2023-06-01`
- Body: `{model, messages, max_tokens, system, stream}`
- 响应: `{content:[{type:"text", text:"..."}], stop_reason, usage}`

### 4.3 Google Gemini (V5.18 新)

```java
ModelProviderAdapter adapter = providerFactory.get("gemini");
ChatResponse resp = adapter.chat("https://generativelanguage.googleapis.com", apiKey, req);
```

**协议差异** (GeminiAdapter 处理):
- URL: `?key=API_KEY` (query string, 不是 header)
- Body: `{contents:[{role:"user"/"model", parts:[{text}]}], generationConfig}`
- 响应: `{candidates:[{content:{parts:[{text}]}}], usageMetadata}`

## 5. SQL 初始化

```sql
-- 22_anthropic_gemini.sql (V5.18)
-- 插入 anthropic + gemini 2 个 provider
-- 插入 5 个模型 (3 Claude + 2 Gemini)
```

执行:
```bash
mysql -uroot -pminimax_pass_2024 minimax_platform < sql/22_anthropic_gemini.sql
```

## 6. 端到端测试

### 6.1 真实 OpenAI 调用

```bash
# 1. 启动 model 服务
java -jar minimax-model.jar

# 2. 准备 JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}' | jq -r .data.accessToken)

# 3. 调 OpenAI
curl -X POST http://localhost:3000/api/v1/model/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### 6.2 真实 Claude 调用

```bash
curl -X POST http://localhost:3000/api/v1/model/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-3-5-sonnet-20241022",
    "messages": [{"role": "user", "content": "Explain quantum entanglement in 3 sentences"}]
  }'
```

### 6.3 真实 Gemini 调用

```bash
curl -X POST http://localhost:3000/api/v1/model/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-1.5-flash",
    "messages": [{"role": "user", "content": "What is 2+2?"}]
  }'
```

## 7. 监控

- `minimax.llm.tokens.total` (Counter) - token 用量
- `minimax.llm.latency` (Timer histogram) - 调用延迟
- 失败次数在 `ApiKeyProviderService.failCount` (内存)

## 8. V5.18 新增文件

| 文件 | 用途 |
|------|------|
| `backend/minimax-model/.../provider/AnthropicAdapter.java` | 11KB, Claude 协议 |
| `backend/minimax-model/.../provider/GeminiAdapter.java` | 11KB, Gemini 协议 |
| `backend/minimax-model/.../service/ApiKeyProviderService.java` | 4.7KB, 多 Key 轮询 |
| `backend/minimax-model/.../service/impl/ModelServiceImpl.java` | 改用 ApiKeyProviderService |
| `sql/21_anthropic_gemini.sql` | 新 provider + 模型 (5 条) |
| `docs/LLM-PROVIDER-GUIDE.md` | 本文档 |
