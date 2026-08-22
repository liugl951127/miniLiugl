package com.minimax.ai.constants;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 训练模型相关常量 (T3-new-code-robustness: 抽取魔法字符串)
 *
 * @since V7.2
 */
public final class TrainedModelConstants {

    private TrainedModelConstants() {}

    /** 状态: 启用 */
    public static final String STATUS_ENABLED = "ENABLED";
    /** 状态: 禁用 */
    public static final String STATUS_DISABLED = "DISABLED";
    /** 状态: 草稿 */
    public static final String STATUS_DRAFT = "DRAFT";

    /** 允许的状态集合 */
    public static final Set<String> ALLOWED_STATUS =
            Set.of(STATUS_ENABLED, STATUS_DISABLED, STATUS_DRAFT);

    /** 测试推理模拟耗时下限 (ms) */
    public static final long TEST_LATENCY_MIN_MS = 50L;
    /** 测试推理模拟耗时上限 (ms) */
    public static final long TEST_LATENCY_RAND_MS = 250L;
    /** 准确率兜底值 (无有效 accuracy 时返回) */
    public static final BigDecimal DEFAULT_ACCURACY_PCT = new BigDecimal("87.5");
}
