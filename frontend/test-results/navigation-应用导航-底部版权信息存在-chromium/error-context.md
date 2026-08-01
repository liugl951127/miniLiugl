# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: navigation.spec.js >> 应用导航 >> 底部版权信息存在
- Location: e2e/navigation.spec.js:53:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('text=Copyright,text=MiniMax').first()
Expected: visible
Timeout: 3000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 3000ms
  - waiting for locator('text=Copyright,text=MiniMax').first()

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
  1  | // e2e/navigation.spec.js
  2  | // 导航与侧边栏 E2E 测试
  3  | import { test, expect } from '@playwright/test'
  4  | 
  5  | test.describe('应用导航', () => {
  6  |   // 使用 localStorage mock 登录状态，跳过真实登录
  7  |   test.beforeEach(async ({ page }) => {
  8  |     await page.goto('/login')
  9  |     // Mock 已登录状态：注入 accessToken 到 localStorage
  10 |     await page.evaluate(() => {
  11 |       localStorage.setItem('access_token', 'mock-token-for-e2e')
  12 |       localStorage.setItem('refresh_token', 'mock-refresh-token')
  13 |       localStorage.setItem('user_info', JSON.stringify({ id: 1, username: 'admin', role: 'admin' }))
  14 |     })
  15 |   })
  16 | 
  17 |   test('侧边栏菜单可见', async ({ page }) => {
  18 |     await page.goto('/chat')
  19 |     await page.waitForTimeout(500)
  20 |     // 检查侧边栏存在
  21 |     const sidebar = page.locator('.sidebar, .el-aside, [class*="sidebar"]').first()
  22 |     await expect(sidebar).toBeVisible({ timeout: 5000 }).catch(() => {
  23 |       // 如果侧边栏 class 不确定，检查 layout 容器
  24 |       expect(page.locator('.app-layout, #app')).toBeVisible()
  25 |     })
  26 |   })
  27 | 
  28 |   test('知识库页面可访问', async ({ page }) => {
  29 |     await page.goto('/knowledge')
  30 |     await page.waitForTimeout(1000)
  31 |     // 页面应显示内容（知识库标题或 tab）
  32 |     await expect(page.locator('text=知识库,text=我的知识库').first()).toBeVisible({ timeout: 5000 })
  33 |   })
  34 | 
  35 |   test('API Key 页面可访问', async ({ page }) => {
  36 |     await page.goto('/knowledge') // apikey 在 knowledge 下通过菜单
  37 |     await page.waitForTimeout(500)
  38 |     // 验证页面加载无 JS 错误
  39 |     const errors = []
  40 |     page.on('pageerror', e => errors.push(e.message))
  41 |     await page.reload()
  42 |     await page.waitForTimeout(500)
  43 |     expect(errors.filter(e => !e.includes('favicon'))).toHaveLength(0)
  44 |   })
  45 | 
  46 |   test('监控页面可访问', async ({ page }) => {
  47 |     await page.goto('/admin/monitor')
  48 |     await page.waitForTimeout(1000)
  49 |     // 页面标题或内容可见
  50 |     await expect(page.locator('text=监控,text=健康,text=告警').first()).toBeVisible({ timeout: 5000 })
  51 |   })
  52 | 
  53 |   test('底部版权信息存在', async ({ page }) => {
  54 |     await page.goto('/chat')
  55 |     await page.waitForTimeout(500)
> 56 |     await expect(page.locator('text=Copyright,text=MiniMax').first()).toBeVisible({ timeout: 3000 })
     |                                                                       ^ Error: expect(locator).toBeVisible() failed
  57 |   })
  58 | })
  59 | 
```