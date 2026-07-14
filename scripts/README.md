# Scripts

## audit-api.py

端到端 API 审计脚本, 扫描前端 API 调用 + 后端 Controller 端点, 找出:
- 前端调用但后端未提供的接口
- 后端提供但前端未调用的接口
- 未注册的 Vue 视图
- 路由引用但文件不存在

### 用法

```bash
python3 scripts/audit-api.py
```

### 集成 CI

`.github/workflows/audit.yml` 配置了 3 种触发:
- 每次 push / PR
- 每周一早上 9 点
- 手动触发

### 输出

- 控制台: 人类可读报告
- `report.json`: 详细结构化数据

### 匹配规则

- 精确匹配: `(path, method) == backend_endpoints`
- 通配符: 前端 `/*` 匹配后端 `/{id}` `/{name}` `/{code}` 等
- 路径段匹配: 关键词集合相同

### 排除

- `/fallback/**` (Spring 兜底)
- `/actuator/**` (Spring Boot 监控)
- 内部端点 (`/internal/*`)
