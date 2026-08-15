# SQL 函数/视图调用情况 (V6.8.1+)

## 现状: 后端 0 处调用 ⚠️

刚才搜了 77 个 Entity + 所有 Service + 所有 Mapper XML:
```bash
grep -rn "fn_calculate_token_cost\|fn_intent_match_score\|fn_user_active_days\|fn_tenant_usage_percent" backend/ --include="*.java"
# → 0 结果

grep -rn "v_user_profile\|v_chat_session_stats\|v_tenant_usage\|v_active_tools" backend/ --include="*.java" --include="*.xml"
# → 0 结果
```

**我加的 4 函数 + 4 视图, 后端 0 处 wire up。**

## 怎么用 (待补)

### 1. 函数调用 (3 种方式)

#### 方式 A: Mapper XML `<select>` 调函数
```xml
<mapper namespace="com.minimax.ai.mapper.TokenMapper">
  <select id="calculateCost" resultType="java.math.BigDecimal">
    SELECT fn_calculate_token_cost(#{inputTokens}, #{outputTokens}, #{model})
  </select>
</mapper>
```

```java
public interface TokenMapper {
  BigDecimal calculateCost(@Param("inputTokens") int input, 
                           @Param("outputTokens") int output, 
                           @Param("model") String model);
}
```

```java
@Service
public class TokenService {
  private final TokenMapper tokenMapper;
  
  public BigDecimal estimate(String model, int input, int output) {
    return tokenMapper.calculateCost(input, output, model);
  }
}
```

#### 方式 B: 注解 `@Select` 直接调
```java
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
  @Select("SELECT fn_tenant_usage_percent(#{tenantId})")
  BigDecimal getUsagePercent(@Param("tenantId") Long tenantId);
}
```

#### 方式 C: 业务 Service 内联 (简单)
```java
@Autowired
private JdbcTemplate jdbc;

public BigDecimal getTenantUsage(Long id) {
  return jdbc.queryForObject(
    "SELECT fn_tenant_usage_percent(?)", BigDecimal.class, id
  );
}
```

### 2. 视图调用 (2 种方式)

#### 方式 A: Mapper XML `<select>` 查视图
```xml
<select id="selectUserProfile" resultMap="profileMap">
  SELECT * FROM v_user_profile WHERE user_id = #{userId}
</select>
```

#### 方式 B: 直接当表查
```java
@Select("SELECT * FROM v_active_tools WHERE enabled = 1")
List<Map<String, Object>> listActiveTools();
```

## 待办: 加 Service 调用

### 优先级 1 (核心)
- [ ] **TokenService.calculateCost()** - AI 调用计费 (fn_calculate_token_cost)
- [ ] **TenantService.getUsagePercent()** - 租户配额查询 (fn_tenant_usage_percent)

### 优先级 2 (统计)
- [ ] **UserProfileService** - 用户画像 (v_user_profile)
- [ ] **ChatStatsService** - 聊天统计 (v_chat_session_stats)
- [ ] **TenantUsageService** - 租户使用 (v_tenant_usage)
- [ ] **ToolStatsService** - 工具统计 (v_active_tools)

### 优先级 3 (低频)
- [ ] **IntentService.matchScore()** - 意图匹配 (fn_intent_match_score)
- [ ] **UserStatsService.activeDays()** - 活跃天数 (fn_user_active_days)

## 触发器 (trg_user_audit) 自动生效

触发器在 schema 部署后自动激活, 无需 Java 调用, 业务代码无侵入:
```sql
UPDATE sys_user SET deleted = 1 WHERE id = 5;
-- 自动插入 admin_audit_log 一条记录
```

## 文件清单

| 文件 | 位置 | 内容 |
|---|---|---|
| 函数定义 | `sql/minimax-mysql-final.sql` L1300+ | 4 个 fn_* 函数 |
| 视图定义 | `sql/minimax-mysql-final.sql` L1500+ | 4 个 v_* 视图 |
| 触发器定义 | `sql/minimax-mysql-final.sql` L1700+ | 2 个 trg_* 触发器 |

## 建议下一步

要 wire up 的话, 我可以加:
1. `TokenMapper` + `TokenService` (调 fn_calculate_token_cost)
2. `TenantMapper.getUsagePercent()` (调 fn_tenant_usage_percent)
3. `UserProfileMapper.selectById()` 改成查 v_user_profile 视图

或者你本地有特殊调用方式, 直接说。
