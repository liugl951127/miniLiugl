# MiniMax Platform 企业文档总览

> **面向企业级生产部署的全套文档** · V3.5.2 · 2026-07-13

## 文档结构

| 文档 | 面向人群 | 内容 |
|------|---------|------|
| 📖 [用户操作手册.md](./用户操作手册.md) | 终端用户 / 业务人员 | 注册/登录/AI 对话/工具使用/管理后台操作 (含截图) |
| 📖 [功能操作手册.md](./功能操作手册.md) | 业务/产品 | 87 表/智能体群/知识库/意图预测/训练可视化/数据看板 操作步骤 |
| 📖 [初级开发手册.md](./初级开发手册.md) | 新入职开发 | 架构/环境搭建/代码规范/常用命令/调试技巧/常见错误 |

## V3.4 新增核心能力

| 能力 | 说明 | 关键文件 |
|------|------|---------|
| 📚 自研知识库 (V3.4.0) | 文档上传→分块→索引→混合检索→引用追踪 | `KnowledgeBaseService` (3 实体 + 11 REST API) |
| 🎯 客户意图精准预测 (V3.4.1) | 一句话预测意图+实体+情感+紧迫度+推荐 Agent | `IntentPredictionService` (8 意图类 + 12 REST API) |
| 🤖 一句话自动生成智能体群 (V3.4.2) | 输入 "写一份季报" → 自动拼装 Agent 群 | `AutoAgentGroupGenerator` (6 模板 + 3 REST API) |
| ⚖️ Raft 分布式一致性 (V3.5.0) | Leader 选举 + 日志复制 + 多数派提交 + 状态机 | `RaftNode` + `RaftCluster` + 6 REST API |
| 📲 推送真实集成 (V3.5.1) | Web Push (VAPID) / APNs (HTTP/2+JWT) / FCM (HTTP v1+OAuth2) | `WebPushProvider` + `ApnsProvider` + `FcmProvider` + 8 REST API |
| 🎟️ License 模板 (V3.5.2) | 4 预置模板 (TRIAL/PERSONAL/COMMERCIAL/ENTERPRISE) + 克隆+签发+对比 | `LicenseTemplateService` (1 实体 + 12 REST API) |

## 工程文档 (开发参考)

- [架构设计.md](../ARCHITECTURE.md) - 18KB 完整架构 (16 微服务 + 1 自研)
- [部署运维.md](../DEPLOYMENT.md) - 单 VPS 一键部署 / Docker / nginx
- [运行监控.md](../OPERATIONS.md) - 监控/告警/性能调优
- [API 接口.md](../API.md) - REST 端点 (360+)
- [AI 工具集.md](../AI-TOOLS.md) - 21 工具详表
- [AI 算法.md](../AI-ALGORITHMS.md) - 核心算法解析
- [移动端开发.md](../MOBILE.md) - Capacitor 移动端
- [变更日志.md](../CHANGELOG.md) - V1.0 → V3.3.3 全量变更

## 测试覆盖

| 维度 | 数量 | 通过率 |
|------|------|--------|
| 后端单元测试 | **435** | 100% (含 1 个软跳过) |
| 前端单元测试 | **44** | 100% |
| 微服务接口 | **360+** | 100% |
| 数据库表 | **73** | 100% 自动生成 |

## 部署信息

- **数据库**: MySQL 8.0 (脚本 `sql/init.sql` 单文件)
- **服务架构**: 16 微服务 + 1 自研 AI + Vue 3 SPA
- **部署方式**: Docker Compose / 裸机 systemd
- **访问端口**: 80 (Nginx) / 7080 (Gateway)
- **默认账号**: `adminLiugl / Liugl@2026`

---

📞 **联系支持**: [issues@minimax.com](mailto:issues@minimax.com)
