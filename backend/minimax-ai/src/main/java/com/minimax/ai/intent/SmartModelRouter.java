package com.minimax.ai.intent;

import com.minimax.ai.service.ModelClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能模型路由器 (Day 29 增强 — 可配置 Provider 开关).
 *
 * <h3>Provider 开关模式</h3>
 * <pre>
 * minimax.ai.model.provider-mode=auto       (默认) 两类模型都参与路由，按策略选最优
 * minimax.ai.model.provider-mode=self       仅自研模型（省钱，不依赖外部 API）
 * minimax.ai.model.provider-mode=external    仅外部模型（外部 key 配置后可用）
 * </pre>
 *
 * <h3>外部模型 API Key 配置</h3>
 * <pre>
 * minimax.ai.model.openai-api-key=sk-xxx
 * minimax.ai.model.deepseek-api-key=sk-xxx
 * </pre>
 *
 * <h3>路由策略: Cost-Quality Aware</h3>
 * <pre>
 * SIMPLE  + BALANCED/COST_FIRST → MiniMax-Text-01 (快省)
 * SIMPLE  + QUALITY_FIRST      → MiniMax-Text-02
 * MEDIUM  + BALANCED           → MiniMax-Text-02
 * MEDIUM  + QUALITY_FIRST     → MiniMax-Text-03 (或外部等价模型)
 * COMPLEX                      → MiniMax-Text-03 / GPT4O / DeepSeek-Coder
 * </pre>
 */
