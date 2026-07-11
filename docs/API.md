# MiniMax Platform API 参考 (V2.8.2)

> 完整 REST API 文档 · 适用 V2.5+ V2.8

## 1. 通用规范

### 1.1 基础 URL
```
http://{host}:7080/api/{version}/{module}
https://your-domain.com/api/{version}/{module}
```

### 1.2 响应格式
所有 API 统一返回:
```json
{
  "code": 0,          // 0 = 成功, 非 0 = 错误
  "message": "success",
  "data": { ... },     // 业务数据
  "timestamp": 1700000000000
}
```

### 1.3 鉴权
请求头: `Authorization: Bearer {jwt_token}`

获取方式: `POST /api/v1/auth/login`

### 1.4 错误码
| 码 | 含义 |
|----|------|
| 0 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器异常 |

## 2. 认证 (auth)

### 2.1 登录
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin@123"
}

→ 200 OK
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOi...",
    "userId": 1,
    "nickname": "Admin",
    "role": "ADMIN"
  }
}
```

### 2.2 注册
```
POST /api/v1/auth/register
{
  "username": "newuser",
  "password": "Pass@123",
  "nickname": "新用户",
  "email": "user@example.com"
}
```

## 3. AI 平台 (V2.5+ V2.7+)

> Base path: `/api/ai/`

### 3.1 智能路由
```
POST /api/ai/dispatch
{
  "text": "画一个柱状图, 维度是 type"
}

→ 200 OK
{
  "code": 0,
  "data": {
    "intent": "GENERATE_CHART",
    "params": {"type": "bar", "dimension": "type"},
    "handler": "ChartGenerator",
    "result": {...}
  }
}
```

### 3.2 图表生成
```
POST /api/ai/chart/render
{
  "type": "BAR",      // BAR/LINE/PIE/SCATTER/RADAR/HEATMAP/SANKEY
  "title": "用户分布",
  "series": [
    {"name": "总数", "values": [10, 20, 30]}
  ]
}

→ 200 OK
{
  "code": 0,
  "data": {
    "imageBase64": "iVBORw0KGgo...",
    "mimeType": "image/png",
    "width": 1024,
    "height": 600
  }
}
```

### 3.3 音乐生成
```
POST /api/ai/music/generate
{
  "style": "POP",
  "key": "C",
  "scale": "major",
  "bpm": 120,
  "bars": 8
}

→ 200 OK
{
  "code": 0,
  "data": {
    "midiBase64": "TVRoZAAAAAY...",
    "size": 1024
  }
}
```

### 3.4 AIGC 图片
```
POST /api/ai/image/generate
{
  "prompt": "蓝色渐变",
  "type": "gradient",        // 可选, 自动推断
  "width": 1024,
  "height": 1024,
  "seed": 42                  // 确定性
}

→ 200 OK
{
  "code": 0,
  "data": {
    "base64": "iVBORw0KGgo...",
    "type": "gradient",
    "sizeBytes": 50000
  }
}
```

### 3.5 视频流式生成 (SSE)
```
GET /api/ai/video/stream/sse?title=test&width=640&height=360&fps=12&duration=4
Accept: text/event-stream

→ event: start
  data: {"taskId":"stream-123","totalFrames":48}

→ event: frame
  data: {"taskId":"stream-123","index":0,"data":"<base64>","isLast":false}

→ event: progress
  data: {"taskId":"stream-123","percent":50,"elapsedMs":2000}

→ event: complete
  data: {"taskId":"stream-123","durationMs":4000}
```

### 3.6 音乐流式生成 (SSE)
```
GET /api/ai/music/stream/sse?style=POP&key=C&scale=major&bpm=120&bars=8
```

### 3.7 文档解析
```
POST /api/ai/document/parse
Content-Type: multipart/form-data

file: <binary>

→ 200 OK
{
  "code": 0,
  "data": {
    "type": "pdf",
    "content": "...",
    "paragraphs": [...],
    "tables": [...],
    "keywords": ["人工智能", "深度学习", ...],
    "summary": "...",
    "wordCount": 5000
  }
}
```

### 3.8 训练任务
```
POST /api/ai/training/start
{
  "name": "训练任务1",
  "model": "mini-transformer",
  "epochs": 5,
  "learningRate": 0.01
}

→ 200 OK
{
  "code": 0,
  "data": {"taskId": "train-1700000000-1234"}
}

