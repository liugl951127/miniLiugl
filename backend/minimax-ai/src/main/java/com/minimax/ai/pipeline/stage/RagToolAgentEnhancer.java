package com.minimax.ai.pipeline.stage;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.generation.ConversationContext;
import com.minimax.ai.generation.KeywordEngine;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.stage.ContextAssembler.AssembledContext;
import com.minimax.ai.tool.AiToolRegistry;
import com.minimax.ai.tool.builtin.AbstractSimpleTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段 6: RAG/工具/智能体增强器 (V2.8.5)
 *
 * <h3>职责</h3>
 * 在送入模型前, 用 3 种方式增强上下文:
 * <ol>
 *   <li><b>RAG</b>: 从知识库检索最相关的 K 条</li>
 *   <li><b>工具调用</b>: 根据意图自动执行工具 (查询数据, 画图, 算预测等)</li>
 *   <li><b>智能体</b>: 多步任务拆解 (简单版: 1 次工具调用)</li>
 * </ol>
 *
 * <h3>算法</h3>
 * 1. 调用 KeywordEngine 识别意图
 * 2. 若意图匹配工具 → 调工具拿结果
 * 3. RAG: 简单版 - 关键词匹配 + 余弦相似度
 * 4. 智能体: 暂用工具结果作为增强
 */
@Slf4j
@Component
public class RagToolAgentEnhancer {

    private final KeywordEngine keywordEngine;
    private final AiToolRegistry toolRegistry;
    private final SimpleEmbedding embedding;
    private final ConversationContext conversationContext;

    /** 简易知识库 (内存缓存, 实际生产可对接向量数据库) */
    private final Map<String, List<Double>> knowledgeBase = new ConcurrentHashMap<>();
    /** 知识库原始文本 */
    private final Map<String, String> knowledgeContent = new ConcurrentHashMap<>();

    /** 启动时加载默认知识 (全参构造器由 Lombok 生成的同時手动初始化默认知识) */
    public RagToolAgentEnhancer(KeywordEngine keywordEngine, AiToolRegistry toolRegistry,
                                SimpleEmbedding embedding, ConversationContext conversationContext) {
        this.keywordEngine = keywordEngine;
        this.toolRegistry = toolRegistry;
        this.embedding = embedding;
        this.conversationContext = conversationContext;
        initDefaultKnowledge();
    }

    /**
     * 初始化默认知识库 (V2.8.5)
     * 实际生产应由知识库管理后台写入
     */
    private void initDefaultKnowledge() {
        addKnowledge("minimax-platform", "MiniMax 平台是 MiniMax 公司开发的企业级 AI 应用平台, 提供 LLM, RAG, AI Agent, 数据分析等能力.");
        addKnowledge("minimax-ai-module", "minimax-ai 是自研 AI 服务, 包含文本生成, 图表生成, 音乐生成, 视频生成, 多模态分析等 19 个 AI 工具.");
        addKnowledge("minimax-deployment", "MiniMax 部署方式: Docker Compose 一键部署, K8s 生产部署, systemd 传统部署, 支持 HTTPS 和自定义域名.");
        addKnowledge("minimax-pricing", "MiniMax 提供 5 个套餐: 社区版 (免费), 标准版 (¥99/月), 企业版 (¥999/月), 旗舰版 (¥9999/月), 定制版 (面议).");
        addKnowledge("minimax-security", "MiniMax 通过等保三级认证, 支持数据加密 (AES-256-GCM), 审计日志, RBAC 权限, 内容审核.");
    }

    /**
     * 添加知识条目 (动态数据: 任何来源的文档片段)
     */
    public void addKnowledge(String id, String text) {
        double[] vec = embedding.embed(text);
        knowledgeBase.put(id, arrayToList(vec));
        knowledgeContent.put(id, text);
    }

