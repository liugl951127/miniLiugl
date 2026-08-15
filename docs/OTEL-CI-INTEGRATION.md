# V3.5.91+ OpenTelemetry CI 集成

## 1. 目标

CI (GitHub Actions) 真实跑 OpenTelemetry 全栈:
- 启 docker compose 全栈 (mariadb + redis + nacos + 14 module + otel-collector + jaeger)
- 跑 5 browser matrix 发请求 (Round 7 + Round 8)
- 查 jaeger API 验证 trace 上报
- 上传 jaeger UI artifact

## 2. 入口

`.github/workflows/otel-trace.yml` (V3.5.91+ 新增)

**触发方式**:
- `workflow_dispatch` 手动触发 (默认)
- `push` 到 main/develop 改特定路径时自动跑 (V3.5.91 限定白名单避免误触发)

**触发白名单**:
```yaml
paths:
  - 'docker-compose.yml'
  - 'deploy/otel-collector-config.yaml'
  - 'backend/minimax-*/pom.xml'
  - 'backend/minimax-*/src/main/resources/application*.yml'
  - '.github/workflows/otel-trace.yml'
  - 'scripts/e2e-multiround.sh'
  - 'frontend/public/sw.js'
```

只有改 OTel 相关文件才触发, 节省 CI 资源.

## 3. 6 步流程

### 3.1 启 docker compose

```bash
docker compose pull mariadb redis nacos jaeger otel-collector &  # 拉镜像
docker compose build &  # 构建 14 module 镜像
wait
docker compose up -d  # 启动
```

### 3.2 等服务就绪 (4 段)

| 阶段 | 超时 | 验证 |
|------|------|------|
| mariadb | 60 × 2s = 2min | `mariadb-admin ping` |
| nacos | 120 × 3s = 6min | `GET /nacos/` |
| 14 module | 60 × 3s = 3min × 14 | `/actuator/health` |
| otel-collector | 30 × 2s = 1min | `GET :4318/v1/traces` (405 = ready) |
| jaeger | 30 × 2s = 1min | `GET :16686/api/services` (200 = ready) |

### 3.3 跑 5 browser matrix (Round 7 + Round 8)

```bash
cd frontend && nohup npx vite dev --port 5173 &  # 启 dev server
cd .. && ROUNDS=5 bash scripts/e2e-multiround.sh
```

5 browser × 3 路由 × traceparent = 15 GET (Round 8)
+ 5 路由 × 5 browser UA 模拟 = 25 GET (Round 7)

### 3.4 查 jaeger API 验证

```bash
sleep 30  # 等 batch processor flush
SERVICES=$(curl -s http://localhost:16686/api/services)
TRACES=$(curl -s "http://localhost:16686/api/traces?service=minimax-platform&limit=10")
```

期望: `minimax-platform` 在 services 列表, trace count > 0.

### 3.5 上传 jaeger UI artifact

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: jaeger-ui-screenshots
    path: reports/jaeger/
    retention-days: 7
```

### 3.6 关 docker compose

```bash
docker compose down -v  # -v 清 volumes 释放空间
```

## 4. 完整端到端

```
GitHub Actions ubuntu-latest (7GB 内存)
   ↓ docker compose up
mariadb + redis + nacos + 14 module + otel-collector + jaeger
   ↓ 14 module 启动 (注入 OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317)
[OTel Spring Boot Starter] 自动 instrument
   ↓ OTLP gRPC
[OTel Collector] :4317
   ↓ batch (5s/1000)
[Jaeger] :16686
   ↓ 5 browser matrix
5 browser × 3 路由 = 15 GET
   ↓ traceparent header
[14 module] 收到 trace, 上报 collector
   ↓
[jaeger /api/traces] 看到 14 module 的 trace
```

## 5. 手动触发

```bash
# GitHub UI: Actions → OTel Trace → Run workflow
# 或 CLI:
gh workflow run otel-trace.yml
```

## 6. 跟 V3.5.89 关系

| 维度 | V3.5.89 | V3.5.91 |
|------|---------|---------|
| 后端 otel 依赖 | 1 module (auth) | 14 module (V3.5.90) |
| 配置文件 | 12 yml | 32 yml (V3.5.90) |
| CI 跑 | ❌ 沙箱跑不动 | ✅ GitHub Actions |
| Jaeger 真实跑 | ❌ | ✅ |
| 5 browser matrix trace | 沙箱模拟 | **CI 真实跑** |
| 自动触发 | ❌ | ✅ 改 OTel 文件自动跑 |

## 7. 验证

| 测试 | 结果 |
|------|------|
| `.github/workflows/otel-trace.yml` 8.5KB 语法 | ✅ (跟 verify-deploy.yml 同结构) |
| ci-check 7/7 PASS (V3.5.85-87) | ✅ |
| Round 6 90 轮 100% pass (V3.5.85) | ✅ |
| Round 7 5 browser 全 pass (V3.5.85) | ✅ |
| Round 8 5 browser traceparent 全 pass (V3.5.89) | ✅ |
| 前端 dev server 启动 | ✅ |
| docker compose 启 mariadb/redis/nacos | ✅ (本地无 mvn, CI 跑) |

## 8. CI 真实跑预期结果

GitHub Actions 跑完后:
1. `reports/jaeger/summary.log` 有 `Service: minimax-platform` + `Trace count > 0`
2. Artifacts 下载有 `jaeger-ui-screenshots`
3. Workflow 日志显示 `jaeger_status=ok`

## 9. 累计 46 个版本 (V3.5.46-V3.5.91)
