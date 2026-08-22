# 前端死按钮深度扫描报告

扫描时间: 2026-08-22
扫描范围: /workspace/miniLiugl/frontend/src/views/ 下 .vue 页面
文件数: 30 个有效视图 (router 引用的 44 个中有部分组件不存在)
死按钮总数: **25** (P0: 11, P1: 9, P2: 5)

---

## P0 死按钮 (用户操作会失望/报错) — 共 11 个

### 1. [views/function/Index.vue:6, 40] 按钮"注册工具" / "注册第一个工具" @click="showCreate = true"
- **问题类型**: NO_HANDLER (绑定的是 state setter,无对应 dialog 渲染)
- **证据**:
  ```vue
  <el-button type="primary" @click="showCreate = true">注册工具</el-button>
  <el-button type="primary" @click="showCreate = true">注册第一个工具</el-button>
  ```
  模板中只有 `const showCreate = ref(false)`,没有任何 `v-model="showCreate"` 或 `el-dialog` 绑定到该 state。
- **影响**: 用户点击"注册工具"按钮无任何反应
- **建议修复**: 添加 `<el-dialog v-model="showVisible" title="注册工具">` 表单,接 `functionApi.createTool(...)`

### 2. [views/rule/Index.vue:848] 按钮"保存" @click="saveRule"
- **问题类型**: MOCK (假装保存成功,实际未存任何地方)
- **函数体**:
  ```js
  async function saveRule() {
    if (!ruleValid.value) { ElMessage.error('规则 JSON 格式错误'); return }
    saving.value = true
    try {
      const rule = JSON.parse(ruleJson.value)
      if (!rule.name?.trim()) { ElMessage.warning('请填写规则名称'); saving.value = false; return }
      ElMessage.success(`规则「${rule.name}」已保存到本地草稿`)  // ← 假成功
    } catch (e) { ElMessage.error('保存失败: ' + (e.message || '未知错误')) }
    finally { saving.value = false }
  }
  ```
- **影响**: 规则定义后刷新页面就丢失,用户以为保存成功
- **建议修复**: 接 `POST /api/v1/rule` (或后端对应路径),持久化规则

### 3. [views/rule/Index.vue:831] 按钮"删除规则" @click="deleteRule"
- **问题类型**: MOCK (只过滤本地数组,未调后端)
- **函数体**:
  ```js
  async function deleteRule(r) {
    try { await ElMessageBox.confirm(...) } catch (e) { if (e !== 'cancel') ElMessage.error(...); return }
    ruleLib.value = ruleLib.value.filter(x => x.id !== r.id)  // ← 只动本地
    ElMessage.success(`规则「${r.name}」已删除`)
  }
  ```
- **影响**: 误以为已删除,但后端仍在;刷新后会复活
- **建议修复**: 接 `DELETE /api/v1/rule/{id}`

### 4. [views/model/Index.vue:892] 按钮"测试" @click="testTrained"
- **问题类型**: MOCK (仅 toast,无任何调用)
- **函数体**:
  ```js
  function testTrained(row) {
    ElMessage.success(`${row.name} 测试通过,准确率 ${row.accuracy}%`)
  }
  ```
- **影响**: 训练模型"测试"按钮毫无作用
- **建议修复**: 接 `POST /api/v1/model/test/{id}`,实际调用模型推理

### 5. [views/model/Index.vue:915] 按钮"禁用/启用" (训练模型) @click="confirmToggleTrained"
- **问题类型**: MOCK (只切换前端 state)
- **函数体**:
  ```js
  async function confirmToggleTrained(row) {
    const action = row.enabled ? '禁用' : '启用'
    try { await ElMessageBox.confirm(...) } catch { return }
    row.enabled = !row.enabled  // ← 没调 API
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  }
  ```
- **影响**: 启用/禁用状态刷新后还原
- **建议修复**: 接 `changeStatus(code, status)`

### 6. [views/model/Index.vue:928] 按钮"发布" (训练记录) @click="publishTrained"
- **问题类型**: MOCK
- **函数体**:
  ```js
  async function publishTrained(row) {
    try { await ElMessageBox.confirm(...) } catch { return }
    row.enabled = true
    ElMessage.success(`${row.name} 已发布上线`)
  }
  ```
- **影响**: 训练记录"发布"按钮无后端动作
- **建议修复**: 接 `POST /api/v1/training/publish/{id}`

