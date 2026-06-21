# MiniMax 数据库分库分表指南 (V5.20 规划)

> 41 张表已稳定运行, 中大规模需要分库分表时参考本文档

## 1. 当前架构 (单库单服务)

```
[12 微服务] → [MySQL minimax_platform] (单实例)
41 张表:
  - auth (10): sys_user, sys_role, sys_user_role, oauth_*, wechat_*, unionid_*, refresh_token
  - chat (3): chat_session, chat_message
  - model (4): model_provider, model_config, model_battle_log, model_quota
  - memory (3): memory_item, memory_summary, memory_preference
  - rag (3): rag_kb, rag_doc, rag_chunk
  - function (3): function_tool, function_log, function_invocation
  - admin (1): admin_audit_log
  - monitor (3): alert_rule, alert_event, metric_snapshot
  - notification (1): notification
  - 其他 (10): ...
```

## 2. 分库分表策略

### 2.1 按业务垂直拆分 (推荐)

```
db_auth    (10 张表, ~5GB)  - 用户/认证/权限
db_chat    (3 张表, ~20GB)  - 会话/消息 (增长最快)
db_memory  (3 张表, ~10GB)  - 短期/长期记忆
db_rag     (3 张表, ~50GB)  - 知识库 + 向量 (最大)
db_model   (4 张表, ~2GB)   - 模型/调用日志
db_monitor (3 张表, ~1GB)   - 告警/指标
db_admin   (1 张表, ~500MB)  - 审计日志
db_func    (3 张表, ~2GB)   - 函数工具
db_notif   (1 张表, ~200MB)  - 通知
```

### 2.2 按租户水平拆分 (多租户)

```sql
-- chat_message 按 userId 取模分 16 张表
chat_message_00  -- user_id % 16 = 0
chat_message_01  -- user_id % 16 = 1
...
chat_message_15  -- user_id % 16 = 15

-- 用 MyBatis-Plus ShardingSphere-JDBC 自动路由
```

### 2.3 按时间水平拆分 (时序数据)

```sql
-- alert_event 按月分表
alert_event_202606  -- 当前月
alert_event_202607  -- 下个月
alert_event_202608
...
-- 用 ScheduledTask 自动建表 + 归档
```

## 3. ShardingSphere-JDBC 接入 (V6.x 计划)

### 3.1 加依赖

```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
    <version>5.4.1</version>
</dependency>
```

### 3.2 application.yml 配置

```yaml
spring:
  shardingsphere:
    rules:
      sharding:
        tables:
          chat_message:
            actual-data-nodes: db_chat.chat_message_${0..15}
            table-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user-mod-16
        sharding-algorithms:
          user-mod-16:
            type: MOD
            props:
              sharding-count: 16
    datasource:
      names: db_chat,db_memory,...
      db_chat:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://db-host:3306/db_chat
        ...
```

### 3.3 业务代码 0 改动

```java
// MyBatis-Plus 原生用法, ShardingSphere 自动路由
chatMessageMapper.insert(new ChatMessage(...));   // 自动写到 chat_message_07
chatMessageMapper.selectById(123L);                // 自动从 chat_message_07 读
```

## 4. 迁移步骤

1. **双写阶段** (1-2 周):
   - 应用同时写老库 + 新库
   - 验证数据一致性
2. **读切换** (1 周):
   - 切读流量到新库
   - 监控延迟/错误率
3. **老库下线** (2 周后):
   - 停止老库写入
   - 归档老库数据 (保留 3 个月)
   - 完全切换

## 5. 现有准备

V5.20 已做的准备:
- ✅ 每个模块的 yml 用 `${MYSQL_HOST}` `${MYSQL_PORT}` 环境变量 (可指向不同 DB)
- ✅ 12 业务模块 + common 都用 `spring.datasource` 标准配置
- ✅ SqlSessionFactory 用 MyBatis-Plus, 容易切换 ShardingSphere
- ✅ admin 模块的 ServiceEndpoints 已支持按服务 URL (后续可加 DB URL)

## 6. 何时做分库分表

| 规模 | 单表行数 | DB 大小 | 建议 |
|------|---------|---------|------|
| 小 | < 1000 万 | < 50GB | 不需要 |
| 中 | 1000 万-1 亿 | 50-500GB | 垂直拆分 |
| 大 | > 1 亿 | > 500GB | 垂直 + 水平 |
| 巨 | > 10 亿 | > 5TB | 水平 + 分库 + 读写分离 |

## 7. V6.x 计划

- V6.1: ShardingSphere-JDBC 接入 (chat_message 水平分 16 张)
- V6.2: 垂直分库 (按业务)
- V6.3: 读写分离 (主从)
- V6.4: 分布式事务 (Seata)
- V6.5: 数据归档 (旧数据 → ClickHouse / HDFS)