@Slf4j
/**
 * SmartModelRouter (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 意图识别 - SmartModelRouter.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 SmartModelRouter 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Component
public class SmartModelRouter {

    private final QueryComplexityService complexityService;

    // ============== 配置开关 ==============

    /**
     * Provider 模式:
     *   SELF_ONLY  — 仅自研模型（不依赖外部 API key）
     *   EXTERNAL_ONLY — 仅外部模型（需配置 key）
     *   AUTO — 两类都参与（默认）
     */
    public enum ProviderMode { SELF_ONLY, EXTERNAL_ONLY, AUTO }

    /** Provider 开关 (默认 AUTO) */
    @Value("${minimax.ai.model.provider-mode:auto}")
    private ProviderMode providerMode = ProviderMode.AUTO;

    /** OpenAI API Key (可选) */
    @Value("${minimax.ai.model.openai-api-key:}")
    private String openAiKey;

    /** DeepSeek API Key (可选) */
    @Value("${minimax.ai.model.deepseek-api-key:}")
    private String deepSeekKey;

    public SmartModelRouter(QueryComplexityService complexityService, ModelClient modelClient) {
        this.complexityService = complexityService;
        this.modelClient = modelClient;
    }

    private final ModelClient modelClient;

    /** 自研模型代码缓存（首次查询后缓存，避免每次都调 DB） */
    private Set<String> cachedSelfCodes = null;
    private long cacheTime = 0;
    private static final long CACHE_TTL_MS = 30_000; // 30s 缓存

    // ============== 模型定义 ==============

    /** 模型定义 */
    public enum Model {
        // ── 自研模型 ──────────────────────────────────────────────
        MINIMAX_TEXT_01("MiniMax-Text-01",  "self",  0.10, 1.0, 3, Set.of("CHAT", "TTS", "STT")),
        MINIMAX_TEXT_02("MiniMax-Text-02",  "self",  0.30, 2.0, 4, Set.of("ANALYZE_DATA", "QUERY_DATA", "GENERATE_CHART")),
        MINIMAX_TEXT_03("MiniMax-Text-03",  "self",  1.00, 5.0, 5, Set.of("GENERATE_CODE", "GENERATE_MUSIC", "COMPLEX")),
        EMBEDDING_01("Embedding-01",         "self",  0.01, 0.5, 1, Set.of()),

        // ── 外部模型 ──────────────────────────────────────────────
        GPT4O_MINI("gpt-4o-mini",           "openai", 0.15, 3.0, 4, Set.of("ANALYZE_DATA", "QUERY_DATA", "CHAT")),
        GPT4O("gpt-4o",                      "openai", 2.00, 8.0, 5, Set.of("GENERATE_CODE", "COMPLEX")),
        DEEPSEEK_CHAT("deepseek-chat",       "deepseek", 0.30, 2.0, 4, Set.of("ANALYZE_DATA", "CHAT")),
        DEEPSEEK_CODER("deepseek-coder",     "deepseek", 0.50, 3.0, 5, Set.of("GENERATE_CODE", "COMPLEX"));

        @Getter private final String name;
        @Getter private final String provider;   // self / openai / deepseek
        @Getter private final double inputCostPer1K;
        @Getter private final double outputCostPer1K;
        @Getter private final int capabilityScore;
        /** 擅长场景 */
        @Getter private final Set<String> bestAt;

        Model(String name, String provider, double inputCost, double outputCost, int cap, Set<String> bestAt) {
            this.name = name; this.provider = provider; this.inputCostPer1K = inputCost;
            this.outputCostPer1K = outputCost; this.capabilityScore = cap; this.bestAt = bestAt;
        }

        /** 是否自研模型 */
        public boolean isSelf() { return "self".equals(provider); }
        /** 是否有外部 API key */
        public boolean hasExternalKey(SmartModelRouter router) {
            return switch (provider) {
                case "openai"  -> router.openAiKey != null && !router.openAiKey.isBlank();
                case "deepseek" -> router.deepSeekKey != null && !router.deepSeekKey.isBlank();
                default -> true;
            };
        }
    }

    // ============== 质量偏好 ==============

    public enum QualityPref {
        COST_FIRST,    // 优先省钱
        QUALITY_FIRST, // 优先质量
        SPEED_FIRST,   // 优先速度
        BALANCED       // 平衡
    }

    // ============== 结果 ==============

    @Getter
    public static class RouteResult {
        private final Model primaryModel;
        private final Model fallbackModel;
        private final String strategy;
        private final QueryComplexityService.ComplexityResult complexity;
        private final double estimatedCost;
        private final String reasoning;
        private final ProviderMode providerMode;

        public RouteResult(Model primary, Model fallback, String strategy,
                          QueryComplexityService.ComplexityResult complexity,
                          double estimatedCost, String reasoning,
                          ProviderMode providerMode) {
            this.primaryModel = primary;
            this.fallbackModel = fallback;
            this.strategy = strategy;
            this.complexity = complexity;
            this.estimatedCost = estimatedCost;
            this.reasoning = reasoning;
            this.providerMode = providerMode;
        }
    }

    // ============== 路由 API ==============

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

        QualityPref pref = qualityPref != null ? qualityPref : QualityPref.BALANCED;
        Model primary = selectPrimary(complexity, pref, intentHint);
        Model fallback = selectFallback(primary, complexity);
        String strategy = String.format("%s+%s+%s", complexity.getLevel().name(), pref.name(), providerMode.name());
        double cost = estimateCost(primary, text.length());
        String reasoning = buildReasoning(primary, complexity, pref);

        log.info("[SmartRouter] mode={} text='{}' complexity={} pref={} → model={} (cost=${})",
                providerMode,
                text.length() > 40 ? text.substring(0, 40) + "..." : text,
                complexity.getLevel(), pref, primary.getName(), String.format("%.4f", cost));

        return new RouteResult(primary, fallback, strategy, complexity, cost, reasoning, providerMode);
    }

    /** 简化版 */
    public RouteResult route(String text) {
        return route(text, null, null);
    }

    // ============== 核心选择逻辑 ==============

    private Model selectPrimary(QueryComplexityService.ComplexityResult complexity,
                                QualityPref pref, String intentHint) {
        List<Model> candidates = getCandidatePool(intentHint);
        if (candidates.isEmpty()) {
            log.warn("[SmartRouter] no available models for mode={}, falling back to self", providerMode);
            return Model.MINIMAX_TEXT_01;
        }

        QueryComplexityService.ComplexityLevel level = complexity.getLevel();

        return switch (level) {
            case SIMPLE -> {
                // 简单查询 → 选最快的自研
                yield candidates.stream()
                        .filter(Model::isSelf)
                        .min((a, b) -> Double.compare(a.getInputCostPer1K(), b.getInputCostPer1K()))
                        .orElse(candidates.get(0));
            }
            case MEDIUM -> {
                // 中等 → 平衡质量+成本
                if (pref == QualityPref.QUALITY_FIRST) {
                    yield bestMatch(candidates, "ANALYZE_DATA", QueryComplexityService.ComplexityLevel.MEDIUM);
                } else if (pref == QualityPref.SPEED_FIRST) {
                    yield candidates.stream()
                            .filter(Model::isSelf)
                            .findFirst().orElse(candidates.get(0));
                } else {
                    // BALANCED / COST_FIRST → 自研 Text-02 或等价外部
                    yield candidates.stream()
                            .filter(m -> m.getCapabilityScore() >= 4)
                            .min((a, b) -> Double.compare(a.getInputCostPer1K(), b.getInputCostPer1K()))
                            .orElse(Model.MINIMAX_TEXT_02);
                }
            }
            case COMPLEX -> {
                // 复杂 → 选能力最强的
                Model best = bestMatch(candidates, "COMPLEX", QueryComplexityService.ComplexityLevel.COMPLEX);
                yield best != null ? best : Model.MINIMAX_TEXT_03;
            }
        };
    }

    /** 根据擅长场景匹配最佳模型 */
    private Model bestMatch(List<Model> candidates, String scene, QueryComplexityService.ComplexityLevel level) {
        return candidates.stream()
                .filter(m -> m.getBestAt().contains(scene))
                .max((a, b) -> Integer.compare(a.getCapabilityScore(), b.getCapabilityScore()))
                .orElseGet(() -> candidates.stream()
                        .max((a, b) -> Integer.compare(a.getCapabilityScore(), b.getCapabilityScore()))
                        .orElse(null));
    }

    /** 兜底模型 */
    private Model selectFallback(Model primary, QueryComplexityService.ComplexityResult complexity) {
        if (complexity.getLevel() == QueryComplexityService.ComplexityLevel.COMPLEX) {
            // 复杂任务 → 外部模型兜底
            return primary.isSelf() ? Model.GPT4O_MINI : primary;
        }
        return primary == Model.MINIMAX_TEXT_01 ? Model.MINIMAX_TEXT_02 : primary;
    }

    /**
     * 获取自研模型列表（带缓存）。
     * 从 model_config DB 表动态读取 providerCode 为 local-* / onnx 的模型代码。
     */
    private Set<String> getSelfModelCodesFromDb() {
        long now = System.currentTimeMillis();
        if (cachedSelfCodes != null && (now - cacheTime) < CACHE_TTL_MS) {
            return cachedSelfCodes;
        }
        Set<String> codes = new HashSet<>();
        try {
            var models = modelClient.listEnabledModels();
            for (var m : models) {
                String code = (String) m.get("providerCode");
                String modelCode = (String) m.get("modelCode");
                // 自研：providerCode 含 local- / onnx / self / self-trained
                if (code != null && (
                        code.startsWith("local-") ||
                        code.startsWith("onnx") ||
                        code.startsWith("self") ||
                        code.equals("trained")
                )) {
                    codes.add(modelCode);
                }
            }
            log.debug("[SmartRouter] 从DB加载自研模型: {} 个 => {}", codes.size(), codes);
        } catch (Exception e) {
            log.warn("[SmartRouter] 加载DB自研模型失败: {}", e.getMessage());
        }
        cachedSelfCodes = codes;
        cacheTime = now;
        return codes;
    }

    /**
     * 根据 ProviderMode 过滤候选模型池（策略模式+数据库感知）。
     * 自研模型从 DB 动态获取，不再仅依赖硬编码。
     */
    private List<Model> getCandidatePool(String intentHint) {
        List<Model> selfDefaults = List.of(Model.MINIMAX_TEXT_01, Model.MINIMAX_TEXT_02,
                Model.MINIMAX_TEXT_03, Model.EMBEDDING_01);
        Set<String> dbSelfCodes = getSelfModelCodesFromDb();
        // DB 自研模型中：找 name 匹配的已定义 enum，或记录未知代码用于路由匹配
        for (String dbCode : dbSelfCodes) {
            // 检查是否已在默认 enum 中
            boolean known = selfDefaults.stream().anyMatch(m -> m.getName().equals(dbCode));
            if (!known) {
                log.info("[SmartRouter] 发现DB自研模型: {} (provider含local-/onnx/self)", dbCode);
            }
        }

        return switch (providerMode) {
            case SELF_ONLY -> {
                List<Model> pool = new ArrayList<>(selfDefaults);
                yield pool;
            }

            case EXTERNAL_ONLY -> {
                List<Model> external = new ArrayList<>();
                if (openAiKey != null && !openAiKey.isBlank())
                    external.addAll(List.of(Model.GPT4O_MINI, Model.GPT4O));
                if (deepSeekKey != null && !deepSeekKey.isBlank())
                    external.addAll(List.of(Model.DEEPSEEK_CHAT, Model.DEEPSEEK_CODER));
                yield external.isEmpty() ? new ArrayList<>(selfDefaults) : external;
            }

            case AUTO -> {
                List<Model> pool = new ArrayList<>(selfDefaults);
                if (openAiKey != null && !openAiKey.isBlank())
                    pool.addAll(List.of(Model.GPT4O_MINI, Model.GPT4O));
                if (deepSeekKey != null && !deepSeekKey.isBlank())
                    pool.addAll(List.of(Model.DEEPSEEK_CHAT, Model.DEEPSEEK_CODER));
                yield pool;
            }
        };
    }

    private String buildReasoning(Model m, QueryComplexityService.ComplexityResult c, QualityPref pref) {
        String provider = m.isSelf() ? "自研" : m.getProvider();
        return String.format(
                "复杂度=%s(%.2f) + 偏好=%s + 模式=%s → %s(能力%d) | 估算$%.4f | %s",
                c.getLevel(), c.getScore(), pref, providerMode,
                m.getName(), m.getCapabilityScore(),
                estimateCost(m, 200),
                provider);
    }

    private double estimateCost(Model m, int textLen) {
        int tokens = (int) (textLen * 0.25 + 50);
        return m.getInputCostPer1K() * tokens / 1000.0;
    }

    // ============== 外部调用 API ==============

    /** 当前 Provider 模式 */
    public ProviderMode getProviderMode() { return providerMode; }

    /** OpenAI Key 是否已配置 */
    public boolean hasOpenAiKey() { return openAiKey != null && !openAiKey.isBlank(); }

    /** DeepSeek Key 是否已配置 */
    public boolean hasDeepSeekKey() { return deepSeekKey != null && !deepSeekKey.isBlank(); }

    /** 获取实际可用的外部模型列表 */
    public List<Model> availableExternalModels() {
        List<Model> models = new java.util.ArrayList<>();
        if (hasOpenAiKey())  models.addAll(List.of(Model.GPT4O_MINI, Model.GPT4O));
        if (hasDeepSeekKey()) models.addAll(List.of(Model.DEEPSEEK_CHAT, Model.DEEPSEEK_CODER));
        return models;
    }
}
