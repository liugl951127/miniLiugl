package com.minimax.ai.eval;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评测用例 (EvalCase)
 *
 * JSON 格式:
 * <pre>
 * {
 *   "id": "java-001",
 *   "category": "编程/Java",
 *   "question": "Java 是什么",
 *   "expected_keywords": ["面向对象", "Sun"],
 *   "must_contain": ["Java"],
 *   "must_not_contain": ["不知道", "未训练"],
 *   "expected_score": 0.7,
 *   "session_id": null,
 *   "tags": ["java", "基础"]
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalCase {

    /** 用例 ID, 唯一 */
    @JsonProperty("id")
    private String id;

    /** 分类: 编程/Java, 金融, 医疗, 法律, ... */
    @JsonProperty("category")
    private String category;

    /** 用户问题 */
    @JsonProperty("question")
    private String question;

    /** 期望包含的关键词 (任一命中即可) */
    @JsonProperty("expected_keywords")
    private List<String> expectedKeywords;

    /** 必须包含的关键词 (全部命中) */
    @JsonProperty("must_contain")
    private List<String> mustContain;

    /** 必须不包含的关键词 (任一出现即失败) */
    @JsonProperty("must_not_contain")
    private List<String> mustNotContain;

    /** 最低分数阈值 */
    @JsonProperty("expected_score")
    private Double expectedScore;

    /** 会话 ID (多轮测试) */
    @JsonProperty("session_id")
    private String sessionId;

    /** 标签 (用于过滤) */
    @JsonProperty("tags")
    private List<String> tags;

    /** 难度: 1-5 */
    @JsonProperty("difficulty")
    private Integer difficulty;

    /**
     * 校验用例是否合法
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && question != null && !question.isEmpty();
    }
}
