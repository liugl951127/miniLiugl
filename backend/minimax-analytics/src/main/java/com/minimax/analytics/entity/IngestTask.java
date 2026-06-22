package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private Long userId;
    private String taskId;           // UUID, 对外 ID
    private String filename;         // 原始文件名
    private String fileType;         // csv / json / log / tsv
    private String encoding;         // UTF-8 / GBK
    private String separator;        // 解析分隔符 (csv/tsv)
    private Long fileSize;           // 字节
    private String status;           // PENDING / PARSING / READY / FAILED
    private String errorMessage;     // 失败原因
    private String qualityJson;      // 质量报告 JSON
    private Long totalRows;          // 解析后行数
    private Long totalColumns;       // 列数
    private String columnsJson;      // 列名 JSON 数组

    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
