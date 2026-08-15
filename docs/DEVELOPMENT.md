# MiniMax Platform 开发文档 (V2.8.6)

> **完整开发指南** · 包含代码规范、模块开发、测试、调试、CI/CD

## 一、开发环境搭建

### 1.1 必备工具

| 工具 | 版本 | 用途 | 安装 |
|------|------|------|------|
| JDK | 17 (LTS) | 后端编译运行 | `apt install openjdk-17-jdk` |
| Maven | 3.9+ | 构建工具 | [下载](https://maven.apache.org/) |
| Node.js | 18+ | 前端 | [官网](https://nodejs.org/) |
| pnpm | 8+ | 包管理 | `npm install -g pnpm` |
| Docker | 24+ | 容器化 | [官网](https://docker.com/) |
| Git | 2.40+ | 版本控制 | 系统包 |
| IDE | IntelliJ IDEA / VSCode | 开发 | 推荐 IDEA Ultimate |

### 1.2 拉取代码

```bash
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl

# 后端编译
cd backend
mvn clean install -DskipTests

# 前端安装
cd ../frontend
pnpm install
```

### 1.3 启动基础设施 (Docker)

```bash
# 启动 MySQL + Redis + Nacos + Kafka
docker compose up -d mysql redis nacos kafka

# 初始化数据库
mysql -h127.0.0.1 -uroot -proot123456 < sql/schema-v2.8.2.sql
mysql -h127.0.0.1 -uroot -proot123456 minimax_platform < sql/seed-v2.8.2.sql
mysql -h127.0.0.1 -uroot -proot123456 minimax_platform < sql/seed-v2.8.3-tools.sql
mysql -h127.0.0.1 -uroot -proot123456 minimax_platform < sql/seed-v2.8.5-pipeline.sql
```

### 1.4 启动后端

```bash
cd backend

# 编译 + 启动 Gateway
mvn spring-boot:run -pl minimax-gateway

# 启动其他模块 (每个新终端)
mvn spring-boot:run -pl minimax-auth
mvn spring-boot:run -pl minimax-chat
...
mvn spring-boot:run -pl minimax-ai
```

### 1.5 启动前端

```bash
cd frontend
pnpm dev

# 访问 http://localhost:5173
# 默认登录: adminLiugl / Liugl@2026
```

## 二、项目结构

```
miniLiugl/
├── backend/                    # 后端
│   ├── pom.xml                 # 父 POM (Spring Cloud)
│   ├── minimax-common/         # 公共模块
│   ├── minimax-gateway/        # API 网关 (7080)
│   ├── minimax-auth/           # 认证 (8081)
│   ├── minimax-chat/           # 对话 (8082)
│   ├── minimax-memory/         # 记忆 (8083)
│   ├── minimax-model/          # 模型 (8084)
│   ├── minimax-rag/            # RAG (8085)
│   ├── minimax-function/       # 函数 (8086)
│   ├── minimax-multimodal/     # 多模态 (8087)
│   ├── minimax-agent/          # Agent (8088)
│   ├── minimax-monitor/        # 监控 (8089)
│   ├── minimax-admin/          # 后台 (8090)
│   ├── minimax-prompt/         # 提示词 (8091)
│   ├── minimax-analytics/      # 分析 (8092)
│   ├── minimax-pipeline/       # 流水线 (8093)
│   ├── minimax-ai/             # 自研 AI (8094) ⭐
│   └── minimax-ws/             # WebSocket (8095)
├── frontend/                   # 前端 (Vue 3)
│   ├── src/
│   │   ├── api/                # 18 个 API 模块
│   │   ├── views/              # 页面 (62 个)
│   │   ├── components/         # 公共组件
│   │   ├── composables/        # 组合式函数
│   │   ├── store/              # Pinia 状态
│   │   ├── router/             # 路由
│   │   ├── i18n/               # 国际化
│   │   ├── utils/              # 工具
│   │   └── assets/             # 静态资源
│   ├── package.json
│   └── vite.config.js
├── sql/                        # 数据库脚本
│   ├── schema-v2.8.2.sql       # 62 表 DDL
│   ├── seed-v2.8.2.sql         # 基础种子
│   ├── seed-v2.8.3-tools.sql   # 工具种子
│   └── seed-v2.8.5-pipeline.sql # Pipeline 种子
├── docs/                       # 文档 (13 份)
├── scripts/                    # 运维脚本
│   ├── gen_ddl.py              # DDL 自动生成
│   ├── gen-test-screenshots.py # 测试截图
│   ├── test-e2e.sh             # 端到端测试
│   ├── upgrade.sh              # 升级
│   ├── backup.sh               # 备份
│   └── ...
├── .github/workflows/          # CI/CD
└── .mvn/                       # Maven 配置
```

## 三、代码规范

### 3.1 Java 规范

**命名**:
- 类名: PascalCase (e.g., `UserController`)
- 方法/变量: camelCase (e.g., `getUserById`)
- 常量: UPPER_SNAKE (e.g., `MAX_HISTORY_TURNS`)
- 包名: lowercase (e.g., `com.minimax.ai.framework`)

**Lombok 使用**:
```java
@Data              // getter + setter + toString + equals
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j             // log 字段
```

**注释规范** (必须):
- 每个类加 Javadoc 说明
- 每个 public 方法加 Javadoc
- 关键算法加 inline 注释
- 中文注释 (本项目)

**示例**:
```java
/**
 * 商品搜索工具 (V2.8.6)
 *
 * <h3>真实数据</h3>
 * 内置 30+ 真实商品 (iPhone/MacBook/华为/小米等)
 */
@Slf4j
@Component
public class ProductSearchTool implements Tool {
    // 商品数据
    @Data @AllArgsConstructor
    public static class Product {
        public String id;          // 商品 ID
        public String name;        // 商品名
        public double price;       // 价格 (元)
    }
}
```

### 3.2 前端规范

**Vue 3 Composition API**:
```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const props = defineProps({
  userId: { type: Number, required: true }
})

const emit = defineEmits(['update'])

// ✅ 用 ref + computed
const count = ref(0)
const doubleCount = computed(() => count.value * 2)

onMounted(() => {
  // ✅ 生命周期
})
</script>
```

**API 调用**: 必须用 `src/api/` 模块, 不直接 axios
```js
import { userApi } from '@/api/admin'
const users = await userApi.list()  // ✅
const users = await axios.get('/users')  // ❌
```

**i18n**: 文本用 `$t('key')`, 不硬编码
```vue
<el-button>{{ $t('common.confirm') }}</el-button>  <!-- ✅ -->
<el-button>确认</el-button>  <!-- ❌ -->
```

**命名**:
- 组件: PascalCase (e.g., `UserList.vue`)
- 函数/变量: camelCase
- 常量: UPPER_SNAKE

### 3.3 Git 规范

**分支**:
- `main` - 主分支, 受保护
- `develop` - 开发分支
- `feature/*` - 功能分支
- `hotfix/*` - 紧急修复

**提交信息**:
```
feat: V2.8.6 AI 框架 (类 LangChain4j/Spring AI)
fix: 修复登录 401 错误
docs: 更新架构文档
refactor: 重构权限检查
test: 添加端到端测试
chore: 更新依赖
```

## 四、模块开发指南

### 4.1 创建新的微服务

**步骤 1: 在 `backend/pom.xml` 添加模块**
```xml
<modules>
    <module>minimax-newservice</module>  <!-- 新模块 -->
</modules>
```

**步骤 2: 创建模块目录**
```bash
mkdir backend/minimax-newservice
cd backend/minimax-newservice
```

**步骤 3: 创建 `pom.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <groupId>com.minimax</groupId>
        <artifactId>minimax-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>minimax-newservice</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.minimax</groupId>
            <artifactId>minimax-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

**步骤 4: 创建启动类**
```java
package com.minimax.newservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewServiceApplication.class, args);
    }
}
```

**步骤 5: 创建 Controller / Service / Mapper / Entity**

**步骤 6: 添加到 docker-compose.yml**
```yaml
minimax-newservice:
  build: ./backend/minimax-newservice
  ports: ["8096:8096"]
  environment:
    DB_HOST: mysql
    ...
