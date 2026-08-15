# 前端代码扫描报告 (V6.8.1)

## 1. Import 路径 + 具名检查
- **错误**: 0
- **警告**: 0 (修过 store/notification.js 误报)

## 2. 语法检查
- **JS 文件**: 87 个, 0 错 (node --check)
- **Vue 脚本块**: 119 个, 0 错

## 3. 未定义函数检测 (启发式)
- **可能未定义**: 30 个 (实际为 callback 形参, 非真错)
  - 形参: onComplete/onChunk/onError/onReconnecting/onDone/onSource/onToolCall 等
  - 形参: callback/loader/cleanup/startTimers
  - 不属于 bug

## 4. 修过的真错
- ✓ store/notification.js + store/session.js 引用 sessionApi/messageApi 之前不存在
  - 修法: api/session.js 加聚合 API 对象
- ✓ agent.js 重复 /api/v1 前缀 (5 处)
  - 修法: 移除冗余前缀, 统一由 http.js 加

## 结论
- ✓ 代码语法/import/未定义函数 0 真错
- 30 个启发式"未定义"都是 callback 形参, 不影响运行
