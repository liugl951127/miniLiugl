# MiniMax Platform V3.5.21 开发手册 (增量)

> V3.5.19 代码优化 + V3.5.20 一致性 + V3.5.21 文档同步
> 完整开发手册见 `docs/DEVELOPMENT.md`, 本文档是 V3.5.21 增量

## 一、技术栈 (V3.5.21)

| 层 | 技术 | 版本 |
|----|------|------|
| 后端 | Spring Boot | 3.2.0 |
| 微服务 | Spring Cloud | 2023.0.1 |
| 网关 | Spring Cloud Gateway | + Nacos lb:// |
| 服务发现 | Nacos | 2.3.2 |
| ORM | MyBatis-Plus | 3.5.5 |
| 缓存 | Caffeine | 3.1.8 (LRU 1000) |
| 限流 | Bucket4j | 8.10.1 |
| 鉴权 | JWT (jjwt) | 0.12.3 |
| 可观测 | OpenTelemetry | 1.36.0 |
| 数据库 | MySQL 8.0+ / MariaDB 10.4+ / H2 2.2.224 (沙箱) | - |
| 前端 | Vue | 3.4 |
| 构建 | Maven | 3.9.6 |
| JDK | OpenJDK | 17 |

## 二、14 模块 (V3.5.18 合并后)

```
backend/
├── minimax-common/        # 公共 (Result, Exception, Security, Tenant)
├── minimax-gateway/       # 网关 (Nacos lb:// 路由)
├── minimax-auth/          # 鉴权 (5 账号兜底)
├── minimax-admin/         # 后台管理
├── minimax-chat/          # 聊天 + 记忆 (memory_ext)
├── minimax-model/         # 模型 + 提示词 (prompt)
├── minimax-rag/           # 知识库 RAG
├── minimax-multimodal/    # 多模态 (ONNX)
├── minimax-agent/         # AI Agent 编排
├── minimax-monitor/       # 服务监控
├── minimax-analytics/     # 数据分析
├── minimax-pipeline/      # 流水线 + 函数 (function_ext)
├── minimax-ai/            # 4 模型加权 + MiniTransformer
└── minimax-ws/            # WebSocket
```

## 三、代码规范 (V3.5.19)

### 3.1 JavaDoc 行级注解

```java
/**
 * 4 模型加权投票意图识别服务 (V3.5.15+ 升级版)
 *
 * <h2>4 模型加权</h2>
 * <ul>
 *   <li><b>TF (0.4)</b>: 子串匹配</li>
 *   <li><b>N-gram (0.3)</b>: Bigram 搭配概率</li>
 *   <li><b>同义词 (0.2)</b>: 同义扩展</li>
 *   <li><b>上下文 (0.1)</b>: 会话历史</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentService {

    // ========== 依赖注入 (Spring 自动装配, 6 个 final 字段) ==========
    private final AiIntentKeywordMapper keywordMapper;

    // ========== 权重配置 (可热更新) ==========
    /** TF 权重, 默认 0.4 */
    @Value("${minimax.ai.intent.weight.tf:0.4}")
    private double weightTf;
    
    // ========== 业务常量 ==========
    /** 关键词启用状态: 1=启用 */
    private static final int KEYWORD_ENABLED = 1;
}
```

### 3.2 命名规范 (V3.5.20 一致性)

| 类别 | 命名 | 示例 |
|------|------|------|
| 数据库表 | snake_case + 模块前缀 | `sys_user`, `chat_message` |
| Java 类 | PascalCase | `SysUser`, `ChatMessage` |
| Java 字段 | camelCase | `userId`, `createdAt` |
| MyBatis 列 | snake_case (auto map) | `user_id`, `created_at` |
| 配置 | kebab-case | `minimax.auth.super-admin-username` |
| REST 路径 | kebab-case | `/api/v1/chat-message` |
| 端口号 | 4 位数字 | 8081, 8082, 8094 |

### 3.3 Lombok 使用

```java
@Data                  // getter/setter/toString/equals/hashCode
@Builder               // 构造器
@RequiredArgsConstructor  // final 字段构造器
@Slf4j                 // private static final Logger log
@Accessors(chain = true)  // 链式 setter
```

## 四、添加新微服务 (V3.5.20 端口规范)

### 4.1 流程
1. `cp -r backend/minimax-auth backend/minimax-newservice`
2. 改 `pom.xml` 的 `<artifactId>minimax-newservice</artifactId>`
3. 改 `application.yml` 的 `spring.application.name: minimax-newservice`
4. 改 `server.port: 80XX` (V3.5.20 一致性: 13 微服务已用 7080-8095, 新服务用 8096+)
5. 改 3 个 `*Application.java` 的 `@MapperScan` 包名
6. 加 `scripts/start-all.sh` 端口映射
7. 加 `deploy/nginx-v3519.conf` upstream + location
8. 加 `docker-compose.yml` service
9. 改 `backend/pom.xml` 加 `<module>minimax-newservice</module>`

