# MiniMax Platform - 部署脚本 (V2.1)

> 一键部署 / 运维 / 备份 / 状态检查

## 📋 脚本清单

| 脚本 | 作用 | 使用频率 |
|------|------|----------|
| `docker-deploy.sh` ⭐ | **主入口** - 一键启动 | 每次 |
| `status.sh` | 状态检查 (开箱即用) | 经常 |
| `backup.sh` | 自动备份 MySQL + 数据 | 每天 |
| `fix-port-80.sh` | 修 80 端口冲突 | 偶尔 |
| `setup-frontend-via-host-nginx.sh` | 配置域名 + HTTPS | 一次 |
| `verify-public-domain.sh` | 验证完整链路 | 偶尔 |
| `os-detect.sh` | OS 适配层 | (内部) |
| `setup-data-dir.sh` | 创建数据目录 | (内部) |

## 🚀 快速开始

### 一键启动

```bash
sudo ./docker-deploy.sh up
```

### 配置域名 + HTTPS

```bash
sudo ./docker-deploy.sh frontend liugeliang.com admin@liugeliang.com
```

### 状态检查

```bash
sudo ./docker-deploy.sh status
```

**输出**:
```
═══════════════════════════════════════════════════════
   MiniMax Platform - 状态检查
═══════════════════════════════════════════════════════

[1/7] 系统信息
  OS:      CentOS Stream 9
  Memory:  7.6G (used: 4.2G, 55%)
  Disk:    28G free of 50G

[2/7] Docker
  ✓ Docker 24.0.7
  ✓ Compose 2.21.0

[3/7] 容器状态
  总计: 16 / 运行: 16
  minimax-mysql         Up (healthy)        0.0.0.0:3306
  minimax-gateway       Up (healthy)        0.0.0.0:7080
  ...

[4/7] 内存占用 Top 5
  minimax-gateway       312MB   4.0%
  ...

[5/7] 关键端口
  ● :80 (nginx)
  ● :443 (nginx)
  ● :7080 (docker-proxy)
  ● :8081 (docker-proxy)
  ...

[6/7] 健康检查
  ✓ http://localhost/actuator/health/liveness (200)
  ✓ http://localhost:7080/actuator/health (200)
  ...

[7/7] 数据卷
  1.2G /opt/minimax/data/mysql
  120M /opt/minimax/data/redis
  45M /opt/minimax/data/nacos
  ...
```

### 自动备份

```bash
# 备份 (默认保留 7 天)
sudo ./docker-deploy.sh backup

# 备份 + 保留 30 天
sudo ./docker-deploy.sh backup --keep=30
```

**输出**:
```
==== 1. MySQL 备份 ====
✓ MySQL 备份完成: 23M
==== 2. 数据目录备份 ====
✓ 数据目录备份完成: 1.2G
==== 3. 应用配置备份 ====
✓ 配置备份完成: 12M
```

### 添加定时备份 (cron)

```bash
# 每天凌晨 3 点自动备份
echo "0 3 * * * root /opt/miniLiugl/deploy-simple/docker-deploy.sh backup --keep=7" | \
  sudo tee /etc/cron.d/minimax-backup
sudo chmod 644 /etc/cron.d/minimax-backup

# 验证
ls -la /etc/cron.d/minimax-backup
```

## 🛠️ 故障排查

### 80 端口被占用

```bash
sudo ./docker-deploy.sh fix-80
```

### 服务启动失败

```bash
# 看具体错误
docker compose logs gateway --tail=50

# 重启
docker compose restart gateway

# 完全重建
./docker-deploy.sh rebuild gateway
```

### 完整链路不通

```bash
sudo ./docker-deploy.sh verify your-domain.com
```

## 📖 进阶

### 强制用宿主机 nginx (避免 docker nginx)

```bash
./docker-deploy.sh up --host-nginx    # 默认
./docker-deploy.sh up --docker-nginx  # 老路径
./docker-deploy.sh up --no-nginx      # 不配 nginx
./docker-deploy.sh up --ip            # IP 模式
```

### 跳过各种校验

```bash
SKIP_NGINX_CHECK=1 ./docker-deploy.sh up
SKIP_PORT_CHECK=1 ./docker-deploy.sh up
SKIP_DOMAIN_CHECK=1 ./docker-deploy.sh up
```

### 高级内存调优

详见 [OPERATIONS.md](../OPERATIONS.md)

## 📊 性能基准 (V2.1)

| 维度 | 数值 |
|------|------|
| 启动时间 | ~5 分钟 (并行编译) |
| 16 微服务内存 | ~4.5GB |
| 单服务平均 | ~280MB |
| 镜像构建 | ~3 分钟 (缓存后) |
| API 响应 | < 100ms (P95) |

## 🔗 相关文档

- [OPERATIONS.md](../OPERATIONS.md) - 运维手册
- [README.md](../README.md) - 项目说明
- [PRODUCTION-DEPLOY.md](../PRODUCTION-DEPLOY.md) - 生产部署