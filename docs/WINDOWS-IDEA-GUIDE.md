# MiniMax V5.30 - Windows IDEA 本地开发指南

> 面向 Windows + IntelliJ IDEA 开发者的完整本地运行手册

## 🎯 系统要求

- **OS**: Windows 10 / 11 (64-bit)
- **JDK**: OpenJDK 17 (Temurin 推荐)
- **Maven**: 3.8+ (项目自带 wrapper 也行)
- **Node**: 18+ (推荐 22 LTS)
- **Docker Desktop**: 4.0+ (可选, 用于中间件)
- **IDEA**: 2023.3+ (Ultimate 或 Community)

## 📥 1. 克隆代码

```powershell
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl
```

## 🛠️ 2. 安装基础环境

### 2.1 JDK 17 (用 Chocolatey)

```powershell
# 以管理员身份打开 PowerShell
choco install temurin17 -y

# 验证
java -version
# openjdk version "17.0.x" ...
```

手动装: https://adoptium.net/temurin/releases/?version=17

### 2.2 Maven 3.9+

```powershell
choco install maven -y

# 验证
mvn -version
```

### 2.3 Node 22 LTS

```powershell
choco install nodejs-lts -y

# 验证
node -v
npm -v
```

### 2.4 Docker Desktop (推荐)

下载: https://www.docker.com/products/docker-desktop/

安装后启动 Docker Desktop, 验证:
```powershell
docker --version
docker compose version
```

### 2.5 IntelliJ IDEA

下载: https://www.jetbrains.com/idea/download/

推荐装以下插件:
- Lombok
- MyBatisX
- Maven Helper
- Docker
- .env files support
- GitToolBox

## 🚀 3. IDEA 导入项目

### 3.1 打开项目

```
File → Open → 选择 miniLiugl 根目录
→ Trust Project (信任 Maven 项目)
```

IDEA 会自动识别 `pom.xml` 作为 Maven 项目.

### 3.2 等待 Maven 同步

右下角进度条显示 "Loading Maven indices" → 等 1-2 分钟.

### 3.3 配置项目 SDK

```
File → Project Structure → Project
→ SDK: 选 17 (Temurin-17)
→ Language Level: 17
→ 确认
```

### 3.4 Maven 配置

```
File → Settings → Build, Execution, Deployment → Build Tools → Maven
→ Maven home: 选自己装的 mvn 路径 (或用 wrapper)
→ User settings file: ~/.m2/settings.xml (可选, 国内镜像)
```

#### 配置国内 Maven 镜像 (重要)

创建 `~/.m2/settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <name>aliyun public</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### 3.5 配置 npm 国内镜像

```powershell
npm config set registry https://registry.npmmirror.com
```

## 🐳 4. 启动中间件 (Docker)

### 4.1 启动 MySQL + Redis + Nacos

```powershell
# 项目根目录
docker compose up -d mysql redis nacos
```

启动后:
- MySQL: `127.0.0.1:3306` (root/minimax_root_2024)
- Redis: `127.0.0.1:6379` (password: minimax_redis_2024)
- Nacos: http://localhost:8848/nacos (nacos/nacos)

### 4.2 验证 SQL 已导入

```powershell
# 等 MySQL 完全就绪 (30s)
docker exec -it minimax-mysql mysql -uroot -pminimax_root_2024 -e "SHOW DATABASES;"
```

应该看到 `minimax_platform` 数据库.

### 4.3 (可选) 启动 Adminer 看数据

```powershell
docker compose --profile tools up -d adminer
# http://localhost:8082  (minimax/minimax_pass_2024)
```

## 🔨 5. 编译后端

### 方式 A: IDEA Run Dashboard (推荐)

```
View → Tool Windows → Services
→ Add Service → Run Configuration Type → Spring Boot
→ 在弹出窗口选中 13 个 *Application 类
→ 点 "Run"
```

IDEA 会并行编译所有模块, 然后批量启动.

### 方式 B: 命令行

```powershell
cd backend
mvn clean install -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true
```

## 🎯 6. 启动微服务 (顺序很重要)

**启动顺序**: nacos → gateway → 12 微服务 (避免 nacos 还没起来就注册失败)

### 6.1 单服务启动 (IDEA)

每个微服务都有 `*Application.java`:

```
backend/minimax-nacos/.../NacosApplication.java
backend/minimax-gateway/.../GatewayApplication.java
backend/minimax-auth/.../AuthApplication.java
... 12 个
```

打开任一 Application 类, 点 main 方法旁的绿色三角 ▶ → Run 'AuthApplication'

**推荐顺序**:
1. nacos (Docker 已启, 跳过)
2. `GatewayApplication` (8080)
3. `AuthApplication` (8081)
4. 其他微服务

### 6.2 Run Configuration 模板

在 IDEA 中:
```
Run → Edit Configurations → + → Application
→ Main class: com.minimax.auth.AuthApplication
→ VM options: -Xms256m -Xmx512m
→ Environment variables:
   SERVER_PORT=8081
   NACOS_HOST=127.0.0.1
   NACOS_PORT=8848
   MYSQL_HOST=127.0.0.1
   MYSQL_PORT=3306
   MYSQL_USER=minimax
   MYSQL_PASSWORD=minimax_pass_2024
   REDIS_HOST=127.0.0.1
   REDIS_PORT=6379
   REDIS_PASSWORD=minimax_redis_2024
   MINIMAX_JWT_SECRET=0f6beadebfcee3e97845856757a3babf97b2af8c80f0b95690783ccc7a595352