    /**
     * 增强处理
     */
    public EnhancementResult enhance(AssembledContext context, String userText, String sessionId) {
        long start = System.currentTimeMillis();
        log.info("[stage-6/enhance] text='{}'", userText != null && userText.length() > 30 ? userText.substring(0, 30) + "..." : userText);

        EnhancementResult r = new EnhancementResult();
        r.intent = KeywordEngine.Intent.UNKNOWN;
        r.ragResults = new ArrayList<>();
        r.toolResults = new LinkedHashMap<>();
        r.agentSteps = new ArrayList<>();

        if (userText == null || userText.isEmpty()) {
            r.costMs = System.currentTimeMillis() - start;
            return r;
        }

        // 1. 意图识别 (复用 V2.8.4 KeywordEngine)
        KeywordEngine.Intent intent = keywordEngine.recognize(userText);
        r.intent = intent;
        log.debug("[stage-6/enhance] intent={}", intent);

        // 2. RAG 检索 (无论什么意图都做, 增强知识)
        List<KnowledgeHit> ragHits = retrieve(userText, PipelineConfig.RAG_TOP_K);
        r.ragResults = ragHits;
        StringBuilder ragStr = new StringBuilder();
        for (KnowledgeHit hit : ragHits) {
            ragStr.append("- ").append(hit.text).append("\n");
        }
        context.segments.put("retrieval", ragStr.length() > 0 ? ragStr.toString() : "");

        // 3. 工具调用 (基于意图)
        Map<String, Object> toolInput = extractToolParams(userText, intent);
        if (!toolInput.isEmpty() && toolRegistry != null) {
            try {
                // 根据意图找到对应 tool code
                String toolCode = mapIntentToTool(intent);
                if (toolCode != null) {
                    Object toolResult = toolRegistry.invoke(toolCode, toolInput, null, null, null);
                    r.toolResults.put(toolCode, toolResult);
                    // 把工具结果注入 context
                    String toolStr = formatToolResult(toolCode, toolResult);
                    context.segments.put("tool", toolStr);
                    r.agentSteps.add("工具调用: " + toolCode);
                }
            } catch (Exception e) {
                log.warn("[stage-6/enhance] tool call failed", e);
            }
        }

        // 4. 智能体: 多步推理 (V2.8.5 简化: 仅记录决策链)
        r.agentSteps.add("意图识别: " + intent.name());
        r.agentSteps.add("RAG 检索: " + ragHits.size() + " 条");
        if (!r.toolResults.isEmpty()) {
            r.agentSteps.add("工具结果注入: " + r.toolResults.keySet());
        }

        r.costMs = System.currentTimeMillis() - start;
        log.info("[stage-6/enhance] → intent={}, rag={}, tools={}, costMs={}",
                intent, ragHits.size(), r.toolResults.size(), r.costMs);
        return r;
    }

    /**
     * RAG 检索: 余弦相似度 top-k
     */
    private List<KnowledgeHit> retrieve(String query, int topK) {
        if (knowledgeBase.isEmpty()) return Collections.emptyList();
        double[] qVec = embedding.embed(query);

        // 计算所有知识条目相似度
        List<KnowledgeHit> hits = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : knowledgeBase.entrySet()) {
            double sim = cosineSimilarity(qVec, listToArray(e.getValue()));
            if (sim >= PipelineConfig.RAG_SIMILARITY_THRESHOLD) {
                hits.add(new KnowledgeHit(e.getKey(), knowledgeContent.get(e.getKey()), sim));
            }
        }
        // 按相似度降序
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        return hits.subList(0, Math.min(topK, hits.size()));
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 意图 → 工具 code 映射
     */
    private String mapIntentToTool(KeywordEngine.Intent intent) {
        switch (intent) {
            case GENERATE_CHART: return "chart.generate";
            case GENERATE_MUSIC: return "music.generate";
            case GENERATE_ANIMATION: return "image.generate";
            case QUERY_DATA: return "text.analyze";
            case ANALYZE_DATA: return "text.analyze";
            case GENERATE_CODE: return "java.project.gen";
            case TTS: return null;  // TTS 需要音频输出
            case IMAGE_ANALYZE: return "vision.analyze";
            case AUDIO_ANALYZE: return "audio.analyze";
            case VIDEO_ANALYZE: return "vision.analyze";
            default: return null;
        }
    }

    /**
     * 从用户输入提取工具参数
     */
    private Map<String, Object> extractToolParams(String text, KeywordEngine.Intent intent) {
        Map<String, Object> params = new HashMap<>();
        if (text == null) return params;
        switch (intent) {
            case GENERATE_CHART:
                params.put("type", "BAR");
                params.put("title", "用户请求图表");
                params.put("series", List.of(Map.of("name", "数据", "values", List.of(10, 20, 30, 25, 35))));
                return params;
            case GENERATE_MUSIC:
                params.put("style", "POP");
                params.put("key", "C");
                params.put("bpm", 120);
                params.put("bars", 8);
                return params;
            case ANALYZE_DATA:
            case QUERY_DATA:
                params.put("text", text);
                params.put("task", "summary");
                return params;
            case GENERATE_CODE:
                params.put("projectName", "minimax-app");
                params.put("version", "1.0.0");
                return params;
            default:
                return params;
        }
    }

    /**
     * 格式化工具结果为可注入 prompt 的字符串
     */
    private String formatToolResult(String toolCode, Object result) {
        if (result == null) return "";
        if (result instanceof Map) {
            return "[工具 " + toolCode + " 结果] " + ((Map<?, ?>) result).toString();
        }
        return "[工具 " + toolCode + " 结果] " + result.toString();
    }

    /** 工具方法: 数组 ↔ List */
    private List<Double> arrayToList(double[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (double v : arr) list.add(v);
        return list;
    }
    private double[] listToArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    /** 知识命中 */
    @lombok.Data
    public static class KnowledgeHit {
        public String id;
        public String text;
        public double score;
        public KnowledgeHit(String id, String text, double score) {
            this.id = id; this.text = text; this.score = score;
        }
    }

    /** 增强结果 */
    @lombok.Data
    public static class EnhancementResult {
        public KeywordEngine.Intent intent;
        public List<KnowledgeHit> ragResults;
        public Map<String, Object> toolResults;
        public List<String> agentSteps;
        public long costMs;
    }
}
