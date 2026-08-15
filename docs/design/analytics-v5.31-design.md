# minimax-analytics 微服务设计文档 (V5.31)

> 第 15 个微服务 — 企业级"数据智能分析"模块
> 模块坐标: `com.minimax.analytics`
> 端口: **8096** (Nacos 注册名 `minimax-analytics`)
> 版本: V5.31.0 / 设计日期 2026-06-22

---

## 0. 设计前置 (Assumptions / 复用现状)

实测核对现有 14 个微服务后, 本设计基于以下事实:

| 现有资产 | 路径 | 复用方式 |
|---|---|---|
| `minimax-common` | `backend/minimax-common` | `Result<T>` / `ResultCode` / `BizException` / `BaseController` / `PageRequest` |
| `minimax-model` | `backend/minimax-model` (端口 8083) | `ModelProviderFactory` + `AnthropicAdapter/OpenAiCompatibleAdapter/GeminiAdapter` + `ChatRequest/ChatResponse` |
| `minimax-gateway` | `backend/minimax-gateway` | `lb://minimax-analytics` 新增路由 |
| `minimax-prompt` | `backend/minimax-prompt` (Day 14) | **建议**: 走 prompt 模块统一管理 NL2SQL / Report 的 prompt 模板 (而非本模块内置 freeMarker 字符串) |
| Nacos | `127.0.0.1:8848 / namespace=minimax-dev` | 复用, group=minimax, metadata.version=5.31.0 |
| application-common.yml | `minimax-common/src/main/resources` | 通过 `spring.config.import` 复用, 无需复制 |

**NOTE**: 用户原话提到 "复用 minimax-model" 和 "用 freeMarker 渲染 markdown 报告" — 本设计两条都支持; 但**强烈建议**把模板类的 Prompt 走 `minimax-prompt` (统一管理), markdown 报告模板仍由本模块维护 (`resources/templates/report/*.md.ftl`)。**待用户决策**: 是否完全合并到 prompt 模块, 见 §11。

---

## 1. 模块边界 (controller / service / entity / dto / vo / config)

```
backend/minimax-analytics/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/minimax/analytics/
    │   │   ├── AnalyticsApplication.java           # @SpringBootApplication 入口
    │   │   ├── package-info.java
    │   │   │
    │   │   ├── controller/                          # REST 入口层 (8 个 controller, 32+ 端点)
    │   │   │   ├── AnalyticsController.java         # 综合面板 + 健康检查
    │   │   │   ├── DataSourceController.java        # 数据源 CRUD + 测试连接
    │   │   │   ├── SchemaController.java            # 表结构 / 画像 / ER 图
    │   │   │   ├── IngestController.java            # 文件上传 + 解析 + 质量报告
    │   │   │   ├── Nl2SqlController.java            # 自然语言 → SQL
    │   │   │   ├── QueryController.java             # SQL 执行 (直接 + 安全执行)
    │   │   │   ├── ReportController.java            # 报告生成 + 列表 + 下载
    │   │   │   └── ChartController.java             # ECharts 配置生成
    │   │   │
    │   │   ├── service/                             # 业务逻辑层 (按子模块)
    │   │   │   ├── datasource/
    │   │   │   │   ├── DataSourceService.java       # 接口
    │   │   │   │   └── DataSourceServiceImpl.java   # 多数据源池管理 (HikariCP + Druid)
    │   │   │   ├── schema/
    │   │   │   │   ├── SchemaService.java
    │   │   │   │   ├── SchemaServiceImpl.java       # information_schema 读取 + 画像
    │   │   │   │   └── ProfileBuilder.java          # 数据画像聚合
    │   │   │   ├── ingest/
    │   │   │   │   ├── FileIngestService.java
    │   │   │   │   ├── FileIngestServiceImpl.java
    │   │   │   │   ├── parser/CsvParser.java
    │   │   │   │   ├── parser/JsonParser.java
    │   │   │   │   ├── parser/LogParser.java
    │   │   │   │   └── parser/EncodingDetector.java
    │   │   │   ├── nlsql/
    │   │   │   │   ├── Nl2SqlService.java
    │   │   │   │   ├── Nl2SqlServiceImpl.java       # LLM 调用 + prompt 拼装
    │   │   │   │   ├── SqlSafetyChecker.java        # SQL 安全校验
    │   │   │   │   └── PromptTemplates.java         # 内置模板 (备选, 见 §11)
    │   │   │   ├── query/
    │   │   │   │   ├── QueryService.java
    │   │   │   │   └── QueryServiceImpl.java        # 安全执行 + 分页 + 超时
    │   │   │   ├── report/
    │   │   │   │   ├── ReportService.java
    │   │   │   │   ├── ReportServiceImpl.java       # SQL → 数据 → Markdown 报告
    │   │   │   │   ├── TrendAnalyzer.java           # 趋势分析 (移动平均/同比环比)
    │   │   │   │   └── AnomalyDetector.java         # 异常值检测 (IQR / z-score)
    │   │   │   └── chart/
    │   │   │       ├── ChartService.java
    │   │   │       ├── ChartServiceImpl.java
    │   │   │       └── ChartTypeDecider.java        # 自动选图策略
    │   │   │
    │   │   ├── entity/                              # 持久化实体 (存元数据/任务/报告)
    │   │   │   ├── AnalyticsDataSource.java         # 数据源配置
    │   │   │   ├── AnalyticsIngestTask.java         # 导入任务
    │   │   │   ├── AnalyticsReport.java             # 生成的报告
    │   │   │   ├── AnalyticsQueryLog.java           # NL2SQL 调用日志
    │   │   │   └── base/BaseEntity.java             # 公共字段 (id/createdAt/updatedAt/deleted)
    │   │   │
    │   │   ├── mapper/                              # MyBatis-Plus mapper
    │   │   │   ├── AnalyticsDataSourceMapper.java
    │   │   │   ├── AnalyticsIngestTaskMapper.java
    │   │   │   ├── AnalyticsReportMapper.java
    │   │   │   └── AnalyticsQueryLogMapper.java
    │   │   │
    │   │   ├── dto/                                 # 入参 (Request)
    │   │   │   ├── datasource/{CreateDataSourceReq,UpdateDataSourceReq,TestConnectionReq}
    │   │   │   ├── schema/{SchemaQueryReq}
    │   │   │   ├── ingest/{UploadReq,ParseOptions}
    │   │   │   ├── nlsql/{Nl2SqlReq,Nl2SqlFeedbackReq}
    │   │   │   ├── query/{SqlExecuteReq}
    │   │   │   ├── report/{GenerateReportReq}
    │   │   │   └── chart/{ChartRecommendReq}
    │   │   │
    │   │   ├── vo/                                  # 出参 (Response)
    │   │   │   ├── datasource/{DataSourceVO,ConnectionTestVO}
    │   │   │   ├── schema/{TableMetaVO,ColumnMetaVO,IndexMetaVO,DataProfileVO,ErGraphVO}
    │   │   │   ├── ingest/{IngestTaskVO,QualityReportVO,ColumnQualityVO}
    │   │   │   ├── nlsql/{Nl2SqlResultVO,SqlExplanationVO}
    │   │   │   ├── query/{SqlResultVO,ColumnStatVO}
    │   │   │   ├── report/{ReportVO,ReportSummaryVO}
    │   │   │   └── chart/{ChartConfigVO,ChartOptionVO}
    │   │   │
    │   │   ├── config/
    │   │   │   ├── AnalyticsProperties.java         # @ConfigurationProperties("minimax.analytics")
    │   │   │   ├── MultiDataSourceConfig.java       # 动态数据源注册
    │   │   │   ├── CaffeineConfig.java              # 表结构缓存
    │   │   │   ├── FreeMarkerConfig.java            # Markdown 模板引擎
    │   │   │   ├── SqlSafetyProperties.java         # 白名单 / 超时配置
    │   │   │   ├── NacosMetadataConfig.java         # 注册元数据
    │   │   │   └── WebMvcConfig.java                # Knife4j / 拦截器
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── SqlSafetyException.java          # 4003 黑名单命中
    │   │   │   ├── DataSourceNotFoundException.java # 4004
    │   │   │   ├── UnsupportedFileFormatException.java # 4005
    │   │   │   └── QueryTimeoutException.java       # 4006
    │   │   │
    │   │   └── util/
    │   │       ├── SqlUtils.java                    # 解析/校验 SQL
    │   │       ├── TypeInfer.java                   # 推断字段类型 (CSV header)
    │   │       └── StatsUtils.java                  # 数值分布/百分位
    │   │
    │   └── resources/
    │       ├── application.yml                      # 服务特有 (port 8096)
    │       ├── application-dev.yml                  # dev profile
    │       ├── mapper/                              # MyBatis XML (4 个)
    │       ├── templates/report/                    # freeMarker markdown 模板
    │       │   ├── standard.md.ftl
    │       │   ├── trend.md.ftl
    │       │   └── distribution.md.ftl
    │       └── db/schema.sql                        # 4 张业务表 DDL
    │
    └── test/java/com/minimax/analytics/
        ├── datasource/DataSourceServiceTest.java
        ├── schema/SchemaServiceTest.java
        ├── ingest/{CsvParserTest,JsonParserTest,EncodingDetectorTest}
        ├── nlsql/{Nl2SqlServiceTest,SqlSafetyCheckerTest}
        ├── query/QueryServiceTest.java
        ├── report/{ReportServiceTest,TrendAnalyzerTest,AnomalyDetectorTest}
        ├── chart/ChartTypeDeciderTest.java
        └── integration/AnalyticsEndToEndTest.java   # @SpringBootTest 跑 H2
```

