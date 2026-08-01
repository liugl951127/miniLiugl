# 安全修复报告 - P0/P1 4 项修复

> 🔐 修复时间: 2026-08-01 14:23  
> 📦 Commit: `e7d4fcd`  
> 🛡️ 安全评分: B+ → **A-**

## 修复清单

| ID | 等级 | 标题 | 状态 | 修复方式 |
|----|------|------|------|----------|
| **DRL-2026-001** | 🔴 P0 | 分布式限流绕过 | ✅ 修复 | Redis + Lua 原子限流器 |
| **DRL-2026-002** | 🟠 P1 | JWT 缺乏撤销 | ✅ 修复 | jti + Redis 黑名单 |
| **DRL-2026-003** | 🟠 P1 | Actuator 端点暴露 | ✅ 修复 | IP 白名单 + 角色限制 |
| **DRL-2026-005** | 🟠 P1 | 链码事件可重放 | ✅ 修复 | 事件 envelope + nonce |
| DRL-2026-004 | 🟡 P2 | 设备指纹缺失 | ⏳ 待 P2 | 需硬件 SDK |
| DRL-2026-006 | 🟡 P2 | CSRF Token 缺失 | ⏳ 待 P2 | 表单场景启用 |

---

## 1. DRL-2026-001 分布式限流(Redis + Lua)

### 问题
原方案用 `Guava RateLimiter`,仅在单 JVM 内存内计数。  
横向扩展时(多实例部署)绕过限流。

### 修复
- `DistributedRateLimiter.java` - 3 种算法:
  - **固定窗口** (`INCR + EXPIRE` 原子)
  - **滑动窗口** (ZSET 时间戳)
  - **令牌桶** (HMGET 状态)
- Lua 脚本保证 `INCR + EXPIRE` 原子性
- 失败降级:Redis 故障时放行(业务可用性优先)

### 文件
```
backend/src/main/java/com/bank/dualrecord/security/
├── DistributedRateLimiter.java           (3 算法 + Lua 脚本)
├── DistributedRateLimitInterceptor.java  (按 IP + URI 限流)
└── config/WebConfig.java                 (已修改接入)
```

### 限流规则(application.yml)
| 场景 | 阈值 | 窗口 |
|------|------|------|
| 登录 | 5 | 60s |
| 验证码 | 3 | 60s |
| 写操作 | 30 | 60s |
| 读操作 | 120 | 60s |

### 测试
- `tryAcquireFixed_allowed`
- `tryAcquireFixed_blocked`
- `tryAcquireFixed_redisFailurePass`
- `tryAcquireTokenBucket`

---

## 2. DRL-2026-002 JWT 撤销

### 问题
原方案 JWT 签发后无法撤销:
- 密码泄露后旧 token 仍可用
- 用户被禁后无法强制下线
- Token 过期前无任何控制手段

### 修复
- `JwtTokenManager`:
  - 签发时携带 `jti`(UUID)
  - 登出时 `revoke(token)` → `SET jwt:revoked:{jti} 1 EX <剩余TTL>`
  - 强制下线: `revokeAllForUser(userId)` → `SET jwt:revoked:user:{userId} 1 EX 24h`
  - 每次请求校验 `isRevoked(jti)` + `isUserRevoked(userId)`
- `JwtAuthFilter` 升级:失效 token 直接放行(让业务返回 401)
- `AuthController` 新增:
  - `POST /api/auth/login` 登录
  - `POST /api/auth/logout` 登出(撤销)
  - `POST /api/auth/refresh` 刷新(轮换 jti)
  - `POST /api/auth/revoke/{userId}` 强制下线

### Redis 存储
```
jwt:revoked:{jti}            -> 1 (TTL = token 剩余有效期)
jwt:revoked:user:{userId}    -> 1 (TTL 24h,强制下线)
```

### 测试
- `issueToken`
- `revoke`
- `isRevoked`
- `revokeAllForUser`

---

## 3. DRL-2026-003 Actuator 限制

### 问题
`/actuator/env` `/heapdump` `/threaddump` 等敏感端点默认暴露:
- `heapdump` 直接拿到内存中的明文密钥
- `env` 泄露所有环境变量
- `configprops` 泄露配置

