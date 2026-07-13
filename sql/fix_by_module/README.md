# 修复 SQL 文件夹 (按模块拆分)

## 文件清单

| 文件 | 模块 | 表数 | 字段数 |
|------|------|------|--------|
| fix_all.sql | 全部 | 75 | 713 |
| fix_admin.sql | admin | 2 | 22 |
| fix_agent.sql | agent | 6 | 45 |
| fix_ai.sql | ai | 27 | 296 |
| fix_analytics.sql | analytics | 4 | 36 |
| fix_auth.sql | auth | 13 | 113 |
| fix_chat.sql | chat | 2 | 13 |
| fix_function.sql | function | 2 | 18 |
| fix_model.sql | model | 4 | 49 |
| fix_monitor.sql | monitor | 4 | 28 |
| fix_pipeline.sql | pipeline | 4 | 33 |
| fix_prompt.sql | prompt | 1 | 9 |
| fix_rag.sql | rag | 3 | 18 |
| fix_ws.sql | ws | 3 | 33 |

## 用法

### 1. 全部模块 (推荐首次部署)

```bash
docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_all.sql
```

### 2. 单个模块 (按需)

```bash
docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_ai.sql
```

### 3. 精简版 (只 apply 精简部署用到的模块)

```bash
docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_auth.sql
docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_ai.sql
```

### 4. 完整 21 容器版本 (全部模块)

```bash
for f in fix_*.sql; do
  docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < $f
done
```

## 模块对应表

| 模块 | Java 包 | 端口 | 说明 |
|------|---------|------|------|
| auth | minimax-auth | 8081 | 鉴权 |
| ai | minimax-ai | 8094 | AI 核心 |
| chat | minimax-chat | 8082 | 聊天 |
| memory | minimax-memory | 8083 | 记忆 |
| model | minimax-model | 8084 | 模型管理 |
| rag | minimax-rag | 8085 | 知识库 |
| function | minimax-function | 8086 | 函数工具 |
| multimodal | minimax-multimodal | 8087 | 多模态 |
| agent | minimax-agent | 8088 | 智能体 |
| monitor | minimax-monitor | 8089 | 监控 |
| admin | minimax-admin | 8090 | 管理后台 |
| prompt | minimax-prompt | 8091 | 提示词 |
| analytics | minimax-analytics | 8092 | 数据分析 |
| pipeline | minimax-pipeline | 8093 | 工作流 |
| gateway | minimax-gateway | 7080 | API 网关 |
| common | minimax-common | - | 公共 |

## 重新生成

```bash
python3 scripts/split_fix_sql_by_module.py
```
