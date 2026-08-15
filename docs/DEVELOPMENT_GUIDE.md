# MiniMax Platform 项目开发规范 (V6.3+)

## 1. 页面功能与智能化

每个业务页面必须包含:
- 智能提示 (placeholder + tooltip)
- 智能填单 (基于上下文自动填充)
- 一键预览 (预览/草稿模式)
- 智能推荐 (基于历史/角色推荐默认值)
- 数据可视化 (图表 + 趋势线)

## 2. 用户操作体验 (UX)

### 必做
- 加载状态 (骨架屏/el-skeleton)
- 错误处理 (toast + 友好提示)
- 空状态 (el-empty + CTA)
- 确认弹窗 (重要操作)
- 快捷键 (Enter/Tab/Ctrl+S)
- 响应式 (桌面 + 移动端)

### 推荐
- 操作历史 (撤销/重做)
- 拖拽 (列表/卡片排序)
- 实时保存 (草稿)
- 暗色主题切换

## 3. 智能填单 / 一键预览 / 自动推荐

### 3.1 智能填单 (AutoFill)
适用场景: 复杂表单 (用户创建/API Key/数据源配置)
- 调用 `/api/ai/autofill` 推荐字段值
- 用户点击"✨ 智能填充"按钮触发
- 字段显示"AI 推荐"标签, 可接受/拒绝

### 3.2 一键预览 (Preview)
适用场景: 复杂编排 (Pipeline/Workflow/数据看板)
- 用户点击"👁 预览"按钮
- 后端返回 mock 数据 + 真实数据混合渲染
- 不需要保存, 立即看效果

### 3.3 智能推荐 (Recommend)
适用场景: 选择器/搜索框/参数配置
- 基于历史使用频率推荐 Top-3
- 基于角色权限推荐可用选项
- 基于上下文推断默认值

## 4. 提交前自动检查 (Pre-commit Hook)

### 4.1 前端检查
- ESLint (语法 + 引入依赖)
- Vitest (单元测试)
- Build (生产构建)

### 4.2 后端检查
- 路由一致性 (前端调用 vs 后端 Controller)
- Gateway 路由 (application.yml + 后端 Controller)
- 编译 (mvn compile)

### 4.3 一致性检查脚本
`scripts/check-routes.sh` 自动跑:
```bash
./scripts/check-routes.sh
```

输出:
- 后端 Controller 路由数
- Gateway 路由数
- 前端 API 调用数
- 缺失路由列表 (前端调但后端没)
- 死路由列表 (后端有但前端没)

## 5. 表结构变更规范

### 5.1 流程
1. 修改 Entity 类
2. 写表结构 DDL (schema-{module}.sql)
3. 写种子数据 (data-{module}.sql)
4. 跑 `scripts/db-reset.sh` 一键重建
5. 验证字段一一对应 (脚本自动检查)

### 5.2 字段一致性检查
- 实体类字段 ↔ 数据库列名
- 实体类字段 ↔ 前端 dto 字段
- 实体类字段 ↔ 前端 vo 字段

### 5.3 必须保持
- 字段名一致 (snake_case ↔ camelCase 通过 MyBatis-Plus map-underscore)
- 类型一致 (LocalDateTime ↔ DATETIME)
- 主键策略一致 (雪花算法 ID)
- 必填/可空 一致

## 6. 智能化提升方向

### 6.1 短期 (本周)
- 智能填单 AI 推荐 (基于历史)
- 一键预览 Pipeline/Workflow
- 智能推荐 (Top-3 选择器)

### 6.2 中期 (本月)
- 表单字段语义化提示
- 操作引导 (新手引导 Tour)
- 异常自动修复建议

### 6.3 长期 (季度)
- AI 辅助决策
- 自适应 UI (根据用户行为调整)
- 预测性输入

