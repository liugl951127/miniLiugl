# 路由冲突修复 V3 (V6.8.1) — /api/v1/ai/admin/tools

## 冲突
- `AiAdminRealController` (@RequestMapping("/api/v1/ai/admin")) + `AiToolAdminController` (@RequestMapping("/api/v1/ai/admin")) 重复
- 7 个路由冲突:
  - GET    /api/v1/ai/admin/tools
  - POST   /api/v1/ai/admin/tools
  - GET    /api/v1/ai/admin/tools/{id}  (1 个用 {id}, 1 个用 {code} - 同)
  - PUT    /api/v1/ai/admin/tools/{id}
  - DELETE /api/v1/ai/admin/tools/{id}
  - POST   /api/v1/ai/admin/tools/{id}/invoke (1 个 {id}, 1 个 {code} - 同)
  - GET    /api/v1/ai/admin/datasources

## 修法
- 保留 `AiToolAdminController` (前端引用, 更专业)
- `AiAdminRealController` 改 `@RequestMapping` 加 `-real` 后缀
  - 改前: `/api/v1/ai/admin` → 改后: `/api/v1/ai/admin-real`

## 最终路径

**AiAdminRealController** (改后):
- GET    /api/v1/ai/admin-real/tools
- POST   /api/v1/ai/admin-real/tools
- GET    /api/v1/ai/admin-real/tools/{id}
- PUT    /api/v1/ai/admin-real/tools/{id}
- DELETE /api/v1/ai/admin-real/tools/{id}
- POST   /api/v1/ai/admin-real/tools/{id}/invoke
- GET    /api/v1/ai/admin-real/templates
- GET    /api/v1/ai/admin-real/datasources
- GET    /api/v1/ai/admin-real/codegen

**AiToolAdminController** (保留):
- GET    /api/v1/ai/admin/tools
- POST   /api/v1/ai/admin/tools
- GET    /api/v1/ai/admin/tools/{code}
- PUT    /api/v1/ai/admin/tools/{id}
- DELETE /api/v1/ai/admin/tools/{id}
- POST   /api/v1/ai/admin/tools/{code}/invoke
- GET    /api/v1/ai/admin/datasources
- POST   /api/v1/ai/admin/datasources
- PUT    /api/v1/ai/admin/datasources/{id}
- DELETE /api/v1/ai/admin/datasources/{id}
- POST   /api/v1/ai/admin/datasources/{id}/test
- POST   /api/v1/ai/admin/codegen

## 编译验证
```
mvn compile -pl minimax-ai -am
[INFO] BUILD SUCCESS

mvn compile -fae (全 14 module)
[INFO] BUILD SUCCESS (0 错)
```

## 前端影响
- ✅ 前端 `/ai/admin/tools` → `AiToolAdminController` (保留) - 无影响
- ✅ 前端 `/ai/admin/datasources` → `AiToolAdminController` (保留) - 无影响
- ✅ 前端 `/ai/admin/codegen` POST → `AiToolAdminController` (保留) - 无影响

## 冲突扫描
```
$ python3 scripts/check-route-conflicts.py
```
- /api/v1/ai/admin/tools/* 冲突已解
- 剩余冲突全为 GlobalMissingController 兜底 (设计保留)
