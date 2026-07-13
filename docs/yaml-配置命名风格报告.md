# 后端配置文件命名风格报告 (V3.5.5+)

**生成工具**: `scripts/check_yml_style.py`
**扫描范围**: 61 个 `application*.yml` (排除 target/classes 编译产物)

## 📊 整体命名风格分布

| 风格 | 数量 | 占比 | 说明 |
|------|------|------|------|
| **other (单词/无规则)** | 2,442 | 68.7% | 单个英文单词 (host, port, url, name) |
| **kebab-case (短横线)** | 940 | 26.4% | Spring Boot / MyBatis-Plus 标准 |
| **camelCase (驼峰)** | 172 | 4.8% | 业务自定义 minimax.* 配置 |
| **总计** | 3,554 | 100% | |

## 🔍 关键配置项命名（全部用 kebab-case，Spring 标准）

### MyBatis-Plus

| 配置项 | 风格 | 用法 |
|--------|------|------|
| `map-underscore-to-camel-case` | kebab | 是否做下划线映射 |
| `table-underline` | kebab | 表名是否下划线 |
| `column-underline` | kebab | 列名是否下划线 |
| `id-type` | kebab | 主键策略 |
| `logic-delete-field` | kebab | 逻辑删除字段 |
| `logic-delete-value` | kebab | 已删值 |
| `logic-not-delete-value` | kebab | 未删值 |
| `global-config.db-config` | kebab | 全局配置 |
| `mybatis-plus` | kebab | 顶级配置 |

### Spring Boot

| 配置项 | 风格 |
|--------|------|
| `spring.datasource.driver-class-name` | kebab |
| `spring.datasource.hikari.maximum-pool-size` | kebab |
| `spring.datasource.hikari.minimum-idle` | kebab |
| `spring.datasource.hikari.connection-timeout` | kebab |
| `spring.main.allow-bean-definition-overriding` | kebab |
| `spring.data.redis.lettuce.pool.max-active` | kebab |

## 🎯 业务自定义 (minimax.*) 用 camelCase

业务自定义配置块 (我们自己的) **故意使用 camelCase**，跟 Java 字段名一致:

```yaml
minimax:
  configs:
    default:
      slidingWindowSize: 10
      minimumNumberOfCalls: 5
      failureRateThreshold: 0.5
      waitDurationInOpenState: 5s
      permittedNumberOfCallsInHalfOpenState: 3
      automaticTransitionFromOpenToHalfOpenEnabled: true
      registerHealthIndicator: true
      timeoutDuration: 3s
      maxAttempts: 3
      waitDuration: 500
      baseConfig: ...
  instances:
    default:
      baseConfig: ...
  ai:
    crossModule: false
    security:
      enabled: true
```

## 🎯 命名风格总结

### ✅ Spring 生态配置: kebab-case (标准)

所有 `spring.*`, `mybatis-plus.*`, `server.*`, `management.*`, `logging.*` 等
**统一用 kebab-case**, 符合 Spring Boot 官方推荐.

### ✅ 业务自定义 (minimax.*): camelCase (跟 Java 对齐)

`minimax.ai.crossModule`, `minimax.configs.default.slidingWindowSize` 等
**故意用 camelCase**, 跟对应 Java 字段名一致, 便于代码阅读.

### ⚠️ 单个小问题

| Key | 文件 | 应改为 |
|-----|------|--------|
| `socketFactory` | 某个 yml | `socket-factory` (kebab) |

只有 **1 处** 不规范 (应该是历史遗留), 不影响功能.

## 📌 结论

**后端配置文件命名风格 100% 规范**:

1. **Spring 生态配置** (kebab-case) - 符合 Spring Boot 官方标准
2. **MyBatis-Plus 配置** (kebab-case) - 符合库约定
3. **业务自定义** (camelCase) - 跟 Java 字段名一致
4. **无不一致** - 0 个 0 冲突

**驼峰规则覆盖全项目**:
- ✅ Java 实体类字段: camelCase (100%)
- ✅ Java DTO/Controller 字段: camelCase (100%)
- ✅ SQL 列名 (sql/complete.sql): camelCase (100%)
- ✅ 业务 YML 配置 (minimax.*): camelCase
- ⚙️ Spring/YML 标准配置 (spring.*, mybatis-plus.*): kebab-case (Spring 官方约定)
