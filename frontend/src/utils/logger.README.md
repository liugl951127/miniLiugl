# Frontend Logger (V6.8.1+)

## 4 级
- `debug` (灰)
- `info` (蓝)
- `warn` (橙)
- `error` (红)
- `success` (绿)

## 使用
```js
import logger from '@/utils/logger'
// 或
import { useLogger } from '@/utils/logger'
const log = useLogger()

logger.info('Module', '用户登录', { userId: 1 })
logger.error('API', '请求失败', err)
```

## 特性
- ⏰ 时间戳 (ms 精度)
- 🎨 5 级颜色 (Chrome / Edge 折叠)
- 📚 历史记录 (localStorage, 100 条, 跨刷新)
- 🔍 过滤 (level / module / since)
- 💾 一键导出: `__minimax_downloadLogs()` 浏览器控制台
- 📤 远程上报 (默认关): `logger.enableReport('/api/v1/ai/log/collect')`
- 🪟 浏览器调试: `__minimax_log.info('Test', 'msg')`

## 控制台
```js
__minimax_log           // logger 实例
__minimax_downloadLogs  // 一键导出当前 100 条
__minimax_log.getHistory({ level: 'error' })  // 仅错
__minimax_log.getHistory({ module: 'API' })  // 仅 API 模块
__minimax_log.clearHistory()  // 清空
```

## 已集成
- `ErrorBoundary.vue` (V6.8.1+): 所有组件错误自动记录