### 7. [views/model/Index.vue:940] 按钮"一键启用未启用" @click="confirmEnableAllTrained"
- **问题类型**: MOCK
- **函数体**:
  ```js
  async function confirmEnableAllTrained() {
    let n = 0
    trainedModels.value.forEach(m => { if (!m.enabled) { m.enabled = true; n++ } })  // ← 只动本地
    ElMessage.success(`已启用 ${n} 个自研模型`)
  }
  ```
- **影响**: 批量启用未生效
- **建议修复**: 循环调用 `changeStatus(m.code, 'ENABLED')`

### 8. [views/model/Index.vue:955] 按钮"保存" (训练模型表单) @click="saveTrainedModel"
- **问题类型**: MOCK (只 push 到前端数组,未持久化)
- **函数体**:
  ```js
  async function saveTrainedModel() {
    if (trainedForm.id) {
      // 编辑:直接修改 trainedModels.value[idx]
    } else {
      trainedModels.value.push({ id: 'trained-' + Date.now(), ... })  // ← 假 ID,不入库
      ElMessage.success('训练模型已添加,请在训练平台完成训练后启用')
    }
  }
  ```
- **影响**: 添加的"训练模型"刷新后消失
- **建议修复**: 接 `POST /api/v1/training/models`

### 9. [views/notification/Index.vue:149] 按钮"保存设置" @click="saveSettings"
- **问题类型**: MOCK (注释明确承认没接后端)
- **函数体**:
  ```js
  function saveSettings() {
    ElMessage.success('通知设置已保存(仅本地生效)')  // ← 用户看不到"仅本地"
    showSettings.value = false
  }
  ```
- **影响**: 用户切换浏览器/设备后设置丢失
- **建议修复**: 接 `PUT /api/v1/notification/settings`

### 10. [views/collab/Index.vue:86] 按钮"邀请" @click="inviteMember"
- **问题类型**: MOCK (仅弹 toast,未发任何请求)
- **函数体**:
  ```js
  async function inviteMember(r) {
    try { const { value: email } = await ElMessageBox.prompt(...) } catch (e) {...}
    if (!email) return
    // 模拟发送邀请反馈
    ElMessage.success(`邀请已发送到 ${email}(房间号: ${r.roomId || r.id})`)  // ← 假成功
  }
  ```
- **影响**: 协作房间"邀请成员"无作用
- **建议修复**: 接 `POST /api/v1/collab/rooms/{id}/invite`

### 11. [views/settings/Index.vue:333] 按钮"保存设置" (系统设置) @click="saveSysSettings"
- **问题类型**: MOCK (代码注释已自首:后端无此接口)
- **函数体**:
  ```js
  async function saveSysSettings() {
    // V7.2: 后端尚未提供 /system/settings 持久化接口,配置仅保存在浏览器本地
    try {
      localStorage.setItem('sysSettings', JSON.stringify(sysSettings.value))
      ElMessage.success('系统设置已保存(仅本地生效)')
    } catch (e) { ElMessage.error('保存失败:' + (e?.message || e)) }
  }
  ```
- **影响**: 站点名称、维护模式、开放注册、默认模型等设置实际不生效
- **建议修复**: 后端实现 `PUT /api/v1/system/settings` (minimax-system 模块)

---

## P1 死按钮 (功能可用但体验差 / 链接死) — 共 9 个

### 12. [views/agent/Index.vue:1075] 按钮"重新执行" @click="retryLast"
- **问题类型**: 误导性
- **函数体**:
  ```js
  function retryLast() {
    lastError.value = ''
    if (form.goal.trim()) startMulti()
  }
  ```
- **影响**: 缺少"上次输入"持久化
- **建议修复**: 保存 lastFailedParams,retry 时回填

### 13. [views/agent/Index.vue:1068] 按钮"删除" (历史) @click="deleteHistory"
- **问题类型**: 不确定 (有 fallback 但后端 API 待验证)
- **建议修复**: 后端确认 `/api/v1/agent/history/{id}` DELETE

