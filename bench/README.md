# MiniMax Platform — 性能压测报告模板

> V5.9 Day 21 | Gateway: `http://localhost:7080`

---

## 压测目标

| 目标 | 指标 |
|------|------|
| 登录 API | P99 < 200ms, TPS > 500 |
| 会话列表 | P99 < 100ms, TPS > 1000 |
| 模型列表 | P99 < 50ms, TPS > 2000 |
| Gateway 健康 | P99 < 20ms, TPS > 5000 |
| RAG 检索 | P99 < 500ms, TPS > 50 |

---

## 压测环境

```
CPU:       Intel Xeon / Apple M-Series（实际填写）
内存:      16GB / 32GB
网络:      localhost / 1Gbps LAN
Java:      OpenJDK 17
Node:      v20.x
Gateway:   Spring Cloud Gateway (Netty)
Backend:   Spring Boot 3
Database:  MySQL 8 (localhost:3306)
Cache:     Redis 7 (localhost:6379)
```

---

## 压测工具

### wrk（推荐，最轻量）
```bash
# 安装
brew install wrk        # macOS
sudo apt install wrk   # Ubuntu/Debian

# 登录 API 压测
wrk -t4 -c100 -d60s -s wrk/login.lua http://localhost:7080 --latency

# 带 Token 压测（聊天 API）
WRK_TOKEN=your_token WRK_SESSION_ID=1 \
  wrk -t4 -c50 -d60s -s wrk/chat.lua http://localhost:7080 --latency
```

### Apache Bench
```bash
ab -n 5000 -c 100 -p login.json -T "application/json" \
   http://localhost:7080/api/v1/auth/login
```

### JMeter（完整场景）
```bash
# GUI 模式
jmeter -t jmeter/minimax-api-test.jmx

# CLI 无头模式
jmeter -n -t jmeter/minimax-api-test.jmx \
  -l result.jtl -e -o html-report \
  -JGATEWAY_URL=http://localhost:7080
```

---

## 压测脚本说明

| 脚本 | 用途 | 并发建议 |
|------|------|---------|
| `wrk/login.lua` | 登录 API 压测 | 10~200 |
| `wrk/chat.lua` | 聊天 API 压测（需 Token） | 10~50 |
| `jmeter/minimax-api-test.jmx` | 完整 API 场景（6 个端点） | 10/50/200 |
| `run.sh` | 一键执行（支持 wrk/ab/JMeter） | 自动 |

### run.sh 用法
```bash
# 单工具压测
./bench/run.sh wrk
./bench/run.sh ab
./bench/run.sh jmeter

# 全部运行
./bench/run.sh all

# 自定义参数
GATEWAY=http://prod-api.example.com CONNECTIONS=200 DURATION=120 ./bench/run.sh wrk
```

---

## 压测维度说明

### 1. 基准测试（Baseline）
- 并发: 10
- 持续: 60s
- 目标: 建立性能基准，识别单用户延迟

### 2. 负载测试（Load）
- 并发: 50
- 持续: 120s
- 目标: 验证 SLA，识别性能拐点

### 3. 压力测试（Stress）
- 并发: 100 → 200 → 500（梯度）
- 持续: 180s
- 目标: 找到系统上限，观察限流行为

### 4. 持久连接测试
- wrk 默认 HTTP/1.1 keepalive
- 可加 `--latency` 看延迟分布

---

## 关键指标解读

| 指标 | 优秀 | 良好 | 警告 |
|------|------|------|------|
| Latency P50 | < 20ms | < 50ms | > 100ms |
| Latency P99 | < 100ms | < 200ms | > 500ms |
| Latency P999 | < 300ms | < 500ms | > 1s |
| Error Rate | < 0.1% | < 1% | > 5% |
| Throughput | > 2000 rps | > 500 rps | < 100 rps |

---

## 报告填写

### 压测结果（实际执行后填写）

| 端点 | 并发 | TPS | P50 | P95 | P99 | 错误率 |
|------|------|-----|-----|-----|-----|--------|
| 登录 | 100 | | | | | |
| 会话列表 | 50 | | | | | |
| 模型列表 | 100 | | | | | |
| RAG 检索 | 20 | | | | | |
| 健康检查 | 200 | | | | | |

### 问题记录

1. **[限流触发]** 描述...
2. **[JVM GC]** 描述...
3. **[连接池耗尽]** 描述...

---

## 优化建议

- [ ] 热点接口增加 Redis 缓存
- [ ] 限流阈值调优（参考监控指标）
- [ ] 数据库连接池参数优化
- [ ] 网关 filter 链精简
- [ ] Nacos 服务发现延迟优化
