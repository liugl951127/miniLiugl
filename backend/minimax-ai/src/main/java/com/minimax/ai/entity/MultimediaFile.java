package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
