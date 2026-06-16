# MiniMax Platform 部署指南

> **3 种部署方案，覆盖 Windows / Linux 单机 / Linux 集群**
> **作者: Mavis  日期: 2026-06-16**

---

## 📦 部署包总览

| 方案 | 脚本 | 适用 | 一键启动 |
|------|------|------|----------|
| **Windows 单机** | `deploy/windows/deploy-windows.bat` | Win 10/11 + Docker Desktop | ✅ |
| **Linux 单机** | `deploy/linux-single/deploy-linux-single.sh` | Ubuntu/CentOS/Debian | ✅ |
| **Linux 集群** | `deploy/linux-cluster/deploy-linux-cluster.sh` | K8s / Swarm / compose | ✅ |
| **DB 初始化** | `deploy/common/init-db.sh` | 任意 MySQL 实例 | ✅ |

```
deploy/
├── windows/
│   └── deploy-windows.bat          # Windows 单机 (3 模式: docker/jar/infra)
├── linux-single/
│   └── deploy-linux-single.sh      # Linux 单机 (3 模式: docker/jar/infra)
├── linux-cluster/
│   ├── deploy-linux-cluster.sh     # 集群入口 (3 模式: k8s/swarm/compose)
│   ├── k8s/                        # K8s manifests
│   │   ├── 00-namespace.yaml
│   │   ├── 10-mysql-statefulset.yaml
│   │   ├── 20-redis.yaml
│   │   ├── 30-auth.yaml
│   │   ├── 31-chat.yaml / 32-memory.yaml / 33-model.yaml / 34-rag.yaml
│   │   ├── 35-gateway.yaml
│   │   ├── 40-ingress.yaml
│   │   └── 50-hpa.yaml             # 自动扩缩
│   └── compose-cluster/            # Docker Swarm / compose 集群
│       ├── docker-stack.yml
│       └── docker-compose.cluster.yml
├── docker/
│   ├── Dockerfile.auth
│   ├── Dockerfile.chat
│   ├── Dockerfile.memory
│   ├── Dockerfile.model
│   ├── Dockerfile.rag
│   └── Dockerfile.gateway
└── common/
    └── init-db.sh                  # 独立 DB 初始化
```

---

## 🚀 快速开始 (TL;DR)

### Windows
```cmd
:: 双击或管理员命令行执行
deploy\windows\deploy-windows.bat

:: 选择 [1] Docker Compose 全栈
:: 等待 5-10 分钟, 浏览器访问 http://localhost
```

### Linux 单机
```bash
cd deploy/linux-single
chmod +x deploy-linux-single.sh
./deploy-linux-single.sh docker     # Docker 模式 (推荐)
# 或
./deploy-linux-single.sh jar        # 本地 jar 模式
# 或
./deploy-linux-single.sh infra      # 仅基础设施
```

### Linux 集群 (K8s)
```bash
cd deploy/linux-cluster
chmod +x deploy-linux-cluster.sh
./deploy-linux-cluster.sh k8s minimax      # K8s 模式 + 命名空间
```

### Linux 集群 (Swarm)
```bash
./deploy-linux-cluster.sh swarm            # Docker Swarm
```

### Linux 集群 (compose 多副本)
```bash
./deploy-linux-cluster.sh compose          # 单机多副本
```

### 仅初始化数据库 (已有 MySQL)
```bash
MYSQL_HOST=192.168.1.10 MYSQL_PASSWORD=xxx ./deploy/common/init-db.sh
```

---

## 🎯 各方案详解

### A. Windows 单机

**前置**: Docker Desktop (含 WSL2)

**3 种模式**:
- `[1] Docker Compose` — 一键启动 MySQL + Redis + ES + MinIO + 6 个应用
- `[2] 本地 jar` — 用 Maven 构建后 java -jar (适合开发调试)
- `[3] 仅基础设施` — 只起 MySQL/Redis/ES，应用 jar 自己 run

**默认账号**: `admin / admin@123`

