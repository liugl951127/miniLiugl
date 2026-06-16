# 🚀 V2.0 新功能交付报告

> 在 14 天基础上, 又加 4 个高级功能, **V2.0 增值包**, 让平台从"能用"走向"好用 + 智能"

## 1 个新模块 (minimax-agent) · 4 大功能

### 1. Agent 自主任务 (ReAct 模式)
- 目标驱动: 给一个 goal, LLM 自主规划 → 调工具 → 反思 → Final Answer
- Thought / Action / Observation 三段式循环, 最多 8 轮
- 可视化: 每轮 Thought/Actions/Observation 全展示, 用户可看到 AI 思考过程
- 与 Function Calling 区别: Agent 是"目标驱动", Function 是"轮次驱动"

### 2. 知识图谱 (KG)
- 实体管理: person/place/org/concept/event
- 关系管理: works_at / located_in / friend_of / owns / ...
- N 跳关联: 1 跳 / 2 跳邻居
- 最短路径: A→B 的 BFS 路径
- 重要性评分 (1-10), 搜索, 别名

### 3. 实时协作 (WebSocket)
- 多人同时编辑一个会话
- 消息广播 / typing 指示 / cursor 位置 / edit 同步
- 在线用户列表
- 房间历史回放
- 持久化: 房间创建/加入/关闭, owner/editor/viewer 角色

### 4. 插件市场
- 系统插件 (4 个内置: 天气/导出/格式化/翻译) + 用户插件
- 评分/下载量/启停
- 4 种类型: Java class / HTTP / JS / WASM
- 分类: UI/导出/增强/通用

## 数据

| 指标 | 数量 |
|------|------|
| 新增 Java 文件 | 22 |
| 新增 Java 行数 | 1,462 |
| 新增 SQL 表 | 6 (agent_task + kg_entity + kg_relation + collab_session + collab_member + plugin) |
| 新增 SQL 行数 | 110 |
| 新增 HTTP 端点 | 19 |
| 新增 WebSocket 端点 | 1 (/ws/collab/{id}) |
| 新增测试用例 | 5 (全过) |
| 新增前端页面 | 4 (Agent/KG/Collab/Plugins) |
| 新增前端组件 | 0 (复用 MarkdownView) |

## 后端模块 (现在 12 个)

```
auth(8081) chat(8082) model(8083) memory(8084) rag(8085)
function(8086) admin(8087) multimodal(8088) monitor(8089)
agent(8090) ⭐ NEW
common(shared)
```

## API (19 端点)

### Agent
- `POST /api/v1/agent/run`               自主任务 (ReAct)

### 知识图谱
- `POST   /api/v1/agent/kg/entities`             upsert 实体
- `GET    /api/v1/agent/kg/entities/{id}`        实体详情
- `GET    /api/v1/agent/kg/entities/search`      搜索
- `DELETE /api/v1/agent/kg/entities/{id}`        删除
- `POST   /api/v1/agent/kg/relations`            创建关系
- `GET    /api/v1/agent/kg/entities/{id}/neighbors`   1 跳
- `GET    /api/v1/agent/kg/entities/{id}/2hop`         2 跳
- `GET    /api/v1/agent/kg/path?from=X&to=Y`         最短路径

### 协作
- `POST /api/v1/agent/collab/sessions`       创建协作会话
- `POST /api/v1/agent/collab/{id}/join`      加入
- `POST /api/v1/agent/collab/{id}/close`     关闭
- `WS   /ws/collab/{sessionId}?userId=1`    WebSocket 实时

### 插件
- `GET    /api/v1/agent/plugins`             列表 (可按分类过滤)
- `GET    /api/v1/agent/plugins/{id}`        详情
- `POST   /api/v1/agent/plugins`             发布
- `POST   /api/v1/agent/plugins/{id}/rate`   评分
- `POST   /api/v1/agent/plugins/{id}/toggle` 启停
- `DELETE /api/v1/agent/plugins/{id}`        删除

## 商业价值

### 1. Agent 让 AI 真正"自主"
- 旧: 用户每步要告诉 AI 调什么工具
- 新: 告诉 AI 一个目标, 它自己想办法

### 2. 知识图谱让 RAG 升级
- 旧: 关键词匹配找文档
- 新: 实体-关系推理, 能回答"张三是哪家公司 CEO?"这种关系型问题

### 3. 实时协作让团队 AI 助手
- 旧: 单人对话
- 新: 团队头脑风暴, AI 参与, 多人协作

### 4. 插件市场让平台可扩展
- 旧: 功能写死
- 新: 开发者可发布插件, 形成生态

## 技术亮点

- **ReAct Prompt Engineering**: Thought/Action 协议, XML 包装 Final Answer
- **MySQL BLOB 向量**: 知识图谱无需专门的图数据库
- **BFS 最短路径**: 双向 BFS, O(V+E)
- **WebSocket 广播**: ConcurrentHashMap rooms, CopyOnWriteArrayList 线程安全
- **插件 4 类型抽象**: class/url/js/wasm 统一注册
- **目标驱动 vs 轮次驱动**: Agent 和 Function 的核心区别

## 文件清单

### 后端 (minimax-agent)
```
pom.xml
src/main/java/com/minimax/agent/
  AgentApp.java                          # Spring Boot 入口 (8090)
  config/MybatisPlusConfig.java
  config/WebSocketConfig.java
  controller/AgentController.java        # 19 endpoints
  service/AgentService.java              # ReAct 核心
  service/KnowledgeGraphService.java     # 知识图谱
  service/CollabService.java             # 房间管理
  service/CollabHandler.java             # WS handler
  service/CollabDbService.java           # 协作持久化
  service/PluginService.java             # 插件市场
  entity/{KgEntity,KgRelation,CollabSession,CollabMember,Plugin}.java
  mapper/{KgEntity,KgRelation,CollabSession,CollabMember,Plugin}Mapper.java
src/main/resources/
  application.yml
  mapper/{CollabSession,CollabMember}Mapper.xml
src/test/java/com/minimax/agent/
  AgentServiceTest.java
  KnowledgeGraphServiceTest.java
```

### 前端
```
src/views/agent/Index.vue      # Agent 任务可视化
src/views/kg/Index.vue         # 知识图谱
src/views/collab/Index.vue     # 实时协作
src/views/plugins/Index.vue    # 插件市场
```

### SQL
```
sql/15_v2_features.sql
```

---

**V2.0 = 14 天基础 + 4 大新模块 + 22 Java + 19 端点 + 4 前端页面 + 1 WebSocket** 🚀
