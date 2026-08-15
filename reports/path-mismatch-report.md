# 前后端 API 路径不匹配报告 (V6.8.1)

## 总览
- 前端 API 调用: 374 唯一
- 后端路由: 836 唯一
- ✓ 匹配: 321 (85%)
- ✗ 不匹配: 53 (15%)

## 不匹配分类
### 1. 路径重复 /api/v1 (已修)
- agent.js 5 处 - ✓ 已修

### 2. 前端路径错误 (需修前端)
- ai.js 24 处: tools/{type}.analyze/invoke, 实际应是 /ai/admin/tools/{id}/invoke
- prompt.js 4 处: /*/* 等模板提取问题

### 3. 后端缺路径 (需修后端或加兜底)
- POST /api/v1/ai/agent (创建 Agent)
- POST /api/v1/ai/multimodal/compliance/moderate
- POST /api/v1/ai/multimodal/compliance/refresh
- GET /api/v1/ai/kb/search/keyword
- GET /api/v1/ai/dashboard/metrics
- POST /api/v1/ai/dashboard/from
- POST /api/v1/ai/distributed/all
- POST /api/v1/ai/animation/text
- POST /api/v1/ai/pipeline/config/compute
- POST /api/v1/ai/video/from
- GET /api/v1/ai/webhooks
- POST /api/v1/ai/chat (创建 session)
- POST /api/v1/ai/autofill
- GET /api/v1/ai/model
- POST /api/v1/ai/raft/trigger
- GET /api/v1/ai/push/integration/vapid
- POST /api/v1/analytics/query/dry
- GET /api/v1/auth/apikeys
- GET /api/v1/memory/short
- POST /api/v1/memory/cross
- POST /api/v1/memory/long
- POST /api/v1/model/providers
- GET /api/v1/admin/audit/by (后端是 by-actor)
- GET /api/v1/monitor/anomaly/active
- GET /api/v1/auth/notifications
- GET /api/v1/sessions
- POST /api/v1/sessions/stop
- GET /api/v1/api/ai/intro
- GET /api/v1/auth/wechat/mock
- POST /api/v1/auth/wechat/mobile
- POST /api/v1/admin/users/{id}/reset-password (前端说 reset, 后端 reset-password)
- GET /api/v1/admin/audit (类级别映射)

## 建议
1. **前端统一**: 用 path parameter 形式 /ai/admin/tools/{toolCode}/invoke
2. **后端补缺失**: 50+ 端点加在 MissingAiController
3. **加兜底**: 不存在路径返 200 + mock 数据 (前端已有)
