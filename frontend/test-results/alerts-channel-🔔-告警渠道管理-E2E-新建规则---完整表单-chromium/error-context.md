# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: alerts-channel.spec.js >> 🔔 告警渠道管理 E2E >> 新建规则 - 完整表单
- Location: e2e/alerts-channel.spec.js:82:3

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
  4   |  * Day 26: 告警渠道 E2E 测试
  5   |  * 覆盖: 登录 → 告警页 → 渠道 CRUD
  6   |  */
  7   | 
  8   | const BASE = process.env.BASE_URL || 'http://localhost:5173'
  9   | const ADMIN_USER = process.env.E2E_USER || 'admin'
  10  | const ADMIN_PWD  = process.env.E2E_PWD  || 'admin@123'
  11  | 
  12  | test.describe('🔔 告警渠道管理 E2E', () => {
  13  | 
  14  |   // 每个测试前先登录
  15  |   test.beforeEach(async ({ page }) => {
> 16  |     await page.goto(`${BASE}/login`)
      |                ^ Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:5173/login
  17  |     await page.getByPlaceholder('用户名').fill(ADMIN_USER)
  18  |     await page.getByPlaceholder('密码').fill(ADMIN_PWD)
  19  |     await page.getByRole('button', { name: /登.{0,4}录|登录/ }).click()
  20  |     await page.waitForURL('**/chat', { timeout: 15_000 })
  21  |     // 导航到告警页
  22  |     await page.goto(`${BASE}/admin/alerts`)
  23  |     await expect(page.getByText('告警中心')).toBeVisible({ timeout: 8_000 })
  24  |   })
  25  | 
  26  |   test('告警页 4 个 Tab 正常显示', async ({ page }) => {
  27  |     await expect(page.getByRole('button', { name: '触发中' })).toBeVisible()
  28  |     await expect(page.getByRole('button', { name: '告警规则' })).toBeVisible()
  29  |     await expect(page.getByRole('button', { name: '通知渠道' })).toBeVisible()
  30  |     await expect(page.getByRole('button', { name: '历史记录' })).toBeVisible()
  31  |   })
  32  | 
  33  |   test('切换到通知渠道 Tab', async ({ page }) => {
  34  |     await page.getByRole('button', { name: '通知渠道' }).click()
  35  |     await expect(page.getByText('新建渠道')).toBeVisible({ timeout: 5_000 })
  36  |     await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
  37  |   })
  38  | 
  39  |   test('新建渠道 - 完整表单保存', async ({ page }) => {
  40  |     await page.getByRole('button', { name: '通知渠道' }).click()
  41  |     await page.getByRole('button', { name: '+ 新建渠道' }).click()
  42  | 
  43  |     // 等待对话框
  44  |     await expect(page.getByText('新建渠道')).toBeVisible({ timeout: 5_000 })
  45  | 
  46  |     // 填写名称
  47  |     await page.locator('.el-dialog .el-input input').first().fill('测试钉钉群')
  48  |     // 选择类型
  49  |     await page.locator('.el-dialog .el-select').first().click()
  50  |     await page.getByRole('option', { name: '钉钉群机器人' }).click()
  51  |     // 填写目标
  52  |     const textareas = page.locator('.el-dialog .el-textarea textarea')
  53  |     await textareas.first().fill('https://oapi.dingtalk.com/robot/send?access_token=test_token_123')
  54  | 
  55  |     // 保存
  56  |     await page.getByRole('button', { name: '保存' }).click()
  57  | 
  58  |     // 保存成功提示或对话框关闭
  59  |     await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
  60  |     const successToast = page.locator('.el-message--success')
  61  |     if (await successToast.isVisible({ timeout: 3_000 }).catch(() => false)) {
  62  |       await expect(successToast).toBeVisible()
  63  |     }
  64  |   })
  65  | 
  66  |   test('编辑渠道 - 打开对话框并关闭', async ({ page }) => {
  67  |     await page.getByRole('button', { name: '通知渠道' }).click()
  68  |     // 等待表格出现 (mock 数据)
  69  |     await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
  70  | 
  71  |     // 点击编辑按钮 (第二个按钮在行内)
  72  |     const editBtn = page.getByRole('button', { name: '编辑' }).first()
  73  |     if (await editBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
  74  |       await editBtn.click()
  75  |       await expect(page.getByText('编辑渠道')).toBeVisible({ timeout: 5_000 })
  76  |       // 关闭
  77  |       await page.getByRole('button', { name: '取消' }).click()
  78  |       await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 3_000 })
  79  |     }
  80  |   })
  81  | 
  82  |   test('新建规则 - 完整表单', async ({ page }) => {
  83  |     await page.getByRole('button', { name: '告警规则' }).click()
  84  |     await expect(page.getByText('新建规则')).toBeVisible({ timeout: 5_000 })
  85  | 
  86  |     await page.getByRole('button', { name: '+ 新建规则' }).click()
  87  |     await expect(page.getByText('新建规则').last()).toBeVisible({ timeout: 5_000 })
  88  | 
  89  |     // 填写规则名称
  90  |     await page.locator('.el-dialog .el-input input').first().fill('CPU 告警规则 E2E')
  91  |     // 保存
  92  |     await page.getByRole('button', { name: '保存' }).click()
  93  |     // 对话框关闭
  94  |     await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5_000 })
  95  |   })
  96  | 
  97  |   test('告警规则 Tab 切换', async ({ page }) => {
  98  |     await page.getByRole('button', { name: '告警规则' }).click()
  99  |     await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
  100 |     // 表格列存在
  101 |     await expect(page.getByText('名称')).toBeVisible()
  102 |     await expect(page.getByText('指标')).toBeVisible()
  103 |     await expect(page.getByText('严重度')).toBeVisible()
  104 |   })
  105 | 
  106 |   test('历史记录 Tab 切换', async ({ page }) => {
  107 |     await page.getByRole('button', { name: '历史记录' }).click()
  108 |     await expect(page.getByRole('table')).toBeVisible({ timeout: 5_000 })
  109 |     await expect(page.getByText('时间')).toBeVisible()
  110 |     await expect(page.getByText('告警')).toBeVisible()
  111 |   })
  112 | 
  113 |   test('触发中 Tab - 空状态显示', async ({ page }) => {
  114 |     // 默认 Tab 是触发中，检查告警卡片或空状态
  115 |     const hasAlert = await page.locator('.alert-card').isVisible({ timeout: 3_000 }).catch(() => false)
  116 |     const hasEmpty = await page.locator('.el-empty').isVisible({ timeout: 3_000 }).catch(() => false)
```