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
            重要: 本数据库使用**驼峰命名** (camelCase), 例如: userId, orderDate, totalAmount, createdAt
            要求:
              1. **只生成 SELECT**, 不要 INSERT/UPDATE/DELETE/DROP/TRUNCATE/ALTER
              2. 使用提供的表结构 (CREATE TABLE) 来推断正确的列名和表名
              3. WHERE 条件要清晰, 时间字段用 DATE()/NOW()/INTERVAL 等函数
              4. 涉及分组时用 GROUP BY + 聚合函数 (COUNT/SUM/AVG/MAX/MIN)
              5. 排序用 ORDER BY, 限制用 LIMIT (默认 100)
              6. SQL 用反引号包裹标识符防止关键字冲突
              7. 解释为什么这样写 (1-2 句话)
              8. **字段名必须用驼峰**, 如 `userId`, `orderDate`, `payAmount`, `productName`

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

    /** Few-shot 例子 (V5.31: 3 个常见模式, 驼峰字段) */
    public static String fewShot() {
        return """
            ## 示例 1: 单表查询
            问题: 2025年8月所有已完成的订单
            SQL:
            ```sql
            SELECT `orderId`, `userId`, `totalAmount`, `orderStatus`, `orderDate`
            FROM `demo_order`
            WHERE `orderDate` >= '2025-08-01' AND `orderDate` < '2025-09-01'
              AND `orderStatus` = 'COMPLETED'
            ORDER BY `orderDate` DESC
            LIMIT 100
            ```
            解释: 用 orderDate 范围限定 2025年8月, 筛选 COMPLETED 状态, 按日期倒序.

            ## 示例 2: 聚合统计
            问题: 统计每个城市的用户数量
            SQL:
            ```sql
            SELECT `city`, COUNT(*) AS userCount
            FROM `demo_user`
            GROUP BY `city`
            ORDER BY userCount DESC
            LIMIT 20
            ```
            解释: 按 city 分组统计用户数, 倒序取前20.

            ## 示例 3: JOIN 多表查询
            问题: 查询买了商品的单价和购买数量
            SQL:
            ```sql
            SELECT oi.`productName`, oi.`unitPrice`, oi.`quantity`, oi.`totalAmount`,
                   o.`orderId`, o.`orderDate`, o.`orderStatus`
            FROM `demo_order_item` oi
            JOIN `demo_order` o ON o.`orderId` = oi.`orderId`
            WHERE o.`orderStatus` = 'COMPLETED'
            ORDER BY oi.`totalAmount` DESC
            LIMIT 100
            ```
            解释: JOIN 订单明细和订单表, 取已完成订单的商品购买情况.
            """;
    }
}
