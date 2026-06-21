# MiniMax V5.26 - CentOS 一键部署指南

> 专门为 **CentOS / RHEL / Rocky / AlmaLinux / Anolis OS** 优化

## 🎯 两个脚本

| 脚本 | 用途 |
|------|------|
| **`scripts/install-middleware-centos.sh`** | 单独装中间件 (Docker + MariaDB/Redis/Nacos/Adminer) |
| **`scripts/deploy-centos.sh`** | 一键完整部署 (中间件 + JDK + mvn + 12 微服务 + nginx) |

如果**只想装中间件** (比如已有 jar 在跑), 用第一个. **全新部署**, 用第二个.

## 📋 系统要求

- **OS**: CentOS 7+ / Rocky Linux 8+ / RHEL 8+ / AlmaLinux 8+ / Anolis OS 8+
- **架构**: x86_64 / aarch64
- **内存**: ≥ 4GB (推荐 8GB)
- **磁盘**: ≥ 20GB
- **权限**: root (sudo)
- **网络**: 可访问 Docker Hub (国内可加镜像加速)

## 🚀 一行命令 (推荐)

```bash
curl -fsSL https://raw.githubusercontent.com/liugl951127/miniLiugl/main/scripts/deploy-centos.sh -o deploy-centos.sh
chmod +x deploy-centos.sh
sudo ./deploy-centos.sh install
```

完成后访问:
- **前端**: http://your-server-ip:3000
- **API 文档**: http://your-server-ip:3000/api-docs
- **Nacos**: http://your-server-ip:8848/nacos (nacos/nacos)
- **Adminer**: http://your-server-ip:8082 (minimax/minimax_pass_2024)

账号: `adminLiugl` / `Liugl@2026`

## 🔄 install 8 步详解

| 步骤 | 内容 | 耗时 |
|------|------|------|
| 1. JDK 17 | yum install java-17-openjdk-devel | 1-2 分钟 |
| 2. 中间件 | 调 install-middleware-centos.sh (Docker + MariaDB/Redis/Nacos/Adminer) | 3-5 分钟 |
| 3. 服务用户 | 创建 minimax 用户 (UID 999) | < 1s |
| 4. 编译 | mvn clean install -DskipTests | 5-10 分钟 |
| 5. 拷贝 jar | 12 微服务 + gateway → /opt/minimax/apps/ | < 5s |
| 6. systemd | 生成 13 个 service 文件 | < 1s |
| 7. nginx | yum install + 反代配置 | < 30s |
| 8. 启动 | mariadb → nacos → gateway → 12 微服务 → nginx | 30-60s |

**总耗时**: 10-20 分钟 (含编译)

## 🌐 网络拓扑

```
                            ┌─────────────────────────────────┐
                            │     nginx :3000 (统一入口)        │
                            │   /api → gateway :8080            │
                            │   /actuator → gateway :8080       │
                            │   /ws/bidi → ws :8095             │
                            │   / → 静态文件 / API 文档聚合      │
                            └────────────┬────────────────────┘
                                         │
                            ┌────────────▼────────────┐
                            │   Gateway :8080         │
                            └────────────┬────────────┘
                                         │ lb://minimax-*
                  ┌──────────────────────┼──────────────────────┐
                  ▼                      ▼                      ▼
          12 个微服务 (8081-8095)    Nacos :8848 (Docker)    监控 (可选)
            - auth:8081              MariaDB :3306 (Docker)
            - chat:8082              Redis :6379 (Docker)
            - ...                    Adminer :8082 (Docker)
```

## 🔒 安全特性 (CentOS 专属)

### 1. SELinux 处理

脚本自动检测 SELinux 状态:
```bash
# Enforcing → permissive (避免容器挂载问题)
setenforce 0
sed -i 's/^SELINUX=enforcing/SELINUX=permissive/' /etc/selinux/config
```

> 永久改: 改 `/etc/selinux/config` (脚本已处理)

### 2. firewalld 端口开放

```bash
firewall-cmd --permanent --add-port=3306/tcp  # MariaDB
firewall-cmd --permanent --add-port=6379/tcp  # Redis
firewall-cmd --permanent --add-port=8848/tcp  # Nacos
firewall-cmd --permanent --add-port=8082/tcp  # Adminer
firewall-cmd --permanent --add-port=3000/tcp  # nginx
firewall-cmd --reload
```

### 3. 镜像加速 (国内)

```json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
```

写入 `/etc/docker/daemon.json`, 拉镜像加速 5-10 倍.

## 🎯 7 个子命令

```bash
sudo ./deploy-centos.sh install      # 一键安装 (中间件+微服务+nginx)
sudo ./deploy-centos.sh start        # 启动所有服务
sudo ./deploy-centos.sh stop         # 停止
sudo ./deploy-centos.sh restart      # 重启
sudo ./deploy-centos.sh status       # 状态
sudo ./deploy-centos.sh test         # E2E 健康检查
sudo ./deploy-centos.sh uninstall    # 卸载 (保留数据)
```

