# MiniMax Platform V3.5.8 — Docker 一键部署

## 🚀 快速开始

```bash
# 1. 一键启动 (自动检测内存, 选 full 或 mini 模式)
./start-all.sh up

# 2. 浏览器访问
open http://localhost/

# 3. 默认账号
#    超级管理员: adminLiugl / Liugl@2026
#    普通管理员: admin_user / admin123
#    测试用户:   test_user / user123
```

## 📊 三种部署模式

| 模式 | 服务数 | 内存需求 | 启动时间 | 适用 |
|------|--------|----------|----------|------|
| **mini** | 6 (gateway/auth/ai/chat/admin/monitor) + 基础 | 4 GB | 2-3 min | 演示 / 学习 / 笔记本 |
| **full** | 17 微服务 + 基础 | 8 GB+ (推荐 16) | 5-8 min | 生产 / 全功能测试 |
| **h2** | 1 (AI 单服务, H2 内存数据库) | 512 MB | 30 sec | 沙箱 / E2E 调试 |

## 💾 持久化设计 (避免重复下载)

```
.docker-volumes/
├── maven-repo/          Maven 本地仓库 (~1.5GB 一次下载, 永久复用)
├── npm-cache/           npm 缓存
├── logs/                所有容器日志
├── backups/             数据库备份
└── frontend-dist/       前端构建产物
```

**关键**: Maven 仓库挂载到 **宿主机磁盘**, 镜像构建时复用, 二次构建从 10 分钟降到 30 秒.

## 🧠 内存优化

每个微服务都使用 **JVM 容器感知**:

```yaml
environment:
  JAVA_OPTS: "-XX:MaxRAMPercentage=70 -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
```

- `MaxRAMPercentage=70`: JVM 最多用容器限制内存的 70%
- `UseG1GC`: 低延迟垃圾回收器
- `MaxGCPauseMillis=100`: GC 暂停 ≤ 100ms

每个服务的内存限制:

```yaml
deploy:
  resources:
    limits: { memory: 384M~512M }    # 容器最大
    reservations: { memory: 256M }   # 容器保证
```

| 服务 | 限制 | 说明 |
|------|------|------|
| mariadb | 1 GB | InnoDB buffer 512M |
| nacos | 1 GB | 配置中心 |
| redis | 384 MB | 缓存 + maxmemory 256M |
| gateway | 512 MB | Spring Cloud Gateway |
| ai | 512 MB | 自研 AI 引擎 |
| agent | 512 MB | 智能体编排 |
| 其它微服务 | 384 MB | 通用业务 |

**full 模式总内存**: 约 6 GB (留 2 GB 给系统)

## 🔧 一键启动 (start-all.sh)

```bash
./start-all.sh up [full|mini]   启动 (默认智能模式)
./start-all.sh down              停止
./start-all.sh status            查看状态
./start-all.sh logs [service]    实时日志
./start-all.sh restart           重启
./start-all.sh repair            修复 (用 repair.sh 的子集)
./start-all.sh clean             清理 (危险)
./start-all.sh help              帮助
```

## 🩹 修复脚本 (repair.sh)

```bash
./repair.sh all          全部修复
./repair.sh maven        修复 Maven 缓存 (损坏元数据)
./repair.sh docker       重建 Docker 镜像
./repair.sh seed         修复种子数据 (同步 + 验证)
./repair.sh db           重置数据库
./repair.sh port         检测端口冲突
./repair.sh memory       检测内存使用
./repair.sh network      检测网络 + 配置镜像加速
```

## 🏗️ 架构 (17 微服务)

```
                ┌─────────────────┐
                │  Host nginx:80  │
                └────────┬────────┘
                         │
            ┌────────────▼────────────┐
            │  Gateway:7080 (Gateway) │
            └──┬─────┬─────┬─────┬────┘
               │     │     │     │
       ┌───────▼┐ ┌──▼──┐ ┌▼───┐ ┌▼──────┐
       │Auth:81 │ │ AI  │ │Chat│ │Memory │
       └────────┘ │:94  │ │:82 │ │  :83  │
                  └──┬──┘ └────┘ └───────┘
       ┌──────────────┼──────────────┐
       │              │              │
   ┌───▼──┐  ┌───────▼┐  ┌────────▼─────┐
   │Model │  │  RAG   │  │  Monitor     │
   │  :84 │  │  :85   │  │    :89       │
   └──────┘  └────────┘  └──────────────┘
       ┌──────────┐  ┌─────────┐  ┌──────────┐
       │Function  │  │Multimodal│  │ Prompt   │
       │  :86     │  │   :87    │  │  :91     │
       └──────────┘  └──────────┘  └──────────┘
       ┌──────────┐  ┌─────────┐  ┌──────────┐
       │  Agent   │  │ Admin   │  │Analytics │
       │  :88     │  │  :90    │  │  :92     │
       └──────────┘  └─────────┘  └──────────┘
       ┌──────────┐
       │ Pipeline │
       │  :93     │
       └──────────┘
```

