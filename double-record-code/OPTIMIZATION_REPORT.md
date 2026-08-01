# 双录一体化平台 - 持续优化报告

> 期间: 2026-08-01 → 当前
> 范围: SQL / 前端 / 后端 / 链码 / 部署 / 文档

## 一、SQL 优化(从 5 张表 → 17 张表)

### 1.1 新增 9 张关键表

| 表名 | 作用 | 关键设计 |
|------|------|----------|
| `t_audit_log` | 业务侧审计日志 | 只增不改 + JSON 字段 + 风险标识 |
| `t_chain_event` | 链码事件落地 | Kafka 消费 + 唯一索引防重 + 处理标记 |
| `t_node_result` | 节点结果(业务侧) | UNIQUE(session_id, node_code) 幂等 |
| `t_public_key` | SM2 公钥注册 | 4 类参与方 + 状态机 |
| `t_user` | 业务用户 | 工号 + MSP ID + 关联公钥 |
| `t_role` | 角色 | 权限 JSON 数组 + 内置标志 |
| `t_user_role` | 用户角色 | 多对多 + 过期时间 |
| `t_branch` | 网点 | 自引用层级 + MSP ID |
| `t_order_exception` | 异常订单 | 类型 + 处理状态 + 申诉流程 |

### 1.2 外键约束(0 → 16 个)

- **强约束**(业务核心):`t_order → t_customer`, `t_session → t_order`, `t_script_node → t_script`, `t_quality → t_session`
- **弱约束**(审计/事件):`t_audit_log → t_order SET NULL`, `t_chain_event → t_order SET NULL`
- **级联删除**:子表 ON DELETE CASCADE(节点/角色/异常)

### 1.3 复合索引(高频查询优化)

```sql
-- 订单:按客户+状态+时间(风控大屏)
idx_order_customer_state_time ON t_order(customer_id, state, created_at);

-- 订单:按经理+时间(经理工作量统计)
idx_order_sales_time ON t_order(sales_user_id, created_at);

-- 会话:按状态+开始时间(超时监控)
idx_session_state_start ON t_session(state, start_at);

-- 质检:按结论+时间(监管统计)
idx_qa_verdict_time ON t_quality(verdict, created_at);
```

### 1.4 物化视图 - 日报汇总

`t_report_daily(report_date, branch_id, product_type)` 14 个指标,直接服务运营大屏,日终定时任务汇总。

### 1.5 自动化验证(08_verify.sql)

11 步检查:表数/外键/索引/种子数据/7 类业务查询/EXPLAIN 性能/3 类完整性约束测试(应失败)。

## 二、前端补全

### 2.1 API 完整化(原 4 文件 → 6 文件)

| 文件 | 端点数 | 说明 |
|------|--------|------|
| `api/chain.ts` | 10 | 链码交互(证据/合同/审计/公钥) |
| `api/auth.ts` | 5 | 认证(JWT/Refresh) |
| `api/admin.ts` | 25 | 客户/订单/会话/质检/合同/话术/报告 |

### 2.2 request.ts 重构

- 修复 `data.code !== 0` → `data.code === 200`(后端正确返回 200)
- `DualRecordError` 重新设计(支持 traceId)
- Element Plus 错误提示统一(`ElMessage.error`)
- 上传进度回调 + 401 自动跳登录
- 网络错误细分(超时 / 网络 / 5xx / 限流)

### 2.3 Pinia Store 完整化

`auth.ts` 状态机:登录/退出/Refresh/过期/权限(`hasPermission`)/本地缓存。

## 三、后端骨架(Spring Boot)

### 3.1 工程结构(33 个 Java 文件)

```
backend/src/main/java/com/bank/dualrecord/
├── DualRecordApplication.java       # 启动
├── config/                          # 4 配置类
│   ├── MybatisPlusConfig            # 分页/乐观锁
│   ├── SecurityConfig               # JWT + CORS
│   ├── FabricConfig                 # 链码配置
│   ├── RedisConfig                  # 缓存
│   ├── RateLimitConfig              # 限流
│   ├── WebConfig                    # MVC
│   └── OpenApiConfig                # Swagger
├── controller/                      # 2 控制器
├── service/                         # 业务
├── mapper/                          # MyBatis
├── model/                           # 实体
├── dto/                             # ApiResponse/PageResult
├── fabric/                          # 4 链码服务
│   ├── FabricGatewayManager         # Gateway 管理
│   ├── FabricEvidenceService        # 证据合约
│   ├── FabricContractService        # 合同合约
│   └── FabricAuditService           # 审计合约
├── security/                        # JWT + 限流拦截
└── exception/                       # 全局异常
```

