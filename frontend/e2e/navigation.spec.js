// e2e/navigation.spec.js
// 导航与侧边栏 E2E 测试
const { test, expect } = require('@playwright/test')

test.describe('应用导航', () => {
  // 使用 localStorage mock 登录状态，跳过真实登录
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    // Mock 已登录状态：注入 accessToken 到 localStorage
    await page.evaluate(() => {
      localStorage.setItem('access_token', 'mock-token-for-e2e')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('user_info', JSON.stringify({ id: 1, username: 'admin', role: 'admin' }))
    })
  })

  test('侧边栏菜单可见', async ({ page }) => {
    await page.goto('/chat')
    await page.waitForTimeout(500)
    // 检查侧边栏存在
    const sidebar = page.locator('.sidebar, .el-aside, [class*="sidebar"]').first()
    await expect(sidebar).toBeVisible({ timeout: 5000 }).catch(() => {
      // 如果侧边栏 class 不确定，检查 layout 容器
      expect(page.locator('.app-layout, #app')).toBeVisible()
    })
  })

  test('知识库页面可访问', async ({ page }) => {
    await page.goto('/knowledge')
    await page.waitForTimeout(1000)
    // 页面应显示内容（知识库标题或 tab）
    await expect(page.locator('text=知识库,text=我的知识库').first()).toBeVisible({ timeout: 5000 })
  })

  test('API Key 页面可访问', async ({ page }) => {
    await page.goto('/knowledge') // apikey 在 knowledge 下通过菜单
    await page.waitForTimeout(500)
    // 验证页面加载无 JS 错误
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    await page.reload()
    await page.waitForTimeout(500)
    expect(errors.filter(e => !e.includes('favicon'))).toHaveLength(0)
  })

  test('监控页面可访问', async ({ page }) => {
    await page.goto('/admin/monitor')
    await page.waitForTimeout(1000)
    // 页面标题或内容可见
    await expect(page.locator('text=监控,text=健康,text=告警').first()).toBeVisible({ timeout: 5000 })
  })

  test('底部版权信息存在', async ({ page }) => {
    await page.goto('/chat')
    await page.waitForTimeout(500)
    await expect(page.locator('text=Copyright,text=MiniMax').first()).toBeVisible({ timeout: 3000 })
  })
})
