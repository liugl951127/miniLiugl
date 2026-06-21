# MiniMax API 文档中心 (V5.11)

## 入口

| 入口 | URL | 说明 |
|------|-----|------|
| **主入口** | `http://localhost:3000/api-docs` | 聚合页 (V5.11 新增) |
| 兼容入口 | `http://localhost:3000/doc.html` | 重定向到主入口 |
| 直连 monitor | `http://localhost:8089/api-docs.html` | (开发调试) |
| 单个服务 | `http://localhost:3000/api/v1/{module}/doc.html` | 单独服务 knife4j |

## 架构

```
┌─────────┐   /api-docs     ┌────────────┐
│ Browser │ ──────────────► │  nginx     │
│         │                 │  :3000     │
└────┬────┘                 └─────┬──────┘
     │                            │ /api/v1/monitor/api-docs
     │                            ▼
     │                      ┌────────────┐
     │  iframe + tab        │ Gateway    │  StripPrefix=2
     │  (13 microservices)  │ :8080      │
     │                      └─────┬──────┘
     │                            │ lb://minimax-monitor
     │                            ▼
     │                      ┌────────────────────┐
     │                      │ minimax-monitor    │
     │  /api-docs.html ◄────│ :8089/monitor/api-docs │
     │                      │ (V5.11 ApiDocsController)│
     │                      └────────────────────┘
     │  tab 切换后 iframe 直接加载目标服务的 knife4j
     │
     ├──► /api/v1/auth/doc.html      → lb://minimax-auth
     ├──► /api/v1/chat/doc.html      → lb://minimax-chat
     ├──► /api/v1/model/doc.html     → lb://minimax-model
     ├──► /api/v1/memory/doc.html    → lb://minimax-memory
     ├──► /api/v1/rag/doc.html       → lb://minimax-rag
     ├──► /api/v1/function/doc.html  → lb://minimax-function
     ├──► /api/v1/admin/doc.html     → lb://minimax-admin
     ├──► /api/v1/multimodal/doc.html→ lb://minimax-multimodal
     ├──► /api/v1/monitor/doc.html   → lb://minimax-monitor
     ├──► /api/v1/agent/doc.html     → lb://minimax-agent
     ├──► /api/v1/prompt/doc.html    → lb://minimax-prompt
     └──► /api/v1/ws/doc.html        → lb://minimax-ws
```

## 聚合页功能 (V5.11)

- **13 个 tab**: 一键切换不同服务的 API 文档
- **iframe 嵌入**: 复用各服务自己的 knife4j UI (无重复开发)
- **本地记忆**: 记住上次查看的服务
- **服务器信息**: 显示服务版本 / 当前时间
- **自定义 baseUrl**: 工具栏可改 (如直连测试)
- **新窗口打开**: ↗ 按钮新窗口看文档

## 关键文件

| 文件 | 用途 |
|------|------|
| `backend/minimax-monitor/src/main/resources/static/api-docs.html` | 聚合页 UI (7KB) |
| `backend/minimax-monitor/src/main/java/com/minimax/monitor/controller/ApiDocsController.java` | `/monitor/api-docs` 入口 |
| `backend/minimax-common/src/main/resources/application-common.yml` | knife4j/springdoc 统一配置 (下沉重构) |
| `scripts/nginx-minimax-3000.conf` | `/api-docs` / `/doc.html` 入口 + 12 个 service location 注释 |

## V5.11 顺手清理 (Knife4j 配置下沉重构)

**Before**: 10 个业务 yml 都有重复 `knife4j:` 块 (V5.7 + day-17 两次叠加)
**After**: knife4j/springdoc 统一在 `application-common.yml`, 各业务 yml 只放差异化配置

**检测脚本**:
```python
import yaml
from collections import Counter
text = open('application.yml').read()
tops = [l.split(':')[0] for l in text.split('\n') if l and not l.startswith(' ')]
dupes = [k for k, v in Counter(tops).items() if v > 1]
print('DUP' if dupes else 'OK')
```

## knife4j 高级配置 (V5.11)

`application-common.yml`:
```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  packages-to-scan: com.minimax
knife4j:
  enable: true
  setting:
    language: zh_cn                       # 中文 UI
    enable-version: true                  # 多版本切换
    enable-swagger-models: true           # 显示实体类列表
    swagger-model-name: 实体类列表
```

## 鉴权

- 文档中心 (`/api-docs.html`) 公开访问 (knife4j 自身无 JWT)
- 内部 API 调用通过浏览器传 JWT (浏览器 localStorage `access_token`)
- 13 个服务的 knife4j 继承 SecurityConfig 白名单 `/doc.html` (V5.5)