**端口分配**:
| 端口 | 服务 |
|------|------|
| 80   | Frontend (Nginx) |
| 8080 | Gateway |
| 8081 | Auth |
| 8082 | Chat |
| 8083 | Model |
| 8084 | Memory |
| 8085 | RAG |
| 3306 | MySQL |
| 6379 | Redis |
| 9200 | Elasticsearch |
| 9000 | MinIO |

---

### B. Linux 单机

**前置**: Docker 20.10+, 可选: JDK 17 + Maven 3.8+

**3 种模式**:
- `docker` — Docker Compose 全栈
- `jar` — 本地 jar 模式 (H2 内存库, 无需 MySQL)
- `infra` — 仅基础设施

**生产建议**: `docker` 模式 + 自定义 `.env` 密码

---

### C. Linux 集群

**3 种模式**:

#### C1. Kubernetes (推荐生产)

**前置**: kubectl + 可用 K8s 集群 (1.24+)

**功能**:
- Namespace: `minimax`
- StatefulSet: MySQL (1 副本, 持久化)
- Deployment: Redis (1 副本)
- Deployment: 6 个应用服务 (默认 2 副本)
- Service: ClusterIP + Gateway LoadBalancer
- Ingress: nginx (7 层路由)
- **HPA**: 自动扩缩 (CPU 70% 触发, 6-10 副本)
- Secret: 密码 + JWT key
- ConfigMap: 应用配置

