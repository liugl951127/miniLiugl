package com.minimax.common.request;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求日志表 (V4.3 补 entity).
 * 对应表: request_log (在 13_optimization.sql)
 *
 * 把 RequestLogFilter 的关键日志(slow/error)异步入库, 替代纯 log 输出.
 */
@Data
@TableName("request_log")
public class RequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链路追踪 ID (UUID) */
    private String traceId;

    /** 请求方法 */
    private String method;

    /** 请求路径 */
    private String path;

    /** query string */
    private String queryString;

    /** 客户端 IP */
    private String clientIp;

    /** User-Agent */
    private String userAgent;

    /** 用户 ID (未登录为 NULL) */
    private Long userId;

    /** 响应状态码 */
    private Integer status;

    /** 耗时 ms */
    private Long latencyMs;

    /** 是否慢请求 (>1000ms) */
    private Boolean slow;

    /** 是否错误 (>=500) */
    private Boolean error;

    /** 模块 (从 path 前缀提取, /api/v1/auth → auth) */
    private String module;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
