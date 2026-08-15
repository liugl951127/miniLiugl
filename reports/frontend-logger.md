# 前端日志工具 (V6.8.1+)

## 新增
- `frontend/src/utils/logger.js` — 通用日志 (4 级 + 历史 + 一键导出 + 远程上报)
- `frontend/src/utils/logger.README.md` — 使用文档
- `frontend/src/__tests__/logger.test.js` — 11 个测试用例

## 集成
- `frontend/src/components/ErrorBoundary.vue` — 组件错误自动记录

## 5 个级别
| 级别 | 颜色 | 用途 |
|---|---|---|
| debug | 灰 | 调试信息 (生产关闭) |
| info | 蓝 | 普通信息 |
| warn | 橙 | 警告 (异常但可恢复) |
| error | 红 | 错误 (功能受损) |
| success | 绿 | 成功事件 |

## 4 大特性
1. **时间戳** (ms 精度 ISO 8601)
2. **历史记录** (localStorage 100 条, 跨刷新)
3. **一键导出** (`__minimax_downloadLogs()` 下载 .log 文件)
4. **远程上报** (默认关, `logger.enableReport(url)` 开启)

## 浏览器控制台
```js
__minimax_log           // logger 实例
__minimax_downloadLogs  // 一键导出
__minimax_log.getHistory({ level: 'error' })  // 过滤
__minimax_log.clearHistory()  // 清空
```

## 测试
- 11 个 logger 测试用例 (全部通过)
- 24 个 test files, 251 tests (原 240 + 11)
- 跑通 vitest run

## 错误信息示例
```
[2026-08-09 09:43:23.119] [INFO] [Test] file-content
[2026-08-09 09:46:00.412] [INFO] [Test] old
[2026-08-09 09:46:00.433] [ERROR] [Test] Error: boom
  at /run/csi/.../src/__tests__/logger.test.js:64:17
```

## API
```js
import logger, { useLogger, downloadLogs } from '@/utils/logger'

// 5 级
logger.debug('Module', 'msg', { data })
logger.info('Module', 'msg')
logger.warn('Module', 'msg')
logger.error('Module', 'msg', err)
logger.success('Module', 'msg')

// 配置
logger.setLevel('warn')  // 只看 warn 以上
logger.enableReport('/api/v1/ai/log/collect')
logger.disableReport()

// 查询
logger.getHistory()                    // 全部
logger.getHistory({ level: 'error' })  // 仅错
logger.getHistory({ module: 'API' })   // 仅某模块
logger.getHistory({ since: Date.now() - 60000 })  // 最近 1 分钟

// 清空
logger.clearHistory()
```

## V6.8.1+ 集成点
1. **ErrorBoundary.vue** — 组件错误自动 `logger.error('ErrorBoundary', ...)`
2. **window 挂载** — `__minimax_log` + `__minimax_downloadLogs`
3. **localStorage** — `minimax_log` (历史) + `minimax_log_config` (配置)
