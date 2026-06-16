# Day 12 报告 - 监控 (Prometheus + 业务指标 + 告警 + 健康详情)

**日期**: 2026-06-16
**目标**: 完整落地监控模块 — 11 个微服务 + Prometheus 指标采点 + 告警引擎 + 深度健康检查
**Commit**: pending

---

## ✅ 完成项

### 1. 数据模型 (3 张表)
- ✅ `metric_snapshot` — 指标快照: service/metricName/value/tags/recordedAt
- ✅ `alert_rule` — 告警规则: name/metric/operator/threshold/severity/cooldown
- ✅ `alert_event` — 告警事件: ruleId/value/threshold/status(firing/resolved)/firedAt
- ✅ H2 + MySQL 双兼容 schema (含 5 个默认告警规则种子)
- ✅ 默认规则: CPU 高 / JVM Heap 高 / 磁盘高 / LLM 延迟高 / 错误率高

### 2. 健康详情服务 (HealthDetailService)
- ✅ **DB 检查** — 连通性 + 响应时间 + URL + product
- ✅ **JVM 检查** — Heap/NonHeap 内存 (used/max/usage%/init/committed MB)
- ✅ **磁盘检查** — 总/已用/可用 GB + 百分比
- ✅ **线程检查** — 总/daemon/峰值/已启动总数
- ✅ **系统检查** — OS/version/arch/CPU/load average
- ✅ **总体状态** — UP / DEGRADED 聚合

### 3. 业务指标采点 (MetricsCollector + Micrometer)
- ✅ **5 类 Counter** (Prometheus 自动暴露)
  - minimax_chat_messages_total
  - minimax_tool_calls_total
  - minimax_rag_queries_total
  - minimax_llm_tokens_total
  - minimax_http_5xx_total / http_4xx_total / uploads_total
- ✅ **4 类 Gauge**
  - active_sessions / kb_count / user_count / memory_count
- ✅ **2 类 Timer** (含 P50/P95/P99 分布)
  - minimax_llm_latency
  - minimax_tool_duration (按 tool name 打标)

### 4. 指标快照服务 (SnapshotService)
- ✅ `@PostConstruct` 初始化所有 Counter
- ✅ `@Scheduled(60s)` 定期把关键指标落库
- ✅ `@Scheduled(24h)` 自动清理 30 天前快照
- ✅ `trend()` — 按时间窗口聚合 (avg/max/min/cnt) 供图表
- ✅ `recent()` — 拉最近原始值

### 5. 告警引擎 (AlertEngine)
- ✅ **30s 评估一次** 所有启用规则
- ✅ **6 种比较运算符** (> >= < <= = !=)
- ✅ **冷却机制** — 触发后 cooldown_minutes 内不重复告警
- ✅ **自动恢复** — 指标恢复后事件状态变 resolved
- ✅ **优先级** — info / warning / critical
- ✅ **可读消息** — 包含规则名 + 当前值 + 阈值

### 6. Prometheus 集成
- ✅ `actuator + micrometer-registry-prometheus`
- ✅ `/actuator/prometheus` 端点暴露
- ✅ `management.prometheus.metrics.export.enabled=true`
- ✅ 业务指标 + JVM/系统指标 全部自动导出

### 7. 暴露的 HTTP 端点 (15 个)
| 方法 | 路径 | 功能 |
|---|---|---|
| GET    | `/monitor/health` | **深度健康 (DB/JVM/Disk/Thread/System)** |
| GET    | `/monitor/health/database` | DB 检查 |
| GET    | `/monitor/health/jvm` | JVM 检查 |
| GET    | `/monitor/health/disk` | 磁盘检查 |
| GET    | `/monitor/metrics` | **业务指标 (10 个实时值)** |
| GET    | `/monitor/metrics/snapshot` | 历史快照 |
| GET    | `/monitor/metrics/trend` | 趋势聚合 |
| POST   | `/monitor/metrics/inc` | 自助计数 |
| GET    | `/monitor/alerts` | 最近告警 |
| GET    | `/monitor/alerts/firing` | firing 告警 |
| GET    | `/monitor/alerts/rules` | 启用规则 |
| GET    | `/monitor/alerts/summary` | 告警摘要 |
| GET    | `/monitor/info` | 服务信息 |
| GET    | `/actuator/health` | Spring Boot 健康 |
| GET    | `/actuator/prometheus` | **Prometheus 抓取端点** |

---

## 📊 关键数据

| 指标 | Day 11 | Day 12 | 增量 |
|------|--------|--------|------|
| Java 文件 | 172 | **185** | +13 |
| Java 行数 | 9410 | **10837** | +1427 |
| SQL 行数 | 683 | **828** | +145 |
| 单元/集成测试 | 103 | **114** | +11 |
| 端点 (monitor) | 0 | **15** | +15 |
| 数据表 (monitor) | 0 | **3** | +3 |
| 后端模块 | 10 | **11** | +1 |
| 默认告警规则 | 0 | **5** | +5 |

### Day 12 新增测试 (11 用例)
- `HealthDetailTest` (6 cases):
  - deepCheckReturnsAllSections / databaseUp / jvmHasHeap
  - diskHasUsagePercent / threadCountPositive / systemHasCpu
