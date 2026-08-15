package com.minimax.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),
    FAIL(500, "fail"),

    // ============ 通用错误 1xxx ============
    BAD_REQUEST(1001, "请求参数错误"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无访问权限"),
    NOT_FOUND(1004, "资源不存在"),
    METHOD_NOT_ALLOWED(1005, "请求方法不允许"),
    RATE_LIMIT(1006, "请求过于频繁，请稍后再试"),

    // ============ 用户模块 2xxx ============
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "用户名或密码错误"),
    USER_DISABLED(2003, "用户已被禁用"),
    USER_EXISTS(2004, "用户已存在"),
    USER_TOKEN_INVALID(2005, "Token 无效"),
    USER_TOKEN_EXPIRED(2006, "Token 已过期"),

    // ============ 会话模块 3xxx ============
    SESSION_NOT_FOUND(3001, "会话不存在"),
    SESSION_CREATE_FAIL(3002, "会话创建失败"),

    // ============ 模型模块 4xxx ============
    MODEL_NOT_FOUND(4001, "模型不存在"),
    MODEL_CALL_FAIL(4002, "模型调用失败"),
    MODEL_TIMEOUT(4003, "模型调用超时"),

    // ============ 知识库模块 5xxx ============
    KB_NOT_FOUND(5001, "知识库不存在"),
    KB_FILE_UPLOAD_FAIL(5002, "文件上传失败"),
    KB_FILE_PARSE_FAIL(5003, "文件解析失败"),

    // ============ 系统级 9xxx ============
    SYSTEM_BUSY(9001, "系统繁忙，请稍后再试"),
    SYSTEM_ERROR(9999, "系统异常");

    private final Integer code;
    private final String message;
}
