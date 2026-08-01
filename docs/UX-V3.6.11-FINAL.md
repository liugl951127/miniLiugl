# V3.6.11 清理 + 模拟登录 + 跳转测试

## 1. V3.6.10 之后

V3.6.10 加了 useToast + typewriter + healthScore + EmptyState/ErrorState 组件。V3.6.11 继续清理 + 测试:
- **清理 .bak / .backup 文件** (V3.6.11+ 17 .bak + 5 目录 + 1 .jwt-backup 删)
- **scripts/simulate-login.sh** (演示模式 ?demo=1 + 21 路由 200 验证)
- **scripts/simulate-jwt.sh** (5 测试账号 + Mock JWT + localStorage 注入 + 21 路由带 Authorization)
- **.gitignore 增强** (.view-*-backup/ + .jwt-backup*/ + package.json.*.bak + *.vue.*.bak)

## 2. V3.6.11 改

### 2.1 清理历史 bak/backup (V3.6.11+)

**清理列表**:
- 5 个 `.view-*-backup/` 目录 (V3.5.74/80/81/82/92 历史)
- 1 个 `.jwt-backup-20260711-021545/` 目录
- 17 个 `*.bak` 文件
- `frontend/package.json.v3.5.73.bak`

**`.gitignore` 增强**:
```gitignore
# V3.6.11+ 清理历史 bak/backup 目录
.view-*-backup/
.jwt-backup*/
package.json.*.bak
*.vue.*.bak
```

**已忽略** (V3.5.97+):
- `*.v3.5.*.bak`
- `*.v3.5.79.bak`
- `*.v3.5.73.bak`
- `*.v3.5.95.bak`
- `*.v3.5.96.bak`
- `*.v3.5.97.bak`

### 2.2 scripts/simulate-login.sh (V3.6.11+)

**演示模式** (`?demo=1` 跳过 auth API):

```bash
$ bash scripts/simulate-login.sh
═══════════════════════════════════════════════════════════
  V3.6.11+ 模拟登录 + 跳转测试
═══════════════════════════════════════════════════════════
  Frontend: http://localhost:3000
  Auth:     9001
  User:     admin
═══════════════════════════════════════════════════════════

--- 21 路由跳转测试 (演示模式 ?demo=1) ---
  ✓ / → 200
  ✓ /login → 200
  ✓ /admin/dashboard → 200
  ...
  ✓ /admin/framework → 200

--- 总结 ---
  ✓ 通过: 21/21
  ❌ 失败: 0/21
  🎉 全部跳转正常 (演示模式)
```

**特性**:
- ✅ 21 路由逐一测试
- ✅ 演示模式 `?demo=1` (V3.5.93+ 跳过 auth)
- ✅ Auth 真实登录 attempt (沙箱无后端时优雅降级)
- ✅ 可自定义 USERNAME / PASSWORD

### 2.3 scripts/simulate-jwt.sh (V3.6.11+ 5 账号 + Mock JWT)

**5 测试账号** (V3.5.5+ BCrypt):
- `admin / admin123` (SUPER_ADMIN)
- `adminLiugl / liugl951127` (SUPER_ADMIN)
- `operator / operator123` (OPERATOR)
- `auditor / auditor123` (AUDITOR)
- `user / user123` (USER)

**Mock JWT** (3 段):
- `header` — base64({"alg":"HS256","typ":"JWT"})
- `payload` — base64({sub, username, roles, iat, exp})
- `signature` — `sim-v3.6.11-{user}-{role}` (沙箱友好, 不验签)

**localStorage 注入** (DevTools Console):
```js
const mockUser = {
  id: 20176,
  username: 'admin',
  roles: ['SUPER_ADMIN'],
  ...
}
localStorage.setItem('minimax_user', JSON.stringify(mockUser))
localStorage.setItem('minimax_token', 'eyJhbGc...sim-v3.6.11-admin-SUPER_ADMIN')
localStorage.setItem('minimax_demo_mode', 'false')
console.log('✅ 模拟登录成功: admin / SUPER_ADMIN')
```

**21 路由 + Authorization**:
```bash
$ bash scripts/simulate-jwt.sh
--- 21 路由跳转测试 (带 mock Authorization) ---
  ✓ / → 200
  ✓ /admin/dashboard → 200
  ...
  ✓ /admin/framework → 200
--- 总结 ---
  ✓ 通过: 21/21
```

### 2.4 .gitignore 增强

| 规则 | 说明 |
|------|------|
| `.view-*-backup/` | 整目录忽略 |
| `.jwt-backup*/` | JWT 备份目录 |
| `package.json.*.bak` | 旧 package.json |
| `*.vue.*.bak` | 旧 .vue |

## 3. 验证

| 测试 | 结果 |
|------|------|
| **simulate-login.sh** 21 路由 | ✅ 21/21 200 |
| **simulate-jwt.sh** 21 路由 + Bearer | ✅ 21/21 200 |
| 演示模式 (`?demo=1`) | ✅ 跳过 auth |
| 真实 Auth attempt | ⚠️ 沙箱无后端 (优雅降级) |
| **ci-check 11/11** | ✅ < 3s |
| vite build 0 错 | ✅ |
| 21 路由 ROUNDS=90 | ✅ 1890 GET 100% pass |

## 4. 累计 66 个版本 (V3.5.46-V3.6.11)