```

### 4.2 创建新的业务 Agent (V2.8.6 框架)

**步骤 1: 实现 Tool**
```java
@Component
public class MyTool implements Tool {
    @Override public String getName() { return "my.tool"; }
    @Override public String getDescription() { return "..."; }
    @Override public Map<String, Object> execute(AgentContext ctx, Map<String, Object> input) {
        // 业务逻辑
        return Map.of("result", "...");
    }
}
```

**步骤 2: 创建 Agent**
```java
@Component
public class MyAgent extends Agent {
    public MyAgent(MyTool tool) {
        super("my-agent", "...", "...", 5);
        registerTool(tool);
        addCapability("...");
        requirePermission(Permission.location());
    }
    
    @Override
    protected String think(AgentContext ctx) {
        // 1. 解析用户输入
        return "...";
    }
    
    @Override
    protected ActionDecision decide(AgentContext ctx, String thought) {
        ActionDecision d = new ActionDecision();
        d.action = "my.tool";
        d.input = Map.of("key", "value");
        return d;
    }
}
```

**步骤 3: 注册到 FrameworkBootstrap**
```java
@PostConstruct
public void init() {
    // 已有...
    agentRegistry.register(myAgent);  // 新增
}
```

**步骤 4: 添加 API 端点 (自动)**
`FrameworkController` 自动支持新 Agent, 无需修改

**步骤 5: 前端路由 (可选)**
在 `frontend/src/router/index.js` 添加菜单项

### 4.3 创建新的 AI 工具

**步骤 1: 继承 AbstractSimpleTool**
```java
@Component
public class MyTool extends AbstractSimpleTool {
    @Override public String getCode() { return "my.tool"; }
    @Override public String getName() { return "我的工具"; }
    @Override public String getDescription() { return "..."; }
    @Override public String getCategory() { return "data"; }
    
    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) throws Exception {
        // 业务逻辑
        return Map.of("success", true, "data", "...");
    }
}
```

**步骤 2: 种子数据 (可选)**
```sql
INSERT INTO ai_tool (code, name, description, category, builtin, enabled) VALUES
('my.tool', '我的工具', '...', 'data', 1, 1);
```

**步骤 3: 前端 SDK**
```js
import { invokeTool } from '@/api/ai'
const result = await invokeTool('my.tool', { input: '...' })
```

### 4.4 创建新的 Pipeline 阶段

**步骤 1: 实现 Stage 类**
```java
@Component
public class MyStage {
    public MyResult process(MyInput input) {
        // 业务逻辑
        return new MyResult();
    }
}
```

**步骤 2: 注入到 PipelineExecutor**
```java
@Component
public class PipelineExecutor {
    private final MyStage myStage;
    
