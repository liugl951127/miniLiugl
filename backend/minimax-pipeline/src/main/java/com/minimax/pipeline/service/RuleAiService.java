package com.minimax.pipeline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.sdk.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则 AI 生成 (V9.1) — 自然语言 → 规则 JSON DSL
 *
 * 之前: 规则只能用前端表单手填, 门槛高
 * 现在: 写一句"当用户年龄 < 18 时禁止访问 X", LLM 生成规则 JSON
 *
 * 用法:
 *   1. 前端 /rule/ai-generate?text=...
 *   2. 后端调 LlmClient (走 LLM Gateway, cloud→local 兜底)
 *   3. 解析 JSON DSL 返给前端预览
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RuleAiService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        你是企业级规则引擎专家, 把用户的自然语言描述转成规则 JSON DSL。

        ## 规则 DSL 结构
        ```json
        {
          "name": "规则名",
          "conditions": [
            {"field": "user.age", "operator": "<", "value": 18}
          ],
          "actions": [
            {"type": "DENY", "message": "未成年禁止访问"}
          ]
        }
        ```

        ## 支持的 operator
        =, !=, <, <=, >, >=, contains, startsWith, in, notIn

        ## 支持的 actions
        ALLOW / DENY / TAG / ROUTE_TO_AGENT / LOG

        ## 例子
        输入: "18 岁以下用户禁止访问购物功能"
        输出: {"name":"未成年禁购","conditions":[{"field":"user.age","operator":"<","value":18}],"actions":[{"type":"DENY","message":"未成年禁止访问"}]}

        只输出 JSON, 不要 markdown, 不要解释。
        """;

    public record AiGenResult(
        String name,
        Map<String, Object> jsonContent,
        String llmSource,        // V9.1: CLOUD | LOCAL | LOCAL_FALLBACK | UNAVAILABLE
        String llmModel,
        long durationMs,
        String reason
    ) {}

    public AiGenResult generate(String naturalLanguage) {
        if (naturalLanguage == null || naturalLanguage.isBlank()) {
            return new AiGenResult(null, null, "UNAVAILABLE", "", 0, "自然语言不能为空");
        }
        long t0 = System.currentTimeMillis();
        // 截断避免超 token
        String input = naturalLanguage.length() > 1000
            ? naturalLanguage.substring(0, 1000) + "..." : naturalLanguage;

        LlmClient.LlmResult r = llmClient.chat(SYSTEM_PROMPT, input);
        if (!r.available()) {
            return new AiGenResult(null, null, r.source().name(), r.model(), r.durationMs(),
                "LLM 不可用: " + r.reason());
        }

        // 清理 markdown 代码块标记
        String text = r.content().trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        int first = text.indexOf('{'), last = text.lastIndexOf('}');
        if (first >= 0 && last > first) text = text.substring(first, last + 1);

        try {
            JsonNode root = objectMapper.readTree(text);
            Map<String, Object> json = objectMapper.convertValue(root, Map.class);
            String name = (String) json.getOrDefault("name", "AI 生成的规则");
            log.info("[RuleAI] 生成成功, source={}, model={}, duration={}ms",
                r.source(), r.model(), r.durationMs());
            return new AiGenResult(name, json, r.source().name(), r.model(),
                System.currentTimeMillis() - t0, null);
        } catch (Exception e) {
            log.warn("[RuleAI] JSON 解析失败: {}", e.getMessage());
            return new AiGenResult(null, null, r.source().name(), r.model(),
                System.currentTimeMillis() - t0,
                "LLM 返回非 JSON, 请换更具体的描述");
        }
    }
}