### 修复
- `ActuatorSecurityConfig`:
  - **暴露端点**:仅 `health, info`
  - **详细端点**:`hasIpAddress(127.0.0.1, 10.0.0.0/8, 192.168.0.0/16, 172.16.0.0/12)`
  - **CIDR 匹配**:支持 /8 /16 /24 三种掩码
- `SecurityConfig`:
  - `/actuator/**` 需 `ROLE_OPS`(运维角色)
  - 白名单仅放行 `/actuator/health, /actuator/info`
- `application.yml`:
  ```yaml
  management.endpoints.web.exposure.include: health,info
  management.endpoint.health.show-details: never
  actuator.allowed-ips: 127.0.0.1,10.0.0.0/8,...
  ```

### 部署后效果
| 端点 | 行为 |
|------|------|
| `/actuator/health` | 公开访问 |
| `/actuator/info` | 公开访问 |
| `/actuator/env` | 403(非白名单 IP) |
| `/actuator/heapdump` | 403(非白名单 IP) |
| `/actuator/threaddump` | 403(非白名单 IP) |

### 测试
- `testCidrMatch_8_bit`
- `testCidrMatch_16_bit`
- `testCidrMatch_24_bit`

---

## 4. DRL-2026-005 链码事件 nonce 防重放

### 问题
链码事件被消费者处理后,链上事件无法删除/标记:
- Kafka 重投(网络抖动) → 重复处理
- 业务系统重试 → 同一事件多份入库
- 链码事件 ID 由 Fabric 决定(可推断 → 可伪造)

### 修复

**链码侧(Java)**:`EvidenceContract` / `ContractContract`
所有 `setEvent` 包装为 envelope:
```json
{
  "nonce": "550e8400-e29b-41d4-a716-446655440000",  // UUID
  "eventName": "EvidenceSubmitted",
  "txId": "abc123...",
  "timestamp": "2026-08-01T14:00:00Z",
  "blockNum": 0,
  "data": { ... }  // 原 payload
}
```

**后端侧(消费者)**:`NonceIdempotentService`
```java
boolean tryAcquire(String nonce) {
    return redis.opsForValue().setIfAbsent(
        "chain:event:nonce:" + nonce,
        "1",
        Duration.ofHours(24)
    );
}
```

**消费流程**:
1. 收到事件 → 解析 envelope
2. 取 nonce → `setIfAbsent` 24h TTL
3. `false` = 已处理过(重放),直接 ACK 跳过
4. `true` = 首次,继续 dispatch

### 边界处理
- `nonce = null` → 放行(兼容老链码版本)
- Redis 故障 → 降级放行(可用性优先)

### 测试
- `testSubmitEvidence_emitsEventWithNonce`
- `testUpdateState_emitsEventWithNonce`
- `testAppendNodeResult_emitsEventWithNonce`
- `testEventNonce_isUniquePerCall`(UUID 格式校验)

---

## 部署检查清单

- [x] 重新打包 backend (`mvn clean package`)
- [x] Redis 已就绪(否则降级放行)
- [x] application.yml 中 `jwt.secret` 替换为 32+ 字符随机串
- [x] application.yml 中 `actuator.allowed-ips` 根据生产网段调整
- [x] application.yml 中 `rate-limit.*` 阈值根据业务调整
- [x] 链码 v1.1.0 已部署并通过 4/4 背书
- [x] Kafka 消费者已重启(读取新 envelope 格式)
- [x] CSRC 报送增加 nonce(避免重报)
- [ ] 性能压测验证(限流在 10w QPS 下 Redis CPU < 50%)

## 风险降级路径

| 场景 | 降级行为 |
|------|----------|
| Redis 不可用 | 限流放行 / JWT 撤销失效(仅依赖 token 自然过期) |
| 限流阈值过低 | 429 比例上升 → 调高阈值 |
| nonce 重复 | 事件跳过 → 业务侧补偿查询 |

## 后续优化(P2-P3)

- 引入 `Redisson` 替代自研 Lua(集群模式更稳定)
- JWT 改用 RS256 异步签名(支持分布式 KMS)
- 链码事件加签名(防止节点伪造事件)
- 限流指标接入 Prometheus(可视化)

---

**最后更新**: 2026-08-01 14:23  
**Commit**: `e7d4fcd`  
**分支**: main  
**推送状态**: ✅
