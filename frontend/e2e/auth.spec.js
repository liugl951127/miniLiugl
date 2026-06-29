import { test, expect } from '@playwright/test'

/**
 * V5.33 Day 23: Playwright E2E 测试套件
 * 覆盖：登录 / 对话 / 知识库 / API Key / 监控
 */

const BASE = process.env.BASE_URL || 'http://localhost:5173'
const ADMIN_USER = process.env.E2E_USER || 'admin'
const ADMIN_PWD  = process.env.E2E_PWD  || 'admin@123'

test.describe('🔐 认证模块', () => {
  test('登录页正常加载', async ({ page }) => {
    await page.goto(BASE)
    // 等待页面渲染
    await expect(page.locator('h1')).toContainText('MiniMax', { timeout: 10_000 })
    // Tab 存在
    await expect(page.getByRole('tab', { name: /密码/ })).toBeVisible()
  })

  test('默认账号提示可见', async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await expect(page.getByText('admin')).toBeVisible({ timeout: 8_000 })
  })

  test('密码登录成功', async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    // 登录后跳转到 chat
    await page.waitForURL('**/chat', { timeout: 15_000 })
    await expect(page.url()).toContain('/chat')
  })

  test('空密码提示必填', async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill('admin')
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    // Element Plus 验证提示
    await expect(page.locator('.el-form-item__error').first()).toBeVisible({ timeout: 5_000 })
  })
})

test.describe('💬 对话模块', () => {
  test.beforeEach(async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    await page.waitForURL('**/chat', { timeout: 15_000 })
  })

  test('Chat 页加载', async ({ page }) => {
    await expect(page.locator('.chat-container, .chat-page, textarea')).toBeVisible({ timeout: 8_000 })
  })

  test('模型选择器存在', async ({ page }) => {
    await expect(page.locator('select, [class*="model"]').first()).toBeVisible({ timeout: 8_000 })
  })
})

test.describe('📚 知识库模块', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    await page.waitForURL('**/chat', { timeout: 15_000 })
  })

  test('知识库页加载', async ({ page }) => {
    await page.goto(`${BASE}/knowledge`)
    await expect(page.getByText(/知识库/, { exact: false }).first()).toBeVisible({ timeout: 8_000 })
  })

  test('新建知识库按钮存在', async ({ page }) => {
    await page.goto(`${BASE}/knowledge`)
    await expect(page.getByRole('button', { name: /新建知识库/ })).toBeVisible({ timeout: 8_000 })
  })

  test('检索问答 Tab 切换', async ({ page }) => {
    await page.goto(`${BASE}/knowledge`)
    await page.getByRole('tab', { name: /检索/ }).click()
    await expect(page.locator('textarea, [placeholder*="问题"]').first()).toBeVisible({ timeout: 5_000 })
  })
})

test.describe('🔑 API Key 模块', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    await page.waitForURL('**/chat', { timeout: 15_000 })
  })

  test('API Key 页加载', async ({ page }) => {
    await page.goto(`${BASE}/apikey`)
    await expect(page.getByText(/API.*Key|我的密钥/, { exact: false }).first()).toBeVisible({ timeout: 8_000 })
  })

  test('创建密钥按钮存在', async ({ page }) => {
    await page.goto(`${BASE}/apikey`)
    await expect(page.getByRole('button', { name: /创建|新建/ })).toBeVisible({ timeout: 8_000 })
  })
})

test.describe('📊 监控模块', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    await page.waitForURL('**/chat', { timeout: 15_000 })
  })

  test('监控页加载', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.getByText(/监控|监控中心/).first()).toBeVisible({ timeout: 8_000 })
  })

  test('健康状态卡片存在', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.locator('.health-card, [class*="health"]').first()).toBeVisible({ timeout: 8_000 })
  })
})
