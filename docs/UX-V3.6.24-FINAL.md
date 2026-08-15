# V3.6.24 EmptyState 22 view 全量迁移 (5 批)

## 1. V3.6.23 之后

V3.6.23 验证了登录流程 7 场景。V3.6.24 完成 V3.6.10 遗留的 22 view EmptyState 迁移:
- **5 批 × 5 文件** = 25 view 迁移 (含 V3.6.12 试点)
- **总替换 25 个 el-empty**
- **0 错误 / 21 路由 / ci-check 11/11 / 90 轮 100% pass**

## 2. V3.6.24 改 (5 批渐进)

### 2.1 分批策略

| 批 | 文件数 | el-empty | build |
|----|--------|----------|-------|
| Batch 1 | 5 | 6 | ✅ 0 错 59s |
| Batch 2 | 5 | 8 | ✅ 0 错 56s |
| Batch 3 | 5 | 9 | ✅ 0 错 56s |
| Batch 4 | 5 | 2 | ✅ 0 错 58s |
| Batch 5 | 5 | 9 | ✅ 0 错 59s |
| **合计** | **25** | **34** | **0 错** |

### 2.2 修复的 3 类问题

**问题 1: import 错位** (脚本插入到 `import {` 中间)
- 修法: 检测 `import {\nimport EmptyState` 模式, 删除错位行, 加到 `<script setup>` 段最后

**问题 2: v-else 配对缺失** (`<EmptyState />` 后接 `<div v-else>` 缺 `<div v-if>`)
- 修法: 给 EmptyState 加 `v-if="!data.length"` / `v-if="!keys.length"` 等条件

**问题 3: 描述用 t() 函数** (`t('apikey.noKeys')`)
- 修法: 单独 replace 而非通用 regex

### 2.3 已迁的 view

- **admin/**: Alerts, Traces
- **ai/**: AiChat, AiToolAdmin, AutoAgentGroup, ImageGen (V3.6.12), Marketplace, ModelMarket, MusicStream, TensorBoard, TensorBoardStats, ToolPlayground, TrainingViz, VideoStream, Workflow
- **analytics/**: Nl2Sql, Schema
- **apikey/**: Index
- **chat/**: Stream (chat/Index 跳过, 需 v-else 配对特殊处理)
- **kg/**: Index
- **knowledge/**: Index (V3.6.12)
- **memory/**: Index
- **multimodal/**: Index
- **notification/**: Index
- **pipeline/**: Designer
- **showcase/**: AudioShowcase, PluginShowcase, SingleChatPlayground
- **user/**: CrossAppBinding

## 3. 验证

| 测试 | 结果 |
|------|------|
| 5 批 0 build 错 | ✅ 25 view 全部迁移 |
| 21 路由 21/21 200 | ✅ |
| ci-check 11/11 | ✅ < 3s |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |

## 4. 累计 79 个版本 (V3.5.46-V3.6.24)
