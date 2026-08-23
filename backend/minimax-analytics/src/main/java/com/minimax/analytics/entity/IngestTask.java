package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件导入任务 (V5.31)
 *
 * 用户上传 csv/json/log 文件, 异步解析, 完成后生成质量报告
 */
@Data
@TableName("analytics_ingest_task")
public class IngestTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("task_id")
    private String taskId;           // UUID, 对外 ID
    @TableField("filename")
    private String filename;         // 原始文件名
    @TableField("file_type")
    private String fileType;         // csv / json / log / tsv
    @TableField("encoding")
    private String encoding;         // UTF-8 / GBK
    @TableField("separator")
    private String separator;        // 解析分隔符 (csv/tsv)
    @TableField("file_size")
    private Long fileSize;           // 字节
    @TableField("status")
    private String status;           // PENDING / PARSING / READY / FAILED
    @TableField("error_message")
    private String errorMessage;     // 失败原因
    @TableField("quality_json")
    private String qualityJson;      // 质量报告 JSON
    @TableField("total_rows")
    private Long totalRows;          // 解析后行数
    @TableField("total_columns")
    private Integer totalColumns;       // 列数
    @TableField("columns_json")
    private String columnsJson;      // 列名 JSON 数组

    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