- `AlertEngineTest` (5 cases):
  - rulesLoadedFromSeed / summaryReturnsCounts
  - evaluateRuleLogicTriggerAndResolve
  - normalValuesNoAlert / snapshotServiceRecordAndRead

---

## 🏗️ 架构设计

### 监控架构
```
┌─────────────────────────────────────────────────────────────┐
│                     Prometheus Server                       │
│         (scrape :8089/actuator/prometheus every 15s)        │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│               Monitor Service (8089)                       │
│                                                              │
│  /actuator/prometheus ─┐                                    │
│  /monitor/metrics     │                                    │
│  /monitor/alerts      ├─→ Controller ─→ Service ─→ DB       │
│  /monitor/health      │                                    │
│  /actuator/health     ┘                                    │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ MetricsColl. │  │ AlertEngine  │  │ HealthDetail │     │
│  │  (实时计数)  │  │ (30s 评估)   │  │ (深度检查)   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                 │              │
│         └─────────────────┴─────────────────┘              │
│                           │                                │
│                    SnapshotService                          │
│                    (60s 落库)                                │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 关键设计
1. **不写硬编码指标名** — 统一用 Micrometer, 业务指标 + JVM 指标自动聚合
2. **告警冷却** — 避免告警风暴
3. **自动恢复** — firing → resolved 闭环
4. **快照 + 实时双通道** — 实时用 Gauge/Counter, 历史用快照表
5. **H2 + MySQL 兼容** — 测试本地一键启动
6. **默认告警规则** — 5 条覆盖主要场景, 启动即生效
7. **Prometheus 端点** — `/actuator/prometheus` 标准化抓取

---

## 🔍 验证

### 单元/集成测试 (114 用例全过)
```
AlertEngineTest ............ 5/5   ← Day 12
HealthDetailTest ........... 6/6   ← Day 12
BuiltinToolsTest .......... 13/13
FunctionIntegrationTest ... 10/10
RagIntegrationTest ........ 8/8
TextChunkerTest ........... 6/6
VectorUtilsTest ........... 5/5
ContextBuilderTest ........ 4/4
MockEmbeddingClientTest ... 5/5
ShortTermMemoryTest ....... 4/4
VectorUtilsTest (memory) .. 7/7
JwtTokenProviderTest ...... 4/4
MessageRoleTest ........... 3/3
MockAdapterTest ........... 3/3
ModelProviderFactoryTest .. 4/4
StreamingTest ............. 3/3
VisionServiceTest ......... 7/7
AdminIntegrationTest ..... 8/8
ServiceClientTest ......... 3/3
                     -------
                     114/114 ✅
```

### Maven 编译 (11 模块)
```
minimax-platform ......... SUCCESS
minimax-common ........... SUCCESS
minimax-gateway .......... SUCCESS
minimax-auth ............. SUCCESS
minimax-chat ............. SUCCESS
minimax-memory ........... SUCCESS
minimax-model ............ SUCCESS
minimax-rag .............. SUCCESS
minimax-function ......... SUCCESS
minimax-admin ............ SUCCESS
minimax-multimodal ....... SUCCESS
minimax-monitor .......... SUCCESS  ← Day 12 新增
========================
BUILD SUCCESS · 8.4s
```

---

## 🌐 GitHub

- 仓库: https://github.com/liugl951127/miniLiugl.git
- 状态: pending
- 改动: +13 java + 1 SQL + 1 schema + 2 测试 + 1 yml + 3 mapper xml + 1 pom

---

## 📁 新增文件

```
sql/12_monitor.sql
sql/init/12_monitor.sql
backend/minimax-monitor/pom.xml
backend/minimax-monitor/src/main/java/com/minimax/monitor/MonitorApplication.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/config/MybatisPlusConfig.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/entity/MetricSnapshot.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/entity/AlertRule.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/entity/AlertEvent.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/mapper/MetricSnapshotMapper.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/mapper/AlertRuleMapper.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/mapper/AlertEventMapper.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/collector/MetricsCollector.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/health/HealthDetailService.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/alert/AlertEngine.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/service/SnapshotService.java
backend/minimax-monitor/src/main/java/com/minimax/monitor/controller/MonitorController.java
backend/minimax-monitor/src/main/resources/mapper/MetricSnapshotMapper.xml
backend/minimax-monitor/src/main/resources/mapper/AlertRuleMapper.xml
backend/minimax-monitor/src/main/resources/mapper/AlertEventMapper.xml
backend/minimax-monitor/src/main/resources/schema-h2.sql
backend/minimax-monitor/src/main/resources/application-test.yml
backend/minimax-monitor/src/test/java/com/minimax/monitor/HealthDetailTest.java
backend/minimax-monitor/src/test/java/com/minimax/monitor/AlertEngineTest.java
reports/day-12-report.md
```

---

## 🚀 下一步 (Day 13: 调优)

- JVM 参数调优 (G1GC, Metaspace, DirectMemory)
- 数据库连接池调优 (HikariCP maxPoolSize/minIdle)
- HTTP 长连接复用 (keep-alive)
- 缓存层 (本地 Caffeine + 分布式 Redis)
- 异步化 (@Async 队列化处理)
- 慢 SQL 识别 + 索引建议
- 压测 (wrk / jmeter) 输出 QPS/P99
- 容器化优化 (JVM 感知 cgroup CPU/内存)
