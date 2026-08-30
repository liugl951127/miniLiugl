# LLM Gateway 验证清单 (V9.0)

## 阶段 1 验证 (你按这个跑, 5 分钟)

### 1. 启动 minimax-ai
```bash
cd backend && mvn -pl minimax-ai -am spring-boot:run
```

### 2. 检查 Qwen2.5-0.5B 是否就绪
```bash
curl http://localhost:8090/api/v1/ai/llm/status
```

期望返回:
```json
{
  "fallbackEnabled": true,
  "localReady": true    // 必须是 true!
}
```

如果 `localReady: false`:
- Qwen 模型未下载, 跑 `bash scripts/download-models.sh qwen`
- 模型在 `data/models/`, 应包含 `qwen2.5-0.5b-instruct/`

### 3. 测试 LLM Gateway (无云端配置, 纯本地)
```bash
# 测 1: 纯本地 (不配 LLM_CLOUD_API_KEY, 必走本地)
curl -X POST http://localhost:8090/api/v1/ai/llm/chat \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "用一句话介绍你自己"}
    ]
  }'
```

期望返回 (1-2 秒):
```json
{
  "content": "我是 Qwen...",
  "source": "LOCAL",            // 或 LOCAL_FALLBACK
  "model": "qwen2.5-0.5b-instruct",
  "durationMs": 1500,
  "reason": "云端未配置"
}
```

### 4. 测试兜底 (配错云端 key, 触发兜底)
```bash
export LLM_CLOUD_API_KEY="sk-fake-key-for-testing"
mvn -pl minimax-ai -am spring-boot:run  # 重启
```

再发请求, 期望:
```json
{
  "content": "我是 Qwen...",
  "source": "LOCAL_FALLBACK",    // 自动兜底!
  "model": "qwen2.5-0.5b-instruct",
  "durationMs": 1800,
  "reason": "云端失败"           // 显示兜底原因
}
```

### 5. 测试云端 (配真 key)
```bash
export LLM_CLOUD_API_KEY="sk-你的真-key"
export LLM_PRIMARY_MODEL="gpt-4o-mini"
mvn -pl minimax-ai -am spring-boot:run  # 重启
```

期望:
```json
{
  "content": "I am GPT...",
  "source": "CLOUD",
  "model": "gpt-4o-mini",
  "durationMs": 1200,
  "reason": null
}
```

## 失败兜底矩阵

| 云端 key | Qwen 就绪 | 期望 source |
|---------|----------|-------------|
| 未配置   | 是       | LOCAL       |
| 错 key   | 是       | LOCAL_FALLBACK (reason: 云端失败) |
| 超时     | 是       | LOCAL_FALLBACK (reason: 云端失败) |
| 正确     | 是       | CLOUD      |
| 任何状态  | 否       | UNAVAILABLE (reason: 需下载 Qwen) |
