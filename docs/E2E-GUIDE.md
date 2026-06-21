# MiniMax E2E 测试指南 (V5.15)

> V5.15 引入完整端到端自动化测试, 覆盖: 健康检查 / JWT 鉴权 / 跨服务调用 / TraceId / Prometheus 指标

## 1. 一句话

```bash
sudo ./scripts/deploy-minimax.sh e2e-full    # 跑全部 7 个 Phase
```

## 2. 两种模式

### 2.1 快速模式 (`e2e`)

```bash
sudo ./scripts/deploy-minimax.sh e2e
# 或
./scripts/e2e-full-test.sh --quick
```

只跑 Phase 1+2 (基础设施 + 13 服务健康检查), 适合部署后立即验证。

### 2.2 完整模式 (`e2e-full`)

```bash
sudo ./scripts/deploy-minimax.sh e2e-full
# 或
./scripts/e2e-full-test.sh --full
```

跑全部 7 个 Phase, 包含真实业务调用。

## 3. 7 个 Phase

| Phase | 内容 | 期望 |
|-------|------|------|
| 1 | 基础设施 (nginx, nacos, redis, mariadb) | HTTP 200/302/401 |
| 2 | Gateway + 12 微服务健康检查 | HTTP 200/401 |
| 3 | JWT 鉴权 (未登录/登录/带 token) | 401 → token → 200 |
| 4 | 跨服务调用 (admin/monitor/chat/model/rag) | HTTP 200 |
| 5 | TraceId 透传 (V5.14 OTel W3C) | 自定义 traceId 回传 |
| 6 | Prometheus 指标 (V5.10) | `/actuator/prometheus` 含 `minimax_http_*` |
| 7 | 错误码一致性 (V5.8) | 404/401/200 符合预期 |

## 4. 自定义参数

```bash
# 自定义入口
BASE=http://staging.example.com:3000 ./scripts/e2e-full-test.sh

# 直连 gateway (绕过 nginx)
GATEWAY=http://localhost:8080 ./scripts/e2e-full-test.sh

# 自定义 Nacos
NACOS=http://nacos.internal:8848 ./scripts/e2e-full-test.sh

# 自定义账号
ADMIN_USER=adminLiugl ADMIN_PASS=Liugl@2026 ./scripts/e2e-full-test.sh
```

## 5. 示例输出

```
════════════════════════════════════════════════════════════
  MiniMax V5.15 端到端测试
  BASE:     http://localhost:3000
  GATEWAY:  http://localhost:8080
  NACOS:    http://localhost:8848
  用户:     adminLiugl
════════════════════════════════════════════════════════════

══════════ Phase 1: 基础设施健康检查 ══════════
[✓]  nginx 入口可达 (HTTP 200)
[✓]  api-docs 重定向 (HTTP 302)
[✓]  actuator/health (HTTP 200)
[✓]  nacos 控制台 (HTTP 200)

══════════ Phase 2: 13 服务健康检查 ══════════
[✓]  gateway :8080 (HTTP 200)
[✓]  auth (HTTP 200)
[✓]  chat (HTTP 200)
... (12 个)

══════════ Phase 3: JWT 鉴权测试 ══════════
[✓]  未鉴权访问 /me (HTTP 401)
[TEST] 登录 adminLiugl ...
[✓]  登录成功, token=eyJhbGciOiJIUzI1NiIs...
[✓]  带 token 访问 /me (HTTP 200)

══════════ Phase 4: 跨服务调用 ══════════
[✓]  admin dashboard (HTTP 200)
[✓]  monitor 健康 (HTTP 200)
[✓]  monitor 指标 (HTTP 200)
[✓]  monitor 告警 (HTTP 200)
[✓]  monitor 告警规则 (HTTP 200)
[✓]  chat 会话列表 (HTTP 200)
[✓]  model 模型列表 (HTTP 200)
[✓]  rag 知识库列表 (HTTP 200)

══════════ Phase 5: TraceId 验证 (V5.14 OTel) ══════════
[✓]  TraceId 透传成功: test1234567890abc
[✓]  W3C traceparent 存在: 00-7465737431...

══════════ Phase 6: Prometheus 指标 (V5.10) ══════════
[✓]  Prometheus 端点有 minimax_http_requests_total 指标

══════════ Phase 7: 错误响应格式 ══════════
[✓]  404/401/200 返回 (实际 404, 符合预期)

══════════ 测试结果 ══════════

  TEST CASE                                 RESULT
  ----------                                ------
  ✓ nginx 入口可达                           200
  ✓ auth                                    200
  ✓ 登录成功                                 got token
  ✓ TraceId 透传                           
  ✓ W3C traceparent W3C
  ... (30+ 项)

  ══════════════════════════════════
  ✓ 全部通过: 35 / 35
  ══════════════════════════════════
```

## 6. CI/CD 集成

### 6.1 GitHub Actions

```yaml
- name: E2E test
  run: |
    sudo ./scripts/deploy-minimax.sh install
    sleep 30
    sudo ./scripts/deploy-minimax.sh e2e-full
```

### 6.2 Jenkins

```bash
stage('E2E') {
    steps {
        sh 'sudo ./scripts/deploy-minimax.sh install'
        sh 'sleep 30 && sudo ./scripts/deploy-minimax.sh e2e-full'
    }
}
```

## 7. 故障排查

### 7.1 登录失败

**症状**: `[✗] 登录失败 (无法获取 token)`

**排查**:
1. 检查 MariaDB 是否初始化 `adminLiugl` 用户:
   ```sql
   SELECT username, status FROM sys_user WHERE username='adminLiugl';
   ```
2. 检查密码 (默认 `Liugl@2026`):
   ```bash
   grep "Liugl@2026" sql/16_super_admin.sql
   ```
3. 直连 auth 测试:
   ```bash
   curl -X POST http://localhost:8081/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"adminLiugl","password":"Liugl@2026"}'
   ```

### 7.2 TraceId 未透传

**症状**: `[!] TraceId 未回传`

**排查**:
1. 检查 nginx 是否过滤:
   ```nginx
   proxy_pass_header X-Trace-Id;  # 默认会传, 但 nginx 0.7+ 默认可能过滤
   ```
2. 检查 gateway TraceFilter (V5.14 升级):
   ```java
   response.getHeaders().add("X-Trace-Id", traceId);
   ```

### 7.3 Prometheus 端点无指标

**症状**: `[✗] Prometheus 端点无业务指标`

**排查**:
1. 检查 common yml 是否有 management 配置 (V5.10):
   ```yaml
   management.endpoints.web.exposure.include: health,info,metrics,prometheus
   ```
2. 检查依赖 (V5.10):
   ```xml
   <dependency>
     <groupId>io.micrometer</groupId>
     <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

## 8. 与 V5.12 `e2e` 的关系

| 子命令 | 用途 | 实现 |
|--------|------|------|
| `e2e` (V5.12) | 快速健康检查 | inline shell 函数 |
| `e2e-full` (V5.15) | 完整端到端测试 | 调用 `e2e-full-test.sh` |

**`e2e-full` 内部会先跑 `e2e-full-test.sh --quick` (≈ V5.12 e2e), 再跑 `--full`**

## 9. V5.15 新增文件

| 文件 | 用途 |
|------|------|
| `scripts/e2e-full-test.sh` | 10KB, 7 Phase + 35+ 测试用例 |
| `scripts/deploy-minimax.sh` | 加 `e2e-full` 子命令 |
| `docs/E2E-GUIDE.md` | 本文档 |