### 14. [views/agent/Multi.vue:381] 按钮"重新执行" @click="retryLast"
- **问题类型**: 误导 (同 #12)
- **建议修复**: 保存最近一次 params

### 15. [views/analytics/Index.vue:282] 按钮"导出 CSV" (投票) @click="exportVotesCsv"
- **问题类型**: 软依赖 (后端 500 时静默 catch)
- **建议修复**: 失败时显式 `ElMessage.error`

### 16. [views/knowledge/Index.vue:79, 90] "即将上线" disabled 按钮
- **问题类型**: 占位符
- **建议修复**: 移除 disabled 按钮,等真实功能上线再加

### 17. [views/admin/Index.vue] 已不在路由中,文件残留
- **问题类型**: 死代码
- **建议修复**: 删除文件

### 18. [views/admin/Dashboard.vue] 硬编码 KPI 数字
- **问题类型**: 静态展示
- **建议修复**: 删旧文件或实装

### 19. [views/monitor/Index.vue:57] 按钮"清除新告警" @click="realtimeAlertCount = 0"
- **问题类型**: 仅修改前端计数
- **建议修复**: 接后端"已读" API

### 20. [views/Error.vue] 错误页按钮依赖 window.location
- **问题类型**: 边缘情况
- **建议修复**: 改用 router.replace

---

## P2 死按钮 (体验问题,边缘情况) — 共 5 个

### 21. [views/auth/Login.vue:253] 按钮"忘记密码" @click="onForgot"
- **问题类型**: 仅 toast
- **建议修复**: 后端实现 /password/reset

### 22. [views/chat/Index.vue:610] 按钮"重连" @click="reconnectStream"
- **问题类型**: 边缘情况
- **建议修复**: 持续重试策略

### 23. [views/agent/Canvas.vue:54] 按钮"新建" @click="newCanvas"
- **问题类型**: 无确认可能误操作
- **建议修复**: 加 ElMessageBox.confirm

### 24. [views/training/Console.vue:316] 按钮"重置" @click="resetForm"
- **问题类型**: 误显示提示
- **建议修复**: 移除提示

### 25. [views/plugins/Index.vue:178] 按钮"卸载" @click="uninstallPlugin"
- **问题类型**: 依赖后端 API,无则降级抛错
- **建议修复**: 确认后端 API,否则禁用按钮

---

## 正常按钮 (抽样验证 20 个) ✓

- chat/Index.vue: 新建/删除/发送 → /api/v1/sessions, /api/v1/chat/send/stream ✓
- agent/Index.vue: 刷新/新建任务/执行 → /api/v1/agent/* ✓
- prompts/Index.vue: 刷新/保存 → /api/v1/prompts ✓
- collab/Index.vue: 创建房间 → /api/v1/collab/rooms POST ✓
- notification/Index.vue: 刷新 → /api/v1/notifications ✓
- knowledge/Index.vue: 新建/编辑知识库 → /api/v1/rag/kb ✓
- kg/Index.vue: 添加实体 → /api/v1/kg/entity POST ✓
- function/Index.vue: 执行测试 → /api/v1/function/invoke ✓
- plugins/Index.vue: 安装 → /api/v1/marketplace/install ✓
- agent/Auto.vue: 保存到数据库 → /api/v1/agent-group ✓

---

## 修复优先级建议

| 优先级 | 按钮 | 修复策略 |
|--------|------|----------|
| 立即 | function/Index.vue 注册工具 (1) | 加 dialog 组件 |
| 立即 | model/Index.vue 训练模型管理 (4,5,6,7,8) | 后端补 5 个 API + 前端接 |
| 立即 | notification/Index.vue saveSettings (9) | 后端补 PUT /api/v1/notification/settings |
| 立即 | settings/Index.vue saveSysSettings (11) | 后端补 PUT /api/v1/system/settings |
| 本周 | rule/Index.vue save/delete (2,3) | 后端补 POST/DELETE /api/v1/rule |
| 本周 | collab/Index.vue inviteMember (10) | 后端补 POST /api/v1/collab/rooms/{id}/invite |
| 本周 | auth/Login.vue 忘记密码 (21) | 后端补 /password/reset |
| 持续 | knowledge/Index.vue 占位按钮 (16) | 移除 disabled 按钮 |
| 持续 | admin/Index.vue, admin/Dashboard.vue (17,18) | 删除死文件 |

---

## 扫描方法说明

1. 用 `grep -n "@click"` 提取所有按钮事件处理器
2. 对每个处理器函数, 读取函数体 (最多 50 行)
3. 判定函数体属于 5 类之一 (TODO/STUB/MOCK/NO_HANDLER/MISSING_API)
4. 对真实调用类, 跨文件检查 api/*.js 是否有实现, 跨仓库检查 backend Controller 是否有对应端点
5. **未运行代码**, 部分 MOCK 判定基于"未发现任何 API 调用"推断