    public PipelineResult execute(PipelineRequest req) {
        // ... 现有 13 阶段
        MyResult r = runStage(MyStage.ORDER, () -> myStage.process(...), ...);
    }
}
```

**步骤 3: 在 `PipelineConfig.Stage` 枚举添加**
```java
MY_STAGE(14, "我的阶段");
```

## 五、测试

### 5.1 单元测试 (JUnit 5)

**位置**: `src/test/java/`

**示例** (V286FrameworkTest.java):
```java
class V286FrameworkTest {
    @BeforeEach
    void setup() {
        // 准备
    }
    
    @Test
    void testShoppingAgentExecution() {
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "test-1";
        ctx.userQuery = "iPhone 15, 不超过 8000 元";
        
        AgentResult r = shoppingAgent.execute(ctx);
        assertTrue(r.success);
        assertTrue(r.finalAnswer.contains("iPhone"));
    }
}
```

**运行**:
```bash
# 单个测试
mvn test -pl minimax-ai -Dtest=V286FrameworkTest -Dmaven.test.skip=false

# 全部测试
mvn test -pl minimax-ai -Dmaven.test.skip=false

# 指定方法
mvn test -pl minimax-ai -Dtest=V286FrameworkTest#testShoppingAgentExecution
```

### 5.2 端到端测试

**脚本**: `scripts/test-e2e.sh`
```bash
# 启动服务
docker compose up -d

# 运行 E2E
./scripts/test-e2e.sh

