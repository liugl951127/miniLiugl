# tags 字段使用情况分析 (V6.8.1+)

## 结论: tags 是真业务字段, 12 个表已加, sys_user 不需要

## 12 个表 (Entity 声明 + 后端代码真用)

| 表 | 后端 setTags/getTags | 前端调 |
|---|---|---|
| agent_group | AgentGroupService, MarketplaceController | ai/Marketplace.vue |
| ai_tool | - | - |
| dashboard_metric | - | - |
| data_source | - | - |
| kb_document | - | - |
| training_checkpoint | TrainingVizService | ai/TensorBoard.vue |
| training_job | TrainingVizService | ai/TensorBoard.vue |
| alert_rule | - | - |
| metric_snapshot | SnapshotService | - |
| function_tool | FunctionToolService | - |
| document | DocumentService | RAG API |
| knowledge_base | KnowledgeBaseService (×2) | RAG API |

**12 个表 schema 全部已加 tags 列** ✅

## sys_user tags: 不需要 ❌

V6.7+ 旧 SQL 给了 `sys_user.tags` 字段 (V3.5.46 旧字段, 业务用 `user.tags` 做用户画像标签, 跟 sys_user DB 字段无关)。

**V6.8.1 不需要 sys_user.tags** 因为:
1. `SysUser` Entity (现在) 没 tags 字段
2. `AiAutoFillController.user.tags` 是 AI 模块自己的**业务概念** (用户画像标签), 不是 DB 字段
3. 后端 0 处 `userMapper.setTags()` 或 `userMapper.updateById(u.setTags(...))`
4. sys_user 业务用 nickname/email/phone 足够, 标签是 AI 模块动态生成

## 14 处后端代码用 tags (真业务)

```
backend/minimax-ai/src/main/java/com/minimax/ai/eval/EvalRunner.java:55
backend/minimax-ai/src/main/java/com/minimax/ai/eval/EvalService.java:428
backend/minimax-ai/src/main/java/com/minimax/ai/knowledgebase/KnowledgeBaseService.java:134
backend/minimax-ai/src/main/java/com/minimax/ai/marketplace/AgentGroupService.java:76
backend/minimax-ai/src/main/java/com/minimax/ai/marketplace/MarketplaceController.java:90
backend/minimax-ai/src/main/java/com/minimax/ai/modelmarket/ModelMarketController.java:120
backend/minimax-ai/src/main/java/com/minimax/ai/training/TrainingVizService.java:69
backend/minimax-ai/src/main/java/com/minimax/ai/training/TrainingVizService.java:192
backend/minimax-chat/src/main/java/com/minimax/chat/memory_ext/longterm/LongTermMemoryService.java:54
backend/minimax-monitor/src/main/java/com/minimax/monitor/service/SnapshotService.java:75
backend/minimax-pipeline/src/main/java/com/minimax/pipeline/function_ext/service/FunctionToolService.java:53
backend/minimax-rag/src/main/java/com/minimax/rag/service/DocumentService.java:73
backend/minimax-rag/src/main/java/com/minimax/rag/service/KnowledgeBaseService.java:35
backend/minimax-rag/src/main/java/com/minimax/rag/service/KnowledgeBaseService.java:89
```

## 修复方案

### 已修: 12 表 schema 已加 tags
我的 V6.8.1 schema 自动扫描 Entity, 12 个表都已加 `tags VARCHAR(255)` 列。

### 不修: sys_user tags 删除
- V6.8.1 schema sys_user 没 tags ✅ (Entity 没声明)
- 旧 V6.7+ SQL 给了 sys_user tags - 是历史遗留, 删了是对的

### 给你的选择

1. **保留旧 sys_user tags** (兼容 V6.7+ 旧 seed)
   - 加 `tags VARCHAR(255) DEFAULT ''` 到 sys_user
   - V6.7+ 旧 seed 能跑
2. **删旧 sys_user tags** (推荐, 业务不需要)
   - 我的 V6.8.1 schema 已删
   - 用我 V6.8.1 完整 sql 重置 DB

## 验证 (沙箱跑过)

```bash
$ mariadb -uroot -proot123456 -e "USE minimax_platform; SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='minimax_platform' AND COLUMN_NAME='tags' ORDER BY TABLE_NAME;"

agent_group         | tags
ai_tool             | tags
alert_rule          | tags
dashboard_metric    | tags
data_source         | tags
document            | tags
function_tool       | tags
kb_document         | tags
knowledge_base      | tags
metric_snapshot     | tags
training_checkpoint | tags
training_job        | tags
```

✓ 12 个表都有 tags
✗ sys_user 没 tags (符合 V6.8.1 实际)
