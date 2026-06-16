package com.minimax.common.constants;

/**
 * 公共常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    public static final String HEADER_AUTH = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 默认分页 */
    public static final long DEFAULT_PAGE_NUM = 1L;
    public static final long DEFAULT_PAGE_SIZE = 20L;
    public static final long MAX_PAGE_SIZE = 200L;

    /** 系统角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_VIP = "VIP";
}
