# MiniMax Platform V3.5.8+ 部署脚本

## 快速开始

### Docker Compose (推荐)
```bash
./scripts/deploy.sh up        # 启动所有 17 微服务 + 3 基础设施 + Nginx
./scripts/deploy.sh status    # 状态
./scripts/deploy.sh logs ai   # 查看 AI 日志
./scripts/deploy.sh down      # 停止
```

### 宿主机模式 (单服务)
```bash
./scripts/services/start-gateway.sh    # 启动 gateway
./scripts/services/start-ai.sh        # 启动 AI
./scripts/services/stop-gateway.sh    # 停止 gateway
```

### 验证
```bash
./scripts/e2e-verify.sh    # 端到端验证所有 API
```

## 文件说明
- `deploy.sh` - Docker 一键部署
- `e2e-verify.sh` - 端到端验证
- `services/start-*.sh` - 16 个服务启动脚本
- `services/stop-*.sh` - 16 个服务停止脚本
- `audit-api.py` - API 审计 (前端 vs 后端)
- `verify-seed-data.py` - 种子数据验证
- `diff_sql_entity.py` - SQL vs Entity 字段对比
- `gen_complete_sql.py` - 从 entity 自动生成 SQL schema
- `fix-crlf.sh` - 修复 Windows CRLF 行尾

## 持久化目录
```
.docker-data/
├── maven-repo/     Maven 仓库 (避免重复下载)
├── mariadb/         MariaDB 数据
├── redis/           Redis 数据
└── logs/            17 服务日志
```
