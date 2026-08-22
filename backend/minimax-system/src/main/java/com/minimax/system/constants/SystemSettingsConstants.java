package com.minimax.system.constants;

import java.util.Set;

/**
 * 系统设置相关常量 (T3-new-code-robustness: 抽取魔法字符串)
 *
 * @since V7.2
 */
public final class SystemSettingsConstants {

    private SystemSettingsConstants() {}

    /** 单行表固定 ID (永远 = 1) */
    public static final long SINGLETON_ID = 1L;

    /** 维护模式: 关闭 */
    public static final int MAINTENANCE_OFF = 0;
    /** 维护模式: 开启 */
    public static final int MAINTENANCE_ON = 1;

    /** 允许注册: 禁止 */
    public static final int REGISTER_DISABLED = 0;
    /** 允许注册: 允许 */
    public static final int REGISTER_ENABLED = 1;

    /** 0/1 字符集合 (用于校验旧版 Integer 转 String) */
    public static final Set<String> ALLOWED_BOOL = Set.of("0", "1");

    /** 默认站点名 */
    public static final String DEFAULT_SITE_NAME = "MiniMax 平台";
    /** 默认模型编码 */
    public static final String DEFAULT_MODEL_CODE = "gpt-4o";
}