## 🐳 Docker 镜像优化 (3-stage build + layered jar)

每个服务的 Dockerfile 3 阶段:

```dockerfile
# Stage 1: 依赖缓存 (99% 命中)
FROM maven:3.9-eclipse-temurin-17 AS deps
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Stage 2: 构建 + 拆层
FROM deps AS build
COPY src ./src
RUN mvn package -DskipTests
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: 运行时 (4 层 COPY)
FROM eclipse-temurin:17-jre-jammy
COPY --from=build dependencies/ .    # 第三方依赖
COPY --from=build spring-boot-loader/ .
COPY --from=build snapshot-dependencies/ .
COPY --from=build application/ .       # 业务代码
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**效果**:
- 镜像大小: 800 MB → 300 MB (**2.7x**)
- 重新构建: 10 min → 30 s (**20x**, 业务代码变更)

## 🔌 端口清单

| 服务 | 端口 | 服务 | 端口 |
|------|------|------|------|
| mariadb | 3306 | function | 8086 |
| redis | 6379 | multimodal | 8087 |
| nacos | 8848, 9848 | agent | 8088 |
| gateway | 7080 | monitor | 8089 |
| auth | 8081 | admin | 8090 |
| chat | 8082 | prompt | 8091 |
| memory | 8083 | analytics | 8092 |
| model | 8084 | pipeline | 8093 |
| rag | 8085 | ai | 8094 |
| | | ws | 8095 |

## 📦 关键文件

```
miniLiugl/
├── start-all.sh             # 一键启动 (341 行)
├── repair.sh                # 修复脚本 (323 行)
├── docker-compose.full.yml  # 全量 17 服务 (392 行)
├── docker-compose.mini.yml  # 精简 6 服务
├── docker-compose.yml       # 兼容旧版
├── .docker-volumes/         # 持久化目录
│   ├── maven-repo/          # Maven 仓库 (避免重复下载)
│   ├── npm-cache/
│   ├── logs/
│   ├── backups/
│   └── frontend-dist/
├── backend/                 # 17 Maven 模块
│   ├── pom.xml              # 父 POM
│   └── minimax-*/           # 16 业务 + common
│       ├── Dockerfile       # 3-stage build
│       └── src/main/resources/
├── sql/
│   ├── complete.sql         # 77 表结构
│   ├── seed-data.sql        # 53 表 178+ 条种子
│   └── README.md
├── scripts/
│   ├── verify-seed-data.py
│   ├── audit-api.py
│   └── check-deploy-logs.sh
├── frontend/                # Vue 3 SPA
└── docs/
    ├── SQL_DICTIONARY.md
    ├── INTENT_ALGORITHM.md
    ├── AUDIT_REPORT.md
    └── DOCKER_OPTIMIZATION.md
```

## 🚨 故障排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 启动后端口不通 | mariadb 未就绪 | `sleep 30` 后重试 |
| `repository ... not found` | Maven 缓存损坏 | `./repair.sh maven` |
| `image ... not found` | 镜像未构建 | `./repair.sh docker` |
| `Column ... not found` | 种子不匹配 | `./repair.sh seed` |
| `Connection refused: mariadb` | 数据库未启动 | `docker logs minimax-mariadb` |
| 容器频繁重启 | OOM (内存超限) | `./repair.sh memory` 改 mini 模式 |
| Docker pull 慢 | 网络问题 | `./repair.sh network` 配置镜像加速 |

## 📈 性能参考 (8GB 主机)

| 操作 | 耗时 |
|------|------|
| 首次构建 (含 Maven 预热) | 15-20 min |
| 二次构建 (复用缓存) | 30 sec |
| 启动 17 服务 | 3-5 min |
| 启动 6 服务 (mini) | 1-2 min |
| 启动 1 服务 (H2 沙箱) | 30 sec |
| 健康检查 (全部 UP) | 30 sec |

## 📝 提交日志

```
ed85a17  perf(docker): 3-stage build + Spring Boot layered jar
8d4c2bc  feat(sql): 种子数据 53 表 178+ 条
974ffd2  feat(audit): API 审计 100% 覆盖
b8f6979  feat(audit): 端到端接口审计
ad810e3  feat(frontend): 浏览器兼容 + Liugl-AI 文字
5bbd790  docs(intent): 完整算法链路 JavaDoc
2c3c5a6  feat(ai): 数据分析引擎
68e7e81  refactor(sql): 种子数据合并单文件
fb794aa  feat(sql): 种子数据 23 表 128 条
```

## 📜 许可证

Apache 2.0
