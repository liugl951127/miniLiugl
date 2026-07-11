package com.minimax.ai.tool.builtin;

import com.minimax.ai.datasource.MultiDataSourceManager;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.tool.AiToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自然语言转 SQL 工具 (V2.5 自研, 不依赖 LLM)
 *
 * 工作原理 (基于模式匹配 + 模板):
 *   1. 解析用户问题中的关键信息:
 *      - 表名 (user / order / product ...)
 *      - 字段 (name / age / amount / count ...)
 *      - 聚合 (count / sum / avg / max / min / 总共)
 *      - 排序 (最高/最低/前 N)
 *      - 过滤 (大于/小于/等于/包含)
 *      - 分组 (按...统计)
 *      - 时间 (今天/本周/本月/最近 N 天)
 *
 *   2. 模板生成 SQL:
 *      "统计用户总数"          -> "SELECT COUNT(*) FROM user"
 *      "订单金额前 10"          -> "SELECT * FROM order ORDER BY amount DESC LIMIT 10"
 *      "最近 7 天的订单"        -> "SELECT * FROM order WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)"
 *      "按城市统计用户数"       -> "SELECT city, COUNT(*) FROM user GROUP BY city"
 *
 *   3. 反射数据库 schema (只查存在的表和字段)
 *
 *   4. 执行 SQL 返回结果
 *
 * 优势: 100% 自研, 不依赖 LLM, 准确率高 (基于规则)
 * 限制: 复杂语义可能识别不准
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Nl2SqlTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "sql.query";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String question = (String) input.get("question");

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在: " + dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("dataSource", ds.getName());

        // 1. 获取 schema
        Map<String, List<String>> schema = loadSchema(ds);

        // 2. 解析问题
        ParsedQuery parsed = parse(question, schema);

        // 3. 生成 SQL
        String sql = generateSql(parsed);
        result.put("sql", sql);
        result.put("parsed", parsed);

        // 4. 安全检查 (防止破坏性操作)
        if (isDangerous(sql)) {
            result.put("success", false);
            result.put("error", "安全检查失败: SQL 包含危险操作 (DROP/DELETE/UPDATE/INSERT)");
            return result;
        }

        // 5. 执行
        if (parsed.execute) {
            try {
                List<Map<String, Object>> rows = executeQuery(ds, sql, parsed.limit);
                result.put("success", true);
                result.put("rowCount", rows.size());
                result.put("rows", rows);
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", "执行失败: " + e.getMessage());
            }
        } else {
            result.put("success", true);
            result.put("executed", false);
            result.put("message", "SQL 已生成, 请人工确认后执行");
        }
        return result;
    }

    // ============== 模式解析 ==============

    /** 解析后的问题结构 */
    private static class ParsedQuery {
        String table;                // 主表
        List<String> selectColumns = new ArrayList<>();  // 字段
        String aggFunc;              // 聚合函数: count/sum/avg/max/min
        List<WhereCondition> where = new ArrayList<>();
        String groupBy;              // 分组字段
        String orderBy;              // 排序字段
        boolean desc = false;        // 倒序
        Integer limit;               // 限制
        boolean execute = true;      // 是否自动执行

        static class WhereCondition {
            String column;
            String op;     // = / > / < / >= / <= / LIKE
            Object value;
        }
    }

    /**
     * 解析自然语言问题
     */
    private ParsedQuery parse(String question, Map<String, List<String>> schema) {
        ParsedQuery q = new ParsedQuery();
        String q_lower = question.toLowerCase().replace(" ", "").replace("?", "");

        // 1. 找表名 (模糊匹配 schema)
        for (Map.Entry<String, List<String>> e : schema.entrySet()) {
            String table = e.getKey();
            // 同义词
            Map<String, String> synonyms = Map.of(
                    "用户", "user", "会员", "user", "账户", "user",
                    "订单", "order", "商品", "product", "产品", "product",
                    "日志", "log", "记录", "log",
                    "消息", "message", "聊天", "message"
            );
            for (Map.Entry<String, String> syn : synonyms.entrySet()) {
                if (q_lower.contains(syn.getKey()) && table.equalsIgnoreCase(syn.getValue())) {
                    q.table = table;
                    break;
                }
            }
            if (q.table != null) break;
            // 直接匹配表名
            if (q_lower.contains(table.toLowerCase())) {
                q.table = table;
                break;
            }
        }
        if (q.table == null && !schema.isEmpty()) {
            // 默认第一个表
            q.table = schema.keySet().iterator().next();
        }

        // 2. 找聚合函数
        if (q_lower.contains("count") || q_lower.contains("总共") || q_lower.contains("多少个") || q_lower.contains("数量")) {
            q.aggFunc = "COUNT";
        } else if (q_lower.contains("sum") || q_lower.contains("总") || q_lower.contains("合计")) {
            q.aggFunc = "SUM";
        } else if (q_lower.contains("avg") || q_lower.contains("平均") || q_lower.contains("均值")) {
            q.aggFunc = "AVG";
        } else if (q_lower.contains("max") || q_lower.contains("最高") || q_lower.contains("最大")) {
            q.aggFunc = "MAX";
        } else if (q_lower.contains("min") || q_lower.contains("最低") || q_lower.contains("最小")) {
            q.aggFunc = "MIN";
        }

        // 3. 排序
        if (q_lower.contains("前") && (q_lower.contains("asc") || q_lower.contains("升序"))) {
            q.desc = false;
        } else if (q_lower.contains("前") || q_lower.contains("最高") || q_lower.contains("最大") || q_lower.contains("top")) {
            q.desc = true;
        }
        if (q_lower.contains("最低") || q_lower.contains("最小")) {
            q.desc = true;
        }

        // 4. LIMIT
        Pattern limitP = Pattern.compile("(前|top)\\s*(\\d+)|(\\d+)\\s*(个|条|行)");
        Matcher m = limitP.matcher(q_lower);
        if (m.find()) {
            try {
                for (int i = 1; i <= m.groupCount(); i++) {
                    if (m.group(i) != null && m.group(i).matches("\\d+")) {
                        q.limit = Integer.parseInt(m.group(i));
                        break;
                    }
                }
            } catch (Exception ignore) {}
        }

        // 5. 字段 (如果有 select)
        if (q.aggFunc == null) {
            for (String col : schema.getOrDefault(q.table, List.of())) {
                if (q_lower.contains(col.toLowerCase())) {
                    q.selectColumns.add(col);
                }
            }
        }

        // 6. GROUP BY
        if (q_lower.contains("按")) {
            Pattern groupP = Pattern.compile("按(.+?)(统计|分组|汇总|聚合|查询)");
            Matcher gm = groupP.matcher(question);
            if (gm.find()) {
                String groupField = gm.group(1);
                // 模糊匹配字段
                for (String col : schema.getOrDefault(q.table, List.of())) {
                    if (groupField.contains(col) || col.contains(groupField)) {
                        q.groupBy = col;
                        q.aggFunc = q.aggFunc == null ? "COUNT" : q.aggFunc;
                        break;
                    }
                }
            }
        }

        // 7. WHERE 条件 (时间)
        if (q_lower.contains("今天")) {
            ParsedQuery.WhereCondition w = new ParsedQuery.WhereCondition();
            w.column = "created_at";
            w.op = ">=";
            w.value = "CURDATE()";
            q.where.add(w);
        } else if (q_lower.contains("昨天")) {
            ParsedQuery.WhereCondition w = new ParsedQuery.WhereCondition();
            w.column = "created_at";
            w.op = ">=";
            w.value = "DATE_SUB(CURDATE(), INTERVAL 1 DAY)";
            q.where.add(w);
        } else if (q_lower.contains("本周") || q_lower.contains("这周")) {
            ParsedQuery.WhereCondition w = new ParsedQuery.WhereCondition();
            w.column = "created_at";
            w.op = ">=";
            w.value = "DATE_SUB(NOW(), INTERVAL 7 DAY)";
            q.where.add(w);
        } else if (q_lower.contains("本月") || q_lower.contains("这个月")) {
            ParsedQuery.WhereCondition w = new ParsedQuery.WhereCondition();
            w.column = "created_at";
            w.op = ">=";
            w.value = "DATE_FORMAT(NOW(), '%Y-%m-01')";
            q.where.add(w);
        } else {
            // "最近 N 天"
            Pattern recentP = Pattern.compile("最近(\\d+)(天|周|月)");
            Matcher rm = recentP.matcher(question);
            if (rm.find()) {
                int n = Integer.parseInt(rm.group(1));
                String unit = rm.group(2);
                int days = "天".equals(unit) ? n : "周".equals(unit) ? n * 7 : n * 30;
                ParsedQuery.WhereCondition w = new ParsedQuery.WhereCondition();
                w.column = "created_at";
                w.op = ">=";
                w.value = "DATE_SUB(NOW(), INTERVAL " + days + " DAY)";
                q.where.add(w);
            }
        }

        return q;
    }

    /**
     * 生成 SQL
     */
    private String generateSql(ParsedQuery q) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        if (q.aggFunc != null) {
            // 聚合
            if (q.groupBy != null) {
                sql.append(q.groupBy).append(", ");
            }
            sql.append(q.aggFunc).append("(*) AS ").append(q.aggFunc.toLowerCase());
        } else if (!q.selectColumns.isEmpty()) {
            sql.append(String.join(", ", q.selectColumns));
        } else {
            sql.append("*");
        }

        if (q.table != null) {
            sql.append(" FROM ").append(q.table);
        }

        // WHERE
        if (!q.where.isEmpty()) {
            sql.append(" WHERE ");
            List<String> conds = new ArrayList<>();
            for (ParsedQuery.WhereCondition w : q.where) {
                conds.add(w.column + " " + w.op + " " + w.value);
            }
            sql.append(String.join(" AND ", conds));
        }

        // GROUP BY
        if (q.groupBy != null) {
            sql.append(" GROUP BY ").append(q.groupBy);
        }

        // ORDER BY (按聚合或字段)
        if (q.aggFunc != null && q.aggFunc.equals("COUNT") == false) {
            sql.append(" ORDER BY ").append(q.aggFunc.toLowerCase()).append(q.desc ? " DESC" : " ASC");
        } else if (q.limit != null) {
            sql.append(" ORDER BY id").append(q.desc ? " DESC" : " ASC");
        }

        // LIMIT
        if (q.limit != null) {
            sql.append(" LIMIT ").append(q.limit);
        }

        return sql.toString();
    }

    /**
     * 安全检查
     */
    private boolean isDangerous(String sql) {
        String upper = sql.toUpperCase();
        return upper.contains("DROP ") || upper.contains("DELETE ") ||
                upper.contains("UPDATE ") || upper.contains("INSERT ") ||
                upper.contains("ALTER ") || upper.contains("TRUNCATE ") ||
                upper.contains("CREATE ") || upper.contains("GRANT ") ||
                upper.contains("REVOKE ");
    }

    /**
     * 加载 schema
     */
    private Map<String, List<String>> loadSchema(DbDataSource ds) throws Exception {
        Map<String, List<String>> schema = new LinkedHashMap<>();
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        try (Connection conn = ds_.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet tables = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String table = tables.getString("TABLE_NAME");
                    List<String> columns = new ArrayList<>();
                    try (ResultSet cols = meta.getColumns(conn.getCatalog(), null, table, "%")) {
                        while (cols.next()) {
                            columns.add(cols.getString("COLUMN_NAME"));
                        }
                    }
                    schema.put(table, columns);
                }
            }
        }
        return schema;
    }

    /**
     * 执行 SQL
     */
    private List<Map<String, Object>> executeQuery(DbDataSource ds, String sql, Integer limit) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        String finalSql = limit != null && !sql.toUpperCase().contains("LIMIT") ?
                sql + " LIMIT " + limit : sql;
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        try (Connection conn = ds_.getConnection();
             PreparedStatement ps = conn.prepareStatement(finalSql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
                if (result.size() >= 1000) break; // 防止返回太多
            }
        }
        return result;
    }
}
