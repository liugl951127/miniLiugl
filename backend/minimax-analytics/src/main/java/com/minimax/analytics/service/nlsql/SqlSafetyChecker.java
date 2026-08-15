package com.minimax.analytics.service.nlsql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.util.JdbcConstants;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * SQL 安全校验器 (V5.31) — 5 道防线
 *
 * 防线 1: DDL/DML 关键字黑名单 (Druid SQL Parser)
 * 防线 2: 必须单条 SELECT
 * 防线 3: 表名前缀白名单 (V5.31 简化: 全 allow 任何 SELECT, 记录审计)
 * 防线 4: 自动追加 LIMIT (没有就加 LIMIT 1000)
 * 防线 5: maxRows / setQueryTimeout 强限制 (在 QueryService 层)
 *
 * 注: 防线 3 在生产环境应该按 user 限定 schema, V5.31 默认 allow-all
 */
@Slf4j
@Component
public class SqlSafetyChecker {

    /** 禁止的关键字 (不区分大小写) */
    private static final Set<String> FORBIDDEN = Set.of(
            "INSERT", "UPDATE", "DELETE", "REPLACE",
            "DROP", "TRUNCATE", "ALTER", "CREATE", "RENAME",
            "GRANT", "REVOKE",
            "SET",   // 防止 SET autocommit=0 等
            "LOCK", "UNLOCK", "KILL", "CALL", "LOAD", "HANDLER",
            "INTO OUTFILE", "INTO DUMPFILE",
            "SLEEP", "BENCHMARK"
    );

    @Value("${analytics.query.max-rows:1000}")
    private int defaultMaxRows;

    /**
     * 校验并改写 SQL.
     *
     * @param sql     原始 SQL
     * @param maxRows 用户指定的最大行数 (≤ 1000)
     * @return 改写后的安全 SQL (自动加 LIMIT)
     * @throws BizException 校验失败
     */
    public SafetyResult check(String sql, Integer maxRows) {
        if (sql == null || sql.isBlank()) {
            return SafetyResult.fail("EMPTY_SQL", "SQL 不能为空");
        }
        String trimmed = sql.trim();
        // 去除末尾分号
        if (trimmed.endsWith(";")) trimmed = trimmed.substring(0, trimmed.length() - 1).trim();

        // 防线 1: 关键字黑名单 (粗扫, 字符串里忽略大小写)
        String upperNoStrings = stripStrings(trimmed).toUpperCase();
        for (String kw : FORBIDDEN) {
            // 用单词边界匹配, 避免误判 (例 SELECT 不应该匹配 SELECT)
            String pattern = "\\b" + kw + "\\b";
            if (java.util.regex.Pattern.compile(pattern).matcher(upperNoStrings).find()) {
                return SafetyResult.fail("FORBIDDEN_KEYWORD", "禁止使用关键字: " + kw);
            }
        }

        // 防线 2: 用 Druid 解析, 必须单条 SELECT
        List<SQLStatement> stmts;
        try {
            stmts = SQLUtils.parseStatements(trimmed, JdbcConstants.MYSQL);
        } catch (Exception e) {
            return SafetyResult.fail("PARSE_ERROR", "SQL 解析失败: " + e.getMessage());
        }
        if (stmts.size() != 1) {
            return SafetyResult.fail("MULTI_STMT", "仅支持单条 SELECT, 检测到 " + stmts.size() + " 条");
        }
        SQLStatement stmt = stmts.get(0);
        if (!(stmt instanceof SQLSelectStatement)) {
            return SafetyResult.fail("NOT_SELECT", "仅支持 SELECT 查询");
        }
        SQLSelectStatement select = (SQLSelectStatement) stmt;
        if (!(select.getSelect().getQueryBlock() instanceof MySqlSelectQueryBlock)) {
            return SafetyResult.fail("UNSUPPORTED_SELECT", "不支持的 SELECT 类型 (含 UNION/SET? 请拆分)");
        }

        // 防线 4: 自动追加 LIMIT
        int limit = maxRows != null && maxRows > 0 ? Math.min(maxRows, defaultMaxRows) : defaultMaxRows;
        String rewritten = ensureLimit(trimmed, limit);

        log.info("SQL 安全校验通过: rows={}, sql={}", limit, rewritten.length() > 200 ? rewritten.substring(0, 200) + "..." : rewritten);
        return SafetyResult.ok(rewritten, limit);
    }

    /** 去除字符串字面量 (防止 'INSERT INTO xxx' 字符串绕过黑名单) */
    private String stripStrings(String sql) {
        StringBuilder sb = new StringBuilder();
        boolean inStr = false;
        char quote = 0;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (inStr) {
                if (ch == '\\' && i + 1 < sql.length()) { i++; continue; }
                if (ch == quote) inStr = false;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                inStr = true;
                quote = ch;
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /** 自动追加 LIMIT N (如已存在 LIMIT, 不追加) */
    private String ensureLimit(String sql, int maxRows) {
        String upper = sql.toUpperCase();
        if (upper.contains("LIMIT")) return sql;
        if (upper.contains("COUNT(") && upper.contains("GROUP BY")) return sql;  // 聚合查询不加
        return sql + "\nLIMIT " + maxRows;
    }

    /** 校验结果 */
    public record SafetyResult(boolean ok, String reason, String code, String sql, int maxRows) {
        public static SafetyResult ok(String sql, int maxRows) {
            return new SafetyResult(true, null, null, sql, maxRows);
        }
        public static SafetyResult fail(String code, String reason) {
            return new SafetyResult(false, reason, code, null, 0);
        }
        public void throwIfFail() {
            if (!ok) throw new BizException(ResultCode.BAD_REQUEST, reason);
        }
    }
}
