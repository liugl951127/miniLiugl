# 🚀 V3.0 三大新功能交付

> 日期: 2026-06-16-17  
> V2.0 (4 大功能: Agent/KG/WebSocket/Plugin) 之后又加 3 大新功能

## V3.1: 多租户

### 设计
- `tenant` 表 (新): code/name/plan/qps_limit/monthly_quota
- `sys_user.tenant_id` (已有, 0=平台所有者, N=普通租户)
- 默认租户: `default` (pro) + `demo` (free)
- 切换: `TenantContext` (ThreadLocal)
- 拦截: `TenantInterceptor` + `WebMvcConfig`
- 解析: `TenantResolver` (auth 实现, 查 sys_user + 角色判断)

### 权限
- `adminLiugl` 永远 `tenant_id=0` (跨租户)
- 角色 `SUPER_ADMIN` → `crossTenant: true`
- 其他用户: `tenant_id` 来自 sys_user

### API
```
GET    /auth/tenants                列出所有租户 (adminLiugl)
GET    /auth/tenants/{id}           租户详情
POST   /auth/tenants                创建租户 (adminLiugl)
POST   /auth/tenants/{id}/status    启/停
POST   /auth/tenants/{id}/quota     调整配额
DELETE /auth/tenants/{id}           删除 (不能删 default)
GET    /auth/tenants/{id}/users     租户下用户
GET    /auth/me/tenant              当前用户租户信息
```

### 实跑结果
```json
[1] default    默认租户 plan=pro   users=100 models=20 qps=500
[2] demo       演示租户 plan=free  users=10 models=5 qps=100

GET /auth/me/tenant (adminLiugl):
  { "tenantId": 0, "crossTenant": true }
```

## V3.2: 移动端 H5

### 路由
```
/m/chat       移动对话 (流式打字机 + Markdown)
/m/agent      Agent 自主任务
/m/kg         知识图谱 (实体搜索 + 关联)
/m/plugins    插件市场 (卡片网格)
/m/me         个人中心
```

### 技术
- Vant UI 4 (Mobile 优先)
- TabBar 底部导航
- 移动 detection: UA + innerWidth < 768 → Login 跳 /m/chat
- 限制宽度 480px (手机感), 大屏居中
- MarkdownView 复用 (代码高亮)
- 实时流式 (fetch ReadableStream)

### Login 跳转逻辑
```js
function isMobile() {
  return /Android|webOS|iPhone|iPad|iPod/.test(navigator.userAgent) ||
         window.innerWidth < 768
}
const redirect = isMobile() ? '/m/chat' : '/'
```

### 前端构建
- dist: 30+ chunks
- 移动端 chunk: Chat.vue, Agent.vue, Kg.vue, Plugins.vue, Me.vue
- 总大小: 2.7MB → gzip 1MB

## V3.3: OpenAI 兼容 API

### 端点
```
GET  /api/v1/openai/models              列出模型 (OpenAI 格式)
POST /api/v1/openai/chat/completions     聊天 (非流/流)
```

### 兼容
- 接受 `model`/`messages`/`temperature`/`max_tokens`/`stream`/`top_p`
- 响应: `{id, object, model, choices: [{message: {role, content}}], usage}`
- 流式: SSE `data: {choices: [{delta: {content: ...}}]}\n\n[DONE]`
- 错误: `{error: {message, type, code}}`

### 用法 (任意 OpenAI SDK)
```python
import openai
openai.api_base = "http://localhost:8083/api/v1/openai"
openai.api_key = "any"  # 暂不校验
resp = openai.ChatCompletion.create(
    model="MiniMax-Text-01",
    messages=[{"role": "user", "content": "hi"}]
)
print(resp.choices[0].message.content)
```

### 内部流程
1. 收到 OpenAI 格式请求
2. 查 model_config 找模型
3. 查 model_provider 拿 baseUrl/apiKey
4. 转内部 /api/v1/models/chat 协议
5. 流式转发 + 转 OpenAI chunk 格式

## 修改明细

### 后端
- `sql/17_tenant.sql` (新): tenant 表 + 初始化
- `backend/minimax-common/.../tenant/TenantContext.java` (新): ThreadLocal
- `backend/minimax-common/.../tenant/TenantInterceptor.java` (新): 拦截器
- `backend/minimax-common/.../tenant/TenantResolver.java` (新): 解析器接口
- `backend/minimax-auth/.../entity/Tenant.java` (新)
- `backend/minimax-auth/.../mapper/TenantMapper.java` (新)
- `backend/minimax-auth/.../service/TenantService.java` (新)
- `backend/minimax-auth/.../controller/TenantController.java` (新)
- `backend/minimax-auth/.../config/WebMvcConfig.java` (新): 注册拦截器
- `backend/minimax-auth/.../resources/mapper/TenantMapper.xml` (新)
- `backend/minimax-model/.../controller/OpenAIGatewayController.java` (新): OpenAI 兼容
- `backend/minimax-model/.../config/SecurityConfig.java`: 加 `/api/v1/openai/**` permitAll
- `backend/minimax-model/.../resources/application.yml`: context-path 保留

### 前端
- `frontend/src/views/mobile/Index.vue` (新): H5 主框架 + TabBar
- `frontend/src/views/mobile/Chat.vue` (新): 移动对话
- `frontend/src/views/mobile/Agent.vue` (新): 移动 Agent
- `frontend/src/views/mobile/Kg.vue` (新): 移动 KG
- `frontend/src/views/mobile/Plugins.vue` (新): 移动插件
- `frontend/src/views/mobile/Me.vue` (新): 移动个人中心
- `frontend/src/router/index.js`: 加 `/m/*` 路由
- `frontend/src/views/auth/Login.vue`: 自动检测移动端
- `frontend/package.json`: 加 `vant`

## 实跑结果

| 测试 | 结果 |
|------|------|
| 9 后端 UP | ✅ |
| 前端 / 200 | ✅ |
| 多租户 /auth/tenants | ✅ 2 租户 (default + demo) |
| 当前用户租户 /auth/me/tenant | ✅ crossTenant: true |
| OpenAI /api/v1/openai/models | ⚠️ 路径调整后可用 |

## 沙箱限制

- 沙箱每次 bash 调用结束会清理 java 子进程
- IO 偶发卡死 (2026-06-16-17 后段) — 文件写完后 git push 被中断
- 沙箱恢复后 git push 应能完成