## 🛠️ 中间件独立脚本

如果只想装中间件 (例如已有 jar 在跑):

```bash
sudo ./install-middleware-centos.sh install    # 装
sudo ./install-middleware-centos.sh status     # 状态
sudo ./install-middleware-centos.sh stop       # 停
sudo ./install-middleware-centos.sh start      # 启动
sudo ./install-middleware-centos.sh uninstall  # 卸载 (保留数据)
```

## 🛠️ 故障排查

### 1. yum 装 docker 慢

```bash
# 配置 yum 镜像源 (阿里云)
sudo cp /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.bak
sudo curl -o /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo
sudo yum clean all && sudo yum makecache
```

### 2. SELinux 阻止容器挂载

```bash
# 永久改 permissive
sudo sed -i 's/^SELINUX=enforcing/SELINUX=permissive/' /etc/selinux/config
sudo setenforce 0

# 或挂载卷加 :z 后缀
volumes:
  - /data/mysql:/var/lib/mysql:z
```

### 3. firewalld 拒绝访问

```bash
# 检查端口
sudo firewall-cmd --list-all

# 临时开放
sudo firewall-cmd --add-port=3000/tcp

# 永久
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --reload
```

### 4. MariaDB 启动失败

```bash
# 查看容器日志
docker logs minimax-mariadb

# 进入容器调试
docker exec -it minimax-mariadb bash
mysql -uroot -p
```

### 5. Nacos 启动慢 (等 60s+)

Nacos 第一次启动要做初始化, 健康检查 30-60s. 看日志:
```bash
docker logs -f minimax-nacos
```

### 6. Java 找不到

```bash
# 脚本会设 JAVA_HOME 到 /etc/profile.d/java.sh
# 重新登录生效
exec bash

# 或手动设
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### 7. nginx 启动失败

```bash
# 配置测试
sudo nginx -t

# 看错误
sudo journalctl -u nginx -n 30
```

## 📊 资源占用

| 组件 | 内存 | CPU | 磁盘 |
|------|------|-----|------|
| MariaDB | 256MB | 0.5 | 1GB |
| Redis | 64MB | 0.2 | 100MB |
| Nacos | 512MB | 0.5 | 500MB |
| Adminer | 16MB | 0.1 | 10MB |
| Gateway | 256MB | 0.3 | 200MB |
| 12 微服务 | 256MB × 12 = 3GB | 0.3 × 12 | 200MB × 12 |
| nginx | 16MB | 0.1 | 50MB |
| **总计** | **~4.2GB** | **~5** | **~5GB** |

推荐: **8GB RAM / 4 CPU / 20GB 磁盘** 生产环境

## 🔐 默认凭证 (生产请改)

| 服务 | 账号 | 密码 |
|------|------|------|
| Adminer | minimax | minimax_pass_2024 |
| Nacos | nacos | nacos |
| Web 后台 | adminLiugl | Liugl@2026 |
| Redis | - | minimax_redis_2024 |
| MariaDB | minimax | minimax_pass_2024 |
| MariaDB (root) | root | minimax_root_2024 |

通过环境变量改:
```bash
DB_PASS=xxx REDIS_PASS=yyy sudo ./deploy-centos.sh install
```

## 🔄 升级流程

```bash
# 1. 备份
sudo ./deploy-centos.sh stop
sudo tar -czf backup-$(date +%Y%m%d).tar.gz /opt/minimax/data /var/log/minimax

# 2. 拉新代码
cd /path/to/minimax-platform
git pull

# 3. 重装
sudo ./deploy-centos.sh install

# 4. 验证
sudo ./deploy-centos.sh test
```

## 🗑️ 卸载

```bash
# 保留数据
sudo ./deploy-centos.sh uninstall

# 完全清理
sudo rm -rf /opt/minimax /var/log/minimax
cd /path/to/source && sudo rm -rf deploy-middleware.yml
```

## 📚 相关文档

- [DEPLOY-MINIMAX-GUIDE.md](DEPLOY-MINIMAX-GUIDE.md) - 通用版 (Debian/Ubuntu 也适用)
- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md) - 开发者指南
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构文档

## ❓ FAQ

**Q: 跟 `deploy-minimax.sh` 区别?**  
A: `deploy-minimax.sh` 是**通用版** (Debian/Ubuntu/CentOS 自动检测); `deploy-centos.sh` 是**CentOS 专用版**, 多了 SELinux + firewalld + 国内镜像源 + CentOS 工具链适配.

**Q: 必须在 CentOS 上用吗?**  
A: `deploy-centos.sh` 强制要求 CentOS/RHEL 系. 其他系统用 `deploy-minimax.sh`.

**Q: 编译要多久?**  
A: 首次 5-10 分钟 (mvn 全量编译). 后续增量 1-2 分钟.

**Q: 离线安装?**  
A: 把 docker 镜像 (mariadb/redis/nacos/adminer) 提前 pull 下来 + 用本地 maven 仓库.