package com.minimax.common.async;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步任务持久化表 (V4.3 补 entity).
 * 对应表: async_task (在 13_optimization.sql)
 *
 * 把 AsyncTaskService 的状态从 ConcurrentMap 搬到 DB.
 */
@Data
@TableName("async_task")
public class AsyncTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务 UUID */
    private String taskId;

    /** 任务类型 (email/export/import 等) */
    private String taskType;

    /** 任务状态: pending/running/done/failed/cancelled */
    private String status;

    /** 输入参数 JSON */
    private String params;

    /** 结果 JSON */
    private String result;

    /** 错误信息 */
    private String errorMsg;

    /** 重试次数 */
    private Integer retryCount;

    /** 耗时 ms */
    private Long latencyMs;

    /** 提交者 user_id */
    private Long submitterId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime finishedAt;

    @TableLogic
    private Integer deleted;
}