# 输出
# ✓ 登录: adminLiugl
# ✓ Agent 路由: hotel-agent
# ✓ 工具调用: chart.generate
# ...
```

### 5.3 测试覆盖率

| 模块 | 用例数 | 覆盖率 |
|------|--------|--------|
| minimax-common | 32 | 95% |
| minimax-auth | 12 | 90% |
| minimax-chat | 8 | 85% |
| minimax-ai | 79 | 90% |
| **总计** | **206** | **90%** |

## 六、调试技巧

### 6.1 后端调试

**IDEA Remote Debug**:
```
1. 启动时加参数: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
2. IDEA: Run → Edit Configurations → Remote JVM Debug
3. Host: localhost, Port: 5005
```

**日志调试**:
```java
log.info("参数: {}", params);                    // 输出参数
log.debug("详细信息: {}", detailedInfo);          // 调试信息
log.error("错误", e);                              // 异常堆栈
```

**SQL 打印** (MyBatis):
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 6.2 前端调试

**Vue DevTools**: 浏览器扩展, 查看组件树/Pinia 状态

**Console 调试**:
```js
console.log('user:', userStore.profile)
console.debug('request:', config)
```

**Network 面板**: 查看 XHR 请求的 Request/Response

### 6.3 性能分析

**后端 - Spring Boot Actuator**:
```bash
curl http://localhost:7080/actuator/metrics/jvm.memory.used
```

**SQL 慢查询** (MySQL):
```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
SHOW VARIABLES LIKE 'slow_query_log_file';
```

**前端 - Performance 面板** (F12):
- First Contentful Paint < 1.5s
- Time to Interactive < 3s
- Total Blocking Time < 300ms

### 6.4 链路追踪

所有请求带 `X-Trace-Id`:
```bash
curl -H "X-Trace-Id: my-trace-001" http://api/
```

**Grafana Tempo 查询**:
```
{service.name="minimax-ai"} && trace_id="abc123"
```

## 七、CI/CD

### 7.1 GitHub Actions

**位置**: `.github/workflows/ci.yml`

**4 个 Job**:
1. **test** - 跑全部测试
2. **build** - 编译 + 打包
3. **lint** - 代码检查 (checkstyle/pmd/spotbugs)
4. **deploy** - 部署到服务器

**触发**:
- Push to main/develop
- Pull Request

### 7.2 本地 CI 脚本

```bash
./scripts/local-ci.sh
# 等价于 .github/workflows/ci.yml
```

### 7.3 发布流程

```bash
# 1. 升级版本号
mvn versions:set -DnewVersion=2.8.7

# 2. 更新 CHANGELOG.md
vim docs/CHANGELOG.md

# 3. 提交并打 tag
git add -A
git commit -m "feat: V2.8.7 ..."
git tag v2.8.7
git push origin main --tags

# 4. 自动触发 .github/workflows/release.yml
# 5. 镜像推送到 Docker Hub
# 6. GitHub Release 创建
```

## 八、常见开发问题

### 8.1 编译错误

**Q: Lombok @Slf4j 不生效**
A: IDEA 安装 Lombok 插件, 启用 annotation processing
   `File → Settings → Build → Compiler → Annotation Processors → Enable`

**Q: 循环依赖**
A: 用 `@Lazy` 或 ApplicationEvent 解耦 (V1.9 已修复)

**Q: MyBatis-Plus Lambda 查询报错**
A: 检查 import `import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper`

### 8.2 运行时错误

**Q: Nacos 连不上**
A: 检查 `application.yml` 的 `spring.cloud.nacos.discovery.server-addr`
   确认 Nacos 已启动: `docker ps | grep nacos`

**Q: Redis 连接超时**
A: 检查 `spring.data.redis.host`, 默认 `127.0.0.1:6379`

**Q: 401 Unauthorized**
A: Token 过期, 重新登录或检查 `Authorization: Bearer xxx` 头

### 8.3 性能问题

**Q: AI Pipeline 慢**
A: 检查 PipelineConfig:
```java
PipelineConfig.MAX_GENERATE_TOKENS = 100;  // 减少生成 token
PipelineConfig.ENABLE_KV_CACHE = true;     // 启用 KV cache
PipelineConfig.BATCH_SIZE = 4;             // 批处理
```

**Q: SQL 慢**
A: 用 `EXPLAIN` 分析查询, 加索引
```sql
EXPLAIN SELECT * FROM chat_message WHERE session_id = 'xxx';
```

## 九、贡献流程

1. **创建分支**: `git checkout -b feature/your-feature`
2. **开发 + 测试**: 写代码, 跑测试 (`mvn test`)
3. **本地 CI**: 跑 `./scripts/local-ci.sh` 确保通过
4. **提交**: `git commit -m "feat: ..."`
5. **推送**: `git push origin feature/your-feature`
6. **PR**: 创建 Pull Request 到 main
7. **Code Review**: 等 CI + 至少 1 个 Approve
8. **合并**: Squash merge

## 十、参考资源

- [Spring Boot 3.2 文档](https://docs.spring.io/spring-boot/docs/3.2.x/reference/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [MyBatis-Plus 3.5](https://baomidou.com/)
- [Vue 3 文档](https://cn.vuejs.org/)
- [Element Plus](https://element-plus.org/zh-CN/)
- [LangChain4j (参考设计)](https://docs.langchain4j.ai/)
- [Spring AI (参考设计)](https://docs.spring.io/spring-ai/reference/)

---

**维护**: MiniMax Team
**最后更新**: 2026-07-12 (V2.8.6)
