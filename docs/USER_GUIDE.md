# 📖 MiniMax Platform — 用户操作手册

> 完整功能说明 + 操作步骤 + 截图位置 + 故障排查

---

## 🔑 账号清单

| 账号 | 密码 | 角色 | 权限 |
|------|------|------|------|
| `adminLiugl` | `Liugl@2026` | SUPER_ADMIN | 全部 (跨租户) |
| `admin` | `admin@123` | ADMIN | 普通管理 |
| 自注册 | - | USER | 默认 (租户 default) |

---

## 1️⃣ 对话 (智能聊天)

### 功能
- 多模型路由 (6 个模型, GPT-4o / MiniMax-Text-01 / VL-01 / Ollama / Qwen / DeepSeek)
- **真流式输出** (打字机效果)
- 多会话管理 (侧边栏)
- Markdown + 代码高亮
- 工具调用展示
- RAG 引用来源
- 模型切换

### 操作
1. 登录 → 默认进入 `/chat`
2. 左侧会话列表 → 点击新建会话或选已有
3. 输入框输入问题 → Enter 发送
4. 流式显示回复 (打字机)
5. 点击代码块右上角"复制"
6. 切换顶部模型下拉

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/chat/Index.vue`
- 后端: `minimax-chat` 8082

### 快捷键
- `Enter` 发送
- `Shift+Enter` 换行

---

## 2️⃣ 知识库 (RAG)

### 功能
- 3 种文档解析: TXT / DOCX / PDF
- 智能分块 (500 字符 / 50 滑动窗口)
- SHA-256 去重
- 3 级降级检索 (向量/关键词/全量)
- 引用来源展示
- 知识库管理

### 操作
1. 进入 `/knowledge`
2. 点击"上传文档" → 选 PDF/DOCX/TXT
3. 自动分块入库
4. 在对话页启用 RAG: 顶部 RAG 开关
5. 提问 → 答案显示引用来源

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/knowledge/Index.vue`
- 后端: `minimax-rag` 8085

---

## 3️⃣ 记忆中心

### 功能
- **短期记忆**: Redis + Caffeine 双层缓存
- **长期记忆**: MySQL BLOB 向量 + 余弦相似度召回
- 4 种记忆类型: 短期 / 长期 / 偏好 / 摘要
- 自动摘要压缩 (LLM 摘要)
- 跨会话记忆召回

### 操作
1. 进入 `/memory`
2. 查看"短期记忆" (最近 5 轮对话)
3. 查看"长期记忆" (按相似度召回)
4. 编辑"用户偏好" (显式)
5. 触发"自动摘要" (LLM 压缩)

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/memory/Index.vue`
- 后端: `minimax-memory` 8084

---

## 4️⃣ Agent 自主任务

### 功能
- **ReAct 模式** (Thought/Action/Observation)
- 自主规划 → 调工具 → 反思 → 给出答案
- 最多 8 轮自动循环
- 4 个内置工具 (time/calc/http/random)
- 可视化思考过程 (时间线)

### 操作
1. 进入 `/agent`
2. 输入目标 (例: "查北京天气, 然后发邮件给张伟")
3. 选择可用工具 (留空 = 全部)
4. 点击"🚀 执行"
5. 观察: 每轮 Round 显示 Thought + Action + Observation
6. 最终答案在底部

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/agent/Index.vue`
- 后端: `minimax-agent` 8090

### 用例
- 简单计算: "123 * 456 - 789"
- 多步: "查天气 → 发邮件"
- 调研: "搜索 5 个竞品价格"

---

## 5️⃣ 知识图谱

### 功能
- 实体管理 (人物/地点/组织/概念/事件)
- 关系管理 (works_at / located_in / friend_of / owns)
- 1 跳 / 2 跳邻居查询
- 最短路径 (BFS)
- 重要性评分 (1-10)
- 别名

