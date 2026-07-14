package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 种子数据验证端点 (V3.5.8)
 * 查 H2 数据库, 确认 seed-data 自动加载
 */
@Tag(name = "种子数据")
@RestController
@RequestMapping("/api/ai/seed")
public class SeedInfoController {
    
    @Autowired
    private JdbcTemplate jdbc;
    
    @Operation(summary = "检查种子数据加载情况")
    @GetMapping("/check")
    public Result<Map<String, Object>> check() {
        String[] tables = {
            "tenant", "sys_role", "sys_user", "sys_user_role",
            "model_provider", "ai_tool", "function_tool", "prompt_template",
            "plugin", "knowledge_base", "sensitive_word", "ai_intent_keyword",
            "user_api_key", "notification", "alert_rule", "alert_channel",
            "pipeline_workflow", "document", "document_chunk", "ai_generation_log",
            "cluster_node", "data_source", "kg_entity"
        };
        Map<String, Object> result = new LinkedHashMap<>();
        int totalCount = 0;
        int totalTables = 0;
        for (String t : tables) {
            try {
                Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM `" + t + "`", Integer.class);
                result.put(t, c);
                totalCount += c;
                totalTables++;
            } catch (Exception e) {
                result.put(t, "❌ " + e.getMessage().split("\n")[0]);
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTables", totalTables);
        summary.put("totalRows", totalCount);
        result.put("_summary", summary);
        return Result.ok(result);
    }
}
