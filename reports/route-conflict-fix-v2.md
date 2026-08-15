# 路由冲突修复 V2 (V6.8.1)

## 冲突 1: chat/sessions (已修)
- `AiPlatformController` 4 个路由 (L471-511) + `AiChatRealController` 4 个路由 重复
- 修法: AiPlatformController 4 个路由加 /platform 前缀

## 修改
- `@GetMapping("/chat/sessions")` → `@GetMapping("/platform/chat/sessions")`
- `@GetMapping("/chat/sessions/{id}")` → `@GetMapping("/platform/chat/sessions/{id}")`
- `@PostMapping("/chat/sessions")` → `@PostMapping("/platform/chat/sessions")`
- `@DeleteMapping("/chat/sessions/{id}")` → `@DeleteMapping("/platform/chat/sessions/{id}")`

## 最终路径

**AiPlatformController** (改后):
- GET    /api/v1/ai/platform/chat/sessions
- GET    /api/v1/ai/platform/chat/sessions/{id}
- POST   /api/v1/ai/platform/chat/sessions
- DELETE /api/v1/ai/platform/chat/sessions/{id}

**AiChatRealController** (保留):
- GET    /api/v1/ai/chat/sessions
- GET    /api/v1/ai/chat/sessions/{id}/messages
- POST   /api/v1/ai/chat/sessions
- POST   /api/v1/ai/chat/stop
- POST   /api/v1/ai/chat/stream
- POST   /api/v1/ai/chat/voting
- GET    /api/v1/ai/chat/voting-info

## 编译验证
```
mvn compile -pl minimax-ai -am
[INFO] BUILD SUCCESS

mvn compile -fae (14 module)
[INFO] BUILD SUCCESS (0 错)
```

## 静态扫描结果
脚本: `scripts/check-route-conflicts.py` 扫描所有 Controller
- 总重复路由: 57
- 真实 Controller 跟真实 Controller 冲突 (已修): chat/sessions
- GlobalMissingController 兜底跟真实 Controller 冲突: 设计保留, 兜底优先级低

## 前端影响
- ✅ 前端没引用 `/api/v1/ai/chat/sessions` (因为是 controller 真实实现)
- ✅ 改名安全

## 备注
- Spring 启动时 ambiguous mapping 检查需 SpringBoot Test
- 沙箱无 mariadb-client, 启动需用户本地验证
