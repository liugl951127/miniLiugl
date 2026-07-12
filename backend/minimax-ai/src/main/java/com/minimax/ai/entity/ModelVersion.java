package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型版本 (V3.3.2)
 *
 * <p>一个 ModelEntry 可有多个版本 (v1.0, v1.1, v2.0 ...), 每个版本独立文件
 */
@Data
@TableName("model_version")
public class ModelVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 versionId (UUID) */
    private String versionId;
    /** 关联的 modelEntry (逻辑外键) */
    private Long modelEntryId;
    /** 版本号 (e.g. "1.0.0", "2.3.1-beta") */
    private String version;
    /** 版本说明 */
    private String changelog;
    /** 文件路径 (相对 ${MINIMAX_MODEL_DIR}/) */
    private String filePath;
    /** 文件大小 (字节) */
    private Long sizeBytes;
    /** SHA256 */
    private String sha256;
    /** 输入 schema (e.g. "text/plain", "image/jpeg") */
    private String inputSchema;
    /** 输出 schema (e.g. "json", "text") */
    private String outputSchema;
    /** 状态: DRAFT / PUBLISHED / DEPRECATED / YANKED */
    private String status;
    /** 是否最新 */
    private Boolean isLatest;
    /** 上传人 */
    private Long uploaderId;
    /** 兼容的上一版本 (e.g. "1.0.x") */
    private String backwardCompatible;
    /** 额外元数据 JSON */
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
