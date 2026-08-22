package com.minimax.auth.constants;

import java.util.Set;

/**
 * 通知设置相关常量 (T3-new-code-robustness: 抽取魔法字符串)
 *
 * @since V7.2
 */
public final class NotificationSettingsConstants {

    private NotificationSettingsConstants() {}

    /** 允许的通知渠道 */
    public static final Set<String> ALLOWED_CHANNELS =
            Set.of("email", "sms", "dingtalk", "webhook", "push");
    /** 允许的通知事件 */
    public static final Set<String> ALLOWED_EVENTS =
            Set.of("login", "error", "alert", "system");

    /** 默认渠道 (CSV) */
    public static final String DEFAULT_CHANNELS = "email,webhook";
    /** 默认事件 (CSV) */
    public static final String DEFAULT_EVENTS = "login,error,alert,system";
    /** 默认免打扰开始 */
    public static final String DEFAULT_QUIET_START = "22:00";
    /** 默认免打扰结束 */
    public static final String DEFAULT_QUIET_END = "08:00";

    /** HH:mm 校验正则 */
    public static final String HHMM_PATTERN = "^\\d{2}:\\d{2}$";
}
