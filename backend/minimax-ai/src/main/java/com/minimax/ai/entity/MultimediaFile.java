package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private String fileId;
    private Long userId;
    private String username;
    private String fileName;
    private String originalName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private String storagePath;
    private String storageType;
    private Integer encrypted;
    private Long durationMs;
    private Integer width;
    private Integer height;
    private Integer bitrate;
    private Integer sampleRate;
    private Integer channels;
    private String codec;
    private String exif;
    private String moderationStatus;
    private Long moderationId;
    private Integer watermarked;
    private Integer isPublic;
    private Integer accessCount;
    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
