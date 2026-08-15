# MiniMax Platform V3.5.21 操作手册 (增量)

> 基于 V3.5.19 (代码优化) + V3.5.20 (一致性修复) + V3.5.21 (深度一致性 + 文档同步)
> 完整操作手册见 `docs/OPERATIONS.md`, 本文档是 V3.5.21 增量

## 一、5 分钟快速启动 (V3.5.21 一致性修复后)

### 1.1 沙箱模式 (单服务 256MB 即可)

```bash
# 环境检查
java -version  # Java 17
mvn -v         # Maven 3.9+

# 启动任一服务
cd backend/minimax-auth
nohup $JAVA_HOME/bin/java -Xmx192m -jar target/minimax-auth-spring-boot.jar \
    --spring.profiles.active=h2local \
    --spring.cloud.nacos.discovery.enabled=false \
    --spring.cloud.nacos.config.enabled=false \
    --otel.metrics.exporter=none --otel.traces.exporter=none --otel.logs.exporter=none \
    --otel.java.global-autoconfigure.enabled=false \
    --management.tracing.enabled=false \
    --server.port=8081 < /dev/null > /tmp/auth.log 2>&1 &
disown
```

### 1.2 一键启停 13 微服务 (V3.5.19 重生)

```bash
# 启动所有 13 微服务
bash scripts/start-all.sh start

# 状态查询 (HTTP code + PID)
bash scripts/status.sh

# 停止所有
bash scripts/stop-all.sh

# 重启
bash scripts/start-all.sh restart
```

### 1.3 5 测试账号 (V3.5.21 一致性 100%)

| 账号 | 密码 | 角色 | 来源 |
|------|------|------|------|
| `adminLiugl` | `Liugl@2026` | SUPER_ADMIN | AdminDataInitializer @Value + seed.sql + e2e |
| `admin` | `admin@123` | ADMIN | AdminDataInitializer + seed.sql + e2e |
| `admin_user` | `admin123` | ADMIN | 3 源一致 |
| `test_user` | `user123` | USER | 3 源一致 |
| `demo_user` | `demo1234` | USER | 3 源一致 |

### 1.4 13 微服务端口 (V3.5.20 一致性 100%)

| 服务 | 端口 | 备注 |
|------|------|------|
| gateway | 7080 | Spring Cloud Gateway (Nacos lb://) |
| auth | 8081 | 5 账号 BCrypt 兜底 |
| chat | **8082** | 含 memory_ext (V3.5.18 合并) |
| model | **8084** | 含 prompt 子包 (V3.5.18 合并) |
| rag | 8085 | 知识库 RAG |
| multimodal | 8087 | ONNX 多模态 |
| agent | 8088 | AI Agent 编排 |
| monitor | 8089 | 服务监控 |
| admin | 8090 | 后台管理 |
| analytics | 8092 | 数据分析 |
| pipeline | **8093** | 含 function_ext (V3.5.18 合并) |
| ai | 8094 | 4 模型加权 + MiniTransformer |
| ws | 8095 | WebSocket (8082 deprecated) |

## 二、Nginx 单端口 80 接入 (V3.5.19 重生)

```bash
# 部署单文件配置
cp deploy/nginx-v3519.conf /etc/nginx/conf.d/minimax.conf
nginx -s reload

# 验证
curl http://localhost/api/v1/ai/route -X POST -H "Content-Type: application/json" -d '{"query":"生成柱状图"}'
```

### 2.1 路由规则 (V3.5.18+ 合并后)

```
/api/v1/auth/        → minimax-auth      :8081
/api/v1/chat/        → minimax-chat      :8082
/api/v1/memory/      → minimax-chat      :8082   ← 合并
/api/v1/model/       → minimax-model     :8084
/api/v1/prompts/     → minimax-model     :8084   ← 合并
/api/v1/rag/         → minimax-rag       :8085
/api/v1/multimodal/  → minimax-multimodal:8087
/api/v1/agent/       → minimax-agent     :8088
/api/v1/monitor/     → minimax-monitor   :8089
/api/v1/admin/       → minimax-admin     :8090
/api/v1/analytics/   → minimax-analytics :8092
/api/v1/pipeline/    → minimax-pipeline  :8093
/api/v1/function/    → minimax-pipeline  :8093   ← 合并
/api/v1/ai/          → minimax-ai        :8094
/ws/                 → minimax-ws        :8095   (WebSocket Upgrade)
/api/* (其他)        → minimax-gateway   :7080   (兜底)
```

## 三、SQL 重灌 (V3.5.19 重生)

```bash
# 重新生成 schema (77 表 1652 行)
python3 scripts/regen-schema.py

# 应用 schema + seed
mysql -uroot -proot123456 < sql/v3.5.19-schema.sql
mysql -uroot -proot123456 < sql/v3.5.19-seed.sql

# 沙箱模式 (h2) 不需要 SQL, AdminDataInitializer 兜底 5 账号
```

### 3.1 一致性保证 (V3.5.20)

- 实体 @TableName 77 = SQL CREATE TABLE 77 ✓
- 字段类型自动推断 (java.util.* → JSON, Long → BIGINT) ✓
- snake_case 命名 (`map-underscore-to-camel-case: true`) ✓

## 四、常见问题 (V3.5.21)

### Q1: 启动报 "Table not found" 
**A**: 沙箱模式 (h2local) 启动会跑 `spring.sql.init` 自动建表; MySQL 模式需先 `mysql < sql/v3.5.19-schema.sql`

### Q2: 端口 8083 / 8086 / 8091 不可用
**A**: V3.5.18 已合并 memory→chat(8082) / function→pipeline(8093) / prompt→model(8084), 旧端口废弃

### Q3: e2e-multiround.sh 跑 0 pass
**A**: 先 `bash scripts/start-all.sh start`, 等 60s Spring Boot 启动, 再跑 e2e

### Q4: 5 账号密码不对
**A**: 检查 3 源 (AdminDataInitializer + seed.sql + e2e-multiround.sh) 一致, BCrypt 10 rounds 加密

## 五、监控与告警

```bash
# actuator 健康
curl http://localhost:7080/actuator/health

# Prometheus 指标
curl http://localhost:7080/actuator/prometheus

# 链路追踪 (OpenTelemetry)
curl http://localhost:7080/actuator/traces
```