---

## 2. 关键类 + 核心方法签名

### 2.1 `datasource` 子模块

```java
// 动态数据源管理 (HikariCP + Druid 双层: 外层动态注册, 内层连接池)
public interface DataSourceService {
    // CRUD
    Long createDataSource(CreateDataSourceReq req, Long userId);          // 加密存储密码 (AES)
    void updateDataSource(Long id, UpdateDataSourceReq req);
    void deleteDataSource(Long id);
    DataSourceVO getDataSource(Long id);                                 // 密码脱敏
    List<DataSourceVO> listByUser(Long userId, int page, int size);

    // 连接管理
    ConnectionTestVO testConnection(TestConnectionReq req);              // 立即尝试连接
    javax.sql.DataSource acquire(Long dataSourceId);                     // 从池中获取 DataSource
    void evict(Long dataSourceId);                                       // 关闭并移除
    Map<String, Object> getPoolStats(Long dataSourceId);                 // HikariCP 指标
}

public class MultiDataSourceConfig {
    // 启动时根据 DB 加载 analytics_data_source 表, 预热常用数据源
    @PostConstruct void preload();
    // 路由: ThreadLocal<DataSourceKey> 决定走哪个 DataSource (用 AbstractRoutingDataSource)
}

// 实体 (MyBatis-Plus)
@Data @TableName("analytics_data_source")
public class AnalyticsDataSource extends BaseEntity {
    private Long id;
    private String name;
    private String jdbcUrl;
    private String username;
    private String passwordEnc;            // AES 加密
    private String driverClass;            // 默认 com.mysql.cj.jdbc.Driver
    private String dbType;                 // mysql/postgresql/h2/oracle
    private Integer poolMaxSize;           // 默认 10
    private Integer poolMinIdle;           // 默认 2
    private String tablePrefixWhitelist;   // 逗号分隔, 留空=全部允许
    private Boolean readOnly;              // 强制只读连接
    private Long ownerUserId;
    private String status;                 // ACTIVE/DISABLED
}
```

### 2.2 `schema` 子模块

```java
public interface SchemaService {
    // 元数据读取 (information_schema)
    List<String> listDatabases(Long dataSourceId);
    List<TableMetaVO> listTables(Long dataSourceId, String dbName, String keyword);

    TableMetaVO getTable(Long dataSourceId, String dbName, String tableName);
    // 返回: columns + indexes + ddl + sampleRows(前5)

    // 数据画像 (数据采样分析)
    DataProfileVO buildProfile(Long dataSourceId, String dbName, String tableName,
                               int sampleSize /* 默认 1000 */);
    // 返回: 行数/列数/每列 typeDistribution/nullRate/uniqueCount/topValues/numericStats

    ErGraphVO buildErGraph(Long dataSourceId, String dbName);
    // 返回: nodes (table) + edges (FK) + 用于 ECharts graph 渲染

    // 缓存 (Caffeine, 1h TTL, max 1000 entries)
    void invalidate(Long dataSourceId);
}
```

