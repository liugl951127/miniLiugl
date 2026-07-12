package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 模型市场条目 (V2.9.1)
 *
 * <p>用户可上传训练好的模型权重到市场, 共享给其他用户使用.</p>
 *
 * <h3>支持的模型类型</h3>
 * <ul>
 *   <li>PYTORCH: PyTorch state_dict (.pt/.pth/.bin)</li>
 *   <li>TENSORFLOW: TF SavedModel / .h5</li>
 *   <li>ONNX: ONNX 跨框架 (.onnx)</li>
 *   <li>SAFETENSORS: HF 标准 (.safetensors)</li>
 *   <li>GGUF: llama.cpp 量化 (.gguf)</li>
 *   <li>OTHER: 其他</li>
 * </ul>
 *
 * <h3>许可协议</h3>
 * <ul>
 *   <li>MIT / APACHE_2_0 / GPL_3 / CC_BY_4 / COMMERCIAL / PROPRIETARY</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.9.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("model_market")
public class ModelEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型唯一标识 (URL slug) */
    @TableField("modelKey")
    private String modelKey;

    /** 模型名 */
    @TableField("name")
    private String name;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 模型类型: PYTORCH/TENSORFLOW/ONNX/SAFETENSORS/GGUF/OTHER */
    @TableField("modelType")
    private String modelType;

    /** 任务类型: TEXT_CLASSIFICATION/NER/SUMMARIZATION/GENERATION/EMBEDDING/IMAGE_CLASSIFICATION/... */
    @TableField("taskType")
    private String taskType;

    /** 基础模型 (e.g. "minimax-7b", "bert-base-chinese") */
    @TableField("baseModel")
    private String baseModel;

    /** 版本 (semver) */
    @TableField("version")
    private String version;

    /** 文件路径 (相对存储根) */
    @TableField("filePath")
    private String filePath;

    /** 文件名 */
    @TableField("fileName")
    private String fileName;

    /** 文件大小 (字节) */
    @TableField("fileSize")
    private Long fileSize;

    /** SHA256 校验 */
    @TableField("sha256")
    private String sha256;

    /** 许可: MIT/APACHE_2_0/GPL_3/CC_BY_4/COMMERCIAL/PROPRIETARY */
    @TableField("license")
    private String license;

    /** 作者用户 ID */
    @TableField("authorId")
    private Long authorId;

    /** 作者用户名 */
    @TableField("authorName")
    private String authorName;

    /** 标签 (逗号分隔) */
    @TableField("tags")
    private String tags;

    /** 指标 JSON (loss/accuracy/...) */
    @TableField("metricsJson")
    private String metricsJson;

    /** 状态: DRAFT/PUBLISHED/DEPRECATED */
    @TableField("status")
    private String status;

    /** 下载次数 */
    @TableField("downloadCount")
    private Long downloadCount;

    /** 平均评分 (1-5) */
    @TableField("avgRating")
    private Double avgRating;

    /** 评分次数 */
    @TableField("ratingCount")
    private Long ratingCount;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updatedAt", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("publishedAt")
    private LocalDateTime publishedAt;
}
