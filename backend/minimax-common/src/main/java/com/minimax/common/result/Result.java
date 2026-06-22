// =============================================================
// MiniMax - 统一 API 响应包装 (V5.4+)
// =============================================================
//
// 位置: minimax-common 模块 (所有微服务复用)
// 作用: 把 controller 返回值统一包装成 {code, message, data, timestamp} 格式
//       前端只需解析 data 字段, code=0 表示成功, 其他都是失败
//
// 为什么需要统一包装:
//   1. 前端不用为每个接口写不同响应处理逻辑
//   2. 后端能统一处理业务异常 (Result.fail)
//   3. traceId / 错误码标准化
//   4. 时间戳方便前后端时间同步
// =============================================================

// 当前包
package com.minimax.common.result;

// Jackson - 序列化时忽略 null 字段 (避免前端拿到一堆 null)
import com.fasterxml.jackson.annotation.JsonInclude;
// Jackson - 序列化异常
import com.fasterxml.jackson.core.JsonProcessingException;
// Jackson - 序列化工具
import com.fasterxml.jackson.databind.ObjectMapper;
// Lombok - 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;
// Lombok - 自动生成无参构造 (Jackson 反序列化需要)
import lombok.NoArgsConstructor;
// Lombok - 自动生成 log 字段
import lombok.extern.slf4j.Slf4j;

// Java 标准 - 序列化版本 UID
import java.io.Serial;
// Java 标准 - 序列化接口 (分布式 Session / 缓存需要)
import java.io.Serializable;

/**
 * 统一响应包装.
 *
 * 所有 controller 返回值都用 Result&lt;T&gt; 包装:
 *   - 成功: code=0, message="success", data=业务数据
 *   - 失败: code=业务错误码, message=错误描述, data=null
 *
 * @param &lt;T&gt; 业务数据类型
 */
@Slf4j                                                                       // Lombok 生成 log
@Data                                                                       // 自动生成 getter/setter
@NoArgsConstructor                                                              // 自动生成无参构造 (Jackson 需要)
@JsonInclude(JsonInclude.Include.NON_NULL)                                 // null 字段不参与序列化
public class Result<T> implements Serializable {                            // 实现 Serializable (缓存/Session 用)

    // 序列化版本号 (反序列化兼容用)
    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码, 0 表示成功, 其他参考 ResultCode 枚举. */
    private Integer code;

    /** 提示信息 (中文或英文, 给前端展示给用户). */
    private String message;

    /** 业务数据 (泛型, 可以是 List / Map / 实体 / null). */
    private T data;

    /** 服务端时间戳 (毫秒), 前端用来计算请求耗时或时间同步. */
    private Long timestamp;

    /**
     * 全参构造.
     *
     * @param code     状态码
     * @param message  提示信息
     * @param data     业务数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();  // 自动记当前时间
    }

    // =============================================================
    // 静态工厂方法 (ok 成功)
    // =============================================================

    /**
     * 成功响应 (无数据).
     *
     * @param &lt;T&gt; 类型
     * @return Result&lt;T&gt; {code:0, message:"success", data:null}
     */
    public static <T> Result<T> ok() {
        return new Result<>(0, "success", null);
    }

    /**
     * 成功响应 (带数据).
     *
     * @param data 业务数据
     * @param &lt;T&gt; 数据类型
     * @return Result&lt;T&gt; {code:0, data:data}
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    /**
     * 成功响应 (自定义消息 + 数据).
     *
     * @param message 自定义成功消息
     * @param data    业务数据
     * @param &lt;T&gt; 数据类型
     * @return Result&lt;T&gt;
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(0, message, data);
    }

    // =============================================================
    // 静态工厂方法 (fail 失败)
    // =============================================================

    /**
     * 失败响应 (默认 500 错误码).
     *
     * @param message 错误消息
     * @param &lt;T&gt; 类型
     * @return Result&lt;T&gt; {code:500, message:msg}
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败响应 (自定义错误码).
     *
     * @param code    自定义错误码
     * @param message 错误消息
     * @param &lt;T&gt; 类型
     * @return Result&lt;T&gt;
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应 (用 ResultCode 枚举).
     *
     * @param resultCode 业务错误码枚举 (e.g. ResultCode.USER_NOT_FOUND)
     * @param &lt;T&gt; 类型
     * @return Result&lt;T&gt;
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应 (用 ResultCode 枚举 + 自定义覆盖消息).
     *
     * @param resultCode 业务错误码枚举
     * @param message    自定义消息 (覆盖枚举的默认消息)
     * @param &lt;T&gt; 类型
     * @return Result&lt;T&gt;
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /**
     * 直接序列化为 JSON 字符串.
     *
     * 用于 Gateway 的 JwtAuthGlobalFilter 等需要手写响应体的场景 (WebFlux 体系).
     *
     * @return JSON 字符串 (失败时返错误占位 JSON)
     */
    public String toJsonString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // 序列化失败 (极少发生, 通常是循环引用)
            log.error("Result 序列化失败", e);
            return "{\"code\":500,\"message\":\"serialize error\"}";
        }
    }
}
