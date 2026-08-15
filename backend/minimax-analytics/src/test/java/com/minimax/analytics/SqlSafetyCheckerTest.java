package com.minimax.analytics;

import com.minimax.analytics.service.nlsql.SqlSafetyChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL 安全校验器测试 (V5.31) - 5 道防线全覆盖
 */
class SqlSafetyCheckerTest {

    private SqlSafetyChecker checker;

    @BeforeEach
    void setup() {
        checker = new SqlSafetyChecker();
        ReflectionTestUtils.setField(checker, "defaultMaxRows", 1000);
    }

    @Test void acceptSimpleSelect() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT * FROM user", 100);
        assertTrue(r.ok());
        assertEquals(100, r.maxRows());
        assertTrue(r.sql().contains("LIMIT 100"));
    }

    @Test void rejectEmpty() {
        SqlSafetyChecker.SafetyResult r = checker.check("", null);
        assertFalse(r.ok());
        assertEquals("EMPTY_SQL", r.code());
    }

    @Test void rejectNull() {
        SqlSafetyChecker.SafetyResult r = checker.check(null, null);
        assertFalse(r.ok());
    }

    @Test void rejectInsert() {
        SqlSafetyChecker.SafetyResult r = checker.check("INSERT INTO user VALUES (1, 'a')", null);
        assertFalse(r.ok());
        assertEquals("FORBIDDEN_KEYWORD", r.code());
    }

    @Test void rejectUpdate() {
        SqlSafetyChecker.SafetyResult r = checker.check("UPDATE user SET name='x' WHERE id=1", null);
        assertFalse(r.ok());
    }

    @Test void rejectDelete() {
        SqlSafetyChecker.SafetyResult r = checker.check("DELETE FROM user WHERE id=1", null);
        assertFalse(r.ok());
    }

    @Test void rejectDrop() {
        SqlSafetyChecker.SafetyResult r = checker.check("DROP TABLE user", null);
        assertFalse(r.ok());
    }

    @Test void rejectTruncate() {
        SqlSafetyChecker.SafetyResult r = checker.check("TRUNCATE user", null);
        assertFalse(r.ok());
    }

    @Test void rejectAlter() {
        SqlSafetyChecker.SafetyResult r = checker.check("ALTER TABLE user ADD COLUMN x INT", null);
        assertFalse(r.ok());
    }

    @Test void rejectGrant() {
        SqlSafetyChecker.SafetyResult r = checker.check("GRANT ALL ON *.* TO 'foo'@'bar'", null);
        assertFalse(r.ok());
    }

    @Test void rejectSleep() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT SLEEP(10)", null);
        assertFalse(r.ok());
    }

    @Test void rejectBenchmark() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT BENCHMARK(1000000, MD5('x'))", null);
        assertFalse(r.ok());
    }

    @Test void rejectSet() {
        SqlSafetyChecker.SafetyResult r = checker.check("SET autocommit=0", null);
        assertFalse(r.ok());
    }

    @Test void rejectIntoOutfile() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT * FROM user INTO OUTFILE '/tmp/x'", null);
        assertFalse(r.ok());
    }

    @Test void rejectMultiStatement() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT 1; SELECT 2", null);
        assertFalse(r.ok());
        assertEquals("MULTI_STMT", r.code());
    }

    @Test void acceptInsertInStringLiteral() {
        // "INSERT INTO" 在字符串里, 不应拦截
        SqlSafetyChecker.SafetyResult r = checker.check(
                "SELECT 'INSERT INTO foo VALUES (1)' AS sql_text", null);
        assertTrue(r.ok());
    }

    @Test void acceptSelectWithWhere() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT id FROM user WHERE age > 18", 50);
        assertTrue(r.ok());
        assertTrue(r.sql().contains("LIMIT 50"));
    }

    @Test void acceptSelectWithJoin() {
        SqlSafetyChecker.SafetyResult r = checker.check(
                "SELECT u.*, o.total FROM user u JOIN `order` o ON o.user_id = u.id", null);
        assertTrue(r.ok());
    }

    @Test void acceptSelectWithAggregate() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT role, COUNT(*) FROM user GROUP BY role", null);
        assertTrue(r.ok());
    }

    @Test void keepExistingLimit() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT * FROM user LIMIT 5", 1000);
        assertTrue(r.ok());
        assertTrue(r.sql().contains("LIMIT 5"));
        assertFalse(r.sql().contains("LIMIT 1000"));
    }

    @Test void trimTrailingSemicolon() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT * FROM user;", 10);
        assertTrue(r.ok());
        assertFalse(r.sql().endsWith(";"));
    }

    @Test void maxRowsClamp() {
        // 用户传 99999, 应被 clamp 到 1000
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT 1", 99999);
        assertTrue(r.ok());
        assertEquals(1000, r.maxRows());
    }

    @Test void throwIfFailHelper() {
        SqlSafetyChecker.SafetyResult r = checker.check("DROP TABLE x", null);
        assertThrows(Exception.class, r::throwIfFail);
    }

    @Test void throwIfFailPass() {
        SqlSafetyChecker.SafetyResult r = checker.check("SELECT 1", 10);
        assertDoesNotThrow(r::throwIfFail);
    }
}
