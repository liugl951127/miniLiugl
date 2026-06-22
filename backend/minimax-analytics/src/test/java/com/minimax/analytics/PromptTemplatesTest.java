package com.minimax.analytics;

import com.minimax.analytics.service.nlsql.PromptTemplates;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NL2SQL Prompt 模板测试 (V5.31)
 */
class PromptTemplatesTest {

    @Test void systemPromptContainsRules() {
        String s = PromptTemplates.system();
        assertTrue(s.contains("SELECT"));
        assertTrue(s.contains("LIMIT"));
        assertTrue(s.contains("MySQL"));
    }

    @Test void userPromptIncludesSchema() {
        String p = PromptTemplates.user("最近 7 天用户数", List.of(
                Map.of("name", "user", "ddl", "CREATE TABLE user (id BIGINT)")
        ));
        assertTrue(p.contains("最近 7 天用户数"));
        assertTrue(p.contains("user"));
        assertTrue(p.contains("CREATE TABLE user"));
    }

    @Test void fewShotCoversThreeCases() {
        String f = PromptTemplates.fewShot();
        assertTrue(f.contains("单表查询"));
        assertTrue(f.contains("聚合"));
        assertTrue(f.contains("JOIN"));
    }

    @Test void userPromptHandlesEmptySchema() {
        String p = PromptTemplates.user("问题", List.of());
        assertTrue(p.contains("数据库 Schema"));
        assertTrue(p.contains("问题"));
    }

    @Test void userPromptMultipleTables() {
        String p = PromptTemplates.user("Q", List.of(
                Map.of("name", "t1", "ddl", "DDL1"),
                Map.of("name", "t2", "ddl", "DDL2")
        ));
        assertTrue(p.contains("t1"));
        assertTrue(p.contains("t2"));
    }
}
