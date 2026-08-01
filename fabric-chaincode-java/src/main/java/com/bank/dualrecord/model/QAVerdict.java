package com.bank.dualrecord.model;

/**
 * 智能质检结论
 */
public enum QAVerdict {
    HIGH_PASS("HIGH_PASS", "高分通过", 90, 100),
    PASS("PASS", "通过", 70, 89.99),
    REVIEW("REVIEW", "需复检", 50, 69.99),
    FAIL("FAIL", "未通过", 0, 49.99);

    private final String code;
    private final String desc;
    private final double minScore;
    private final double maxScore;

    QAVerdict(String code, String desc, double minScore, double maxScore) {
        this.code = code;
        this.desc = desc;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static QAVerdict fromScore(double score) {
        for (QAVerdict v : values()) {
            if (score >= v.minScore && score <= v.maxScore) return v;
        }
        return FAIL;
    }
}
