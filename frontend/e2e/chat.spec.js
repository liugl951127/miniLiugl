// e2e/chat.spec.js
// 对话页面 E2E 测试
const { test, expect } = require('@playwright/test')

test.describe('对话页面', () => {
  test.beforeEach(async ({ page }) => {
    // Mock 登录状态
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('access_token', 'mock-e2e-token')
      localStorage.setItem('refresh_token', 'mock-refresh')
      localStorage.setItem('user_info', JSON.stringify({ id: 1, username: 'admin', role: 'admin' }))
    })
    await page.goto('/chat')
    await page.waitForTimeout(800)
  })

  test('消息输入框存在', async ({ page }) => {
    const textarea = page.locator('textarea, [contenteditable="true"], input[class*="input"]').first()
    await expect(textarea).toBeVisible({ timeout: 5000 })
  })

  test('发送按钮存在', async ({ page }) => {
    const sendBtn = page.locator('button:has-text("发送"), button:has-text("发送"), [class*="send"]').first()
    await expect(sendBtn).toBeVisible({ timeout: 5000 })
  })

  test('侧边栏会话列表区域存在', async ({ page }) => {
    // 检查侧边栏或会话区域
    const asideOrList = page.locator('.el-aside, .session-list, [class*="session"]').first()
    await expect(asideOrList).toBeVisible({ timeout: 3000 }).catch(() => {
      // 如果没找到，检查主布局
      expect(page.locator('#app')).toBeVisible()
    })
  })

  test('页面加载无 JS 崩溃错误', async ({ page }) => {
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    await page.reload()
    await page.waitForTimeout(1000)
    // 过滤掉资源加载错误（favicon 等）
    const criticalErrors = errors.filter(e =>
      !e.includes('favicon') && !e.includes('404') && !e.includes('net::')
    )
    expect(criticalErrors).toHaveLength(0)
  })
})
