# 双录一体化平台 - 业务后端 (Spring Boot)

> Spring Boot 2.7 + Fabric Gateway 2.2 + MyBatis Plus + 国密 SM2/SM3/SM4

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 应用框架 |
| Spring Security | 5.7 | 安全框架 + JWT |
| MyBatis Plus | 3.5.5 | ORM |
| Hyperledger Fabric Gateway | 2.2.0 | 区块链 SDK |
| BouncyCastle | 1.70 | 国密算法 |
| Hutool | 5.8.22 | 国产工具集 |
| MySQL | 8.0+ | 业务数据库 |
| HikariCP | 内置 | 数据库连接池 |
| JJWT | 0.11.5 | JWT |
| Springdoc OpenAPI | 3.0 | API 文档 |

## 目录结构

```
backend/
├── pom.xml                                  # Maven 配置
├── README.md
├── src/main/
│   ├── java/com/bank/dualrecord/
│   │   ├── DualRecordApplication.java      # 启动类
│   │   ├── config/                          # 配置
│   │   │   ├── MybatisPlusConfig.java       # MyBatis Plus
│   │   │   ├── SecurityConfig.java          # Spring Security
│   │   │   ├── FabricConfig.java            # Fabric 配置
│   │   │   └── OpenApiConfig.java           # Swagger
│   │   ├── controller/                      # REST 控制器
│   │   │   ├── OrderController.java
│   │   │   └── ChainController.java         # 链码调用
│   │   ├── service/                         # 业务服务
│   │   │   └── OrderService.java
│   │   ├── mapper/                          # MyBatis
│   │   │   └── OrderMapper.java
│   │   ├── model/                           # 实体
│   │   │   └── Order.java
│   │   ├── dto/                             # 通用 DTO
│   │   │   ├── ApiResponse.java
│   │   │   └── PageResult.java
│   │   ├── fabric/                          # Fabric 集成
│   │   │   ├── FabricGatewayManager.java
│   │   │   ├── FabricEvidenceService.java
│   │   │   ├── FabricContractService.java
│   │   │   └── FabricAuditService.java
│   │   ├── security/                        # 安全
│   │   │   └── JwtAuthFilter.java
│   │   └── exception/                       # 异常处理
│   │       ├── BusinessException.java
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       ├── application.yml
│       ├── fabric/connection-profile.yaml
│       └── mapper/OrderMapper.xml
└── src/test/                                # 测试
```

## API 端点

### 链码交互(对应 fabric-chaincode-java)

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/chain/evidence/submit` | 提交证据 |
| GET | `/api/chain/evidence/{orderId}` | 查询证据 |
| POST | `/api/chain/evidence/verify` | 验证证据 |
| GET | `/api/chain/evidence/{orderId}/history` | 证据历史 |
| POST | `/api/chain/contract/submit` | 提交合同 |
| POST | `/api/chain/contract/sign` | 签署合同 |
| GET | `/api/chain/audit/{orderId}` | 审计历史 |
| POST | `/api/chain/publickey/register` | 注册公钥 |
| GET | `/api/chain/health` | 健康检查 |

### 业务

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/orders` | 创建订单 |
| GET | `/api/orders/{id}` | 订单详情 |
| GET | `/api/orders` | 订单列表 |
| POST | `/api/orders/{id}/cancel` | 取消订单 |

## 快速启动

```bash
# 1. 编译
mvn clean package -DskipTests

# 2. 启动(需先准备 application-local.yml)
java -jar target/dual-record-backend.jar \
  --spring.profiles.active=local \
  --DB_PASSWORD=xxx \
  --SM4_KEY=xxx

# 3. 访问 Swagger
open http://localhost:8080/swagger-ui.html
```

## 配置说明

`application.yml` 默认配置:
- MySQL: `localhost:3306/dual_record`
- Fabric: `classpath:fabric/connection-profile.yaml`
- 国密 SM4 Key: `${SM4_KEY}` 环境变量
- JWT Secret: `${JWT_SECRET}` 环境变量

**生产环境务必**:
- 切换到 HTTPS
- 修改所有默认密钥
- 启用 Redis(限流 + 缓存)
- 部署到 K8s + ConfigMap/Secret

## 与 fabric-chaincode-java 对接

```java
// 后端调用链码
String orderId = fabricEvidenceService.submitEvidence(Map.of(
    "orderId", "ORD20260801001",
    "customerId", "C001",
    "productType", "INSURANCE",
    "videoHash", "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
    "audioHash", "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d",
    "contractHash", "b3a8e0e1f9c18a6d54c9b65a0c5e0a3b2c1d4e5f6789abcdef0123456789abcd",
    ...
));

// 查询链上证据
Map<String, Object> evidence = fabricEvidenceService.queryEvidence("ORD20260801001");
```

## 与 script-sdk 对接

script-sdk(浏览器/小程序) → 后端 API(/api/orders) → 链码(/api/chain)

完整链路:
1. 前端用 script-sdk 执行话术节点
2. 节点结果 → `getChainPayload()` → 上报后端
3. 后端 → Fabric 链码 → 链上存证
4. 后端 → MySQL → 业务存储(节点结果表)
5. 异步:Kafka 消费链码事件 → 写 `t_chain_event`

## License

Apache-2.0
