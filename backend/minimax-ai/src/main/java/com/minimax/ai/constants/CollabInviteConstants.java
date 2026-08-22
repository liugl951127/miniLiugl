package com.minimax.ai.constants;

/**
 * 协作邀请相关常量 (T3-new-code-robustness)
 *
 * @since V7.2
 */
public final class CollabInviteConstants {

    private CollabInviteConstants() {}

    /** 邀请状态: 待接受 */
    public static final String STATUS_PENDING = "PENDING";
    /** 邀请状态: 已接受 */
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    /** 邀请状态: 已过期 */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "inv-";
    /** Token 截取长度 (UUID 去横线后 32 字符的子串) */
    public static final int TOKEN_BODY_LENGTH = 24;

    /** 默认邀请过期天数 */
    public static final int DEFAULT_EXPIRE_DAYS = 14;
}
