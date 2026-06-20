# 🔗 UnionID 跨应用打通指南 (V5.1)

> 同一微信开放平台下, 公众号/小程序/App 共享账号
> 用户在一个应用绑定, 其他应用自动识别为同一账号

---

## 📑 目录

- [背景](#背景)
- [整体架构](#整体架构)
- [数据库设计](#数据库设计)
- [查找/创建流程](#查找创建流程)
- [API 端点](#api-端点)
- [小程序对接](#小程序对接)
- [公众号 OAuth](#公众号-oauth)
- [账号合并](#账号合并)
- [沙箱演示](#沙箱演示)
- [生产部署](#生产部署)
- [常见问题](#常见问题)

---

## 背景

微信生态下, 同一个用户在不同应用有不同的 `openid`:
- 公众号: `oXyz_ABC123` (网页授权 openid)
- 小程序: `oXyz_DEF456` (小程序 openid, **完全不同**)
- App: `oXyz_GHI789` (移动应用 openid, **完全不同**)

但 **unionid 是同一个** (前提: 同一微信开放平台账号下申请的应用).

**没有 unionid 的痛点**:
- 用户在公众号扫码登录后, 换小程序又要重新注册
- 用户在公众号绑定了手机号, 换小程序得重新绑定
- 用户在不同应用看到不同的"历史记录"

**有 unionid 的解决方案**:
- 一个 unionid → 一个平台账号
- 用户在不同应用扫码, 自动识别为同一账号
- 历史记录、聊天、订单跨应用共享

---

## 整体架构

```
┌──────────────────────────┐
│   微信开放平台账号         │
│   ┌──────────────────┐   │
│   │ unionid: U_xyz   │ ← 同一用户跨应用唯一
│   └──────────────────┘   │
│      │         │         │
│      ▼         ▼         │
│   公众号      小程序      │
│   openid     openid      │ ← 每个应用不同
│   A_xxx      B_yyy       │
└──────────────────────────┘
        │
        │  (扫码/code 换 access_token)
        ▼
┌──────────────────────────────────────┐
│  MiniMax 平台                         │
│                                       │
│   wechat_user_binding                │
│   ┌────────┬────────┬─────────┐      │
│   │ user_id│ app    │ unionid │      │
│   ├────────┼────────┼─────────┤      │
│   │   42   │  mp    │  U_xyz  │      │
│   │   42   │  mini  │  U_xyz  │      │
│   └────────┴────────┴─────────┘      │
│                                       │
│   unionid_relations                   │
│   ┌────────┬─────────┬──────────┐    │
│   │ user_id│ unionid │ count    │    │
│   ├────────┼─────────┼──────────┤    │
│   │   42   │  U_xyz  │    2     │    │
│   └────────┴─────────┴──────────┘    │
└──────────────────────────────────────┘
```

---

## 数据库设计

### 表 1: `wechat_user_binding` (V5.1 升级)

唯一约束从 `openid` 改为 `(app_type, openid)`:

```sql
-- V5.0:
UNIQUE KEY uk_openid (openid)

-- V5.1:
UNIQUE KEY uk_app_openid (app_type, openid)
```

原因: 同一微信在不同应用 openid 不同, 唯一约束应区分 app_type.

### 表 2: `unionid_relations` (新增)

```sql
CREATE TABLE unionid_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    unionid VARCHAR(64) NOT NULL,
    platform VARCHAR(32) DEFAULT 'wechat',  -- wechat/qq/alipay
    first_seen_at DATETIME,
    last_seen_at DATETIME,
    binding_count INT DEFAULT 1,           -- 该 user 下该 unionid 关联了几个 app_type
    UNIQUE KEY uk_user_unionid (user_id, unionid),
    UNIQUE KEY uk_unionid (unionid),
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform)
);
```

### 关系

```
sys_user 1 ── N wechat_user_binding      (一个用户多个 app binding)
sys_user 1 ── N unionid_relations         (一个用户可能多个 unionid, 来自不同平台)
wechat_user_binding N ── 1 unionid_relations  (binding 共享 unionid)
```

---

## 查找/创建流程

`WechatScanLoginService.findOrCreateUser(appType, openid, unionid, nickname, avatar, mockMode)`:

```
┌──────────────────────────────────────────────────────────┐
│  Step 1: binding (app_type + openid) 精确匹配             │
│                                                          │
│    SELECT * FROM wechat_user_binding                     │
│    WHERE app_type=? AND openid=?                          │
│                                                          │
│    找到 → 直接登录, 更新 last_login_at                    │
└──────────────────────────────────────────────────────────┘
                            ↓ 找不到
┌──────────────────────────────────────────────────────────┐
│  Step 2: ★ unionid 跨应用打通                            │
│                                                          │
│    SELECT * FROM wechat_user_binding                     │
│    WHERE unionid=? AND app_type<>?                       │
│                                                          │
│    找到 → 复用 user_id, 新增 binding (新 app_type)        │
└──────────────────────────────────────────────────────────┘
                            ↓ 找不到
┌──────────────────────────────────────────────────────────┐
│  Step 3: sys_user.wechat_openid 主应用号                  │
│                                                          │
│    SELECT * FROM sys_user WHERE wechat_openid=?          │
│                                                          │
│    找到 → 写新 binding                                    │
└──────────────────────────────────────────────────────────┘
                            ↓ 找不到
┌──────────────────────────────────────────────────────────┐
│  Step 4: 新建账号                                         │
│                                                          │
│    INSERT sys_user + INSERT wechat_user_binding +        │
│    INSERT unionid_relations                              │
└──────────────────────────────────────────────────────────┘
```

---

## API 端点

### 用户端

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/wechat/unionid/me` | 我的所有应用绑定 (unionid 关联) |
| POST | `/auth/wechat/mobile-login` | 公众号/小程序静默登录 |

### 管理端 (adminLiugl)

| Method | Path | 用途 |
|--------|------|------|
| GET | `/auth/admin/wechat/unionid-relations` | 列出所有 unionid 关联 |
| GET | `/auth/admin/wechat/users-by-unionid` | 按 unionid 查用户 |
| POST | `/auth/admin/wechat/merge-accounts` | 合并重复账号 |

### 端点示例

#### mobile-login (公众号/小程序 通用)

```bash
# 小程序: wx.login() 拿到 code
curl -X POST http://localhost:8081/auth/wechat/mobile-login \
  -H "Content-Type: application/json" \
  -d '{"code":"wx.login() 拿到的 code", "appType":"mini"}'

# 公众号: 用户在微信内打开页面, 触发 OAuth 拿到 code
curl -X POST http://localhost:8081/auth/wechat/mobile-login \
  -H "Content-Type: application/json" \
  -d '{"code":"OAuth callback 拿到的 code", "appType":"mp"}'
```

返回:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "user": { "id": 42, "nickname": "张三" }
}
```

#### 按 unionid 查用户

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8081/auth/admin/wechat/users-by-unionid?unionid=test_unionid_TU_d"
```

返回:
```json
[{
  "userId": 3,
  "username": "wx_...",
  "nickname": "微信用户",
  "unionid": "test_unionid_TU_d",
  "bindingCount": 2,
  "bindings": [
    {"appType":"mp", "openid":"...", "lastLoginAt":"..."},
    {"appType":"mini", "openid":"...", "lastLoginAt":"..."}
  ]
}]
```

#### 合并账号

```bash
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8081/auth/admin/wechat/merge-accounts \
  -d '{"userToId":3,"userFromId":4,"reason":"用户投诉有两个账号"}'
```

效果:
- user 4 的所有 binding 转给 user 3
- user 4 的所有 unionid_relations 转给 user 3
- user 4 软删 (status=0, nickname 加 `[已合并]`)

---

## 小程序对接

**小程序前端** (`app.js`):

```javascript
App({
  onLaunch() {
    // 微信小程序登录
    wx.login({
      success: res => {
        if (res.code) {
          wx.request({
            url: 'https://api.your-domain.com/auth/wechat/mobile-login',
            method: 'POST',
            data: {
              code: res.code,
              appType: 'mini'   // 关键: 告诉后端这是小程序
            },
            success: r => {
              if (r.data.code === 0) {
                wx.setStorageSync('access_token', r.data.data.accessToken)
                wx.setStorageSync('refresh_token', r.data.data.refreshToken)
                // 后续请求带 token
              }
            }
          })
        }
      }
    })
  }
})
```

**后端处理** (`mini` appType):

```java
// WechatScanLoginService.mobileLogin()
if ("mini".equals(appType)) {
    // 小程序走 jscode2session API
    Map<String, Object> session = wechatApi.jscode2session(appId, appSecret, code);
    openid = (String) session.get("openid");
    unionid = (String) session.get("unionid");
    // 不需要后续 userinfo 调用 (unionid 已经拿到)
}
```

**API 区别**:

| API | 适用 | 返回 unionid |
|-----|------|--------------|
| `/sns/oauth2/access_token` | 公众号/网站 | ✅ (需要 unionid 机制开通) |
| `/sns/jscode2session` | 小程序 | ✅ (同一开放平台自动) |

---

## 公众号 OAuth

**前端** (微信内嵌页面):

```javascript
// 在微信内打开页面时
function checkWechatAuth() {
  const ua = navigator.userAgent.toLowerCase()
  if (ua.indexOf('micromessenger') === -1) return  // 非微信
  const token = localStorage.getItem('access_token')
  if (token) return

  // 跳微信授权
  const appId = '公众号 AppID'
  const redirect = encodeURIComponent(window.location.origin + '/auth/wechat/mobile-login')
  window.location.href =
    `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${appId}` +
    `&redirect_uri=${redirect}&response_type=code&scope=snsapi_base&state=mp#wechat_redirect`
}
```

**后端**:

```bash
# 用户扫码/授权 → 微信重定向到:
# https://your-domain.com/auth/wechat/mobile-login?code=xxx&state=mp

# 前端拿到 code 后, POST 给后端:
curl -X POST http://localhost:8081/auth/wechat/mobile-login \
  -d '{"code":"OAuth callback code", "appType":"mp"}'
```

---

## 账号合并

### 场景

用户投诉: "我有两个账号, 帮我合并"

### 手动合并 (管理员端点)

```bash
POST /auth/admin/wechat/merge-accounts
{
  "userToId": 3,      // 保留账号
  "userFromId": 4,    // 合并后软删
  "reason": "用户投诉有两个账号"
}
```

### 自动化合并 (推荐方案)

1. **首次扫码 → 新建账号, 引导用户绑定手机号/邮箱**
2. **二次扫码 → 检测到同手机号/邮箱 → 提示用户是否合并**
3. **用户确认 → 自动调用 merge-accounts**

```javascript
// 前端在二次扫码后
if (existingUserByPhone && existingUserByPhone.id !== newUserId) {
  ElMessageBox.confirm(
    `检测到您的微信已绑定其他账号, 是否合并到当前账号?`,
    '合并账号',
    { type: 'warning' }
  ).then(() => {
    api.post('/auth/admin/wechat/merge-accounts', {
      userToId: newUserId,
      userFromId: existingUserByPhone.id,
      reason: '用户确认合并'
    })
  })
}
```

### 合并效果

- ✅ 所有 binding 转移到目标账号
- ✅ 所有 unionid_relations 转移
- ✅ 源账号软删 (status=0, 不影响历史订单/聊天记录)
- ✅ 用户的聊天记录、订单、知识库 全部保留在目标账号

---

## 沙箱演示

环境无 AppID, 默认走 mock 模式. **mock 模式额外支持 unionid 测试**:

- 普通 code: 返回 `mock_openid_<code前8>` 和 `mock_unionid_<code前8>` (openid ≠ unionid 的简化场景)
- **TU_ prefix** 的 code: 返回 `mock_openid_<code>` 和 `test_unionid_<code前4>` (同 prefix → 同 unionid, 模拟跨应用打通)

### 演示步骤

```bash
# Step 1: 公众号扫码 (模拟用户 A 在公众号登录)
curl -X POST http://localhost:8081/auth/wechat/mobile-login \
  -d '{"code":"TU_demo1234_mp", "appType":"mp"}'
# → user 3 创建, binding(mp, openid=TU_demo1234_mp, unionid=test_unionid_TU_d)

# Step 2: 小程序登录 (同用户 A 在小程序登录, 期望识别为同一账号)
curl -X POST http://localhost:8081/auth/wechat/mobile-login \
  -d '{"code":"TU_demo1234_mini", "appType":"mini"}'
# → 复用 user 3, 新增 binding(mini, openid=TU_demo1234_mini, unionid=test_unionid_TU_d)

# Step 3: 验证
mysql -e "SELECT user_id, app_type, openid, unionid FROM wechat_user_binding WHERE user_id=3;"
# 应该有 2 行 (mp + mini)
```

---

## 生产部署

### 1. 微信开放平台申请

1. 登录 https://open.weixin.qq.com
2. 创建应用: 公众号 / 小程序 / 网站应用 / 移动应用
3. **关键**: 所有应用必须绑定到**同一个开放平台账号**, 否则 unionid 不通

### 2. 配置 wechat_config

```sql
UPDATE wechat_config SET
  app_id = '公众号 AppID', app_secret = '公众号 AppSecret',
  redirect_uri = 'https://api.your-domain.com/auth/wechat/mobile-login',
  enabled = 1
WHERE app_type = 'mp';

UPDATE wechat_config SET
  app_id = '小程序 AppID', app_secret = '小程序 AppSecret',
  enabled = 1
WHERE app_type = 'mini';
```

### 3. 验证 unionid 获取

- 公众号: 必须勾选"用户授权 scope: snsapi_userinfo" 或 "unionid 机制" (网页应用默认返回 unionid)
- 小程序: 必须绑定到开放平台 → jscode2session 自动返回 unionid
- 网站应用: 固定返回 unionid

### 4. 数据库迁移

```bash
# V5.0 → V5.1 升级
mysql -uroot -p"${DB_PASS}" minimax_platform < sql/init/21_v5_1_unionid.sql
```

---

## 常见问题

### Q1: 同一个微信, 在不同开放平台的应用, unionid 会一样吗?

A: **不会**. 必须绑定到**同一个开放平台账号**才能 unionid 打通.

### Q2: 公众号 snsapi_base 静默授权能拿 unionid 吗?

A: 能. `code2AccessToken` 返回 access_token + openid + unionid, 后续可用 `userinfo` 拿昵称头像.

### Q3: 小程序必须授权才能拿 unionid 吗?

A: 不是. `wx.login()` 拿到 code → `jscode2session` 换 session → 直接返回 unionid (无需用户授权).

### Q4: 跨应用打通后, 用户聊天记录会跨应用共享吗?

A: 取决于业务. unionid 打通只是"账号共享", 具体业务 (聊天/订单/知识库) 仍按 user_id 查询.
如果希望跨应用共享, 把 user_id 作为关联键即可 (如 `chat_record.user_id` 跨应用).

### Q5: 一个用户能绑定多少个 unionid?

A: 理论上无限制. `unionid_relations` 1 对 N, 一个 user_id 可以有多个 unionid (例如: 同时用微信 + QQ 登录).

### Q6: 合并账号后, 软删的用户能恢复吗?

A: 能. 合并是软删 (status=0), 数据库记录还在. 管理员可以 `UPDATE sys_user SET status=1 WHERE id=?` 恢复.

### Q7: 微信开放平台"用户标识 unionid 转换"功能?

A: 是开放平台给未绑定的应用提供的 unionid 转换 API.
本平台不需要 — 我们直接用 `userinfo` 拿 unionid (前提是同一开放平台).

---

## 🔗 相关文档

- [WECHAT-GUIDE.md](WECHAT-GUIDE.md) - 微信扫码登录基础
- [USER_GUIDE.md](USER_GUIDE.md) - 用户指南
- [MODULES.md](MODULES.md) - 模块清单

## 📊 代码量

| 模块 | 行数 |
|------|------|
| WechatScanLoginService (findOrCreateUser 重构) | +80 |
| WechatApiClient (jscode2session) | +30 |
| WechatUnionidService | ~250 |
| WechatUnionidController | ~120 |
| UnionidRelations entity + mapper | ~60 |
| sql/init/21_v5_1_unionid.sql | ~60 |
| CrossAppBinding.vue | ~170 |
| WechatUnionidAdmin.vue | ~270 |
| **合计** | **~1040 行** |

---

> MiniMax 大模型平台 · V5.1 unionid 跨应用打通 · 2026-06