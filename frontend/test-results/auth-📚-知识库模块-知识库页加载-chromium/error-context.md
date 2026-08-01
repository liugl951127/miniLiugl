# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: auth.spec.js >> 📚 知识库模块 >> 知识库页加载
- Location: e2e/auth.spec.js:73:3

# Error details

```
Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:5173/login
Call log:
  - navigating to "http://localhost:5173/login", waiting until "load"

```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test'
  2   | 
  3   | /**
  4   |  * V5.33 Day 23: Playwright E2E 测试套件
  5   |  * 覆盖：登录 / 对话 / 知识库 / API Key / 监控
  6   |  */
  7   | 
  8   | const BASE = process.env.BASE_URL || 'http://localhost:5173'
  9   | const ADMIN_USER = process.env.E2E_USER || 'admin'
  10  | const ADMIN_PWD  = process.env.E2E_PWD  || 'admin@123'
  11  | 
  12  | test.describe('🔐 认证模块', () => {
  13  |   test('登录页正常加载', async ({ page }) => {
  14  |     await page.goto(BASE)
  15  |     // 等待页面渲染
  16  |     await expect(page.locator('h1')).toContainText('MiniMax', { timeout: 10_000 })
  17  |     // Tab 存在
  18  |     await expect(page.getByRole('tab', { name: /密码/ })).toBeVisible()
  19  |   })
  20  | 
  21  |   test('默认账号提示可见', async ({ page }) => {
  22  |     await page.goto(`${BASE}/login`)
  23  |     await expect(page.getByText('admin')).toBeVisible({ timeout: 8_000 })
  24  |   })
  25  | 
  26  |   test('密码登录成功', async ({ page }) => {
  27  |     await page.goto(`${BASE}/login`)
  28  |     await page.getByPlaceholder('用户名').fill(ADMIN_USER)
  29  |     await page.getByPlaceholder('密码').fill(ADMIN_PWD)
  30  |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  31  |     // 登录后跳转到 chat
  32  |     await page.waitForURL('**/chat', { timeout: 15_000 })
  33  |     await expect(page.url()).toContain('/chat')
  34  |   })
  35  | 
  36  |   test('空密码提示必填', async ({ page }) => {
  37  |     await page.goto(`${BASE}/login`)
  38  |     await page.getByPlaceholder('用户名').fill('admin')
  39  |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  40  |     // Element Plus 验证提示
  41  |     await expect(page.locator('.el-form-item__error').first()).toBeVisible({ timeout: 5_000 })
  42  |   })
  43  | })
  44  | 
  45  | test.describe('💬 对话模块', () => {
  46  |   test.beforeEach(async ({ page }) => {
  47  |     // 先登录
  48  |     await page.goto(`${BASE}/login`)
  49  |     await page.getByPlaceholder('用户名').fill(ADMIN_USER)
  50  |     await page.getByPlaceholder('密码').fill(ADMIN_PWD)
  51  |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  52  |     await page.waitForURL('**/chat', { timeout: 15_000 })
  53  |   })
  54  | 
  55  |   test('Chat 页加载', async ({ page }) => {
  56  |     await expect(page.locator('.chat-container, .chat-page, textarea')).toBeVisible({ timeout: 8_000 })
  57  |   })
  58  | 
  59  |   test('模型选择器存在', async ({ page }) => {
  60  |     await expect(page.locator('select, [class*="model"]').first()).toBeVisible({ timeout: 8_000 })
  61  |   })
  62  | })
  63  | 
  64  | test.describe('📚 知识库模块', () => {
  65  |   test.beforeEach(async ({ page }) => {
> 66  |     await page.goto(`${BASE}/login`)
      |                ^ Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:5173/login
  67  |     await page.getByPlaceholder('用户名').fill(ADMIN_USER)
  68  |     await page.getByPlaceholder('密码').fill(ADMIN_PWD)
  69  |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  70  |     await page.waitForURL('**/chat', { timeout: 15_000 })
  71  |   })
  72  | 
  73  |   test('知识库页加载', async ({ page }) => {
  74  |     await page.goto(`${BASE}/knowledge`)
  75  |     await expect(page.getByText(/知识库/, { exact: false }).first()).toBeVisible({ timeout: 8_000 })
  76  |   })
  77  | 
  78  |   test('新建知识库按钮存在', async ({ page }) => {
  79  |     await page.goto(`${BASE}/knowledge`)
  80  |     await expect(page.getByRole('button', { name: /新建知识库/ })).toBeVisible({ timeout: 8_000 })
  81  |   })
  82  | 
  83  |   test('检索问答 Tab 切换', async ({ page }) => {
  84  |     await page.goto(`${BASE}/knowledge`)
  85  |     await page.getByRole('tab', { name: /检索/ }).click()
  86  |     await expect(page.locator('textarea, [placeholder*="问题"]').first()).toBeVisible({ timeout: 5_000 })
  87  |   })
  88  | })
  89  | 
  90  | test.describe('🔑 API Key 模块', () => {
  91  |   test.beforeEach(async ({ page }) => {
  92  |     await page.goto(`${BASE}/login`)
  93  |     await page.getByPlaceholder('用户名').fill(ADMIN_USER)
  94  |     await page.getByPlaceholder('密码').fill(ADMIN_PWD)
  95  |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  96  |     await page.waitForURL('**/chat', { timeout: 15_000 })
  97  |   })
  98  | 
  99  |   test('API Key 页加载', async ({ page }) => {
  100 |     await page.goto(`${BASE}/apikey`)
  101 |     await expect(page.getByText(/API.*Key|我的密钥/, { exact: false }).first()).toBeVisible({ timeout: 8_000 })
  102 |   })
  103 | 
  104 |   test('创建密钥按钮存在', async ({ page }) => {
  105 |     await page.goto(`${BASE}/apikey`)
  106 |     await expect(page.getByRole('button', { name: /创建|新建/ })).toBeVisible({ timeout: 8_000 })
  107 |   })
  108 | })
  109 | 
  110 | test.describe('📊 监控模块', () => {
  111 |   test.beforeEach(async ({ page }) => {
  112 |     await page.goto(`${BASE}/login`)
  113 |     await page.getByPlaceholder('用户名').fill(ADMIN_USER)
  114 |     await page.getByPlaceholder('密码').fill(ADMIN_PWD)
  115 |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  116 |     await page.waitForURL('**/chat', { timeout: 15_000 })
  117 |   })
  118 | 
  119 |   test('监控页加载', async ({ page }) => {
  120 |     await page.goto(`${BASE}/monitor`)
  121 |     await expect(page.getByText(/监控|监控中心/).first()).toBeVisible({ timeout: 8_000 })
  122 |   })
  123 | 
  124 |   test('健康状态卡片存在', async ({ page }) => {
  125 |     await page.goto(`${BASE}/monitor`)
  126 |     await expect(page.locator('.health-card, [class*="health"]').first()).toBeVisible({ timeout: 8_000 })
  127 |   })
  128 | })
  129 | 
```