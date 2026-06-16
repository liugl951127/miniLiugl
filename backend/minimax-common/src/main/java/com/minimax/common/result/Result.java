package com.minimax.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

@Slf4j

/**
 * 统一响应包装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码，0 表示成功 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 服务端时间戳（毫秒） */
    private Long timestamp;

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(0, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /** 直接序列化为 JSON 字符串，认证失败等需要手写响应体时使用。 */
    public String toJsonString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Result 序列化失败", e);
            return "{\"code\":500,\"message\":\"serialize error\"}";
        }
    }
}
