package com.minimax.ai.pipeline.stage;

import com.minimax.ai.generation.ConversationContext;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.stage.MultimodalParser.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阶段 4: 上下文拼接器 (V2.8.5)
 *
 * <h3>职责</h3>
 * 把"系统提示 + 历史对话 + 当前输入"拼成 LLM 可消费的 prompt.
 *
 * <h3>拼接顺序</h3>
 * <pre>
 *   [SYSTEM] 系统角色 + 行为约束
 *   [HISTORY]  最近 N 轮用户/助手交替
 *   [RETRIEVAL] RAG 增强上下文 (阶段 6 注入)
 *   [TOOL]    工具/智能体结果 (阶段 6 注入)
 *   [USER]    当前用户输入
 * </pre>
 *
 * <h3>长度控制</h3>
 * - 总 token 数不能超 MAX_SEQ_LEN
 * - 超长时按优先级裁剪: USER > RETRIEVAL > TOOL > HISTORY > SYSTEM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextAssembler {

    private final ConversationContext conversationContext;

    /** 系统提示模板 (可被 DB 中 prompt 表覆盖) */
    public static final String DEFAULT_SYSTEM_PROMPT =
            "你是 MiniMax 企业 AI 助手.\n" +
            "1. 用简洁专业的语言回答.\n" +
            "2. 涉及数据/代码时给出具体示例.\n" +
            "3. 不确定时明确说明, 不要编造.\n" +
            "4. 敏感内容拒绝回答并提示联系人工.";

    /**
     * 上下文组装
     *
     * @param parseResult 阶段 3 多模态解析结果
     * @param sessionId   会话 ID
     * @return 组装好的 Prompt 结构
     */
    public AssembledContext assemble(ParseResult parseResult, String sessionId) {
        long start = System.currentTimeMillis();
        log.info("[stage-4/context] session={}, unifiedText='{}' ({} chars)",
                sessionId,
                parseResult.unifiedText != null && parseResult.unifiedText.length() > 30
                        ? parseResult.unifiedText.substring(0, 30) + "..." : parseResult.unifiedText,
                parseResult.unifiedText != null ? parseResult.unifiedText.length() : 0);

        AssembledContext ctx = new AssembledContext();
        ctx.sessionId = sessionId;
        ctx.segments = new LinkedHashMap<>();

        // 1. System segment (角色 + 行为约束)
        ctx.segments.put("system", DEFAULT_SYSTEM_PROMPT);

        // 2. History segment (从 ConversationContext 拿)
        if (sessionId != null) {
            List<String> recentTexts = conversationContext.recentTexts(sessionId, PipelineConfig.MAX_HISTORY_TURNS);
            StringBuilder hist = new StringBuilder();
            for (int i = 0; i < recentTexts.size(); i++) {
                hist.append("[历史 ").append(i + 1).append("] ").append(recentTexts.get(i)).append("\n");
            }
            if (hist.length() > 0) {
                ctx.segments.put("history", hist.toString());
            }
        }

        // 3. Retrieval segment 占位 (阶段 6 注入)
        ctx.segments.put("retrieval", "");

        // 4. Tool segment 占位 (阶段 6 注入)
        ctx.segments.put("tool", "");

        // 5. User segment (当前输入)
        ctx.segments.put("user", parseResult.unifiedText != null ? parseResult.unifiedText : "");

        // 6. 拼接为完整 prompt
        ctx.fullPrompt = buildPrompt(ctx.segments);

        // 7. 估算 token 数 (1 字符 ≈ 0.7 token, 中文)
        ctx.estimatedTokens = Math.max(1, (int) (ctx.fullPrompt.length() * 0.7));

        ctx.costMs = System.currentTimeMillis() - start;
        log.info("[stage-4/context] → promptLen={} chars, estTokens={}, segments={}, costMs={}",
                ctx.fullPrompt.length(), ctx.estimatedTokens, ctx.segments.keySet(), ctx.costMs);
        return ctx;
    }

    /**
     * 按顺序拼接各段
     */
    private String buildPrompt(Map<String, String> segments) {
        StringBuilder sb = new StringBuilder();
        // 固定顺序拼接
        String[] order = {"system", "history", "retrieval", "tool", "user"};
        for (String key : order) {
            String v = segments.get(key);
            if (v == null || v.isEmpty()) continue;
            sb.append("<").append(key).append(">\n");
            sb.append(v);
            if (!v.endsWith("\n")) sb.append("\n");
            sb.append("</").append(key).append(">\n\n");
        }
        return sb.toString();
    }

    /** 组装后的上下文 */
    @lombok.Data
    public static class AssembledContext {
        public String sessionId;
        /** 各段内容, 顺序敏感 */
        public Map<String, String> segments;
        /** 拼接后的完整 prompt */
        public String fullPrompt;
        /** 估算 token 数 */
        public int estimatedTokens;
        public long costMs;
    }
}