### 2.3 `ingest` 子模块

```java
public interface FileIngestService {
    // 上传 + 解析 (流式, 100MB 上限)
    IngestTaskVO upload(MultipartFile file, ParseOptions opts, Long userId);

    // 状态查询
    IngestTaskVO getTask(String taskId);
    QualityReportVO getQualityReport(String taskId);

    // 重新解析 (参数变化时)
    IngestTaskVO reparse(String taskId, ParseOptions opts);

    // 取前 N 行 (预览)
    List<Map<String, Object>> preview(String taskId, int limit);
}

// 解析器策略
public interface FileParser {
    boolean supports(String contentType, String filename);
    ParseResult parse(InputStream in, ParseOptions opts) throws IOException;
}
// 实现: CsvParser / JsonParser / LogParser (text+regex)

public class ParseOptions {
    private String encoding;          // 自动检测: UTF-8 / GBK / ISO-8859-1
    private String delimiter;         // CSV: , ; \t |  (空 = 自动推断)
    private Boolean hasHeader;        // 默认 true
    private Integer previewRows;      // 默认 100
    private String jsonPath;          // JSON: 提取数组的路径 (e.g. "$.data")
    private String logPattern;        // Log: 正则模板 (e.g. "%d{ISO8601} %-5p %c - %m%n")
    private Integer maxRows;          // null=无限
}

// 质量报告
public class QualityReportBuilder {
    QualityReportVO build(ParseResult pr);
    // 输出: totalRows/totalCols/perCol{nullRate,type,uniqueCount,numericStats:{min,max,mean,p50,p95,stddev},topValues,anomalies}
}
```

### 2.4 `nlsql` 子模块 (核心)

```java
public interface Nl2SqlService {
    Nl2SqlResultVO nl2sql(Nl2SqlReq req, Long userId);
    // req: { dataSourceId, dbName, question, modelCode?, history[] }
    // 返回: { sql, explanation, confidence, latencyMs, modelUsed, tokenUsage, safetyCheck }

    // 反馈学习 (可选): 用户修改 SQL 后回写, 训练样本入库
    void feedback(Nl2SqlFeedbackReq req);

    // 元数据注入辅助: 把当前 db 的表结构压缩成 prompt context
    String buildSchemaContext(Long dataSourceId, String dbName, List<String> hintTables);
}

public class SqlSafetyChecker {
    SafetyResult check(String sql, Set<String> tablePrefixWhitelist);
    // 规则 (见 §4)
}

// prompt 模板占位: ${DB_TYPE} / ${SCHEMA_CONTEXT} / ${QUESTION} / ${HISTORY} / ${FEW_SHOTS}
public class PromptTemplates {
    static final String NL2SQL_SYSTEM =
        "你是 SQL 专家. 当前数据库: ${DB_TYPE}. 仅生成 SELECT, 禁止任何写操作.";
    static final String NL2SQL_USER =
        "## 表结构\n${SCHEMA_CONTEXT}\n\n## 历史对话\n${HISTORY}\n\n## 用户问题\n${QUESTION}\n\n" +
        "## 要求\n1. 仅输出 JSON {sql, explanation}\n2. SQL 用反引号包裹\n3. 复杂查询加 LIMIT";
}
```

### 2.5 `query` 子模块

```java
public interface QueryService {
    SqlResultVO execute(SqlExecuteReq req, Long userId);
    // req: { dataSourceId, sql, params, maxRows(默认 1000), timeoutSec(默认 30) }

    // 流式 (大结果集) — 返回 StreamingResponseBody
    void executeStream(SqlExecuteReq req, Long userId, OutputStream out);
}

public class SqlResultVO {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Integer totalRows;
    private Long latencyMs;
    private Boolean truncated;          // rows.size == maxRows
    private ColumnStatVO[] columnStats; // 自动跑列统计 (用于推荐图表)
}
```

### 2.6 `report` 子模块

```java
public interface ReportService {
    ReportVO generate(GenerateReportReq req, Long userId);
    // req: { dataSourceId, sql, question?, templateType(STANDARD/TREND/DISTRIBUTION), title? }

    ReportVO get(Long reportId);
    List<ReportSummaryVO> list(Long userId, int page, int size);
    String exportMarkdown(Long reportId);   // 返回 md 字符串
    byte[] exportPdf(Long reportId);       // 通过 openhtmltopdf (可选, 见 §11)
}

public class TrendAnalyzer {
    TrendAnalysisVO analyze(List<Map<String, Object>> rows, String timeColumn, String valueColumn);
    // 输出: direction(UP/DOWN/FLAT), slope, period(daily/weekly), forecast(next N)
}

public class AnomalyDetector {
    List<AnomalyVO> detect(List<Map<String, Object>> rows, String column, AnomalyMethod method);
    // method: IQR (默认) / ZSCORE / ISOLATION_FOREST (后两者可选)
}
```

### 2.7 `chart` 子模块

```java
public interface ChartService {
    ChartConfigVO recommend(ChartRecommendReq req);
    // req: { rows, columns, userHint?("bar"/"line"/"pie"/null) }

    ChartOptionVO buildOption(ChartType type, List<Map<String, Object>> rows,
                              String xField, String yField, String seriesField);
    // 输出 ECharts Option JSON (兼容 echarts 5.x)
}

public class ChartTypeDecider {
    ChartType decide(DataShape shape, String userHint);
    // 决策矩阵 (见 §6)
}

// 类型
public enum ChartType { BAR, LINE, PIE, SCATTER, AREA, FUNNEL, HEATMAP, TABLE }
```

---

## 3. 依赖 (pom)

### 3.1 父 `pom.xml` 新增一行

在 `<modules>` 块里加:

```xml
<module>minimax-analytics</module>
```