**部署步骤** (脚本自动):
1. 创建 namespace
2. 应用 secret + configmap
3. 部署 MySQL StatefulSet + 等就绪
4. 初始化 DB (执行 sql/init/*.sql)
5. 部署 Redis
6. Build & Push 镜像到 registry
7. 部署 6 个应用
8. 配置 Ingress
9. 配置 HPA
10. 验证

**运维**:
```bash
# 端口转发 (开发)
kubectl port-forward -n minimax svc/gateway-svc 8080:8080

# 扩缩
kubectl scale -n minimax deploy/chat --replicas=5

# 查看日志
kubectl logs -n minimax -l app=chat -f

# 删除整套
kubectl delete namespace minimax
```

#### C2. Docker Swarm

**前置**: 已初始化 Swarm (`docker swarm init`)

**特点**:
- 复用 docker-compose 语法
- 自动 Service Discovery + 负载均衡
- 内置滚动更新
- 多机集群

**部署**:
```bash
./deploy-linux-cluster.sh swarm
# 自动 scale 到 3 副本
# 自动初始化 DB
```

#### C3. compose-cluster (单机多副本)

**前置**: Docker Compose 2.x

**特点**:
- 单机模拟多副本 (replicas: 2~3)
- 适合 K8s 之前的中等规模
- 完整 overlay 网络 + 负载均衡

---

## 🛠️ 自定义配置

### 1. 修改密码 (生产前必做)

```bash
# 方式 A: 修改 .env
cd <deploy dir>
cp .env .env.local
# 编辑 .env.local
MYSQL_PASSWORD=YourStrongPass!

# 方式 B: 启动前 export
export MYSQL_PASSWORD=YourStrongPass!
./deploy-linux-single.sh docker
```

### 2. 配置 LLM API Key (真实模型)

```bash
# .env
OPENAI_API_KEY=sk-xxxxxxxxxxxx
MINIMAX_API_KEY=xxxxxxxxxxxx

# 或在 model 模块 application.yml
minimax:
  model:
    mock-mode: false
    providers:
      openai:
        api-key: ${OPENAI_API_KEY}
```

### 3. 切换 Profile

```bash
# 默认 test profile (H2 内存库, 无需 MySQL)
# 生产用 prod profile (真实 MySQL/Redis)
SPRING_PROFILES_ACTIVE=prod java -jar minimax-auth.jar
```

### 4. 端口冲突

```bash
# 修改 .env 端口
MYSQL_PORT=3307
REDIS_PORT=6380

# 或 docker-compose.yml
ports:
  - "8081:8081"   # 改左边
```

---

## 🔍 部署后验证

### 健康检查
```bash
# Auth
curl http://localhost:8081/api/v1/auth/health
# → {"code":0,"message":"OK"}

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}'
# → {"code":0,"data":{"accessToken":"...","refreshToken":"..."}}
```

### 业务验证
```bash
# 1) 拿 token
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin@123"}' \
  | jq -r .data.accessToken)

# 2) 创建会话
curl -X POST http://localhost:8082/api/v1/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试会话","modelId":1}'

# 3) 流式对话
curl -N -X POST http://localhost:8082/api/v1/sessions/1/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role":"user","content":"你好"}'
```

### K8s 集群验证
```bash
# Pods
kubectl get pods -n minimax

# Services
kubectl get svc -n minimax

# HPA
kubectl get hpa -n minimax

# Ingress
kubectl get ingress -n minimax

# 资源使用
kubectl top pods -n minimax
```

---

## 🐛 故障排查

| 问题 | 排查 |
|------|------|
| 端口被占 | `netstat -tlnp \| grep 8081` (Linux) / `netstat -ano \| findstr 8081` (Win) |
| MySQL 启动失败 | `docker logs minimax-mysql` 查看 5s 内错误 |
| 应用 401 | JWT secret 不一致 → 检查 `minimax.jwt.secret` |
| 应用 404 | 路径前缀 `/api/v1/...` 是否加 |
| 内存 OOM | 提高 `-Xmx` (e.g. `-Xmx2g`) |
| 拉镜像慢 | 配 `~/.m2/settings.xml` + Docker registry mirror |
| 集群 NodeNotReady | `kubectl describe node` 看资源/CNI |
| Ingress 502 | backend Service 未就绪 / 健康检查失败 |

---

## 📊 资源需求 (推荐配置)

| 规模 | CPU | RAM | Disk | 备注 |
|------|-----|-----|------|------|
| **开发** | 4 核 | 8 GB | 50 GB | 单机 docker compose |
| **小规模** (≤100 并发) | 8 核 | 16 GB | 200 GB | 单机多副本 / 小 K8s |
| **中规模** (≤1k 并发) | 32 核 | 64 GB | 500 GB | K8s 3 节点 + HPA |
| **大规模** (≥1w 并发) | 128+ 核 | 256+ GB | 1 TB+ | K8s 多节点 + 分库分表 + ES 集群 |

---

## 🔐 安全清单 (生产)

- [ ] 修改所有默认密码 (MySQL/Redis/MinIO/admin)
- [ ] 启用 HTTPS (Ingress + cert-manager)
- [ ] JWT secret 用 64+ 字节随机串
- [ ] 启用 NetworkPolicy (K8s 限制 pod 通信)
- [ ] 启用 RBAC (K8s)
- [ ] 镜像签名验证 (cosign / Notary)
- [ ] 日志接入 ELK / Loki
- [ ] 监控接入 Prometheus + AlertManager
- [ ] 备份策略 (MySQL 每日 + binlog)
- [ ] 限流 (Gateway Bucket4j)

---

## 📞 运维速查

### 查看日志
```bash
# Docker
docker compose logs -f [service]
docker logs -f minimax-auth

# K8s
kubectl logs -n minimax -l app=chat -f --tail=100

# 本地 jar
tail -f runtime/logs/auth.log
```

### 备份与恢复
```bash
# 备份 MySQL
docker exec minimax-mysql mysqldump -uroot -pMinMax2026! minimax | gzip > backup_$(date +%F).sql.gz

# 恢复
gunzip < backup_2026-06-16.sql.gz | docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax
```

### 升级
```bash
# 1) 拉新代码 + 重新构建
git pull
mvn -B -DskipTests clean package

# 2) 重新构建镜像
docker build -f deploy/docker/Dockerfile.auth -t minimax-auth:v1.1.0 .

# 3) 滚动更新
docker compose up -d --no-deps auth
# 或 K8s: kubectl set image deploy/auth auth=minimax-auth:v1.1.0 -n minimax
```

---

## 📜 License

MIT
