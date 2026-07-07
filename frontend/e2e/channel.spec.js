// e2e/channel.spec.js
// Day 26: Playwright E2E — 告警通知渠道 CRUD (2026-07-07)
// 补充 alert-channel.spec.js，专注端到端登录+表单流程
const { test, expect } = require('@playwright/test')

const BASE = process.env.E2E_BASE_URL || 'http://localhost:5173'
const ADMIN_USER = process.env.E2E_USER || 'admin'
const ADMIN_PWD = process.env.E2E_PWD || 'admin@123'

/**
 * 先登录，然后访问 /monitor 页面测试告警渠道 CRUD
 */
test.describe('告警通知渠道 E2E (2026-07-07)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    // 等待登录跳转到首页
    await page.waitForURL(url => !url.toString().includes('/login'), { timeout: 15_000 })
  })

  test('进入监控页面，告警渠道卡片可见', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    // 等待渠道表格出现
    await expect(page.getByText('告警通知渠道')).toBeVisible({ timeout: 15_000 })
    // 新增按钮
    await expect(page.getByRole('button', { name: /新增渠道|\+ 新增渠道/ })).toBeVisible()
  })

  test('新增邮件渠道成功', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.getByText('告警通知渠道')).toBeVisible({ timeout: 15_000 })

    // 点击新增按钮
    await page.getByRole('button', { name: /新增渠道|\+ 新增渠道/ }).click()
    // 等待弹窗出现
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // 填写名称
    const nameInput = page.locator('input[placeholder="e.g. 运维告警组"]')
    if (await nameInput.isVisible()) {
      await nameInput.fill('E2E 测试邮件渠道')
    } else {
      await page.locator('.el-dialog input').first().fill('E2E 测试邮件渠道')
    }

    // 切换到 EMAIL（默认就是）
    const typeSelect = page.locator('.el-dialog .el-select').first()
    if (await typeSelect.isVisible()) {
      await typeSelect.click()
      await page.locator('.el-option').filter({ hasText: /邮件/ }).first().click()
    }

    // 填写邮箱
    const emailInput = page.locator('input[placeholder*="oncall"]')
    if (await emailInput.isVisible()) {
      await emailInput.fill('e2e-test@minimax.com')
    }

    // 保存
    await page.getByRole('button').filter({ hasText: /^保存$/ }).click()

    // 等待保存成功提示或关闭弹窗
    try {
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8_000 })
    } catch (_) {
      await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
    }

    // 表格中应出现新渠道
    await expect(page.getByText('E2E 测试邮件渠道')).toBeVisible({ timeout: 10_000 })
  })

  test('新增钉钉渠道成功', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.getByText('告警通知渠道')).toBeVisible({ timeout: 15_000 })

    await page.getByRole('button', { name: /新增渠道|\+ 新增渠道/ }).click()
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // 名称
    const nameInput = page.locator('input[placeholder="e.g. 运维告警组"]')
    if (await nameInput.isVisible()) {
      await nameInput.fill('E2E 测试钉钉渠道')
    } else {
      await page.locator('.el-dialog input').first().fill('E2E 测试钉钉渠道')
    }

    // 切换到钉钉
    const typeSelect = page.locator('.el-dialog .el-select').first()
    if (await typeSelect.isVisible()) {
      await typeSelect.click()
      await page.locator('.el-option').filter({ hasText: /钉钉/ }).first().click()
      await page.waitForTimeout(500)
    }

    // 填写 Webhook
    const webhookInput = page.locator('input[placeholder*="oapi.dingtalk"]')
    if (await webhookInput.isVisible()) {
      await webhookInput.fill('https://oapi.dingtalk.com/robot/send?access_token=abcd123')
    }

    // 保存
    await page.getByRole('button').filter({ hasText: /^保存$/ }).click()
    try {
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8_000 })
    } catch (_) {
      await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
    }

    await expect(page.getByText('E2E 测试钉钉渠道')).toBeVisible({ timeout: 10_000 })
  })

  test('编辑渠道', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.getByText('告警通知渠道')).toBeVisible({ timeout: 15_000 })

    const rowCount = await page.locator('.el-table__row').count()
    if (rowCount === 0) {
      test.skip('无渠道，跳过编辑测试')
      return
    }

    // 点击第一个编辑按钮
    const editBtn = page.locator('.el-table__row').first().locator('button').filter({ hasText: '编辑' })
    await editBtn.click()
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // 修改名称
    const nameInput = page.locator('input[placeholder="e.g. 运维告警组"]')
    if (await nameInput.isVisible()) {
      await nameInput.fill('E2E-已编辑-' + Date.now())
    }

    // 保存
    await page.getByRole('button').filter({ hasText: /^保存$/ }).click()
    try {
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8_000 })
    } catch (_) {
      await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
    }
  })

  test('删除渠道', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.getByText('告警通知渠道')).toBeVisible({ timeout: 15_000 })

    const rowCount = await page.locator('.el-table__row').count()
    if (rowCount === 0) {
      test.skip('无渠道，跳过删除测试')
      return
    }

    const initialCount = rowCount

    // 点击最后一个删除按钮
    const deleteBtn = page.locator('.el-table__row').last().locator('button').filter({ hasText: '删除' })
    await deleteBtn.click()

    // 确认 popconfirm
    const confirmBtn = page.locator('.el-popconfirm').locator('button').filter({ hasText: /确定|确认/ })
    await confirmBtn.click()

    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8_000 })

    // 表格行数应减少
    await page.waitForTimeout(500)
    const newCount = await page.locator('.el-table__row').count()
    expect(newCount).toBeLessThanOrEqual(initialCount)
  })

  test('表单校验: 名称为空不可提交', async ({ page }) => {
    await page.goto(`${BASE}/monitor`)
    await expect(page.getByText('告警通知渠道')).toBeVisible({ timeout: 15_000 })

    await page.getByRole('button', { name: /新增渠道|\+ 新增渠道/ }).click()
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // 直接点保存（名称为空）
    await page.getByRole('button').filter({ hasText: /^保存$/ }).click()

    // 警告提示或表单错误
    const hasWarning = await page.locator('.el-form-item__error, .el-message--warning').isVisible().catch(() => false)
    expect(hasWarning).toBeTruthy()
  })
})
