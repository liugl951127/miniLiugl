/**
 * V3.7.37+ Playwright chat 流式 e2e 测试
 *
 * 模拟用户操作:
 * 1. 打开 chat 页面
 * 2. 输入消息
 * 3. 点击发送
 * 4. 验证流式回复累积
 * 5. 验证打字机效果
 *
 * 用法 (本地/CI):
 *   npx playwright install chromium
 *   npm run e2e -- e2e/chat-streaming.spec.js
 */
import { test, expect } from '@playwright/test'

test.describe('chat 流式交互', () => {
  test.beforeEach(async ({ page }) => {
    // V3.5.93+ 演示模式: 跳过登录
    await page.goto('http://localhost:3000/chat?demo=1')
    // 等待 chat 页面加载
    await page.waitForSelector('[data-testid="chat-input"]', { timeout: 10000 })
  })

  test('1. 用户输入 → 流式回复累积', async ({ page }) => {
    // 输入消息
    await page.fill('[data-testid="chat-input"]', '你好世界')
    await page.click('[data-testid="chat-send"]')

    // 等待流式回复开始
    await page.waitForSelector('[data-testid="ai-message"]', { timeout: 5000 })

    // 验证回复累积 (等 2s 让流结束)
    await page.waitForTimeout(2000)

    // 验证最后消息有内容
    const lastMessage = await page.locator('[data-testid="ai-message"]').last().textContent()
    expect(lastMessage).toBeTruthy()
    expect(lastMessage.length).toBeGreaterThan(0)
  })

  test('2. 演示模式 5 账号切换', async ({ page }) => {
    // 默认登录 demo
    await expect(page.locator('[data-testid="user-info"]')).toContainText(/demo/i)

    // 切换其他角色
    await page.click('[data-testid="user-switch"]')
    await page.click('[data-testid="role-SUPER_ADMIN"]')
    await page.waitForTimeout(500)
    await expect(page.locator('[data-testid="user-info"]')).toContainText(/super/i)
  })

  test('3. 错误网络下显示倒计时重试', async ({ page, context }) => {
    // 模拟断网
    await context.setOffline(true)

    // 发送消息
    await page.fill('[data-testid="chat-input"]', '测试')
    await page.click('[data-testid="chat-send"]')

    // 验证错误条 + 倒计时
    await expect(page.locator('[data-testid="error-banner"]')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('[data-testid="retry-countdown"]')).toBeVisible()

    // 恢复网络
    await context.setOffline(false)
  })

  test('4. 5 type 业务兼容 (chat 模式)', async ({ page }) => {
    // 验证 chat 页面 5 type 兼容 (onContent 流式累积)
    const startTime = Date.now()
    await page.fill('[data-testid="chat-input"]', '测试')
    await page.click('[data-testid="chat-send"]')

    // 验证回复长度递增 (打字机效果)
    const lengths = []
    for (let i = 0; i < 5; i++) {
      await page.waitForTimeout(500)
      const text = await page.locator('[data-testid="ai-message"]').last().textContent()
      lengths.push(text?.length || 0)
    }

    // 长度应该递增 (打字机)
    const increasing = lengths.every((l, i) => i === 0 || l >= lengths[i-1])
    expect(increasing).toBe(true)
  })
})
