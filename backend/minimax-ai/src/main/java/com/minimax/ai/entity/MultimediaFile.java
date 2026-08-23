package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * MultimediaFile (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 数据库实体 - MultimediaFile.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 MultimediaFile 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@TableName("multimedia_file")
public class MultimediaFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("file_id")
    private String fileId;
    @TableField("user_id")
    private Long userId;
    @TableField("username")
    private String username;
    @TableField("file_name")
    private String fileName;
    @TableField("original_name")
    private String originalName;
    @TableField("file_type")
    private String fileType;
    @TableField("mime_type")
    private String mimeType;
    @TableField("file_size")
    private Long fileSize;
    @TableField("file_hash")
    private String fileHash;
    @TableField("storage_path")
    private String storagePath;
    @TableField("storage_type")
    private String storageType;
    @TableField("encrypted")
    private Integer encrypted;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("width")
    private Integer width;
    @TableField("height")
    private Integer height;
    @TableField("bitrate")
    private Integer bitrate;
    @TableField("sample_rate")
    private Integer sampleRate;
    @TableField("channels")
    private Integer channels;
    @TableField("codec")
    private String codec;
    @TableField("exif")
    private String exif;
    @TableField("moderation_status")
    private String moderationStatus;
    @TableField("moderation_id")
    private Long moderationId;
    @TableField("watermarked")
    private Integer watermarked;
    @TableField("is_public")
    private Integer isPublic;
    @TableField("access_count")
    private Integer accessCount;
    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
