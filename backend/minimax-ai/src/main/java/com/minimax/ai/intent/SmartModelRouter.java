package com.minimax.ai.intent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能模型路由器 (Day 29 - 智能化提升).
 *
 * <h3>策略: Cost-Quality Aware Routing</h3>
 *
 * 根据查询复杂度 + 业务策略自动选择最合适的模型，在质量、速度、成本之间取得最优平衡。
 *
 * <h3>模型能力矩阵</h3>
 * <table>
 *   <tr><th>模型</th><th>能力</th><th>速度</th><th>成本</th><th>适用场景</th></tr>
 *   <tr><td>MiniMax-Text-01</td><td>★★★</td><td>快</td><td>低</td><td>简单问答、闲聊</td></tr>
 *   <tr><td>MiniMax-Text-02</td><td>★★★★</td><td>中</td><td>中</td><td>数据分析、NL2SQL</td></tr>
 *   <tr><td>MiniMax-Text-03</td><td>★★★★★</td><td>慢</td><td>高</td><td>代码生成、复杂推理</td></tr>
 *   <tr><td>Embedding-01</td><td>嵌入</td><td>快</td><td>低</td><td>RAG 检索</td></tr>
 * </table>
 *
 * <h3>路由规则</h3>
 * <pre>
 * SIMPLE  + quality_pref=balanced  → MiniMax-Text-01 (快省)
 * SIMPLE  + quality_pref=high     → MiniMax-Text-02
 * MEDIUM  + quality_pref=balanced → MiniMax-Text-02
 * MEDIUM  + quality_pref=high    → MiniMax-Text-03
 * COMPLEX + quality_pref=balanced → MiniMax-Text-03
 * COMPLEX + quality_pref=high    → MiniMax-Text-03 + 反思
 * </pre>
 *
 * <h3>支持多 Provider</h3>
 * 可配置 OpenAI / DeepSeek / MiniMax 官方等多种 provider，
 * 路由根据 latency / cost / quota 动态选择最优节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartModelRouter {

    private final QueryComplexityService complexityService;

    /** 模型定义 */
    public enum Model {
        // 自研模型 (V3.5)
        MINIMAX_TEXT_01("MiniMax-Text-01", "self", 0.1, 1.0, 3),
        MINIMAX_TEXT_02("MiniMax-Text-02", "self", 0.3, 2.0, 4),
        MINIMAX_TEXT_03("MiniMax-Text-03", "self", 1.0, 5.0, 5),
        EMBEDDING_01("Embedding-01", "self", 0.01, 0.5, 1),

        // 外部模型 (可按需启用)
        GPT4O_MINI("gpt-4o-mini", "openai", 0.15, 3.0, 4),
        GPT4O("gpt-4o", "openai", 2.0, 8.0, 5),
        DEEPSEEK_CHAT("deepseek-chat", "deepseek", 0.3, 2.0, 4),
        DEEPSEEK_CODER("deepseek-coder", "deepseek", 0.5, 3.0, 5);

        @Getter private final String name;
        @Getter private final String provider; // self/openai/deepseek
        @Getter private final double inputCostPer1K;  // $ / 1K tokens
        @Getter private final double outputCostPer1K;
        @Getter private final int capabilityScore; // 1-5

        Model(String name, String provider, double inputCost, double outputCost, int cap) {
            this.name = name; this.provider = provider; this.inputCostPer1K = inputCost;
            this.outputCostPer1K = outputCost; this.capabilityScore = cap;
        }
    }

    /** 质量偏好 */
    public enum QualityPref {
        /** 优先省钱（默认）*/
        COST_FIRST,
        /** 质量优先 */
        QUALITY_FIRST,
        /** 速度优先 */
        SPEED_FIRST,
        /** 平衡（默认） */
        BALANCED
    }

    /** 路由上下文 */
    @Getter
    public static class RouteContext {
        private final String text;
        private final QueryComplexityService.ComplexityResult complexity;
        private final QualityPref pref;
        private final String intentHint;

        public RouteContext(String text, QueryComplexityService.ComplexityResult complexity,
                           QualityPref pref, String intentHint) {
            this.text = text;
            this.complexity = complexity;
            this.pref = pref != null ? pref : QualityPref.BALANCED;
            this.intentHint = intentHint;
        }
    }

    /** 路由结果 */
    @Getter
    public static class RouteResult {
        private final Model primaryModel;
        private final Model fallbackModel;
        private final String strategy;
        private final QueryComplexityService.ComplexityResult complexity;
        private final double estimatedCost;  // 估算成本
        private final String reasoning;

        public RouteResult(Model primary, Model fallback, String strategy,
                          QueryComplexityService.ComplexityResult complexity,
                          double estimatedCost, String reasoning) {
            this.primaryModel = primary;
            this.fallbackModel = fallback;
            this.strategy = strategy;
            this.complexity = complexity;
            this.estimatedCost = estimatedCost;
            this.reasoning = reasoning;
        }
    }

    /**
     * 路由到最优模型。
     *
     * @param text         用户输入
     * @param intentHint   意图名称（可选）
     * @param qualityPref  质量偏好（可选，默认 BALANCED）
     * @return 路由结果
     */
    public RouteResult route(String text, String intentHint, QualityPref qualityPref) {
        QueryComplexityService.ComplexityResult complexity =
                complexityService.analyze(text, intentHint);

        RouteContext ctx = new RouteContext(text, complexity, qualityPref, intentHint);

        Model primary = selectPrimary(ctx);
        Model fallback = selectFallback(primary, complexity);
        String strategy = buildStrategy(ctx);
        double cost = estimateCost(primary, text.length());
        String reasoning = buildReasoning(primary, complexity, qualityPref);

        log.info("[SmartRouter] text='{}' complexity={} pref={} → model={} (cost=${})",
                text.length() > 40 ? text.substring(0, 40) + "..." : text,
                complexity.getLevel(), qualityPref, primary.getName(), String.format("%.4f", cost));

        return new RouteResult(primary, fallback, strategy, complexity, cost, reasoning);
    }

    /** 路由到最优模型（简化版）*/
    public RouteResult route(String text) {
        return route(text, null, null);
    }

    /** 根据上下文选择主模型 */
    private Model selectPrimary(RouteContext ctx) {
        QueryComplexityService.ComplexityLevel level = ctx.getComplexity().getLevel();
        QualityPref pref = ctx.getPref();

        // 特殊意图强制路由
        if (ctx.getIntentHint() != null) {
            Model forced = forceByIntent(ctx.getIntentHint());
            if (forced != null) return forced;
        }

        return switch (level) {
            case SIMPLE -> switch (pref) {
                case QUALITY_FIRST -> Model.MINIMAX_TEXT_02;
                case SPEED_FIRST   -> Model.MINIMAX_TEXT_01;
                default             -> Model.MINIMAX_TEXT_01; // COST_FIRST / BALANCED
            };
            case MEDIUM -> switch (pref) {
                case QUALITY_FIRST -> Model.MINIMAX_TEXT_03;
                case SPEED_FIRST   -> Model.MINIMAX_TEXT_01;
                default            -> Model.MINIMAX_TEXT_02;
            };
            case COMPLEX -> switch (pref) {
                case SPEED_FIRST   -> Model.MINIMAX_TEXT_02;
                default            -> Model.MINIMAX_TEXT_03;
            };
        };
    }

    /** 兜底模型 */
    private Model selectFallback(Model primary, QueryComplexityService.ComplexityResult complexity) {
        if (complexity.getLevel() == QueryComplexityService.ComplexityLevel.COMPLEX) {
            return primary == Model.MINIMAX_TEXT_03 ? Model.GPT4O_MINI : primary;
        }
        return primary == Model.MINIMAX_TEXT_01 ? Model.MINIMAX_TEXT_02 : primary;
    }

    /** 根据意图强制路由 */
    private Model forceByIntent(String intent) {
        return switch (intent.toUpperCase()) {
            case "GENERATE_CODE" -> Model.MINIMAX_TEXT_03;
            case "ANALYZE_DATA", "GENERATE_CHART" -> Model.MINIMAX_TEXT_02;
            case "CHAT", "TTS", "STT" -> Model.MINIMAX_TEXT_01;
            default -> null;
        };
    }

    private String buildStrategy(RouteContext ctx) {
        return String.format("%s+%s",
                ctx.getComplexity().getLevel().name(),
                ctx.getPref().name());
    }

    private String buildReasoning(Model m, QueryComplexityService.ComplexityResult c, QualityPref pref) {
        return String.format(
                "复杂度=%s(%.2f) + 偏好=%s → 选择%s(能力%d) | 估算成本 $%.4f | %s",
                c.getLevel(), c.getScore(), pref,
                m.getName(), m.getCapabilityScore(),
                estimateCost(m, 200),
                m.getProvider().equals("self") ? "自研模型" : m.getProvider() + "外部模型");
    }

    /** 估算成本 (假设平均 200 tokens) */
    private double estimateCost(Model m, int textLen) {
        int tokens = (int) (textLen * 0.25 + 50); // 粗估
        return m.getInputCostPer1K() * tokens / 1000.0;
    }

    /**
     * 批量路由（一次返回多个候选模型，按优先级排序）。
     */
    public List<Model> routeCandidates(String text, String intentHint) {
        RouteResult r = route(text, intentHint, null);
        return List.of(r.getPrimaryModel(), r.getFallbackModel());
    }
}
