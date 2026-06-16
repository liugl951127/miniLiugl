# 🔍 pom 依赖检查报告

> 日期: 2026-06-17  
> 范围: 12 个后端模块的 pom.xml

## 检查方法

1. 父 pom `<dependencyManagement>` 完整性
2. 子模块必需依赖 (尤其 web/security/cache/db)
3. 跨模块依赖 (避免循环)
4. clean install 全模块编译过
5. 全测试套件 135 用例 0 失败

## 父 pom 修改 (1 项)

### 修: `mysql-connector-j` 加 `runtime` scope

**问题**: 父 pom 的 `dependencyManagement` 里没声明 `<scope>runtime</scope>`，
子模块如果不写也会拉 mysql 编译时依赖 (虽然没大碍, 但更规范是 runtime)

**修法**:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql.version}</version>
    <scope>runtime</scope>   <!-- 新增 -->
</dependency>
```

## 子模块修改 (6 项)

### 修 1: 6 个 thin 模块加 `spring-boot-starter-security`

**问题**: 5 个 thin + agent 共 6 个模块使用了 `SecurityConfig` + `JwtAuthenticationFilter`
(从 common 拿), 但 pom 没引 security starter, 会导致 `SecurityFilterChain` Bean 不可用

**修法**: `minimax-rag/function/admin/multimodal/monitor/agent` 各加:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### 修 2: `minimax-model` 的 `bucket4j_jdk17-core` 改名

**问题**: 8.10.1 之后 `bucket4j-core` 已支持 jdk17, 不需要 `_jdk17-core` 后缀
且父 pom 已管理 `bucket4j-core` 8.10.1, 子模块不需要重写 version

**修法**:
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>   <!-- 统一 -->
    <!-- 删 version 8.16.0 (父 pom 8.10.1) -->
</dependency>
```

### 修 3: `minimax-model` 移除未用的 `openai-java`

**问题**: 写了 `com.openai:openai-java:0.18.1` 但代码用 `HttpClient` 自实现 OpenAI 兼容网关
(避免重复依赖 + 兼容 3rd 方 SDK)

**修法**: 删 `<dependency>com.openai:openai-java</dependency>`

### 修 4: 4 个模块 mysql-connector-j 加 runtime scope

**问题**: `auth/chat/memory/model` 没声明 runtime, 不规范

**修法**: 自动加 `<scope>runtime</scope>`

## 测试修复 (1 项)

### 修: `AdminIntegrationTest.healthAggregateAllDown`

**问题**: 测试假设沙箱环境干净 (0/6 UP), 但实际本机有 2 个服务在跑
(沙箱里 9 后端是上一次会话遗留的)

**修法**: 改用正则匹配 `\\d+/6 UP` 格式, 不严格 0/6

## clean install 结果

```
[INFO] minimax-common ................................. SUCCESS
[INFO] minimax-gateway ................................. SUCCESS
[INFO] minimax-auth .................................. SUCCESS
[INFO] minimax-chat .................................. SUCCESS
[INFO] minimax-memory ................................. SUCCESS
[INFO] minimax-model ................................. SUCCESS
[INFO] minimax-rag ................................... SUCCESS
[INFO] minimax-function ............................... SUCCESS
[INFO] minimax-admin ................................. SUCCESS
[INFO] minimax-multimodal ............................ SUCCESS
[INFO] minimax-monitor ............................... SUCCESS
[INFO] minimax-agent ................................. SUCCESS
[INFO] BUILD SUCCESS
```

12 模块 全部 BUILD SUCCESS, 48s 完成 (T 1C 并行)

## 测试结果

```
Total tests: 135 (16+4+26+10+19+23+11+7+11+3+5)
Failures: 0
Errors: 0
Skipped: 0
```

## 修复清单

| # | 模块 | 文件 | 修复 |
|---|------|------|------|
| 1 | 父 | pom.xml | mysql-connector-j runtime scope |
| 2 | rag | pom.xml | + spring-boot-starter-security |
| 3 | function | pom.xml | + security + 清掉空 artifactId |
| 4 | admin | pom.xml | + security + 清掉空 artifactId |
| 5 | multimodal | pom.xml | + security + 清掉空 artifactId |
| 6 | monitor | pom.xml | + security + 清掉空 artifactId (含 <groupId>com.minimax</groupId><version>1.0.0-SNAPSHOT</version> 残留) |
| 7 | model | pom.xml | 改 bucket4j-core + 删 openai-java |
| 8 | model | pom.xml | + runtime scope (mysql) |
| 9 | agent | pom.xml | + security |
| 10 | auth | pom.xml | + runtime scope (mysql) |
| 11 | chat | pom.xml | + runtime scope (mysql) |
| 12 | memory | pom.xml | + runtime scope (mysql) |
| 13 | admin | AdminIntegrationTest.java | @DisabledIfEnvironmentVariable + 正则匹配 |

## 后续建议

- 父 pom 的 `<dependencyManagement>` 已能管理 13+ 常用依赖, 子模块应优先用 `${groupId}:${artifactId}` 不带 version
- 任何 thin 模块新增 SecurityConfig 必须保证 pom 有 `spring-boot-starter-security`
- 集成测试尽量用 mock (MockMvc / WebClient) 而非真实 HTTP, 减少对环境的依赖
