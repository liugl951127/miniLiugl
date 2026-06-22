package com.minimax.analytics.service.nlsql;

import java.util.List;
import java.util.Map;

/**
 * NL2SQL Prompt 模板 (V5.31)
 *
 * 策略: System 角色定规矩, User 角色提供 schema + 问题
 * Few-shot 3 个例子覆盖: 单表查询 / JOIN / 聚合
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    public static String system() {
        return """
            你是 SQL 专家助手. 用户会用自然语言提问, 你需要生成 MySQL 8.0 兼容的 SELECT 语句.
            要求:
              1. **只生成 SELECT**, 不要 INSERT/UPDATE/DELETE/DROP
              2. 使用提供的表结构 (CREATE TABLE) 来推断正确的列名和表名
              3. WHERE 条件要清晰, 时间字段用 DATE()/NOW()/INTERVAL 等函数
              4. 涉及分组时用 GROUP BY + 聚合函数 (COUNT/SUM/AVG)
              5. 排序用 ORDER BY, 限制用 LIMIT (默认 100)
              6. SQL 用反引号包裹标识符防止关键字冲突
              7. 解释为什么这样写 (1-2 句话)

            输出格式 (严格):
              SQL:
              ```sql
              <你的 SQL>
              ```
              解释: <1-2 句话说明>
            """;
    }

    public static String user(String question, List<Map<String, String>> tableSchemas) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 数据库 Schema\n");
        for (Map<String, String> t : tableSchemas) {
            sb.append("### 表 `").append(t.get("name")).append("`\n");
            sb.append("```sql\n").append(t.get("ddl")).append("\n```\n\n");
        }
        sb.append("## 用户问题\n").append(question).append("\n\n");
        sb.append("请按要求生成 SQL.");
        return sb.toString();
    }

    /** Few-shot 例子 (V5.31: 3 个常见模式) */
    public static String fewShot() {
        return """
            ## 示例 1: 单表查询
            问题: 2024 年注册的所有用户
            SQL:
            ```sql
            SELECT id, username, email, created_at
            FROM `user`
            WHERE `created_at` >= '2024-01-01' AND `created_at` < '2025-01-01'
            ORDER BY `created_at` DESC
            LIMIT 100
            ```
            解释: 用 created_at 范围限定 2024 年, 按时间倒序.

            ## 示例 2: 聚合
            问题: 统计每个角色的用户数
            SQL:
            ```sql
            SELECT `role`, COUNT(*) AS cnt
            FROM `user`
            GROUP BY `role`
            ORDER BY cnt DESC
            ```
            解释: 按 role 分组, COUNT 统计, 倒序.

            ## 示例 3: JOIN
            问题: 查询每个用户最新的一条订单
            SQL:
            ```sql
            SELECT u.username, o.id AS order_id, o.amount, o.created_at
            FROM `user` u
            JOIN `order` o ON o.user_id = u.id
            WHERE u.id IN (SELECT user_id FROM `order` GROUP BY user_id)
            ORDER BY u.id, o.created_at DESC
            ```
            解释: 子查询拿有订单的用户, JOIN 后取最新订单.
            """;
    }
}
