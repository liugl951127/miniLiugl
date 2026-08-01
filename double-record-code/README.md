# 双录一体化平台 - 代码补全包(完整版)

> Spring Boot 2.7 + Vue 3 + TypeScript + Fabric 2.4 + MySQL 8 + 国密 SM2/SM3/SM4

## 目录结构

```
double-record-code/
├── README.md                        # 本文档
├── OPTIMIZATION_REPORT.md           # 持续优化报告
├── frontend/                        # 前端代码(Vue 3 + TypeScript)
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── README.md
│   └── src/
│       ├── api/                     # 6 个 API 服务
│       │   ├── order.ts             # 订单 API
│       │   ├── script.ts            # 话术 API
│       │   ├── session.ts           # 双录会话 API
│       │   ├── risk.ts              # 风评+合同 API
│       │   ├── chain.ts             # 区块链 API  ⭐ 新增
│       │   └── admin.ts             # 管理 API    ⭐ 新增
│       ├── components/              # 7 个业务组件
│       ├── views/                   # 页面
│       ├── store/                   # Pinia 状态
│       │   ├── dualRecord.ts
│       │   └── auth.ts              # 认证 Store  ⭐ 新增
│       ├── router/
│       ├── types/
│       └── utils/
│           ├── request.ts           # HTTP 封装(增强版)
│           ├── webrtc.ts
│           └── asr.ts
├── backend/                         # 后端代码(Spring Boot)  ⭐ 全新
│   ├── pom.xml
│   ├── README.md
│   ├── deploy/
│   │   ├── Dockerfile
│   │   └── docker-compose.yml
│   └── src/
│       ├── main/java/com/bank/dualrecord/
│       │   ├── DualRecordApplication.java
│       │   ├── config/              # 7 个配置类
│       │   ├── controller/          # REST 控制器
│       │   ├── service/             # 业务服务
│       │   ├── mapper/              # MyBatis
│       │   ├── model/               # 实体
│       │   ├── dto/                 # 通用 DTO
│       │   ├── fabric/              # 链码集成
│       │   ├── security/            # JWT + 限流
│       │   └── exception/           # 全局异常
│       └── test/                    # 单元测试
├── sql/                             # SQL 脚本
│   ├── 01_schema.sql                # 8 张核心表
│   ├── 02_indexes.sql
│   ├── 03_init_data.sql
│   ├── 04_test_data.sql
│   ├── 05_runbook.sql
│   ├── 06_audit_log.sql             # 9 张扩展表  ⭐ 新增
│   ├── 07_foreign_keys.sql          # 外键 + 复合索引 + 物化视图  ⭐ 新增
│   ├── 08_verify.sql                # 自动验证脚本  ⭐ 新增
│   └── 09_runbook.sql               # 完整执行手册  ⭐ 新增
└── deploy/                          # 部署  ⭐ 新增
    └── e2e-test.sh                  # 端到端集成测试
```

## 核心指标(本轮优化后)

| 维度 | 数据 |
|------|------|
| SQL 业务表 | 17 张(8 核心 + 9 扩展) |
| SQL 外键约束 | 16 个 |
| SQL 索引 | ~60 个(含 12 个复合索引) |
| SQL 自动验证 | 11 步检查 |
| 前端 API 文件 | 6 个(原 4 个) |
| 前端 API 端点 | ~50 个 |
| 后端 Java 文件 | 33 个 |
| 后端 API 端点 | 12+ 个 |
| 单元测试 | Java 53 + TypeScript 62 + 后端 6 = 121 |
| Swagger 文档 | 完整覆盖 |

## 快速启动(端到端)

```bash
# 1. 跑全部 SQL 脚本
mysql -u root -p < sql/09_runbook.sql

# 2. 编译 + 启动后端
cd backend
mvn clean package -DskipTests
java -jar target/dual-record-backend.jar

# 3. 启动前端
cd ../frontend
npm install
npm run dev

# 4. 访问
# - 业务系统: http://localhost:5173
# - Swagger:  http://localhost:8080/swagger-ui.html
# - 链码健康: http://localhost:8080/api/chain/health
```

## Docker 一键启动(开发/测试)

```bash
cd backend/deploy
docker-compose up -d
# 自动: MySQL 灌库 + Redis + 后端
# 访问 http://localhost:8080
```

## 端到端测试

```bash
# 跑完整 E2E: SQL 灌库 → 链码编译 → 后端编译 → 单元测试 → 启动 + API 冒烟
bash deploy/e2e-test.sh
```

## 详细文档

- **持续优化报告**:`OPTIMIZATION_REPORT.md` - 本轮所有改动
- **后端 README**:`backend/README.md` - Spring Boot 详细说明
- **前端 README**:`frontend/README.md` - Vue 详细说明
- **链码 README**:`../fabric-chaincode-java/README.md` - 链码详细说明
- **SDK README**:`../script-sdk/README.md` - TS SDK 详细说明

## 关键特性

### 1. SQL 完整性
- 17 张表覆盖业务全流程
- 16 个外键保证数据一致性
- 60+ 索引(含复合)保证查询性能
- 物化视图 + 日报汇总表
- 自动化 11 步验证

### 2. 前端完整 API
- 6 个 API 文件,50+ 端点
- request.ts 增强(401 自动跳登录 + ElMessage 统一提示)
- Pinia auth store(登录/退出/Refresh/权限)

### 3. 后端 Spring Boot
- 33 个 Java 文件,完整骨架
- Fabric Gateway 集成(3 合约)
- JWT + Spring Security
- Redis 缓存(分层 TTL)
- Guava RateLimiter 限流
- Springdoc OpenAPI 文档
- 全局异常处理

### 4. 链码 ↔ 后端 ↔ 前端 端到端
```
浏览器/小程序 (script-sdk)
    ↓ HTTP
后端 API (Spring Boot)
    ↓ Fabric Gateway
Java 链码 (Fabric 2.4)
    ↓ 共识
4 组织 Peer (本行/银保监/保险/公证处)
```

## 部署清单

### 1. 前端
- `npm run build` → `dist/`
- nginx 静态托管
- HTTPS 证书
- 跨域:代理 `/api` 到后端

### 2. 后端
- Spring Boot jar 部署
- MySQL 8.0 MGR 3 节点
- Redis 6.2 Cluster 6 节点
- Kafka 3 broker
- 4 节点 Fabric 联盟链
- OSS + 区块链事件消费者

### 3. 监控
- Prometheus + Grafana
- ELK 日志
- APM 链路追踪(SkyWalking)
- 告警:钉钉/企业微信

## 待办(下一轮)

- [ ] 链码事件 Kafka 消费者
- [ ] WebRTC 推流(PAD 端)
- [ ] AI 质检模型集成(LLM)
- [ ] 监管报送接口(银保监)
- [ ] K8s Helm Chart
- [ ] Prometheus + Grafana
- [ ] 性能压测(WRK)
- [ ] 渗透测试报告

## 联系方式

- 架构:Mavis
- 前端:Vue 团队
- 后端:Java 团队
- 区块链:Fabric 团队
- 国密:信息安全部
- DBA:数据库团队

## 许可证

Apache-2.0