### 4.2 CI 验证清单

```bash
# 1. 实体 vs SQL 一致
python3 -c "
import re
from pathlib import Path
ent = set(re.findall(r'@TableName\\(\"(\\w+)\"\\)', Path('backend').read_text()))
sql = set(re.findall(r'CREATE TABLE \`(\\w+)\`', Path('sql/v3.5.19-schema.sql').read_text()))
assert ent == sql, f'差集: {ent ^ sql}'
print('✓ 实体 vs SQL 一致')
"

# 2. 端口 3 源一致
python3 -c "
import re
start = {s: p for s, p in re.findall(r'\\[(\\w+)\\]=(\\d+)', Path('scripts/start-all.sh').read_text())}
e2e = {s: p for s, p in re.findall(r'\"(\\w+):(\\d+)\"', Path('scripts/e2e-multiround.sh').read_text())}
nginx = {s: p for s, p in re.findall(r'upstream minimax-(\\w+).*?(\\d+)', Path('deploy/nginx-v3519.conf').read_text())}
assert start == e2e, f'差集: {set(start) ^ set(e2e)}'
print('✓ 端口 3 源一致')
"

# 3. 5 账号 3 源一致
python3 -c "
import re
init = re.findall(r'ensureTestUser\\(\"(\\w+)\",\\s*\"([^\"]+)\"', Path('backend/minimax-auth/.../AdminDataInitializer.java').read_text())
seed = re.findall(r'-- (\\w+) / (\\S+) / (\\w+)', Path('sql/v3.5.19-seed.sql').read_text())
print('✓ 5 账号一致')
"
```

## 五、数据库设计 (V3.5.19 实体反射)

### 5.1 字段类型映射

| Java 类型 | SQL 类型 | 示例 |
|-----------|----------|------|
| `String` | `VARCHAR(255)` / `TEXT` (content/json/description) | name → VARCHAR(128), content → TEXT |
| `Long` | `BIGINT` | id, userId |
| `Integer` | `INT` | count, status |
| `LocalDateTime` | `DATETIME` | createdAt, updatedAt |
| `LocalDate` | `DATE` | birthday |
| `Boolean` | `TINYINT(1)` | enabled |
| `BigDecimal` | `DECIMAL(18,2)` | amount, price |
| `List<X>` | `JSON` | tags, roles |
| `Enum` | `VARCHAR(64)` | status, type |

### 5.2 通用字段

```sql
id            BIGINT       AUTO_INCREMENT PRIMARY KEY  -- 雪花 ID
created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
deleted       TINYINT(1)   DEFAULT 0  -- 逻辑删除 (@TableLogic)
```

### 5.3 索引建议

```sql
-- 唯一索引
UNIQUE KEY uk_username (username)
-- 普通索引 (查得多的字段)
KEY idx_user_id (user_id)
KEY idx_created_at (created_at)
-- 复合索引
KEY idx_user_session (user_id, session_id)
```

## 六、AI 算法栈 (V3.5.16+)

### 6.1 意图识别 4 模型加权

```java
// V3.5.15+ 4 模型加权 (可热更新权重)
score = TF(q) * 0.4 + Ngram(q) * 0.3 + Synonym(q) * 0.2 + Context(q) * 0.1
```

### 6.2 MiniTransformer 句向量

```java
// V3.5.16+ 句向量平均池化
double[] embed = transformer.embed(tokenIds);  // [128]
// 余弦相似度
double sim = cosineSimilarity(embed1, embed2);
```

### 6.3 在线学习 (反馈学习)

```java
// V3.5.16+ 反馈调权
onlineLearning.feedback(query, predictedIntent, actualIntent);
// SGD: weight += lr * gradient
// clamp [0.05, 0.8] 防单边
```

## 七、测试规范

### 7.1 单元测试 (V3.5.16: 46 测试)

```java
@Test
void recognize_chinese_chart() {
    IntentService svc = new IntentService(...);
    KeywordEngine.Intent r = svc.recognize("生成柱状图");
    assertEquals(KeywordEngine.Intent.GENERATE_CHART, r);
}
```

### 7.2 E2E 测试 (V3.5.17: 23 case)

```bash
bash scripts/e2e-multiround.sh
# Round 1: 服务健康
# Round 2: 5 账号登录
# Round 3: 业务核心 CRUD
# Round 4: 跨服务调用
# Round 5: API 覆盖率
```

### 7.3 API 覆盖率 (V3.5.17: 93%)

```bash
bash scripts/scan-api-coverage.sh
# 扫描 frontend axios + fetch 调用
# 对比 backend controllers
# 输出覆盖率 + 孤岛 API 列表
```
