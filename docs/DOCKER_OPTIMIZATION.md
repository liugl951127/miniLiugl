# Docker 构建优化 (V3.5.8)

> 解决 3 个核心问题:
> 1. **重复下载公共包**: 每个镜像独立下 200+ MB 依赖
> 2. **构建慢**: 每次代码变更都全量重 build
> 3. **镜像大**: 单个 Spring Boot fat jar 110MB

## 🎯 优化策略

### 1️⃣ 3-Stage Build (构建 / 缓存 / 运行 分离)

```dockerfile
# Stage 1: 依赖层 (缓存最久)
FROM maven:3.9-eclipse-temurin-17 AS deps
COPY pom.xml minimax-*/pom.xml ./
RUN mvn dependency:go-offline

# Stage 2: 构建层 (源码变化)
FROM deps AS build
COPY minimax-*/ ./
RUN mvn clean package

# Stage 3: 运行层 (最终镜像)
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /workspace/extracted/dependencies/         ./
COPY --from=build /workspace/extracted/spring-boot-loader/   ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/           ./
```

### 2️⃣ Spring Boot Layered Jar (4 层独立缓存)

**原理**: Spring Boot 3.x 支持把 fat jar 拆成 4 层独立缓存:

| 层 | 内容 | 大小 | 变化频率 |
|---|---|---|---|
| **dependencies** | 200+ 个 3rd-party jar | ~110 MB | 几乎不变 |
| **spring-boot-loader** | Spring Boot 启动器 | ~636 KB | 跟随 Spring Boot 版本 |
| **snapshot-dependencies** | SNAPSHOT 依赖 | ~108 KB | 中等 |
| **application** | 业务代码 + 配置 | ~5 MB | 每次构建都变 |

**关键**: `application` 是变化最多的层, COPY 必须放最后.

**启用方式** (pom.xml):

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

### 3️⃣ COPY 顺序优化

**变化少 → 变化多**:

```dockerfile
# 缓存命中率高
COPY --from=build extracted/dependencies/         ./  # 几乎不变
COPY --from=build extracted/spring-boot-loader/   ./  # 跟随 Spring Boot
COPY --from=build extracted/snapshot-dependencies/ ./  # 中等
COPY --from=build extracted/application/           ./  # 频繁变 (最后)
```

Docker 构建时, 每条 COPY 对应一层, **只有变化的层及之后的层需要重新构建**.

## 📊 优化效果

| 场景 | 优化前 | 优化后 | 提升 |
|---|---|---|---|
| **首次构建** | ~10 min | ~10 min | (持平, 都要下依赖) |
| **代码变更重 build** | ~10 min | **~30s** | **20x** |
| **依赖变更** | ~10 min | ~5 min | 2x |
| **镜像大小** | 800 MB | **300 MB** | 2.7x |
| **拉取镜像** | 800 MB × 3 = 2.4 GB | 300 MB × 3 = 900 MB | 2.7x |

## 🚀 验证

```bash
# 1. 验证 jar 已拆层
bash scripts/verify-layers.sh ai

# 输出:
#   ✓ dependencies  (223 files, 110M)
#   ✓ spring-boot-loader  (92 files, 636K)
#   ✓ snapshot-dependencies  (1 files, 108K)
#   ✓ application  (业务代码)
#   fat jar: 111M
#   拆层后总: 115M (节省 Docker 层重复)

# 2. 部署后日志检查
bash scripts/check-deploy-logs.sh

# 输出:
#   ✓ auth 启动成功 (in 25.4 seconds)
#   ✓ ai 启动成功 (in 32.1 seconds)
#   ✓ gateway 启动成功 (in 28.6 seconds)
#   ✓ auth 端口 8081 已监听
#   ✓ ai 端口 8094 已监听
#   ✓ gateway 端口 7080 已监听
#   ✓ GET /actuator/health -> 200
#   ✓ GET /api/ai/intro -> 200
#   ✅ 部署日志检查通过
```

## 🔧 关键文件

| 文件 | 作用 |
|---|---|
| `backend/minimax-gateway/Dockerfile` | 3-stage build, layered |
| `backend/minimax-ai/Dockerfile` | 3-stage build, layered |
| `backend/minimax-auth/Dockerfile` | 3-stage build, layered |
| `backend/pom.xml` | spring-boot-maven-plugin layered 启用 |
| `scripts/verify-layers.sh` | 验证 jar 拆层 |
| `scripts/check-deploy-logs.sh` | 部署后日志检查 |

## 💡 进一步优化

1. **JLink**: 用 jdeps + jlink 自定义 JRE (~50 MB vs 170 MB)
2. **distroless**: Google distroless 镜像 (~20 MB)
3. **多阶段并行**: docker buildx bake 并行构建多镜像
4. **CI 缓存**: GitHub Actions cache@v4 缓存 Maven 仓库

## 📌 注意事项

1. **Layered jar 需 Spring Boot 2.3+**: 我们用 3.2.x 完全支持
2. **`-am` 自动构建依赖**: minimax-common 是 packaging=pom, 必须先 install
3. **缓存清理**: `docker builder prune` 定期清理悬空缓存
4. **多架构**: `docker buildx build --platform linux/amd64,linux/arm64`

## 🔗 相关文档

- [Spring Boot Layered Jar](https://docs.spring.io/spring-boot/docs/3.2.x/reference/htmlsingle/#container-images.efficient-images.layering)
- [Docker Multi-Stage Build](https://docs.docker.com/build/building/multi-stage/)
- [Maven Cache in Docker](https://www.baeldung.com/ops/docker-cache-maven-dependencies)
