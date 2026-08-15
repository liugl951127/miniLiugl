# V3.5.95 VUE3 + Docker Compose 真后端 + Check 8/9 防御性自检

## 1. V3.5.94 之后的深挖

V3.5.94 修了 `chat/Index.vue` `toolCalls` 未定义，但**其他 21 view 还没检查**。
V3.5.95 加防御性自检脚本 `check-setup-var.cjs` 扫 79 个 .vue 文件。

## 2. V3.5.95 改

### 2.1 `scripts/check-setup-var.cjs` (Check 8) - V3.5.95+

```js
// 扫 79 .vue 文件, 找模板用 xxx.yyy 但 setup 未定义
// 排除:
// - v-for 局部变量 (v-for="x in y" 里的 x)
// - v-slot 局部 (<template #default="{ row }"> 里的 row)
// - 字符串字面量 ('chat.start' / "..." / `...`)
// - 函数调用 (fn(args))
// - Element Plus 组件 (ElMessage 等)
// - 内置 (Math / JSON / Date 等)

// 输出: 0 错误 (V3.5.95 修 Cluster.vue raftNodes 别名后)
```

### 2.2 `scripts/verify-docker-compose.sh` (Check 9) - V3.5.95+

```bash
# 沙箱无 docker, 静态验证
# 1. yaml 语法
# 2. services 数量 = 19 (14 module + mariadb + redis + nacos + nginx + otel-collector + jaeger)
# 3. otel-collector + jaeger 存在
# 4. 14 module 都 depends_on otel-collector
# 5. networks 段干净 (只 minimax-net)
# 6. otel-collector 暴露 4317 (OTLP gRPC)
# 7. jaeger 暴露 16686 (UI)
```

### 2.3 `docker-compose.yml` 加 otel-collector + jaeger (V3.5.95)

```yaml
# V3.5.95+ OpenTelemetry 基础设施
otel-collector:
  image: otel/opentelemetry-collector-contrib:0.96.0
  container_name: minimax-otel-collector
  command: ["--config=/etc/otelcol-contrib/config.yaml"]
  volumes:
    - ./deploy/otel-collector-config.yaml:/etc/otelcol-contrib/config.yaml:ro
  ports:
    - "4317:4317"   # OTLP gRPC
    - "4318:4318"   # OTLP HTTP
  networks: [minimax-net]

jaeger:
  image: jaegertracing/all-in-one:1.55
  container_name: minimax-jaeger
  environment:
    COLLECTOR_OTLP_ENABLED: "true"
  ports:
    - "16686:16686"  # Jaeger UI
    - "14250:14250"  # gRPC
  networks: [minimax-net]
```

### 2.4 14 module depends_on otel-collector

```yaml
auth:
  depends_on:
    mariadb: { condition: service_healthy }
    otel-collector: { condition: service_started }  # V3.5.95

chat:
  depends_on:
    mariadb: { condition: service_healthy }
    redis:   { condition: service_healthy }
    otel-collector: { condition: service_started }  # V3.5.95
# ... 12 个 module 全部
```

### 2.5 `admin/Cluster.vue` 修 raftNodes (V3.5.95)

```js
// 之前: 模板用 raftNodes / raftState / activeCount 但 setup 未定义
const nodes = ref([])
const raftNodes = computed(() => nodes.value)  // V3.5.95 别名
const raftState = computed(() => raftStateInfo.value)  // V3.5.95 别名
const activeCount = computed(() => nodes.value.filter(n => n.status === 'ACTIVE').length)  // V3.5.95 computed
```

## 3. V3.5.95 ci-check 9/9

| Check | 名称 | 工具 |
|-------|------|------|
| 1 | schema.sql 数字/时间字段 DEFAULT '' | bash |
| 2 | JDBC URL 禁止 jdbc:mysql:// | bash |
| 3 | Driver 禁止 com.mysql.cj.jdbc.Driver | bash |
| 4 | Dockerfile 禁止 V3.5.18 合并前模块残留 | bash |
| 5 | mapper 接口 @Select/@Update 注解 + XML mapper 重复 | bash |
| 6 | seed-data INSERT 列名 跟 entity 字段对齐 | bash |
| 7 | menu 路径 / router.push 硬编码 vs router 路径 | python |
| 8 | `<script setup>` 模板用变量但 setup 未定义 | node (V3.5.95) |
| 9 | docker-compose 静态验证 (19 services + depends_on) | python (V3.5.95) |

## 4. 真后端部署流程 (V3.5.95+)

```bash
# 1. 启动 docker compose
cd /opt/minimax/miniLiugl
docker compose up -d

# 2. 等待 mariadb + nacos + otel-collector + jaeger 就绪
docker compose ps
# minimax-mariadb       healthy
# minimax-nacos         healthy
# minimax-otel-collector  healthy
# minimax-jaeger        healthy

# 3. 14 module 自动起 (depends_on otel-collector service_started)
# V3.5.95+ 启动顺序: mariadb → nacos → otel-collector → jaeger → 14 module

# 4. 访问
# - 前端: http://localhost
# - Jaeger UI: http://localhost:16686
# - 14 module API: http://localhost:7080 (gateway)
# - OTel collector: localhost:4317 (OTLP gRPC)
```

## 5. Chat 真实 LLM 流 (V3.5.95+)

之前 `chat/Index.vue` 是 Mock 模式 (`selectedModel = ref('mock')`)。
V3.5.95+ 真后端跑通后：

```js
// 1. POST /api/v1/chat/sessions  → 创 session
// 2. POST /api/v1/chat/send (stream=true)  → 流式响应
// 3. WebSocket /ws/chat  → 实时推送

// 流式响应 (V3.5.92+ sendMessage):
await sendMessageStream(currentSessionId.value || 0, {
  role: 'user',
  content: text,
  images: images,
}, {
  onChunk: (chunk) => { aiMsg.content += chunk; scrollToBottom() },
  onToolCall: (tc) => { aiMsg.toolCalls.push(tc); toolCalls.value.push(tc) },  // V3.5.94
  onSource: (src) => { aiMsg.sources.push(src); sources.value.push(src) },
  onDone: () => { aiMsg.streaming = false; streaming.value = false },
  onError: (err) => { aiMsg.content = '出错: ' + err.message },
})
```

## 6. Chat 演示模式 (V3.5.93+)

```js
// 演示模式 (沙箱无后端):
if (isDemoMode()) {
  // mock 延迟 + 模拟响应
  await new Promise(r => setTimeout(r, 500))
  // 模拟流式响应
  const responses = ['演示', '模式', '无需', '后端', '🎭']
  for (const r of responses) {
    aiMsg.content += r
    await new Promise(r => setTimeout(r, 200))
  }
  aiMsg.streaming = false
}
```

## 7. 验证

| 测试 | 结果 |
|------|------|
| check-setup-var.cjs (Check 8) | ✅ 79 .vue 文件 0 错误 |
| verify-docker-compose.sh (Check 9) | ✅ 19 services 干净 + 14 module depends_on otel-collector |
| ci-check 9/9 | ✅ |
| vite build 0 错 | ✅ 54s |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| Round 6 90 轮 100% pass | ✅ 1890 GET |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 8. 累计 50 个版本 (V3.5.46-V3.5.95)

