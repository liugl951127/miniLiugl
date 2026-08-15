package com.minimax.common.audit;

import java.lang.annotation.*;

/**
 * 审计注解，标记在 Controller 或 Service 方法上。
 *
 * <p>自动记录：who / when / where / what / how / result</p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#64;Audited(action = "CREATE_API_KEY", resourceType = "ApiKey")
 * &#64;PostMapping
 * public Result&lt;ApiKeyResponse&gt; create(...) { ... }
 *
 * &#64;Audited(action = "DELETE_USER", resourceType = "User",
 *           resourceId = "#userId")
 * &#64;DeleteMapping("/{userId}")
 * public Result&lt;Void&gt; delete(@PathVariable Long userId) { ... }
 * </pre>
 *
 * @author MiniMax
 * @since V6.8.2
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** 操作名称，如 CREATE_USER / DELETE_API_KEY / LOGIN */
    String action();

    /** 资源类型，如 User / ApiKey / Prompt */
    String resourceType() default "";

    /**
     * SpEL 表达式，从方法参数提取资源 ID。
     * 默认取第一个 Long/Integer 参数。
     * 示例: "#userId" / "#request.keyId"
     */
    String resourceId() default "";

    /**
     * 是否记录请求体（默认 false，避免大 body 拖慢审计）。
     * 设为 true 时，detail 字段记录请求参数。
     */
    boolean logRequestBody() default false;

    /**
     * 是否记录响应体（默认 false，响应数据通常不需要入库）。
     */
    boolean logResponseBody() default false;
}