GET /api/ai/training/tasks/{id}/history
→ { "points": [...], "emaLoss": [...], "finalLoss": 4.2 }
```

### 3.9 工作流编排
```
POST /api/ai/workflow/execute
{
  "name": "ad-hoc",
  "nodes": [
    {"id": "s1", "toolCode": "sql.query", "input": {"dataSourceId": 1, "question": "..."}},
    {"id": "s2", "toolCode": "data.analyze.stats", "input": {"dataSourceId": 1, "table": "user"}}
  ],
  "edges": [{"from": "s1", "to": "s2"}]
}
```

### 3.10 AI 工具管理
```
GET  /api/ai/tools                # 列出所有工具
GET  /api/ai/tools/{code}         # 工具详情
POST /api/ai/tools/{code}/invoke  # 调用工具
{
  "dataSourceId": 1,
  "table": "user",
  "column": "age"
}
```

### 3.11 AI 会话管理
```
GET    /api/ai/chat/sessions?userId=1
GET    /api/ai/chat/sessions/{id}
POST   /api/ai/chat/sessions       { title, userId }
DELETE /api/ai/chat/sessions/{id}
```

## 4. 权限 (V2.7.9)

```
GET  /api/ai/permission/me          # 当前用户权限
GET  /api/ai/permission/roles       # 所有角色
POST /api/ai/permission/check       { role, permissions: ["ai.use"] }
```

## 5. 多模态分析

### 5.1 图像
```
POST /api/multimodal/image/analyze  (multipart: file)
→ { pHash, histogram, embedding, colorTone, dominantColor }
```

### 5.2 音频
```
POST /api/multimodal/audio/analyze  (multipart: file)
→ { duration, rms, dBFS, zcr, spectrum, emotion }
```

### 5.3 视频
```
POST /api/multimodal/video/analyze  (multipart: file)
→ { duration, format, tracks, bitrate }
```

## 6. 监控 (monitor)

### 6.1 告警
```
GET  /api/monitor/alerts?limit=20
GET  /api/monitor/alerts/firing
GET  /api/monitor/alerts/rules
GET  /api/monitor/alerts/channels
GET  /api/monitor/alerts/summary
POST /api/monitor/alerts/rules
PUT  /api/monitor/alerts/rules/{id}
DELETE /api/monitor/alerts/rules/{id}
POST /api/monitor/alerts/ack/{eventId}
```

### 6.2 审计
```
GET /api/admin/audit/recent?limit=50
GET /api/admin/audit/by-actor/{userId}
GET /api/admin/audit/by-day
GET /api/admin/audit/export
```

## 7. 数据源管理

```
GET  /api/ai/datasources
POST /api/ai/datasources      { name, type, url, username, password }
PUT  /api/ai/datasources/{id}
DELETE /api/ai/datasources/{id}
POST /api/ai/datasources/{id}/test    # 测试连接
GET  /api/ai/datasources/{id}/schema  # 查表结构
POST /api/ai/datasources/{id}/query   # 查数据
```

**支持类型**: MySQL, PostgreSQL, Oracle, SQLServer, H2, ClickHouse, Doris

## 8. 合规 (V2.6+)

### 8.1 数据脱敏
```
POST /api/ai/multimodal/compliance/mask
{
  "text": "手机 13800138000, 身份证 110101199001011234",
  "types": ["MOBILE", "ID_CARD"]
}

→ { "masked": "手机 138****8000, 身份证 110***********1234" }
```

### 8.2 文件加密
```
POST /api/ai/multimodal/compliance/encrypt  (file)
→ { "encryptedBase64": "MMX1..." }
POST /api/ai/multimodal/compliance/decrypt  (file)
→ { "decryptedBase64": "..." }
```

## 9. WebSocket 协议

### 9.1 端点
```
ws://{host}:7080/ws/queue
```

### 9.2 消息格式
```json
// 入站
{"type": "subscribe", "channel": "user-1"}

// 出站 (服务器推送)
{
  "type": "message",
  "channel": "user-1",
  "payload": {
    "text": "...",
    "from": "agent-1"
  }
}
```

## 10. SDK

### 10.1 前端 (ai.js)
```javascript
import { dispatchPrompt, generateImage, demoTraining } from '@/api/ai'

const r = await dispatchPrompt('画个饼图')
const img = await generateImage({ prompt: '蓝色', type: 'gradient' })
const task = await demoTraining()
```

### 10.2 权限指令
```vue
<el-button v-permission="'ai.admin'">删除</el-button>
```

## 11. 错误排查

| 错误 | 原因 | 解决 |
|------|------|------|
| 401 Unauthorized | Token 过期 | 重新登录 |
| 403 Forbidden | 无权限 | 联系管理员分配角色 |
| 404 Not Found | API 不存在 | 检查 URL/版本号 |
| 500 Internal Error | 后端异常 | 查看日志 |
| SSE 连接立即关闭 | Nginx 缓冲 | 加 `proxy_buffering off;` |

## 12. 限流

默认配置 (Bucket4j):
- 全局: 100 req/s
- 单 IP: 20 req/s
- 写操作: 10 req/s

超限返回: `429 Too Many Requests`
