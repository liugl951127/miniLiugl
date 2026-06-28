// e2e/login.spec.js
// 登录页 E2E 测试
const { test, expect } = require('@playwright/test')

test.describe('登录页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('页面标题正确', async ({ page }) => {
    await expect(page).toHaveTitle(/MiniMax/i)
  })

  test('3 个登录 tab 存在', async ({ page }) => {
    await expect(page.locator('.el-tabs__item').nth(0)).toContainText('账号密码')
    await expect(page.locator('.el-tabs__item').nth(1)).toContainText('注册')
    await expect(page.locator('.el-tabs__item').nth(2)).toContainText('微信扫码')
  })

  test('默认显示账号密码登录表单', async ({ page }) => {
    await expect(page.locator('input[placeholder="用户名"]')).toBeVisible()
    await expect(page.locator('input[placeholder="密码"]')).toBeVisible()
  })

  test('默认账号预填 admin/admin@123', async ({ page }) => {
    const usernameInput = page.locator('input[placeholder="用户名"]')
    const passwordInput = page.locator('input[placeholder="密码"]')
    await expect(usernameInput).toHaveValue('admin')
    await expect(passwordInput).toHaveValue('admin@123')
  })

  test('切换到注册 tab 清空表单', async ({ page }) => {
    await page.locator('.el-tabs__item').nth(1).click()
    await expect(page.locator('input[placeholder="用户名"]')).toHaveValue('')
    await expect(page.locator('input[placeholder="密码"]')).toHaveValue('')
  })

  test('切换到微信扫码 tab 显示扫码组件', async ({ page }) => {
    await page.locator('.el-tabs__item').nth(2).click()
    await expect(page.locator('.wechat-tab')).toBeVisible()
  })

  test('用户名过短提示', async ({ page }) => {
    await page.locator('input[placeholder="用户名"]').fill('ab')
    await page.locator('input[placeholder="密码"]').click() // 触发 blur
    await expect(page.locator('.el-form-item__error').first()).toBeVisible()
  })

  test('登录按钮存在且可点击', async ({ page }) => {
    const btn = page.locator('button:has-text("登录")')
    await expect(btn).toBeVisible()
    await expect(btn).toBeEnabled()
  })
})
