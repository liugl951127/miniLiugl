# V3.6.26 4 项升级 - UX 报告

## 1. useToast 迁移 (admin/Push + admin/Provider = 19 处)

| 文件 | success | error | warning | 总数 |
|------|---------|-------|---------|------|
| admin/Push.vue | 9 | 0 | 1 | 10 |
| admin/Provider.vue | 4 | 4 | 1 | 9 |
| **累计** (Alerts+Push+Provider) | 20 | 11 | 4 | **35** |

**剩余 admin ElMessage Top 10**:
- WechatUnionidAdmin (8) / WechatBindings (8) / Framework (6) / Cluster (5) / Leaderboard (4) / Document (3) / Audit (2)
- 计划 V3.6.27+ 继续迁移

## 2. kg/Index 节点属性面板 (drawer)

**触发**: 点击 entities 列表项 → `openNodeDrawer(e)`

**4 段内容**:
- 📋 基本信息: ID / 名称 (可编辑) / 类型 / 重要性 (el-rate) / 创建时间
- 📝 描述: el-input textarea (可编辑) / 静态文本
- 🔗 关联关系: el-table (关系/目标/权重)
- 操作按钮: 编辑 / 刷新关系 / 删除

**3 状态**:
- 查看模式 (默认): 静态显示
- 编辑模式: el-input + el-rate + 保存/取消
- 删除模式: ElMessageBox.confirm 二次确认

**API**:
- `GET /api/v1/agent/kg/relations?userId&entityId` - 加载关系
- `PUT /api/v1/agent/kg/entities/{id}` - 保存编辑
- `DELETE /api/v1/agent/kg/entities/{id}` - 删除

## 3. chat/Index ToolCalls 可视化 (卡片式)

**V3.6.26 vs V3.6.5+**:
- V3.6.5: el-collapse + el-table (4 列, 折叠)
- V3.6.26: el-row + el-col 卡片 (响应式, 详情展开)

**每张卡片**:
- 头部: 状态 emoji (✅/❌) + 名称 + duration (ms)
- 中间: args (JSON 格式化, monospace 字体)
- 展开: result (前 200 字符 + 折叠)
- 边框色: 成功绿 / 失败红

**统计栏**: 顶部 `✓ N/M` el-tag (绿色全成功, 黄色部分失败)
**清空**: 卡片右上角 × 按钮

## 4. admin/Cluster 节点健康时间线 (ECharts 滚动图)

**7 节点 mock 数据**:
- gateway-1 / auth-1 / chat-1 / model-1 / agent-1 / rag-1 / monitor-1
- 每节点 30 个数据点 (5s/点, 2.5min 历史)
- Y 轴 0-100 健康度, 7 色折线

**功能**:
- 🔄 自动刷新 (5s): el-switch + 定时 tickHealth
- ⏸ 手动模式: 暂停定时, 手动刷新
- 📊 dataZoom: inside + slider (双缩放)
- 鼠标 hover: tooltip 显示具体节点/值/时间
- 窗口 resize: 图表自适应

**3 段 KPI**:
- 卡片 + 折线 + dataZoom 组合
- 7 节点 (颜色 + 图例滚动) - V3.6.26 升级点

## 累计 81 个版本 (V3.5.46-V3.6.26)
