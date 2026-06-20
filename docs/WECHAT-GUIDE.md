# 📱 微信扫码登录 (V5) 完整指南

> MiniMax 大模型平台的微信生态集成方案
> 支持: 网站扫码登录 / 公众号 OAuth / 小程序静默登录 / 多应用 unionid 打通

---

## 📑 目录

- [整体架构](#整体架构)
- [数据库设计](#数据库设计)
- [API 端点](#api-端点)
- [沙箱演示](#沙箱演示)
- [生产部署](#生产部署)
- [公众号 OAuth](#公众号-oauth)
- [小程序对接](#小程序对接)
- [用户绑定管理](#用户绑定管理)
- [异常告警](#异常告警)
- [常见问题](#常见问题)

---

## 整体架构

```
┌────────────┐    1.扫码    ┌──────────┐
│  PC 浏览器 │◀────────────▶│  微信 App │
└────────────┘              └──────────┘
       │                            │
       │ 2.拉二维码 URL              │ 6.回调
       ▼                            ▼
┌────────────┐  3.轮询    ┌─────────────────┐
│  前端      │───────────▶│  auth 服务      │
│  Login.vue │            │  /auth/wechat/* │
└────────────┘  ◀──────── └─────────────────┘
       │  7.拿到 JWT          │
       │   自动登录           │ 4.换 access_token
       ▼                     ▼
┌────────────┐            ┌─────────────────┐
│  业务页    │            │ 微信开放平台 API │
└────────────┘            │ /sns/oauth2/*   │
                          └─────────────────┘
```

### 核心流程 (1-7 步)

| 步骤 | 描述 | 端点 |
|------|------|------|
| 1 | 用户打开登录页 → 选"微信扫码" tab | 前端 |
| 2 | 前端调 `/auth/wechat/qrcode` 拿 ticket + 二维码 URL | 后端 |
| 3 | 前端用 `qrcode` 库渲染二维码, 2s 轮询 `/auth/wechat/status` | 前端 |
| 4 | 用户用微信扫码 → 微信回调到 `/auth/wechat/callback?code=` | 微信 |
| 5 | 后端 `code` 换 `access_token` + `openid` (调微信 API) | 后端 |
| 6 | 后端拉用户信息, findOrCreateUser (openid 自动注册/绑定) | 后端 |
| 7 | 后端写 session.access_token, 前端轮询拿到 → 自动登录 | 后端 |

---

## 数据库设计

### 4 张微信表

#### 1. `wechat_scan_session` 二维码 ticket 状态机

```sql
CREATE TABLE wechat_scan_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket VARCHAR(64) UNIQUE NOT NULL,        -- 二维码唯一标识
    scene_id VARCHAR(64),                       -- 场景 ID
    status VARCHAR(20) DEFAULT 'pending',       -- pending/scanned/confirmed/expired
    user_id BIGINT,                             -- 确认后关联的用户 ID
    openid VARCHAR(64),                         -- 微信 openid
    unionid VARCHAR(64),                        -- 微信 unionid
    nickname VARCHAR(64),                       -- 微信昵称
    avatar VARCHAR(512),                        -- 微信头像
    access_token VARCHAR(512),                  -- JWT (confirmed 后写入)
    refresh_token VARCHAR(512),
    client_ip VARCHAR(64),                      -- 扫码 IP
    user_agent VARCHAR(512),                    -- User-Agent
    expires_at DATETIME,                        -- 过期时间 (5 分钟)
    confirmed_at DATETIME,                      -- 确认时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid),
    INDEX idx_status_expires (status, expires_at),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB;
```

#### 2. `wechat_user_binding` 多应用绑定

```sql
CREATE TABLE wechat_user_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,                    -- 平台用户 ID
    openid VARCHAR(64) NOT NULL,                -- 微信 openid
    unionid VARCHAR(64),                        -- unionid (跨应用)
    app_type VARCHAR(20) DEFAULT 'mp',          -- mp/mini/open/web
    nickname VARCHAR(64),
    avatar VARCHAR(512),
    bound_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    UNIQUE KEY uk_openid (openid),
    INDEX idx_unionid (unionid),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB;
```

#### 3. `wechat_config` 应用配置

```sql
CREATE TABLE wechat_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    app_type VARCHAR(20) NOT NULL,              -- mp/mini/open/web
    app_id VARCHAR(64) NOT NULL,
    app_secret VARCHAR(128) NOT NULL,
    redirect_uri VARCHAR(256),
    enabled TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_type (app_type)
) ENGINE=InnoDB;

-- 预置 3 种占位
INSERT INTO wechat_config (app_type, app_id, app_secret, enabled) VALUES
('mp',   'PLACEHOLDER_MP',   'PLACEHOLDER_MP_SECRET',   0),
('mini', 'PLACEHOLDER_MINI', 'PLACEHOLDER_MINI_SECRET', 0),
('open', 'PLACEHOLDER_OPEN', 'PLACEHOLDER_OPEN_SECRET', 0);
```

#### 4. `wechat_scan_log` 扫码日志

```sql
CREATE TABLE wechat_scan_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket VARCHAR(64),
    openid VARCHAR(64),
    user_id BIGINT,
    action VARCHAR(32),                         -- qrcode/scan/confirm/fail
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    detail VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket (ticket),
    INDEX idx_openid (openid),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;
```

### sys_user 加 5 字段

```sql
ALTER TABLE sys_user ADD COLUMN wechat_openid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN wechat_unionid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN wechat_nickname VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN wechat_avatar VARCHAR(512);
ALTER TABLE sys_user ADD COLUMN wechat_bound_at DATETIME;
ALTER TABLE sys_user ADD INDEX idx_wechat_openid (wechat_openid);
```

---

## API 端点

### 用户端 (公开)

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/wechat/qrcode` | 生成二维码 + ticket |
| GET | `/auth/wechat/status?ticket=` | 轮询扫码状态 (2s/次) |
| GET | `/auth/wechat/callback?code=&state=` | 微信回调 |
| GET | `/auth/wechat/mock-scan?ticket=` | 沙箱演示用 |
| POST | `/auth/wechat/mobile-login` {code, appType} | 公众号/小程序静默登录 |

### 用户端 (需登录)

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/wechat/binding/me` | 查我的绑定 |
| DELETE | `/auth/wechat/binding/me` | 解绑我的微信 |

### 管理端 (adminLiugl)

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/admin/wechat/bindings` | 列出全部绑定 |
| GET | `/auth/admin/wechat/find?openid=` | 按 openid 查找 |
| POST | `/auth/admin/wechat/bind` | 强制绑定 |
| DELETE | `/auth/admin/wechat/bind/{userId}` | 强制解绑 |

### 端点示例

#### 1) 生成二维码

```bash
$ curl http://localhost:8081/auth/wechat/qrcode
{
  "code": 0,
  "data": {
    "ticket": "t_f4fc06d832114724b044c921",
    "sceneId": "383655aadedd4037",
    "qrcodeUrl": "https://open.weixin.qq.com/connect/qrconnect?appid=...&redirect_uri=...#wechat_redirect",
    "expiresIn": 300,
    "expiresAt": "2026-06-20T01:38:24",
    "mock": true,
    "scanUrl": "/auth/wechat/mock-scan?ticket=t_f4fc06d832114724b044c921"
  }
}
```

#### 2) 轮询状态

```bash
# pending (未扫码)
$ curl "http://localhost:8081/auth/wechat/status?ticket=t_xxx"
{"data":{"status":"pending"}}

# confirmed (扫码 + 确认)
$ curl "http://localhost:8081/auth/wechat/status?ticket=t_xxx"
{"data":{
  "status":"confirmed",
  "openid":"oXyz1234",
  "nickname":"张三",
  "avatar":"https://thirdwx.qlogo.cn/...",
  "accessToken":"eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken":"...",
  "userId": 42
}}
```

#### 3) 用户绑定查询

```bash
$ curl -H "Authorization: Bearer <jwt>" http://localhost:8081/auth/wechat/binding/me
{
  "data": {
    "bound": true,
    "openid": "oXyz1234",
    "unionid": "uXyz5678",
    "nickname": "张三",
    "avatar": "https://thirdwx.qlogo.cn/...",
    "boundAt": "2026-06-20T01:48:35",
    "bindings": [
      {
        "userId": 5,
        "openid": "oXyz1234",
        "appType": "mp",
        "lastLoginAt": "2026-06-20T01:48:35"
      }
    ]
  }
}
```

---

## 沙箱演示

环境无 AppID, 默认走 **mock 模式** (`app_id` 以 `PLACEHOLDER` 开头):

```bash
# 1. 生成二维码
TICKET=$(curl -s http://localhost:8081/auth/wechat/qrcode | jq -r '.data.ticket')

# 2. 模拟扫码 (相当于用户用微信扫了码 + 点确认)
curl "http://localhost:8081/auth/wechat/mock-scan?ticket=$TICKET"

# 3. 轮询拿 token
TOKEN=$(curl -s "http://localhost:8081/auth/wechat/status?ticket=$TICKET" | jq -r '.data.accessToken')

# 4. 用 token 访问
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/auth/me
```

mock 模式下:
- openid: `mock_openid_<ticket前16>`
- nickname: `微信用户_<ticket前6>`
- avatar: `https://thirdwx.qlogo.cn/mmopen/mock/<ticket前8>.png`

---

## 生产部署

### 步骤 1: 申请微信应用

**网站扫码登录 (开放平台)**:
1. 登录 https://open.weixin.qq.com
2. 创建"网站应用" → 获得 AppID + AppSecret
3. 配置回调域: `api.your-domain.com`

**公众号 (OAuth 网页授权)**:
1. 登录 https://mp.weixin.qq.com
2. 公众号设置 → 功能设置 → 网页授权域名: `api.your-domain.com`

**小程序**:
1. 登录 https://mp.weixin.qq.com
2. 开发管理 → 开发设置 → 获得 AppID + AppSecret
3. 服务器域名: `api.your-domain.com`

### 步骤 2: 写 DB

```sql
UPDATE wechat_config SET
  app_id = 'wx你的AppID',
  app_secret = '你的AppSecret',
  redirect_uri = 'https://api.your-domain.com/auth/wechat/callback',
  enabled = 1
WHERE app_type = 'open';
```

### 步骤 3: 配置 nginx

```nginx
server {
    listen 443 ssl http2;
    server_name api.your-domain.com;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /auth/wechat/callback {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 步骤 4: 微信公众平台填写回调

| 应用类型 | 回调域 |
|---------|--------|
| 网站应用 | `api.your-domain.com` |
| 公众号 | `api.your-domain.com` |
| 小程序 | request 合法域名: `https://api.your-domain.com` |

---

## 公众号 OAuth

移动端微信内嵌场景, 用 `snsapi_base` 静默授权:

```
1. 前端跳 https://open.weixin.qq.com/connect/oauth2/authorize
   ?appid=xxx
   &redirect_uri=https%3A%2F%2Fapi.your-domain.com%2Fauth%2Fwechat%2Fmobile-login
   &response_type=code
   &scope=snsapi_base
   &state=mp#wechat_redirect

2. 微信重定向到 /auth/wechat/mobile-login?code=xxx&state=mp

3. 后端 POST /auth/wechat/mobile-login {code, appType="mp"}
   - 调微信 API 换 access_token + openid + unionid
   - findOrCreateUser
   - 返回 JWT
```

**前端集成**:

```javascript
// 在微信内打开页面时, 检查 cookie/localStorage, 没有 token 就跳授权
function checkWechatAuth() {
  const ua = navigator.userAgent.toLowerCase()
  if (ua.indexOf('micromessenger') === -1) return  // 非微信
  const token = localStorage.getItem('access_token')
  if (token) return

  // 跳微信授权
  const appId = '你的公众号AppID'
  const redirect = encodeURIComponent(window.location.origin + '/auth/wechat/mobile-login')
  window.location.href = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${appId}&redirect_uri=${redirect}&response_type=code&scope=snsapi_base&state=mp#wechat_redirect`
}
```

---

## 小程序对接

小程序端 `wx.login()` 拿 code → POST 后端换 JWT:

**小程序端** (`miniprogram/app.js`):

```javascript
App({
  onLaunch() {
    wx.login({
      success: res => {
        if (res.code) {
          wx.request({
            url: 'https://api.your-domain.com/auth/wechat/mobile-login',
            method: 'POST',
            data: { code: res.code, appType: 'mini' },
            success: r => {
              wx.setStorageSync('access_token', r.data.data.accessToken)
              wx.setStorageSync('refresh_token', r.data.data.refreshToken)
            }
          })
        }
      }
    })
  }
})
```

**后端处理** (`mini` appType 走不同的 API):

```java
// WechatApiClient.miniCode2Session(code, appId, appSecret)
// 调 https://api.weixin.qq.com/sns/jscode2session?appid=&secret=&js_code=&grant_type=authorization_code
// 返回 { openid, session_key, unionid }
```

---

## 用户绑定管理

### 用户侧: 我的微信 (Profile.vue)

进入 `/profile/wechat` 查看/解绑:

- 已绑定: 头像 + 昵称 + OpenID + 绑定时间 + 多应用列表 + 解绑按钮
- 未绑定: "去扫码绑定" 按钮

### 管理员侧: 微信绑定管理 (/admin/wechat)

adminLiugl 专属, 功能:
- **全部绑定**: 表格列全部 binding (含用户名 join), 支持搜索, 一键解绑
- **按 OpenID 查找**: 输入完整 openid 反查用户
- **强制绑定**: 给指定 user_id 强制绑 openid (用于客服迁移账号)

### 强制绑定 API 示例

```bash
ADMIN_TOKEN=$(curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}' | jq -r '.data.accessToken')

# 强制绑 user 5 到 openid "oXyz1234"
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8081/auth/admin/wechat/bind \
  -d '{
    "userId": 5,
    "openid": "oXyz1234",
    "unionid": "uXyz5678",
    "nickname": "老王",
    "appType": "mp"
  }'

# 列出全部绑定
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8081/auth/admin/wechat/bindings?limit=50"

# 强制解绑
curl -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8081/auth/admin/wechat/bind/5
```

---

## 异常告警

`WechatScanMonitorService` 检测:

| 告警码 | 触发条件 | 默认阈值 |
|--------|---------|---------|
| `qrcode_flood` | 单 IP 1 分钟内生成二维码 > N 次 | 10 次 |
| `confirm_fail_flood` | 单 IP 1 小时内确认失败 > N 次 | 5 次 |
| `multi_ip_scan` | 同一 openid 跨多 IP 扫码 | > 3 IP |

**配置** (`application.yml`):

```yaml
minimax:
  wechat:
    alert:
      ip-qrcode-threshold: 10
      ip-confirm-fail-threshold: 5
```

**告警方式** (扩展): 控制台 WARN 日志 + 可扩展到 alert_event 表 + 企业微信机器人

---

## 常见问题

### Q1: 微信回调失败?

A: 检查
1. 微信公众平台配置的回调域是否正确 (`api.your-domain.com`, 不带协议)
2. nginx 是否正确转发到后端
3. 后端日志 `redirect_uri 参数错误` 表示 AppID 对应的应用没有配置该回调域

### Q2: 沙箱模式自动启用?

A: 是, 当 `wechat_config.app_id` 以 `PLACEHOLDER` 或 `mock` 开头时, 自动走 mock 模式.
生产部署时填写真实 AppID 即可关闭 mock.

### Q3: 已登录用户扫码会怎样?

A: 当前实现是**新注册账号**. 如需"扫码绑定当前账号", 调用:
```
GET /auth/wechat/binding/me    查当前绑定状态
DELETE /auth/wechat/binding/me 解绑旧微信
重新扫码 → 自动绑定到当前 user_id
```

或使用管理端 `/auth/admin/wechat/bind` 强制绑定.

### Q4: 多用户共享同一 openid?

A: 通过 `wechat_user_binding` 表, openid 唯一. 如需允许多账号共享, 改表结构为 `app_type + openid` 联合唯一.

### Q5: unionid 怎么用?

A: 同一微信开放平台下多个应用 (公众号/小程序/App) 共享 unionid.
建议在扫码时存 unionid, 然后按 unionid 关联多应用账号.

### Q6: 二维码过期时间?

A: 默认 5 分钟 (`expiresIn: 300`). 可在 `WechatScanLoginService.generateSession` 修改.

### Q7: 如何禁用微信扫码登录?

A: 把 `wechat_config` 所有 app_type 的 `enabled` 设为 0, 后端会返回 "未启用" 错误.

---

## 🔗 相关模块

- [USER_GUIDE.md](USER_GUIDE.md) - 整体用户指南
- [BUILD.md](BUILD.md) - 打包部署
- [MODULES.md](MODULES.md) - 模块清单

## 📊 代码量

| 模块 | 行数 |
|------|------|
| WechatController | ~120 |
| WechatScanLoginService | ~340 |
| WechatBindingService | ~200 |
| WechatApiClient | ~120 |
| WechatScanMonitorService | ~140 |
| WechatBindingController | ~120 |
| WechatScanLogin.vue | ~200 |
| MyWechat.vue | ~120 |
| WechatBindings.vue | ~280 |
| WechatScanPage.vue | ~50 |
| Profile.vue | ~130 |
| **合计** | **~1820 行** |

---

> MiniMax 大模型平台 · V5 微信扫码登录 · 2026-06