```

每个微服务换 SERVER_PORT 即可.

### 6.3 一键启动所有 (脚本)

用项目自带的脚本:
```powershell
.\scripts\dev-start.bat backend
```

(注: 这是后台启动, 日志在 `logs/` 目录)

## 🎨 7. 启动前端

### 7.1 IDEA 启动

```
打开 frontend 目录 (IDEA 会识别为 npm 项目)
→ 右下角 "npm scripts" 面板
→ 双击 "dev" 任务
```

### 7.2 命令行启动

```powershell
cd frontend
npm install     # 首次装依赖
npm run dev
```

前端默认: http://localhost:5173

### 7.3 后端代理配置

`frontend/vite.config.js` 已配:
```js
server: {
  port: 5173,
  proxy: {
    '/api': 'http://localhost:8080'  // gateway
  }
}
```

前端 `/api/*` 自动转发到 gateway :8080.

## 🔍 8. 验证

打开浏览器:

| URL | 说明 |
|-----|------|
| http://localhost:5173 | 前端 (Vite dev) |
| http://localhost:8080/api/v1/auth/health | Gateway 健康 |
| http://localhost:8080/actuator/health | Gateway Actuator |
| http://localhost:8848/nacos | Nacos 控制台 (nacos/nacos) |
| http://localhost:8082 | Adminer (如果启了) |

### 测试登录

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "adminLiugl",
  "password": "Liugl@2026"
}
```

应该返回 accessToken.

## 🛠️ 9. 常用技巧

### 9.1 修改代码自动重启

后端: 用 IDEA 的 `Devtools` (需加依赖, 见 application-common.yml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

前端: Vite 默认 HMR, 改代码自动刷新浏览器.

### 9.2 端口冲突

如果 8081 已被占用:
```powershell
netstat -ano | findstr :8081
taskkill /F /PID <进程ID>
```

或改 application.yml 的 `server.port`.

### 9.3 IDEA 内存调优

```
Help → Change Memory Settings
→ Maximum Heap Size: 4096 MB (大型 Spring 项目)
```

### 9.4 调试技巧

- **断点**: 点行号左边设断点 → Debug 模式启动 (虫子图标)
- **条件断点**: 右键断点 → Condition: `userId == 1`
- **热部署**: Ctrl+Shift+F9 重新编译当前类
- **远程调试**: VM options 加 `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`

## 🔧 10. 故障排查

### 10.1 Maven 依赖下载失败

**症状**: `Could not find artifact ...`

**解决**:
1. 检查 `~/.m2/settings.xml` 配了 aliyun 镜像
2. 删除 `~/.m2/repository/` 缓存, 重试
3. IDEA: Build → Rebuild Project

### 10.2 端口 8080 已被占用

```powershell
netstat -ano | findstr :8080
# 找到 PID 后 taskkill /F /PID <PID>
```

### 10.3 Nacos 连不上

**症状**: 微服务启动报 `cannot connect to nacos`

**解决**:
1. 检查 Docker: `docker ps | grep nacos`
2. 检查端口: `curl http://localhost:8848/nacos/`
3. 检查 yml: `spring.cloud.nacos.discovery.server-addr: 127.0.0.1:8848`

### 10.4 数据库连不上

**症状**: `Communications link failure`

**解决**:
1. 检查 Docker MySQL 状态: `docker ps | grep mysql`
2. 测试连通: `docker exec -it minimax-mysql mysql -uroot -pminimax_root_2024 -e "SELECT 1"`
3. 检查 yml: `spring.datasource.url` 用 `127.0.0.1` (不是 localhost)

### 10.5 前端 npm install 失败

```powershell
# 清缓存重试
npm cache clean --force
rm -rf node_modules package-lock.json
npm install --registry=https://registry.npmmirror.com
```

## 📚 11. 开发流程

1. **拉代码**: `git pull origin main`
2. **编译**: IDEA 自动或 `mvn clean install -DskipTests`
3. **启动中间件**: `docker compose up -d mysql redis nacos`
4. **启动后端**: Run `*Application.java` (顺序: gateway → 微服务)
5. **启动前端**: `npm run dev`
6. **开发**: 改代码 → 自动 HMR / 热部署
7. **提交**: `git add .` → `git commit -m "..."` → `git push`

## 🎯 12. 项目结构

```
miniLiugl/
├── backend/                           # 后端 14 个 Maven 模块
│   ├── pom.xml                        # 父 pom (dependencyManagement)
│   ├── minimax-common/                # 公共 (Result / Filter / 工具)
│   ├── minimax-auth/                  # 认证 + 授权
│   ├── minimax-chat/                  # 对话 / 会话 / 消息
│   ├── minimax-model/                 # LLM 路由 / Provider
│   ├── minimax-memory/                # 短期/长期记忆
│   ├── minimax-rag/                   # RAG 知识库
│   ├── minimax-function/              # Function Calling
│   ├── minimax-admin/                 # 管理后台
│   ├── minimax-multimodal/            # 多模态
│   ├── minimax-monitor/               # 监控 / Trace
│   ├── minimax-agent/                 # Agent 自主任务
│   ├── minimax-prompt/                # Prompt 模板
│   ├── minimax-ws/                    # WebSocket 双向流
│   └── minimax-gateway/               # Spring Cloud Gateway
├── frontend/                          # Vue 3 前端
│   ├── src/
│   │   ├── views/                     # 45 个页面
│   │   ├── api/                       # 13 个 API 封装
│   │   ├── stores/                    # Pinia
│   │   └── router/                    # 路由
│   ├── vite.config.js                 # Vite + 代理
│   └── package.json
├── sql/                               # SQL 初始化
│   └── init-minimax.sql               # 41 张表 + 40 INSERT
├── scripts/                           # 部署 + 工具脚本
│   ├── deploy-minimax.sh              # Linux 一键部署
│   ├── deploy-centos.sh               # CentOS 专用部署
│   ├── install-middleware-centos.sh   # CentOS 中间件安装
│   ├── dev-start.bat                  # Windows 一键启动 (本指南)
│   └── ...
├── docs/                              # 完整文档
├── docker-compose.yml                 # Docker 全栈编排
└── .github/workflows/ci.yml          # CI 自动化
```

## 🔗 相关链接

- [README.md](../README.md) - 项目总览
- [DEPLOY-MINIMAX-GUIDE.md](DEPLOY-MINIMAX-GUIDE.md) - Linux 部署
- [DEPLOY-CENTOS-GUIDE.md](DEPLOY-CENTOS-GUIDE.md) - CentOS 部署
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构详解
- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md) - 开发指南

## 💡 性能调优 (可选)

### 后端 JVM 调优

启动参数 (IDEA Run Config):
```
-Xms512m
-Xmx1024m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
-Dfile.encoding=UTF-8
-Duser.timezone=Asia/Shanghai
```

### 前端 Vite 优化

`frontend/vite.config.js`:
```js
export default {
  server: {
    port: 5173,
    open: true,        // 自动开浏览器
    host: '0.0.0.0',   // 允许局域网访问
    proxy: { '/api': 'http://localhost:8080' }
  },
  build: {
    target: 'es2020',
    sourcemap: false,  // 生产关闭
    rollupOptions: {
      output: { manualChunks: { vue: ['vue', 'vue-router', 'pinia'] } }
    }
  }
}
```

## 🎉 完成!

按本指南配置后, Windows + IDEA 本地跑通 MiniMax 大模型平台.
遇到问题先看 [故障排查](#10-故障排查), 还不行看 GitHub Issues.