(无需新增 `<dependencyManagement>` 项, 因为所用依赖都已在父 pom 锁定版本)

### 3.2 模块 `pom.xml` 依赖清单

```xml
<dependencies>
    <!-- 基础栈 (与其他模块一致) -->
    <dependency><groupId>com.minimax</groupId><artifactId>minimax-common</artifactId></dependency>
    <dependency><groupId>com.minimax</groupId><artifactId>minimax-model</artifactId></dependency>

    <!-- Web / Validation -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>

    <!-- 持久化 (本模块自身元数据存业务库) -->
    <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId></dependency>
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>

    <!-- 多数据源 + 动态注册 (本模块核心) -->
    <dependency><groupId>com.alibaba</groupId><artifactId>druid-spring-boot-3-starter</artifactId></dependency>

    <!-- 缓存 (表结构 1h) -->
    <dependency><groupId>com.github.ben-manes.caffeine</groupId><artifactId>caffeine</artifactId></dependency>

    <!-- 文件解析 -->
    <dependency><groupId>cn.hutool</groupId><artifactId>hutool-all</artifactId></dependency>
    <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId></dependency>
    <dependency><groupId>com.opencsv</groupId><artifactId>opencsv</artifactId><version>5.9</version></dependency>
    <!-- (opencsv 版本在父 pom 锁定; 若不愿新增, 用 hutool 的 CsvUtil 也可) -->

    <!-- Markdown 模板 -->
    <dependency><groupId>org.freemarker</groupId><artifactId>freemarker</artifactId></dependency>

    <!-- SQL Parser (用于 SQL 安全审计, 比正则更准确) -->
    <dependency><groupId>com.alibaba.druid</groupId><artifactId>druid-parser</artifactId><version>${druid.version}</version></dependency>

    <!-- 客户端 (调 prompt 模块 / 网关调用) -->
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-loadbalancer</artifactId></dependency>

    <!-- 鉴权 (复用 common 的 JJWT) -->
    <!-- 已由 minimax-common 传递引入, 无需重复 -->

    <!-- 文档 -->
    <dependency><groupId>com.github.xiaoymin</groupId><artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId></dependency>

    <!-- 工具 -->
    <dependency><groupId>cn.hutool</groupId><artifactId>hutool-all</artifactId></dependency>
    <dependency><groupId>com.alibaba.fastjson2</groupId><artifactId>fastjson2</artifactId></dependency>

    <!-- 测试 -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
</dependencies>
```

**注意**:
1. `opencsv` 是父 pom 没锁版本的新依赖, 有两种选择 — A) 在父 pom 加 `<dependencyManagement>` 锁定; B) 直接用 `hutool` 的 `CsvUtil` (简化方案)。**推荐 B**, 因为 hutool 已在父 pom。
2. **不**额外引入 PDF 库 (openhtmltopdf), 见 §11 决策点。

---

## 4. SQL 执行安全方案 (五道防线)

### 防线 1 — DDL/DML 关键字黑名单 (基于 Druid SQL Parser)

```java
public class SqlSafetyChecker {
    private static final Set<String> FORBIDDEN = Set.of(
        "INSERT","UPDATE","DELETE","REPLACE","DROP","TRUNCATE",
        "ALTER","CREATE","RENAME","GRANT","REVOKE",
        "SET","LOCK","UNLOCK","KILL","CALL","LOAD","HANDLER",
        "INTO OUTFILE","INTO DUMPFILE","SLEEP","BENCHMARK"
    );

    public SafetyResult check(String sql, Set<String> whitelist) {
        List<SQLStatement> stmts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        // 1. 必须是单条 SELECT (Druid 拆分)
        if (stmts.size() != 1) return reject("MULTI_STMT", "仅支持单条 SELECT");
        SQLStatement stmt = stmts.get(0);
        if (!(stmt instanceof MySqlSelectQueryBlock)) return reject("NOT_SELECT", "仅支持 SELECT");
        // 2. 黑名单扫描 (注释/字符串里的忽略)
        // 3. 表名前缀白名单: 解析 FROM/JOIN, 每个表名前缀必须命中 whitelist
        Set<String> tables = extractor.extractTableNames(stmt);
        for (String t : tables) if (!matchesWhitelist(t, whitelist)) return reject(...);
        // 4. 限制 LIMIT (无 LIMIT 自动追加 LIMIT 1000)
        // 5. 敏感函数拦截 (SLEEP/BENCHMARK/LOAD_FILE 已列入黑名单)
        return SafetyResult.ok(rewrittenSql);
    }
}
```

### 防线 2 — 数据源只读连接 (`AnalyticsDataSource.readOnly = true`)

- 通过 JDBC URL 参数 `?readonly=true` 或 HikariCP `connectionReadOnly=true` 强制只读
- MySQL: 创建专用账号 `SELECT ONLY` 角色 (由用户在外部预先配置, 我们不操作权限)

### 防线 3 — HikariCP 连接级超时

```yaml
spring.datasource:
  hikari:
    connection-timeout: 5000      # 获取连接 5s 超时
    validation-timeout: 3000
    max-lifetime: 1800000
    leak-detection-threshold: 30000
```

### 防线 4 — Statement 超时 (JDBC `Statement.setQueryTimeout`)

```java
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setQueryTimeout(timeoutSec);          // 秒级, MySQL 会发 KILL QUERY
    ps.setMaxRows(maxRows);
    ps.setFetchSize(1000);
    ResultSet rs = ps.executeQuery();
    // ...
}
```

### 防线 5 — 行数/字节上限

- `maxRows` 默认 1000, 超过 `truncated=true` 提示
- 大数据集走流式端点 (`executeStream`)
- 业务库连接池上限 `poolMaxSize=20`, 单租户限流 5 QPS (Bucket4j)

### 防线 6 (可选) — SQL 审计日志

- 所有执行过的 SQL 入 `analytics_query_log` 表 (userId/dataSourceId/sql/latencyMs/rowCount/createdAt)
- 保留 90 天, 异步清理

---

## 5. NL2SQL Prompt 模板

### 5.1 System Prompt

