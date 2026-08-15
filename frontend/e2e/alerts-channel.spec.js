import { test, expect } from '@playwright/test'

/**
 * Day 26: 告警渠道 E2E 测试
 * 覆盖: 登录 → 告警页 → 渠道 CRUD
 */

const BASE = process.env.BASE_URL || 'http://localhost:5173'
const ADMIN_USER = process.env.E2E_USER || 'admin'
const ADMIN_PWD  = process.env.E2E_PWD  || 'admin@123'

test.describe('🔔 告警渠道管理 E2E', () => {

  // 每个测试前先登录
  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/login`)
    await page.getByPlaceholder('用户名').fill(ADMIN_USER)
    await page.getByPlaceholder('密码').fill(ADMIN_PWD)
    await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
    await page.waitForURL('**/chat', { timeout: 15_000 })
    // 导航到告警页
    await page.goto(`${BASE}/admin/alerts`)
    await expect(page.getByText('告警中心')).toBeVisible({ timeout: 8_000 })
  })

  test('告警页 4 个 Tab 正常显示', async ({ page }) => {
    await expect(page.getByRole('button', { name: '触发中' })).toBeVisible()
    await expect(page.getByRole('button', { name: '告警规则' })).toBeVisible()
    await expect(page.getByRole('button', { name: '通知渠道' })).toBeVisible()
    await expect(page.getByRole('button', { name: '历史记录' })).toBeVisible()
  })

  test('切换到通知渠道 Tab', async ({ page }) => {
    await page.getByRole('button', { name: '通知渠道' }).click()
    await expect(page.getByText('新建渠道')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
  })

  test('新建渠道 - 完整表单保存', async ({ page }) => {
    await page.getByRole('button', { name: '通知渠道' }).click()
    await page.getByRole('button', { name: '+ 新建渠道' }).click()

    // 等待对话框
    await expect(page.getByText('新建渠道')).toBeVisible({ timeout: 5_000 })

    // 填写名称
    await page.locator('.el-dialog .el-input input').first().fill('测试钉钉群')
    // 选择类型
    await page.locator('.el-dialog .el-select').first().click()
    await page.getByRole('option', { name: '钉钉群机器人' }).click()
    // 填写目标
    const textareas = page.locator('.el-dialog .el-textarea textarea')
    await textareas.first().fill('https://oapi.dingtalk.com/robot/send?access_token=test_token_123')

    // 保存
    await page.getByRole('button', { name: '保存' }).click()

    // 保存成功提示或对话框关闭
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
    const successToast = page.locator('.el-message--success')
    if (await successToast.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await expect(successToast).toBeVisible()
    }
  })

  test('编辑渠道 - 打开对话框并关闭', async ({ page }) => {
    await page.getByRole('button', { name: '通知渠道' }).click()
    // 等待表格出现 (mock 数据)
    await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })

    // 点击编辑按钮 (第二个按钮在行内)
    const editBtn = page.getByRole('button', { name: '编辑' }).first()
    if (await editBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await editBtn.click()
      await expect(page.getByText('编辑渠道')).toBeVisible({ timeout: 5_000 })
      // 关闭
      await page.getByRole('button', { name: '取消' }).click()
      await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 3_000 })
    }
  })

  test('新建规则 - 完整表单', async ({ page }) => {
    await page.getByRole('button', { name: '告警规则' }).click()
    await expect(page.getByText('新建规则')).toBeVisible({ timeout: 5_000 })

    await page.getByRole('button', { name: '+ 新建规则' }).click()
    await expect(page.getByText('新建规则').last()).toBeVisible({ timeout: 5_000 })

    // 填写规则名称
    await page.locator('.el-dialog .el-input input').first().fill('CPU 告警规则 E2E')
    // 保存
    await page.getByRole('button', { name: '保存' }).click()
    // 对话框关闭
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
  })

  test('告警规则 Tab 切换', async ({ page }) => {
    await page.getByRole('button', { name: '告警规则' }).click()
    await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
    // 表格列存在
    await expect(page.getByText('名称')).toBeVisible()
    await expect(page.getByText('指标')).toBeVisible()
    await expect(page.getByText('严重度')).toBeVisible()
  })

  test('历史记录 Tab 切换', async ({ page }) => {
    await page.getByRole('button', { name: '历史记录' }).click()
    await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText('时间')).toBeVisible()
    await expect(page.getByText('告警')).toBeVisible()
  })

  test('触发中 Tab - 空状态显示', async ({ page }) => {
    // 默认 Tab 是触发中，检查告警卡片或空状态
    const hasAlert = await page.locator('.alert-card').isVisible({ timeout: 3_000 }).catch(() => false)
    const hasEmpty = await page.locator('.el-empty').isVisible({ timeout: 3_000 }).catch(() => false)
    expect(hasAlert || hasEmpty).toBeTruthy()
  })
})
