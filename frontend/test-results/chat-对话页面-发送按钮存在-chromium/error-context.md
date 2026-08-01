# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: chat.spec.js >> 对话页面 >> 发送按钮存在
- Location: e2e/chat.spec.js:23:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('button:has-text("发送"), button:has-text("发送"), [class*="send"]').first()
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for locator('button:has-text("发送"), button:has-text("发送"), [class*="send"]').first()

```

```yaml
- heading "🚀 Liugl-AI Platform" [level=1]
- paragraph: 企业级 AI · 智能协作 · 自研大模型
- list:
  - listitem: ✅ 17 微服务 · 145+ 单元测试
  - listitem: 🤖 自研 AI (无外部 LLM 依赖)
  - listitem: 🎬 视频/音乐 实时流式 生成
  - listitem: 📊 7 种图表 / 看板 / 多模态
  - listitem: 🔐 RBAC + 审计 + 脱敏
  - listitem: 🌐 i18n 中英双语
- text: 12 微服务 211+ API 62 页面 532 i18n
- heading "Liugl-AI LLM Platform" [level=1]
- paragraph: Enterprise LLM Application Platform
- tablist:
  - tab "🔑 Login" [selected]
  - tab "📝 Register"
  - tab "📱 微信扫码"
- tabpanel "🔑 Login"
- img
- textbox "用户名"
- img
- textbox "密码"
- checkbox "记住我" [checked]
- text: 记住我 忘记密码？
- button "登录"
- separator: 快速登录（演示）
- text: 👑 adminLiugl (超管) 🔑 admin 👤 user
- alert:
  - img
  - text: "💡 演示账号:"
  - code: admin
  - text: /
  - code: admin@123
  - text: (或点击上方快速登录)
```

# Test source

```ts
  1  | // e2e/chat.spec.js
  2  | // 对话页面 E2E 测试
  3  | import { test, expect } from '@playwright/test'
  4  | 
  5  | test.describe('对话页面', () => {
  6  |   test.beforeEach(async ({ page }) => {
  7  |     // Mock 登录状态
  8  |     await page.goto('/login')
  9  |     await page.evaluate(() => {
  10 |       localStorage.setItem('access_token', 'mock-e2e-token')
  11 |       localStorage.setItem('refresh_token', 'mock-refresh')
  12 |       localStorage.setItem('user_info', JSON.stringify({ id: 1, username: 'admin', role: 'admin' }))
  13 |     })
  14 |     await page.goto('/chat')
  15 |     await page.waitForTimeout(800)
  16 |   })
  17 | 
  18 |   test('消息输入框存在', async ({ page }) => {
  19 |     const textarea = page.locator('textarea, [contenteditable="true"], input[class*="input"]').first()
  20 |     await expect(textarea).toBeVisible({ timeout: 5000 })
  21 |   })
  22 | 
  23 |   test('发送按钮存在', async ({ page }) => {
  24 |     const sendBtn = page.locator('button:has-text("发送"), button:has-text("发送"), [class*="send"]').first()
> 25 |     await expect(sendBtn).toBeVisible({ timeout: 5000 })
     |                           ^ Error: expect(locator).toBeVisible() failed
  26 |   })
  27 | 
  28 |   test('侧边栏会话列表区域存在', async ({ page }) => {
  29 |     // 检查侧边栏或会话区域
  30 |     const asideOrList = page.locator('.el-aside, .session-list, [class*="session"]').first()
  31 |     await expect(asideOrList).toBeVisible({ timeout: 3000 }).catch(() => {
  32 |       // 如果没找到，检查主布局
  33 |       expect(page.locator('#app')).toBeVisible()
  34 |     })
  35 |   })
  36 | 
  37 |   test('页面加载无 JS 崩溃错误', async ({ page }) => {
  38 |     const errors = []
  39 |     page.on('pageerror', e => errors.push(e.message))
  40 |     await page.reload()
  41 |     await page.waitForTimeout(1000)
  42 |     // 过滤掉资源加载错误（favicon 等）
  43 |     const criticalErrors = errors.filter(e =>
  44 |       !e.includes('favicon') && !e.includes('404') && !e.includes('net::')
  45 |     )
  46 |     expect(criticalErrors).toHaveLength(0)
  47 |   })
  48 | })
  49 | 
```