```
你是 SQL 专家, 当前数据库类型: ${DB_TYPE} (MySQL 8.0).
仅允许生成 SELECT 查询, 禁止任何 INSERT/UPDATE/DELETE/DDL.
所有表名/字段名必须严格匹配下方 schema 中的实际名称, 必要时用反引号包裹.

## 输出格式 (严格 JSON, 不要 Markdown 代码块)
{"sql": "<SELECT ...>", "explanation": "<用中文说明查询逻辑>", "confidence": 0.0~1.0}

## 安全规则
1. 必须带 LIMIT (最多 ${MAX_ROWS} 行, 没有 LIMIT 时自动追加)
2. 涉及金额/计费字段必须 ROUND(x, 2)
3. 时间范围使用 created_at >= DATE_SUB(NOW(), INTERVAL N DAY)
4. 模糊查询用 LIKE, 不用 REGEXP (性能差)
5. 不要用 SELECT *, 必须显式列出字段
6. 复杂聚合用 CTE (WITH ... AS) 提高可读性
```

### 5.2 User Prompt

```
## 数据库 Schema
${DB_TYPE}://${DB_NAME}

${SCHEMA_CONTEXT}
-- 表结构示例:
-- CREATE TABLE `user` (
--   id BIGINT PRIMARY KEY AUTO_INCREMENT,
--   username VARCHAR(64) NOT NULL,
--   email VARCHAR(128),
--   status TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
--   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
--   KEY idx_created (created_at)
-- ) ENGINE=InnoDB;

${ER_HINT}        -- 外键关系提示

## 历史对话 (最近 3 轮)
${HISTORY_JSON}

## 用户当前问题
${QUESTION}

## 推断提示
${INTENT_HINTS}   -- 由意图识别模块推断: 时间范围/聚合方式/排序字段
```

### 5.3 Few-shot 示例 (3 条, 提升准确率)

```json
[
  {"q":"最近7天注册用户数","sql":"SELECT DATE(created_at) d, COUNT(*) c FROM `user` WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(created_at) ORDER BY d","exp":"按天统计近7天新增用户"},
  {"q":"金额最高的前10订单","sql":"SELECT id, user_id, ROUND(amount,2) amount FROM `order` ORDER BY amount DESC LIMIT 10","exp":"按金额降序取前10"},
  {"q":"各状态订单占比","sql":"SELECT status, COUNT(*) c, ROUND(COUNT(*)*100/(SELECT COUNT(*) FROM `order`),2) pct FROM `order` GROUP BY status","exp":"统计订单状态分布及占比"}
]
```

### 5.4 自校环节 (Reflection)

首次生成的 SQL 经 `SqlSafetyChecker` 拒绝时, 二次 prompt:

```
你上次生成的 SQL 被安全检查拒绝:
原因: ${REASON}
原 SQL: ${ORIGINAL_SQL}
请重新生成合规的 SELECT, 仍然只输出 JSON.
```

最多 retry 1 次, 仍失败则返回错误给前端, 引导用户重写问题。

---

## 6. ECharts 配置生成策略

### 6.1 自动选图决策矩阵 (`ChartTypeDecider.decide`)

| 数据形状 (DataShape) | userHint | 推荐图表 |
|---|---|---|
| 1 列分类 + 1 列数值, 类别 ≤ 8 | null | **PIE** |
| 1 列分类 + 1 列数值, 类别 ≤ 30 | null | **BAR** |
| 1 列分类 + 1 列数值, 类别 > 30 | null | **BAR** (横向滚动) |
| 1 列时间 + 1 列数值 | null | **LINE** |
| 1 列时间 + 多列数值 | null | **LINE** (多 series) |
| 2 列数值 (无类别) | null | **SCATTER** |
| 1 列分类 + 1 列数值, 类别 ∈ [8, 30] | null | **FUNNEL** (若值单调递减) |
| ≥ 3 列数值 | null | **HEATMAP** |
| 其他 / 行数 ≤ 50 | null | **TABLE** |

### 6.2 Option 生成模板 (ECharts 5.x)

```ts
{
  title: { text, subtext, left: 'center' },
  tooltip: { trigger: 'axis' | 'item', axisPointer: { type: 'shadow' } },
  legend: { bottom: 10 },
  grid: { left: 60, right: 30, top: 40, bottom: 60, containLabel: true },
  xAxis: { type: 'category', data: xValues, axisLabel: { rotate: xValues.length>8 ? 30 : 0 } },
  yAxis: { type: 'value' },
  series: [{ name, type: 'bar'|'line'|'pie', data, itemStyle, label: { show: true } }],
  color: ['#5470c6','#91cc75','#fac858','#ee6666','#73c0de','#3ba272','#fc8452','#9a60b4']
}
```

### 6.3 前端集成

- 后端只返回 `ChartOptionVO { type, option, recommendedColumns }` JSON
- 前端 (复用现有 frontend 项目) 用 `echarts@5` 直接 `chart.setOption(option)`
- 服务端**不**渲染图片 (避免依赖 phantomjs/chromium)

---

## 7. API 端点清单 (32 个, 全部 `/api/v1/analytics/...`)

> 所有端点统一前缀 `/api/v1/analytics`, 经 gateway StripPrefix 后转发到本服务 `lb://minimax-analytics`。
> 鉴权: 除 `GET /health` 外, 均需 `Authorization: Bearer <jwt>` (复用 common 的 JwtAuthenticationFilter)。

### 7.1 系统 / 综合 (AnalyticsController — 3)

| # | Method | Path | 用途 |
|---|---|---|---|
| 1 | GET | `/health` | 健康检查 (含 Nacos/disk) |
| 2 | GET | `/dashboard/summary` | 首页聚合 (数据源数/任务数/今日查询数) |
| 3 | GET | `/dashboard/recent-queries` | 最近 20 条 NL2SQL 调用 |

### 7.2 数据源 (DataSourceController — 6)

| # | Method | Path | 用途 |
|---|---|---|---|
| 4 | POST | `/datasources` | 新建数据源 (加密存储密码) |
| 5 | PUT | `/datasources/{id}` | 更新 |
| 6 | DELETE | `/datasources/{id}` | 软删除 |
| 7 | GET | `/datasources/{id}` | 详情 (密码脱敏) |
| 8 | GET | `/datasources` | 分页列表 (按 userId) |
| 9 | POST | `/datasources/test` | 测试连接 (不保存) |

