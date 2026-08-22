package com.minimax.pipeline.constants;

/**
 * 规则相关常量 (T3-new-code-robustness: 抽取魔法字符串)
 *
 * @since V7.2
 */
public final class RuleConstants {

    private RuleConstants() {}

    /** 规则作用域: 全局 */
    public static final String SCOPE_GLOBAL = "GLOBAL";
    /** 规则作用域: 租户 */
    public static final String SCOPE_TENANT = "TENANT";
    /** 规则作用域: 用户 */
    public static final String SCOPE_USER = "USER";

    /** 启用 */
    public static final int ENABLED = 1;
    /** 禁用 */
    public static final int DISABLED = 0;
}
