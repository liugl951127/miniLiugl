# JWT Secret 安全规范 (V2.4)

> **生成 256-bit 强随机密钥, 用于 HS256 签名算法**

## 📋 规范要求

| 项 | 要求 |
|----|------|
| **算法** | HS256 (HMAC SHA-256) |
| **长度** | >= 256 bit (32 字节) |
| **编码** | 64 字符十六进制 (推荐) |
| **强度** | 加密随机源 (`openssl rand` / `/dev/urandom`) |
| **禁止** | 生日 / 姓名 / 单词 / 简单模式 |
| **跨服务** | 16 个微服务必须用**同一个** secret |
| **轮转** | 定期更换, 旧 token 失效 (用户需重新登录) |

## 🚀 一键生成

```bash
cd /opt/miniLiugl

# 只生成 (显示 secret)
./deploy-simple/generate-jwt-secret.sh

# 生成 + 自动替换所有 yml + 备份原文件
./deploy-simple/generate-jwt-secret.sh --apply

# 用指定 secret
./deploy-simple/generate-jwt-secret.sh --apply \
  --secret=a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478
```

**输出**:
```
==========================================
 MiniMax Platform - JWT Secret 生成器
==========================================

生成的 secret (HS256 / 256 bit / 64 hex chars):

  a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478

强度检查:
  ✓ 长度: 64 字符 (256 bit)
  ✓ 格式: 十六进制编码
  ✓ 数字: 21 / 小写: 43 / 大写: 0
```

## 📂 配置文件位置

| 文件 | 作用 |
|------|------|
| `backend/minimax-common/src/main/resources/application-common.yml` | **主配置** (16 服务共享) |
| `backend/minimax-common/src/main/resources/application-minimal.yml` | 精简配置 (备份) |
| `.env` | 运行时覆盖 (可选) |
| `.env.example` | 模板 |

**默认配置** (`application-common.yml`):
```yaml
minimax:
  jwt:
    # 优先用环境变量, 默认值 (开发环境)
    secret: ${MINIMAX_JWT_SECRET:a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478}
    issuer: minimax-platform
    header: Authorization
    prefix: "Bearer "
    expire-minutes: 10080        # 7 天
    refresh-expire-minutes: 43200  # 30 天
  common:
    secret: ${MINIMAX_COMMON_SECRET:a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478}
```

## 🌍 环境变量覆盖 (生产推荐)

```bash
# 写到 /etc/environment 或 docker-compose env
export MINIMAX_JWT_SECRET=$(openssl rand -hex 32)
export MINIMAX_COMMON_SECRET=$(openssl rand -hex 32)
```

**Spring 占位符** `${MINIMAX_JWT_SECRET:default}` 语法:
- 有环境变量 → 用环境变量
- 没环境变量 → 用默认值
- 启动时报错 → 用 `:` 提示

## 🔒 安全实践

### 1. 不要提交到 git
- 默认 secret 在 yml 里是**开发用**的占位符
- 生产环境必须用环境变量 / 密钥管理服务
- 检查: `git log --all -p | grep -i jwt_secret` 应该找不到

### 2. 备份和轮转
```bash
# 自动备份 (脚本自带)
./deploy-simple/generate-jwt-secret.sh --apply
# → 备份到 .jwt-backup-<时间戳>/

# 定期轮转 (建议 90 天一次)
./deploy-simple/generate-jwt-secret.sh --apply
# → 旧 token 全部失效, 用户重新登录
```

### 3. 跨服务一致性
**所有 16 个微服务必须用同一个 secret**:
- gateway 签发的 token, auth 能验证
- auth 签发的 token, chat 能验证
- 一致性靠 `application-common.yml` (公共模块) 保证

### 4. JWT 字段加密
```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: false
  jackson:
    serialization:
      write-dates-as-timestamps: false
# Token payload 用 JSON, 不存敏感信息 (明文可见)
# 敏感信息: 数据库加密 (AES), 不放 token
```

### 5. 监控异常
```yaml
# 监控: 同一 secret 短时间内多次签发失败 → 暴力破解
# 监控: 同一 user 短时间多次 401 → token 伪造
# 告警: >100 401/分钟 → 自动封 IP
```

## 🛠️ 常见问题

### Q1: 改了 secret 后用户登录失败
**A**: 预期行为。secret 改了,旧 token 失效,用户需要重新登录。

### Q2: 多个服务用了不同 secret
**A**: **会 401**!所有服务必须用同一个 secret。
- 用 `grep -r "secret:" backend/*/src/main/resources/application*.yml` 检查
- 全部必须一致

### Q3: 怎么轮转不中断服务?
**A**: JWT 没法无缝轮转, 只能:
1. 短期: 双 secret 验证 (自定义 JwtAuthenticationFilter)
2. 长期: 用户重新登录 (用新 secret 签发)

### Q4: 生成多个不同环境的 secret
**bash**:
```bash
# dev
DEV=$(openssl rand -hex 32)
# staging
STAGING=$(openssl rand -hex 32)
# production
PROD=$(openssl rand -hex 32)

# 写到不同 .env
echo "MINIMAX_JWT_SECRET=$DEV" > .env.dev
echo "MINIMAX_JWT_SECRET=$STAGING" > .env.staging
echo "MINIMAX_JWT_SECRET=$PROD" > .env.production
```

### Q5: 怎么验证 secret 强度?
**bash**:
```bash
SECRET="a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478"

# 长度
[ ${#SECRET} -ge 64 ] && echo "OK length 64+" || echo "FAIL"

# 字符分布
echo "$SECRET" | fold -w1 | sort | uniq -c | sort -rn | head
# OK: 字符应该分布均匀, 不要聚集

# 编码格式
[[ "$SECRET" =~ ^[0-9a-f]{64}$ ]] && echo "OK hex" || echo "FAIL"
```

## 📊 完整流程图

```
┌─────────────────────────────────┐
│  生成强随机 secret                │
│  openssl rand -hex 32           │
│  256 bit / 64 hex chars          │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  写到哪里?                        │
│  1. application-common.yml       │  ← 默认值 (开发用)
│  2. .env / 环境变量              │  ← 生产推荐
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  16 个微服务共享                   │
│  Spring 占位符自动读取             │
│  ${MINIMAX_JWT_SECRET:default}  │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  签发 / 验证 token                │
│  gateway 签发, auth 验证         │
│  跨服务一致 = 共享 secret         │
└─────────────────────────────────┘
```

## 🔗 相关文件

- `deploy-simple/generate-jwt-secret.sh` - 一键生成 + 替换
- `backend/minimax-common/src/main/resources/application-common.yml` - 主配置
- `.env.example` - 环境变量模板
- `backend/minimax-common/src/main/java/com/minimax/common/security/JwtAuthenticationFilter.java` - JWT 过滤器
- `backend/minimax-common/src/main/java/com/minimax/common/security/JwtProperties.java` - JWT 配置类

## 📚 参考

- [RFC 7519 - JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 7518 - JWA Algorithms](https://datatracker.ietf.org/doc/html/rfc7518#section-3.2)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [JJWT 0.12.x Docs](https://github.com/jwtk/jjwt)