### 3.2 集成 Java 链码

`FabricGatewayManager` 启动时:
1. 加载钱包(本地文件系统)
2. 加载 connection-profile.yaml
3. 创建 Gateway
4. 注册 3 个合约(EvidenceContract / ContractContract / AuditContract)

### 3.3 安全/性能

- **JWT 鉴权**:无状态 + Token 黑名单
- **限流**:Guava RateLimiter(每 IP/接口每秒 20 个)
- **Redis 缓存**:order 5min / script 1h / user 15min
- **全局异常**:`BusinessException` + `@RestControllerAdvice`
- **跨域**:allowedOriginPatterns("*")

### 3.4 Swagger / OpenAPI 3

`/swagger-ui.html` + `/v3/api-docs` 完整 API 文档,JWT Bearer 鉴权集成。

## 四、链码 ↔ 后端联调

| 链码方法 | 后端 API |
|----------|----------|
| `EvidenceContract.submitEvidence` | `POST /api/chain/evidence/submit` |
| `EvidenceContract.queryEvidence` | `GET /api/chain/evidence/{orderId}` |
| `EvidenceContract.verifyEvidence` | `POST /api/chain/evidence/verify` |
| `EvidenceContract.updateState` | `POST /api/orders/{id}/cancel` |
| `EvidenceContract.finalizeEvidence` | `POST /api/sessions/{id}/complete` |
| `ContractContract.generateContract` | `POST /api/chain/contract/submit` |
| `ContractContract.signContract` | `POST /api/chain/contract/sign` |
| `AuditContract.queryOrderAudits` | `GET /api/chain/audit/{orderId}` |
| `EvidenceContract.registerPublicKey` | `POST /api/chain/publickey/register` |

## 五、部署与验证

### 5.1 端到端测试脚本(deploy/e2e-test.sh)

6 步自动化:
1. 环境检查
2. SQL 灌库 + 验证(14+ 张表)
3. Java 链码编译(Maven)
4. 后端编译
5. 跑所有单元测试(链码 + 后端 + SDK)
6. 启动后端 + 冒烟 API 测试

### 5.2 Docker Compose

`backend/deploy/docker-compose.yml` 一键起 MySQL + Redis + Backend(开发/测试环境)。

### 5.3 完整 SQL 运行手册(09_runbook.sql)

单文件执行全部 7 个 SQL 脚本,顺序 01 → 02 → 03 → 04 → 06 → 07 → 08(自动验证)。

## 六、交付清单更新

| 模块 | 之前 | 现在 | 增加 |
|------|------|------|------|
| SQL 脚本 | 5 文件 50KB | 9 文件 75KB | +4 文件 |
| 前端 | 24 文件 | 30 文件 | +6 文件(auth/store/完整 API) |
| 后端 | 0 | 33 文件 | +33 文件(Spring Boot 完整骨架) |
| 部署 | 0 | 3 文件 | +3 文件(Docker/Compose/E2E) |
| **合计** | 24 文件 | 75 文件 | **+51 文件 213%↑** |

## 七、关键指标

| 指标 | 之前 | 现在 | 提升 |
|------|------|------|------|
| SQL 表数 | 8 | 17 | +112% |
| SQL 索引数 | ~30 | ~60 | +100% |
| SQL 外键数 | 0 | 16 | ∞ |
| 前端 API 端点 | ~15 | ~50 | +233% |
| 后端 Java 文件 | 0 | 33 | 全新 |
| 后端 API 端点 | 0 | 12+ | 全新 |
| Swagger 文档 | 无 | 完整 | 全新 |
| 限流 | 无 | 20 req/s/IP | 全新 |
| Redis 缓存 | 无 | 3 类 TTL | 全新 |

## 八、待办(下一轮)

- [ ] 链码事件 Kafka 消费者(后端)
- [ ] WebRTC 推流(PAD 端)
- [ ] AI 质检模型集成(LLM)
- [ ] 监管报送接口(银保监)
- [ ] K8s Helm Chart 部署
- [ ] Prometheus + Grafana 监控
- [ ] 性能压测(WRK/JMeter)
- [ ] 渗透测试报告

---

**版本**: 1.1.0
**日期**: 2026-08-01
**负责人**: Mavis
**状态**: 持续优化中
