# Deploy

部署相关配置文件目录。

## 子目录

- `prometheus/` - Prometheus 配置（Day 12 启用）
- `nginx/`      - Nginx 反向代理（Day 14 启用）
- `k8s/`        - Kubernetes 部署清单（Day 14 启用）

## 当前可用服务

参考根目录 `docker-compose.yml`：

```bash
# 仅启动基础设施
docker compose up -d mysql redis minio

# 启动应用（auth + gateway + web）
docker compose --profile app up -d
```

## 端口约定

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Elasticsearch | 9200 | 全文检索（Day 8 启用） |
| MinIO | 9000 / 9001 | 对象存储 / 控制台 |
| Gateway | 8080 | 后端统一入口 |
| Auth | 8081 | 鉴权服务 |
| Chat | 8082 | 会话服务（Day 3） |
| Model | 8083 | 模型路由（Day 4） |
| Memory | 8084 | 记忆服务（Day 6） |
| RAG | 8085 | 知识库（Day 8） |
| Web | 5173 / 80 | 前端 |
| Prometheus | 9090 | 监控（Day 12） |
| Grafana | 3000 | 监控看板（Day 12） |