### 操作
1. 进入 `/kg`
2. 左侧"添加实体" → 填名称/类型/描述/重要性
3. 搜索框 → 输入关键词
4. 点击实体 → 查看 1 跳 / 2 跳邻居
5. 创建关系: 选源实体 + 选目标 + 关系类型
6. 路径查询: `/kg/path?from=X&to=Y`

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/kg/Index.vue`
- 后端: `minimax-agent/kg/*` 8090

### 用例
- 关系推理: "张三是哪家公司 CEO?"
- 影响分析: "我删 X, 哪些关联受影响?"

---

## 6️⃣ 实时协作

### 功能
- 多人同时编辑会话 (WebSocket)
- 消息广播
- typing 指示
- cursor 位置同步
- 在线用户列表
- 房间历史回放

### 操作
1. 进入 `/collab`
2. 点击"创建协作会话" → 拿到 sessionId
3. 其他用户输入同一 sessionId → 加入
4. 消息实时广播给所有成员
5. 头部查看在线用户

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/collab/Index.vue`
- 后端: `minimax-agent/collab/*` 8090
- WS: `/ws/collab/{sessionId}?userId=N`

---

## 7️⃣ 插件市场

### 功能
- 系统插件 (4 个内置) + 用户插件
- 4 种类型: class / url / js / wasm
- 评分 + 下载量
- 启停控制
- 分类: UI/导出/增强/通用

### 操作
1. 进入 `/plugins`
2. 浏览 4 个系统插件 (天气小组件/Markdown导出/代码格式化/翻译)
3. 点击 → 查看详情 + 评分
4. 顶部"发布我的插件" → 填表
5. 评分/启停 (adminLiugl)

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/plugins/Index.vue`
- 后端: `minimax-agent/plugins/*` 8090

### 内置插件
| 名称 | 类别 | 功能 |
|------|------|------|
| weather-widget | ui | 天气小组件 |
| markdown-export | export | 会话导出 Markdown |
| code-formatter | enhance | 代码块自动格式化 |
| translation | enhance | 中英互译 |

---

## 8️⃣ 管理后台 (admin)

### 功能
- 仪表盘 (ECharts: KPI / 折线 / 饼图)
- 服务健康监控 (30s 自动刷新)
- 跨服务用户管理
- 模型配额管理
- 审计日志
- API 限流监控

### 操作
1. 进入 `/admin`
2. 仪表盘: 查看用户数/调用量/错误率
3. 服务健康: 顶部 pill, 11/11 UP
4. 审计日志: 时间倒序查看操作
5. (adminLiugl 专属) 跨服务管理

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/admin/Index.vue` + `Dashboard.vue`
- 后端: `minimax-admin` 8087

---

## 9️⃣ 多模态 (视觉)

### 功能
- 图片上传 (PNG/JPEG/GIF/WebP)
- Vision 模型理解 (VL-01)
- 图片信息提取
- 拖拽上传
- 在对话中直接插入图片

### 操作
1. 进入 `/chat`
2. 拖拽图片到输入框
3. 自动调用 vision 模型
4. 文字 + 图片一起提问

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/chat/Index.vue` (拖拽)
- 后端: `minimax-multimodal` 8088

---

## 🔟 监控告警

### 功能
- **5 类业务指标**: chat 调用 / token 用量 / 用户活跃 / 错误率 / 延迟
- **4 类网关指标**: HTTP/DB/Redis/JVM
- **2 类技术指标**: GC / 内存
- **5 条默认告警规则**: 高错误率/高延迟/服务下线/磁盘满/内存爆
- **告警冷却 + 恢复通知**
- **5 维度健康详情**: CPU/内存/磁盘/网络/进程
- **60s 落库快照**

### 操作
1. 进入 `/monitor` 或 `http://<server>:8089/actuator/prometheus`
2. 查看实时指标
3. 5 类业务指标: `minimax_*` 前缀
4. 告警规则在 `alert_rule` 表, 可自定义

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/admin/Dashboard.vue` (ECharts)
- 后端: `minimax-monitor` 8089
- 指标: `http://<server>:8089/actuator/prometheus`

### 告警规则
| 名称 | 条件 | 冷却 |
|------|------|------|
| HighErrorRate | error_rate > 5% 持续 5min | 15min |
| SlowResponse | p99 > 3s 持续 5min | 15min |
| ServiceDown | 服务下线 1min | 5min |
| DiskFull | disk > 90% | 30min |
| MemoryHigh | heap > 85% 持续 5min | 15min |

---

## 1️⃣1️⃣ 👑 超级管理 (adminLiugl 专属)

### 功能
- 用户管理 (增删改查, 禁用/启用)
- 密码重置
- 跨租户切换
- 系统配置
- 5 大能力:
  1. 管理所有用户
  2. 重置任意密码
  3. 模拟用户登录
  4. 审计日志
  5. 重启服务

### 操作
1. 用 `adminLiugl / Liugl@2026` 登录
2. 顶部头像显示 👑 SUPER 徽章
3. 侧边栏出现"超级管理"菜单
4. 点击 → 进入 `/super`
5. 可用: 用户管理 / 租户管理 / 密码重置 / 系统设置

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/super/Index.vue`
- 后端: `minimax-auth/super/*` 8081

### 安全规则
- 唯一超级管理员
- 不能被自己禁用
- 普通 admin 访问 `/auth/super/*` → 403
- 前端路由 guard: 非 super 跳首页

---

## 1️⃣2️⃣ 🏢 多租户 (V3.1)

### 功能
- 租户隔离: 数据按 `tenant_id` 分区
- 配额管理: 用户数/模型数/QPS/月度调用
- 3 个套餐: free / pro / enterprise
- 过期管理

### 操作
1. adminLiugl → `/super` → 租户管理
2. 创建租户: 填 code/name/plan/quota
3. 启停租户
4. 调整月度配额
5. 普通用户登录 → 自动归属租户

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/super/Index.vue` (租户 tab)
- 后端: `minimax-auth/tenants/*` 8081

### 租户示例
| Code | Plan | Max Users | QPS | 月度 |
|------|------|-----------|-----|------|
| default | pro | 100 | 500 | 1M |
| demo | free | 10 | 100 | 100K |

### adminLiugl 特殊
- `tenant_id=0` (跨租户)
- `crossTenant=true`
- 看到所有租户数据

---

## 1️⃣3️⃣ 🤖 OpenAI 兼容 API (V3.3)

### 功能
- 100% OpenAI 协议兼容
- 可被任何 OpenAI SDK 调用 (Python/JS/Go)
- 流式 + 非流式
- 端点: `/api/v1/openai/*`

### 用法 (Python)
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

### 端点
- `GET  /api/v1/openai/models` — 列出模型
- `POST /api/v1/openai/chat/completions` — 聊天 (支持 stream)

### 路径
- 后端: `minimax-model/OpenAIGatewayController` 8083

---

## 1️⃣4️⃣ 📱 移动端 H5 (V3.2)

### 功能
- 移动端 SPA (max-width 480px)
- 5 个 Tab: 对话/Agent/图谱/插件/我的
- 流式打字机
- 拖拽上传
- Login 自动检测 UA → 跳 `/m/chat`

### 入口
- 移动端访问 `http://<server>:5173` → 自动跳 `/m/chat`
- 桌面访问 → 桌面版

### 路径
- 前端: `/workspace/minimax-platform/frontend/src/views/mobile/*`

---

## 🔧 故障排查

### 问题 1: 服务起不来
```bash
# 1. 看日志
tail -f logs/services/auth.log

# 2. 检查端口
ss -tlnp | grep 8081

# 3. 看 Java 进程
ps -ef | grep java

# 4. 重新启动
bash scripts/start-platform.sh
```

### 问题 2: 数据库连接失败
```bash
# 1. 检查 MySQL
mysql -uroot -e "SELECT 1;"

# 2. 检查用户
mysql -uroot -e "SELECT user, host FROM mysql.user WHERE user='minimax';"

# 3. 重建
mysql -uroot -e "CREATE USER 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024';"
mysql -uroot -e "GRANT ALL ON minimax.* TO 'minimax'@'127.0.0.1';"
mysql -uroot -e "FLUSH PRIVILEGES;"

# 4. 验证
mysql -uminimax -pminimax -h 127.0.0.1 minimax -e "SHOW TABLES;"
```

### 问题 3: 前端空白
```bash
# 1. 检查 vite
ps -ef | grep vite

# 2. 重新构建
cd frontend
npm run build

# 3. 看 console 报错 (F12)
# 4. 重新启动
cd ..
nohup npm run dev -- --port 5173 > logs/frontend.log 2>&1 &
```

### 问题 4: 流式输出断流
- 检查 SSE 路径 `/api/v1/chat/sessions/{id}/messages/stream`
- 浏览器 F12 → Network → EventStream

### 问题 5: JWT 过期
- access token 30 分钟
- 自动用 refresh token 续期
- 401 → 自动跳登录

### 问题 6: adminLiugl 不能登录
- 确认用 `Liugl@2026` (有 @)
- 数据库初始化时由 AdminDataInitializer 创建
- 手动重置:
  ```sql
  UPDATE sys_user SET password = '$2a$10$<BCrypt>' WHERE username = 'adminLiugl';
  -- 重启 auth 让它重新 BCrypt
  ```

### 问题 7: Agent 报错
- 确认 model 服务在 8083 跑
- 确认 OpenAI 兼容网关 (V3.3) 已部署
- 临时改 model 模块: `minimax.model.mock-mode=true` (无 key 也能跑)

### 问题 8: RAG 检索不到
- 检查文档是否上传成功
- 切向量: 切关键词: 切全量 (3 级降级)
- 文档数 < 1 → 关键词
- 相似度阈值默认 0.5

---

## 📞 完整路径速查

| 功能 | 前端 | 后端 |
|------|------|------|
| 对话 | `/chat` | chat 8082 |
| 知识库 | `/knowledge` | rag 8085 |
| 记忆 | `/memory` | memory 8084 |
| Agent | `/agent` | agent 8090 |
| 知识图谱 | `/kg` | agent 8090 |
| 协作 | `/collab` | agent 8090 |
| 插件 | `/plugins` | agent 8090 |
| 管理 | `/admin` | admin 8087 |
| 监控 | `/admin/dashboard` | monitor 8089 |
| 多模态 | `/chat` (拖拽) | multimodal 8088 |
| 超级管理 | `/super` | auth 8081 |
| 移动端 | `/m/*` | (复用桌面 API) |
| OpenAI | `http://<srv>:8083/api/v1/openai` | model 8083 |
