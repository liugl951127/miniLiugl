package com.minimax.ai.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单条评测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalResult {

    /** 用例 ID */
    public String caseId;

    /** 类别 */
    public String category;

    /** 问题 */
    public String question;

    /** AI 回答 */
    public String answer;

    /** AI 分数 (0-1) */
    public Double score;

    /** 是否通过 */
    public boolean passed;

    /** 失败原因 (pass=true 时为 null) */
    public String reason;

    /** 评估耗时 (ms) */
    public long latencyMs;

    /** 命中的 must_contain */
    public List<String> hitMustContain;

    /** 漏掉的 must_contain */
    public List<String> missMustContain;

    /** 命中的 expected_keywords */
    public List<String> hitKeywords;

    /** 触发的 must_not_contain (违规则 fail) */
    public List<String> triggeredForbidden;
}