### 7.3 元数据 / Schema (SchemaController — 6)

| # | Method | Path | 用途 |
|---|---|---|---|
| 10 | GET | `/datasources/{dsId}/databases` | 列出所有数据库 |
| 11 | GET | `/datasources/{dsId}/databases/{db}/tables` | 列出表 (支持 keyword 搜索) |
| 12 | GET | `/datasources/{dsId}/databases/{db}/tables/{table}` | 表结构详情 (columns+indexes+DDL+样本) |
| 13 | GET | `/datasources/{dsId}/databases/{db}/tables/{table}/profile` | 数据画像 (空值率/分布/类型) |
| 14 | GET | `/datasources/{dsId}/databases/{db}/er-graph` | ER 关系图 (ECharts graph 配置) |
| 15 | POST | `/datasources/{dsId}/cache/invalidate` | 手动清除 Caffeine 缓存 |

### 7.4 文件导入 (IngestController — 6)

| # | Method | Path | 用途 |
|---|---|---|---|
| 16 | POST | `/ingest/upload` | 上传文件 (multipart, ≤100MB) → 返回 taskId |
| 17 | GET | `/ingest/tasks/{taskId}` | 任务状态 (PARSING/READY/FAILED) |
| 18 | GET | `/ingest/tasks/{taskId}/quality` | 质量报告 |
| 19 | GET | `/ingest/tasks/{taskId}/preview` | 预览前 100 行 |
| 20 | POST | `/ingest/tasks/{taskId}/reparse` | 修改解析参数重新解析 |
| 21 | GET | `/ingest/tasks` | 历史任务列表 (按 userId) |

### 7.5 NL2SQL (Nl2SqlController — 4)

| # | Method | Path | 用途 |
|---|---|---|---|
| 22 | POST | `/nlsql/ask` | 自然语言 → SQL (含安全校验) |
| 23 | POST | `/nlsql/explain` | 给定 SQL, LLM 解释 (教学/审核) |
| 24 | POST | `/nlsql/feedback` | 用户反馈修改后 SQL (训练样本) |
| 25 | GET | `/nlsql/history` | 调用历史 (按 userId 分页) |

### 7.6 SQL 执行 (QueryController — 3)

| # | Method | Path | 用途 |
|---|---|---|---|
| 26 | POST | `/query/execute` | 安全执行 SELECT (≤1000 行) |
| 27 | POST | `/query/stream` | 流式执行 (大结果集, NDJSON) |
| 28 | POST | `/query/dry-run` | 仅校验不执行 (EXPLAIN) |

### 7.7 报告 (ReportController — 4)

| # | Method | Path | 用途 |
|---|---|---|---|
| 29 | POST | `/reports/generate` | SQL + 元数据 → Markdown 报告 |
| 30 | GET | `/reports/{reportId}` | 报告详情 (含 chart options) |
| 31 | GET | `/reports/{reportId}/markdown` | 导出 .md |
| 32 | GET | `/reports` | 历史报告列表 |

> **总计 32 个**, 已超过 30 的最低要求。如需精简可合并 19/20/21 三个 ingest 任务相关, 但保留更符合 RESTful 设计。

---

## 8. 与现有 14 个模块的集成

### 8.1 端口分配

| 模块 | 端口 | 模块 | 端口 |
|---|---|---|---|
| minimax-gateway | 8080 | minimax-multimodal | 8088 |
| minimax-auth | 8081 | minimax-monitor | 8089 |
| minimax-chat | 8082 | minimax-agent | 8090 |
| minimax-model | 8083 | minimax-prompt | 8091 |
| minimax-memory | 8084 | minimax-admin | 8092 |
| minimax-rag | 8085 | minimax-function | 8093 |
| minimax-ws | 8086 | **(空)** | 8094 |
| minimax-common | (无 server) | **(空)** | 8095 |
| | | **minimax-analytics (本模块)** | **8096** |

注: 当前空闲 8094/8095/8096, 本模块占用 8096 保留未来扩展余地。

### 8.2 Nacos 注册

`application.yml` (本模块) 新增:

```yaml
spring:
  application:
    name: minimax-analytics
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_HOST:127.0.0.1}:${NACOS_PORT:8848}
        namespace: ${NACOS_NS:minimax-dev}
        username: ${NACOS_USER:nacos}
        password: ${NACOS_PASS:nacos}
        register-enabled: true
        metadata:
          version: 5.31.0
          group: minimax
          capabilities: "nlsql,schema,ingest,report,chart"
```

### 8.3 Gateway 路由

**修改 `minimax-gateway/src/main/resources/application.yml`** 在 `routes:` 块新增:

```yaml
    - id: analytics
      uri: lb://minimax-analytics
      predicates:
        - Path=/api/v1/analytics/**,/api/analytics/**
      filters:
        - StripPrefix=2
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 30
            redis-rate-limiter.burstCapacity: 60
            key-resolver: "#{@userKeyResolver}"
```

公开路径 (JWT 校验白名单) 在 gateway 的 `minimax.jwt.gateway-auth.public-paths` 里**不**新增, 默认全部需要鉴权; 但若 `/api/v1/analytics/health` 需健康检查直通, 可追加 `- /api/v1/analytics/health`。

### 8.4 与其他模块的协作

| 目标模块 | 调用方式 | 场景 |
|---|---|---|
| **minimax-model** | 直接 `import ModelProviderFactory` (同 JVM 跨模块 import) | NL2SQL 调 LLM |
| **minimax-prompt** | OpenFeign `lb://minimax-prompt` (可选) | 拉取 Prompt 模板 (替代 §11 中内置) |
| **minimax-admin** | OpenFeign (可选) | 操作审计日志上行 |
| **minimax-monitor** | Micrometer 自动 | `minimax_analytics_query_total` 等指标 |
| **minimax-rag** | 可选: 把"表结构文档"灌进 RAG 知识库 | 用 `/datasources/{dsId}/databases/{db}/profile` 当 source |

---

## 9. 单元测试覆盖目标

