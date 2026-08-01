# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: login.spec.js >> 登录页面 >> 3 个登录 tab 存在
- Location: e2e/login.spec.js:14:3

# Error details

```
Error: expect(locator).toContainText(expected) failed

Locator: locator('.el-tabs__item').first()
Expected substring: "账号密码"
Received string:    "🔑 Login"
Timeout: 10000ms

Call log:
  - Expect "toContainText" with timeout 10000ms
  - waiting for locator('.el-tabs__item').first()
    23 × locator resolved to <div role="tab" tabindex="0" id="tab-login" aria-selected="true" aria-controls="pane-login" class="el-tabs__item is-top is-active">…</div>
       - unexpected value "🔑 Login"

```

```yaml
- tab "🔑 Login" [selected]
```

# Test source

```ts
  1  | // e2e/login.spec.js
  2  | // 登录页 E2E 测试
  3  | import { test, expect } from '@playwright/test'
  4  | 
  5  | test.describe('登录页面', () => {
  6  |   test.beforeEach(async ({ page }) => {
  7  |     await page.goto('/login')
  8  |   })
  9  | 
  10 |   test('页面标题正确', async ({ page }) => {
  11 |     await expect(page).toHaveTitle(/MiniMax/i)
  12 |   })
  13 | 
  14 |   test('3 个登录 tab 存在', async ({ page }) => {
> 15 |     await expect(page.locator('.el-tabs__item').nth(0)).toContainText('账号密码')
     |                                                         ^ Error: expect(locator).toContainText(expected) failed
  16 |     await expect(page.locator('.el-tabs__item').nth(1)).toContainText('注册')
  17 |     await expect(page.locator('.el-tabs__item').nth(2)).toContainText('微信扫码')
  18 |   })
  19 | 
  20 |   test('默认显示账号密码登录表单', async ({ page }) => {
  21 |     await expect(page.locator('input[placeholder="用户名"]')).toBeVisible()
  22 |     await expect(page.locator('input[placeholder="密码"]')).toBeVisible()
  23 |   })
  24 | 
  25 |   test('默认账号预填 admin/admin@123', async ({ page }) => {
  26 |     const usernameInput = page.locator('input[placeholder="用户名"]')
  27 |     const passwordInput = page.locator('input[placeholder="密码"]')
  28 |     await expect(usernameInput).toHaveValue('admin')
  29 |     await expect(passwordInput).toHaveValue('admin@123')
  30 |   })
  31 | 
  32 |   test('切换到注册 tab 清空表单', async ({ page }) => {
  33 |     await page.locator('.el-tabs__item').nth(1).click()
  34 |     await expect(page.locator('input[placeholder="用户名"]')).toHaveValue('')
  35 |     await expect(page.locator('input[placeholder="密码"]')).toHaveValue('')
  36 |   })
  37 | 
  38 |   test('切换到微信扫码 tab 显示扫码组件', async ({ page }) => {
  39 |     await page.locator('.el-tabs__item').nth(2).click()
  40 |     await expect(page.locator('.wechat-tab')).toBeVisible()
  41 |   })
  42 | 
  43 |   test('用户名过短提示', async ({ page }) => {
  44 |     await page.locator('input[placeholder="用户名"]').fill('ab')
  45 |     await page.locator('input[placeholder="密码"]').click() // 触发 blur
  46 |     await expect(page.locator('.el-form-item__error').first()).toBeVisible()
  47 |   })
  48 | 
  49 |   test('登录按钮存在且可点击', async ({ page }) => {
  50 |     const btn = page.locator('button:has-text("登录")')
  51 |     await expect(btn).toBeVisible()
  52 |     await expect(btn).toBeEnabled()
  53 |   })
  54 | })
  55 | 
```