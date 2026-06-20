# 🔄 跨平台 OAuth 指南 (V5.2)

> 微信 / QQ / 支付宝 / 微博 / GitHub 5 大平台统一 OAuth
> 同一用户跨平台扫码, 自动识别为同一账号 (unionid 打通)

---

## 📑 目录

- [背景](#背景)
- [整体架构](#整体架构)
- [数据库设计](#数据库设计)
- [OAuth 抽象层](#oauth-抽象层)
- [API 端点](#api-端点)
- [H5 集成](#h5-集成)
- [小程序集成](#小程序集成)
- [跨平台 unionid 打通](#跨平台-unionid-打通)
- [生产部署](#生产部署)
- [跨平台统计](#跨平台统计)

---

## 背景

V5 阶段只支持微信扫码. 但实际用户场景:

| 场景 | 用户偏好 |
|------|----------|
| 商务人士 | 微信 (工作绑定) |
| 年轻用户 | QQ |
| 支付场景 | 支付宝 |
| 开发者 | GitHub |

**多账号问题**:
- 用户在微信登录后, 换 QQ 又是新账号 → 历史聊天/订单丢失
- 用户在支付宝扫码, 又去微信扫码 → 两个账号

**解决方案**:
- 统一 OAuth 抽象层 (`OAuthPlatformClient`)
- 同一用户用不同平台登录 → 自动识别 (通过 openid + unionid)
- 跨平台数据共享 (聊天/订单/知识库)

---

## 整体架构

```
┌──────────────────────────────────────────────────┐
│               用户 (5 大平台账号)                   │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────┐ ┌────┐ │
│  │ 微信    │ │  QQ    │ │ 支付宝 │ │微博 │ │GH  │ │
│  └───┬────┘ └───┬────┘ └───┬────┘ └──┬─┘ └──┬─┘ │
│      │ unionid  │          │         │      │    │
└──────┼──────────┼──────────┼─────────┼──────┼────┘
       │          │          │         │      │
       ▼          ▼          ▼         ▼      ▼
   ┌──────────────────────────────────────────────┐
   │  MiniMax 平台                                │
   │                                               │
   │   oauth_app_config  (应用凭证)                │
   │   oauth_binding      (binding 记录)           │
   │   unionid_relations  (跨平台关联)             │
   │   sys_user.*         (主平台字段)             │
   │                                               │
   │   OAuthPlatformClient (抽象层)                │
   │     ├── WechatApiClientBridge                │
   │     ├── QqOAuthClient                        │
   │     ├── AlipayOAuthClient                    │
   │     └── (未来: Weibo/GitHub)                 │
   └──────────────────────────────────────────────┘
```

---

## 数据库设计

### 1. `oauth_app_config` (新增)

每行 = 一个平台 × 一个应用类型:

```sql
CREATE TABLE oauth_app_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(20),    -- wechat/qq/alipay/weibo/github
    app_type VARCHAR(20),    -- mp/mini/open/web/app/h5
    app_id VARCHAR(128),
    app_secret VARCHAR(256),
    public_key TEXT,         -- 支付宝 RSA 公钥
    redirect_uri VARCHAR(512),
    scopes VARCHAR(256),
    enabled TINYINT(1) DEFAULT 1,
    UNIQUE KEY uk_platform_app_type (platform, app_type)
);
```

预置 11 行 (5 平台 × 多 app_type).

### 2. `oauth_binding` (新增)

替代 V5 的 `wechat_user_binding`, 支持任意平台:

```sql
CREATE TABLE oauth_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform VARCHAR(20),     -- wechat/qq/alipay/weibo/github
    app_type VARCHAR(20),     -- mp/mini/open/web/app/h5
    openid VARCHAR(128),
    unionid VARCHAR(128),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    access_token VARCHAR(512),
    refresh_token VARCHAR(512),
    raw_data TEXT,
    bound_at DATETIME,
    last_login_at DATETIME,
    UNIQUE KEY uk_platform_app_openid (platform, app_type, openid)
);
```

### 3. `sys_user` 加 10 字段 (V5.2)

```sql
qq_openid, qq_unionid, qq_nickname, qq_avatar, qq_bound_at,
alipay_openid, alipay_user_id, alipay_nickname, alipay_avatar, alipay_bound_at
```

### 4. `unionid_relations` 加复合索引

```sql
ALTER TABLE unionid_relations ADD INDEX idx_platform_unionid (platform, unionid);
```

---

## OAuth 抽象层

### 接口 (`OAuthPlatformClient`)

```java
public interface OAuthPlatformClient {
    String platform();                                          // wechat/qq/alipay
    Map<String, Object> code2Token(...);                        // code → access_token
    Map<String, Object> getUserInfo(...);                       // 拿昵称/头像
    Map<String, Object> refreshToken(...);                      // 刷新
    String buildAuthorizeUrl(...);                              // 生成授权 URL
    boolean isMock(...);                                        // 是否沙箱
}
```

### 3 个实现

| 类 | 平台 | 关键 API |
|---|------|----------|
| `WechatApiClientBridge` | 微信 | `/sns/oauth2/access_token`, `/sns/userinfo`, `/sns/jscode2session` |
| `QqOAuthClient` | QQ | `/oauth2.0/token`, `/oauth2.0/me`, `/user/get_user_info` |
| `AlipayOAuthClient` | 支付宝 | `/oauth2/publicAppAuthorize.htm`, `/gateway.do` |

### 统一调度 (`OAuthPlatformService`)

```java
public LoginResponse login(String platform, String appType, String code, String redirectUri) {
    OAuthPlatformClient client = getClient(platform);
    Map<String, Object> token = client.code2Token(appId, appSecret, code, redirectUri);
    Map<String, Object> userInfo = client.getUserInfo(token.accessToken, token.openid);
    SysUser sysUser = findOrCreateUser(platform, appType, openid, unionid, nickname, avatar, mock);
    upsertBinding(sysUser.id, platform, appType, ...);
    recordUnionid(sysUser.id, unionid, platform);
    return authService.issueLoginResponse(sysUser);
}
```

---

## API 端点

### H5 通用

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/oauth/{platform}/authorize-url` | 生成授权 URL |
| GET | `/auth/oauth/{platform}/callback?code=` | 平台回调 |
| POST | `/auth/oauth/{platform}/login` | 移动端传 code |
| GET | `/auth/oauth/{platform}/config` | 查应用配置 |

### Admin

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/oauth/configs` | 列出所有配置 |
| GET | `/auth/admin/wechat/users-by-unionid?unionid=` | 按 unionid 查 (含跨平台 binding) |
| GET | `/auth/admin/wechat/cross-platform-stats` | 跨平台统计 |

### 端点示例

#### 1) 生成授权 URL

```bash
$ curl "http://localhost:8081/auth/oauth/qq/authorize-url?appType=web&redirectUri=https://api.example.com/cb&state=test123"
{
  "platform": "qq",
  "appType": "web",
  "authorizeUrl": "https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=...&redirect_uri=...&state=test123&scope=get_user_info",
  "mock": true
}
```

#### 2) 跨平台登录 (POST code)

```bash
$ curl -X POST http://localhost:8081/auth/oauth/qq/login \
    -H "Content-Type: application/json" \
    -d '{"code":"mock_qq_code_abc", "appType":"web"}'

{
  "code": 0,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "...",
    "user": {
      "id": 3,
      "username": "qq_xxx",
      "nickname": "QQ用户_xxx",
      "avatar": "https://q.qlogo.cn/..."
    }
  }
}
```

#### 3) 跨平台统计

```bash
$ curl -H "Authorization: Bearer $ADMIN_TOKEN" \
    http://localhost:8081/auth/admin/wechat/cross-platform-stats

{
  "bindingByPlatform": [
    {"platform": "wechat", "count": 5},
    {"platform": "qq", "count": 3},
    {"platform": "alipay", "count": 2},
    {"platform": "weibo", "count": 0},
    {"platform": "github", "count": 0}
  ],
  "totalBindings": 10,
  "wechatUsers": 5,
  "qqUsers": 3,
  "alipayUsers": 2,
  "multiPlatformUsers": 1,
  "unionidRelations": 8
}
```

---

## H5 集成

前端 `H5Login.vue` 提供 4 个平台按钮:

```vue
<button @click="loginWechat">📱 微信扫码</button>
<button @click="loginMp">📢 微信公众号</button>
<button @click="loginQq">🐧 QQ 登录</button>
<button @click="loginAlipay">💰 支付宝登录</button>
```

### 流程

1. **前端**: 用户点 "QQ 登录" → 调 `/auth/oauth/qq/authorize-url`
2. **后端**: 返回 QQ 授权 URL (含 redirect_uri + state)
3. **前端**: `window.location.href = url` 跳转到 QQ
4. **用户**: 在 QQ 授权
5. **QQ**: 重定向到 `redirect_uri?code=xxx&state=xxx`
6. **后端**: `/auth/oauth/qq/callback?code=xxx` 处理
7. **后端**: 跨平台 unionid 打通 → 找/建账号 → 写 oauth_binding → 生成 JWT
8. **前端**: 拿到 JWT → 登录成功 → 跳业务页

### 沙箱模式

mock 模式 (`app_id` 是 PLACEHOLDER) 下:
- 自动跳过跳转, 直接调 `/auth/oauth/{platform}/login`
- 用假 code 模拟用户授权
- 返回 mock 的用户信息

---

## 小程序集成

### 微信小程序

```javascript
// miniprogram/app.js
App({
  onLaunch() {
    wx.login({
      success: res => {
        if (res.code) {
          // 走 OAuthPlatformService.login("wechat", "mini", code)
          wx.request({
            url: 'https://api.your-domain.com/auth/oauth/wechat/login',
            method: 'POST',
            data: { code: res.code, appType: 'mini' },
            success: r => {
              wx.setStorageSync('access_token', r.data.data.accessToken)
            }
          })
        }
      }
    })
  }
})
```

### QQ 小程序

```javascript
// 1. 调 qq.login 拿 code
// 2. POST /auth/oauth/qq/login {code, appType: "app"}
```

### 支付宝小程序

```javascript
// 1. 调 my.getAuthCode 拿 authCode
// 2. POST /auth/oauth/alipay/login {code: authCode, appType: "app"}
```

---

## 跨平台 unionid 打通

### 原理

同一用户在不同平台 openid 完全不同, 但 unionid 可能相同 (前提: 同一开放平台或商户号).

**没有 unionid 时的退化方案**:
- 同一手机号/邮箱 → 合并账号
- 同一 IP + 短时间内登录 → 提示用户合并

### 4 步查找流程

```
1. oauth_binding (platform + app_type + openid) 精确匹配 → 直接登录
2. ★ unionid 跨平台打通 → 复用 user_id
3. sys_user 主平台字段 (wechat/qq/alipay openid) → 写新 binding
4. 新建账号 → 自动写 3 张表
```

### 测试场景

```
1. 用户用微信扫码登录 → 创建 user A (id=42)
2. 同用户用 QQ 登录 (mock unionid 相同) → 复用 user A
3. 同用户用支付宝登录 (mock unionid 相同) → 复用 user A
4. 最终: user A 有 3 个 binding (wechat + qq + alipay)
5. unionid_relations.binding_count = 3
```

---

## 生产部署

### 微信开放平台

1. https://open.weixin.qq.com 申请"网站应用" / "移动应用"
2. 拿到 AppID + AppSecret
3. 配置回调域: `api.your-domain.com`

```sql
UPDATE oauth_app_config SET
  app_id = 'wx你的AppID',
  app_secret = '你的AppSecret',
  enabled = 1
WHERE platform = 'wechat' AND app_type = 'web';
```

### QQ 互联

1. https://connect.qq.com 申请
2. 拿到 AppID + AppKey
3. 配置回调: `https://api.your-domain.com/auth/oauth/qq/callback`

```sql
UPDATE oauth_app_config SET
  app_id = 'QQ AppID',
  app_secret = 'QQ AppKey',
  enabled = 1
WHERE platform = 'qq' AND app_type = 'web';
```

### 支付宝开放平台

1. https://open.alipay.com 申请"网页应用"
2. 拿到 AppID + RSA 私钥/公钥
3. 配置回调: `https://api.your-domain.com/auth/oauth/alipay/callback`

```sql
UPDATE oauth_app_config SET
  app_id = '支付宝 AppID',
  app_secret = 'RSA 私钥',
  public_key = '支付宝公钥',
  enabled = 1
WHERE platform = 'alipay' AND app_type = 'web';
```

注: 支付宝生产环境需要集成 `alipay-sdk-java` (RSA 签名).

---

## 跨平台统计

admin 端点 `/auth/admin/wechat/cross-platform-stats` 返回:

```json
{
  "bindingByPlatform": [...],
  "totalBindings": 10,
  "wechatUsers": 5,
  "qqUsers": 3,
  "alipayUsers": 2,
  "multiPlatformUsers": 1,
  "unionidRelations": 8
}
```

**业务指标**:
- `multiPlatformUsers` / `totalBindings` = 跨平台覆盖率
- 单平台用户 → 引导绑定更多平台
- 高 unionid 关联 → 用户活跃度高

---

## 🔗 相关文档

- [WECHAT-GUIDE.md](WECHAT-GUIDE.md) - 微信扫码登录
- [UNIONID-GUIDE.md](UNIONID-GUIDE.md) - unionid 跨应用
- [DEPLOY-README.md](../scripts/DEPLOY-README.md) - Linux 部署

## 📊 代码量

| 模块 | 行数 |
|------|------|
| OAuthPlatformClient 接口 | ~30 |
| WechatApiClientBridge | ~70 |
| QqOAuthClient | ~180 |
| AlipayOAuthClient | ~110 |
| OAuthPlatformService | ~360 |
| OAuthController | ~150 |
| OAuthBinding/AppConfig entity+mapper | ~80 |
| SysUser 加 10 字段 | ~30 |
| sql/init/22_v5_2_cross_platform.sql | ~120 |
| H5Login.vue | ~250 |
| utils/platform.js | ~30 |
| WechatUnionidService 扩 oauth_binding 查询 | ~40 |
| 跨平台统计端点 | ~70 |
| **合计** | **~1520 行** |

---

> MiniMax 大模型平台 · V5.2 跨平台 OAuth · 2026-06