### 9.1 覆盖率指标

| 子模块 | 目标行覆盖 | 目标分支覆盖 |
|---|---|---|
| datasource | 75% | 65% |
| schema | 70% | 60% |
| ingest (parsers) | 85% | 75% |
| nlsql (SqlSafetyChecker) | **95%** | **90%** (安全关键) |
| query | 80% | 70% |
| report | 70% | 60% |
| chart | 80% | 70% |
| controller | 60% (集成测试覆盖) | 50% |
| **整体** | **≥ 75%** | **≥ 65%** |

### 9.2 必测用例清单 (重点)

| 测试类 | 关键用例 | 数量 |
|---|---|---|
| `SqlSafetyCheckerTest` | 黑名单 14 类关键字、单条/多条、表名白名单命中/拒绝、注释绕过、无 LIMIT 自动追加 | 25 |
| `CsvParserTest` | UTF-8/GBK 编码、分隔符推断、引号转义、表头推断、空值 | 12 |
| `JsonParserTest` | 顶层数组、嵌套数组 (jsonPath 提取)、空数组、非法 JSON | 8 |
| `LogParserTest` | logback 模式、nginx 模式、自定义正则、跨行堆栈 | 6 |
| `EncodingDetectorTest` | UTF-8/GBK/ISO-8859-1 自动检测 | 4 |
| `SchemaServiceTest` | H2 模拟元数据查询、画像统计准确性 | 8 |
| `Nl2SqlServiceTest` | Mock LLM 返回值、prompt 拼接、retry 逻辑 | 6 |
| `QueryServiceTest` | 超时 kill、行数截断、prepared statement 注入防护 | 6 |
| `TrendAnalyzerTest` | 上升/下降/平稳判定、同比环比、forecast 边界 | 8 |
| `AnomalyDetectorTest` | IQR 边界、z-score 边界、空数据 | 5 |
| `ChartTypeDeciderTest` | 决策矩阵全分支 (12 种组合) | 12 |
| `DataSourceServiceTest` | 密码加密/解密、动态注册/移除、连接池指标 | 6 |
| `AnalyticsEndToEndTest` | `@SpringBootTest` + H2, 跑"上传 CSV → NL2SQL → 报告"全链路 | 4 |
| **合计** | | **≥ 110 用例** |

### 9.3 测试隔离

- 数据源相关测试用 **H2 in-memory** (MySQL 兼容模式)
- LLM 调用全部 **Mock** (`@MockBean ModelProviderFactory`)
- 不依赖真实 Nacos / Redis (用 `spring.profiles.active=test` + 简化配置)

---

## 10. 工作量估算

### 10.1 代码量 (LoC, 不含注释/空行)

| 子模块 | 实体 | DTO/VO | Service | Controller | 配置/Util | 测试 | 小计 |
|---|---|---|---|---|---|---|---|
| datasource | 80 | 200 | 600 | 200 | 150 | 400 | **1630** |
| schema | 0 | 400 | 700 | 250 | 100 | 350 | **1800** |
| ingest | 80 | 300 | 900 | 250 | 200 | 600 | **2330** |
| nlsql | 0 | 250 | 500 | 200 | 100 | 500 | **1550** |
| query | 0 | 200 | 400 | 150 | 100 | 250 | **1100** |
| report | 0 | 350 | 800 | 200 | 200 | 500 | **2050** |
| chart | 0 | 200 | 350 | 100 | 100 | 250 | **1000** |
| common (启动/配置/异常) | 100 | 0 | 0 | 0 | 300 | 100 | **500** |
| **合计** | **260** | **1900** | **4250** | **1350** | **1250** | **2950** | **~11,960** |

加 20% buffer (注释/sql/yml) ≈ **14,500 LoC**

### 10.2 实现时间 (按 1 个中级 Java 开发)

| 阶段 | 内容 | 人天 |
|---|---|---|
| Day 1 | 模块脚手架 + pom + application.yml + Nacos + 启动验证 | 1 |
| Day 2 | datasource 子模块 (CRUD + 动态池 + 加密) | 1 |
| Day 3 | schema 子模块 (information_schema + 画像 + ER) | 1.5 |
| Day 4 | ingest 子模块 (3 解析器 + 质量报告) | 1.5 |
| Day 5 | SqlSafetyChecker (五道防线 + Druid Parser) + QueryService | 1.5 |
| Day 6 | nlsql 子模块 (Prompt + LLM 集成 + 自校 retry) | 1.5 |
| Day 7 | report 子模块 (freeMarker 模板 + Trend + Anomaly) | 1.5 |
| Day 8 | chart 子模块 (ECharts 自动选图) | 1 |
| Day 9 | gateway 路由 + Knife4j 文档完善 + 跨模块联调 | 1 |
| Day 10 | 单元测试 + 集成测试 (≥110 用例) + bug 修复 | 1.5 |
| Day 11 | buffer / code review / 文档 (CHANGELOG/PROGRESS) | 1 |
| **合计** | | **13 人天 (~2.5 周)** |

如**不引入 PDF 导出** (见 §11) 可省 0.5 天; 如**复用 minimax-prompt** 替代内置模板可省 0.5 天。

---

## 11. 待用户决策 (设计 review)

| # | 决策点 | 选项 A (推荐) | 选项 B |
|---|---|---|---|
| 1 | Prompt 模板存放 | **走 `minimax-prompt` 模块** (统一管理, 支持版本/热更新) | 本模块内置 `PromptTemplates.java` (简单, 启动快) |
| 2 | 报告 PDF 导出 | **不实现** (前端用 `markdown-it` + `html2pdf.js`) | 后端引入 `openhtmltopdf` (~5MB 依赖) |
| 3 | CSV 解析 | **hutool `CsvUtil`** (零新增依赖) | `opencsv 5.9` (功能更强, 需父 pom 锁版本) |
| 4 | 异常检测算法 | **仅 IQR** (无 ML 依赖, 性能好) | 加 `smile-core` Isolation Forest (精度高, 依赖 +2MB) |
| 5 | 多租户隔离 | **按 userId 过滤** (简单, 与现有模块一致) | 接入 `minimax-common/tenant` (复杂, V5.x 已有基础) |
| 6 | 鉴权粒度 | **JWT 即可** (复用 common JwtAuthenticationFilter) | 加 RBAC 角色校验 (需 admin 模块配合) |

---

## 12. 数据库 DDL 摘要 (供 SQL 迁移)

```sql
-- 4 张业务表, 与其他模块同库 minimax_platform
CREATE TABLE analytics_data_source (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  jdbc_url VARCHAR(512) NOT NULL,
  username VARCHAR(128),
  password_enc VARCHAR(512),
  driver_class VARCHAR(256) DEFAULT 'com.mysql.cj.jdbc.Driver',
  db_type VARCHAR(32) DEFAULT 'mysql',
  pool_max_size INT DEFAULT 10,
  pool_min_idle INT DEFAULT 2,
  table_prefix_whitelist VARCHAR(1024),
  read_only TINYINT(1) DEFAULT 1,
  owner_user_id BIGINT NOT NULL,
  status VARCHAR(16) DEFAULT 'ACTIVE',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_owner (owner_user_id, deleted)
) ENGINE=InnoDB COMMENT='数据分析-数据源配置';

CREATE TABLE analytics_ingest_task (
  id VARCHAR(32) PRIMARY KEY,           -- taskId (UUID)
  user_id BIGINT NOT NULL,
  filename VARCHAR(256),
  size_bytes BIGINT,
  format VARCHAR(16),                    -- CSV/JSON/LOG
  options_json TEXT,
  status VARCHAR(16),                    -- UPLOADED/PARSING/READY/FAILED
  row_count INT,
  col_count INT,
  error_message VARCHAR(1024),
  file_path VARCHAR(512),                -- 本地存储路径
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_user (user_id, status, created_at)
) ENGINE=InnoDB COMMENT='数据分析-文件导入任务';

CREATE TABLE analytics_report (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  data_source_id BIGINT,
  title VARCHAR(256),
  sql_text TEXT,
  template_type VARCHAR(32),             -- STANDARD/TREND/DISTRIBUTION
  markdown MEDIUMTEXT,
  chart_config_json MEDIUMTEXT,
  row_count INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='数据分析-生成的报告';

CREATE TABLE analytics_query_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  data_source_id BIGINT,
  question VARCHAR(1024),
  generated_sql TEXT,
  model_code VARCHAR(64),
  prompt_tokens INT,
  completion_tokens INT,
  latency_ms BIGINT,
  status VARCHAR(16),                    -- OK/REJECTED/FAILED
  reject_reason VARCHAR(256),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_time (user_id, created_at),
  KEY idx_status (status, created_at)
) ENGINE=InnoDB COMMENT='数据分析-NL2SQL调用日志';
```

---

## 13. 风险与回滚

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| LLM 生成的 SQL 注入/越权 | 中 | 高 | §4 六道防线, 默认拒绝一切非 SELECT |
| 大数据源 (亿级表) 元数据读取慢 | 中 | 中 | Caffeine 1h 缓存 + 分页/搜索限制 |
| 大文件上传 OOM | 低 | 高 | 流式解析 + 100MB 硬上限 (application-common.yml 已配) |
| 多数据源连接耗尽 | 中 | 中 | HikariCP 池上限 + 业务库 20 连接 / 单租户 Bucket4j 限流 |
| 跨服务链路 (analytics → model → LLM) 延迟 | 高 | 中 | Resilience4j 超时 5s + 重试 1 次 + 降级返回"服务繁忙" |
| Prompt 模板变更影响线上 | 低 | 中 | (若选 A) `minimax-prompt` 自带版本号 + 灰度 |
| 报告 PDF 渲染 CPU 高 | — | — | 已决策不引入 PDF, 走前端 |

---

## 附录 A: 一次完整的 NL2SQL → 报告 时序

```
[Frontend] POST /api/v1/analytics/nlsql/ask {dsId:1, db:"minimax", question:"最近7天注册用户数"}
   ↓ (gateway 路由 + JWT)
[Analytics] Nl2SqlController.ask
   ↓
[Analytics] Nl2SqlService.nl2sql
   ├─ 1. SchemaService.buildContext(dsId, "minimax", hint=null)
   │     → Caffeine cache hit → 返回 schema 摘要 (压缩到 4000 tokens)
   ├─ 2. PromptTemplates 拼接 system + user + fewshots
   ├─ 3. ModelProviderFactory.get("minimax-m3").chat(...)
   │     → ChatResponse {content: "{\"sql\":\"SELECT ...\", \"explanation\":\"...\", \"confidence\":0.92}"}
   ├─ 4. SqlSafetyChecker.check(sql, whitelist=["minimax_"])
   │     → SafetyResult.ok(rewrittenSql)
   ├─ 5. QueryService.execute
   │     → SqlResultVO {columns:[d,c], rows:7, latencyMs:42}
   ├─ 6. ReportService.generate (templateType=TREND)
   │     ├─ TrendAnalyzer.analyze(rows, "d", "c") → TrendVO{direction:"UP", slope:+12.3}
   │     ├─ AnomalyDetector.detect(rows, "c") → []  (无异常)
   │     ├─ ChartService.recommend → ChartConfigVO{type:LINE, option:{...}}
   │     ├─ freeMarker 渲染 templates/report/trend.md.ftl → markdown 字符串
   │     └─ 持久化 AnalyticsReport
   └─ 7. 返回 Nl2SqlResultVO {sql, explanation, confidence, latencyMs, modelUsed, tokenUsage, previewReportId}
   ↓
[Frontend] 拿到结果, 弹窗"是否生成完整报告?" → 用户点确认 → GET /api/v1/analytics/reports/{reportId}
   → 渲染 markdown + ECharts 图
```

## 附录 B: 一句话总结

**minimax-analytics = 多数据源连接管理 + 元数据画像 + 文件导入 + LLM 驱动的 NL2SQL + 自动图表 + Markdown 报告**, 复用 minimax-model 的 LLM 适配器和 minimax-common 的鉴权/响应包装, 是 15 个微服务中最"数据密集 + AI 驱动"的模块, 实现工期约 13 人天, 代码量约 1.45 万 LoC (含测试)。
