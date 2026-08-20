package com.minimax.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地 ONNX 模型配置 (V7.0: 替代数据库 model_config 中的 onnx provider)
 *
 * 用法: 在 application.yml 中配置 minimax.model.local-onnx:
 *   enabled: true
 *   model-name: mini-transformer        # 推理服务上的模型标识
 *   display-name: Mini Transformer      # 前端显示名
 *   max-context: 512
 *   max-output: 256
 *
 * 当 enabled=true 且数据库无 onnx 模型时，ModelServiceImpl 会自动使用此配置。
 *
 * @since 2026-08-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "minimax.model.local-onnx")
public class OnnxModelConfig {

    /** 是否启用本地 ONNX 模型（优先级低于数据库配置） */
    private boolean enabled = false;

    /** 推理服务地址（http://host:port），留空则用 minimax-ai 本地推理 */
    private String inferenceUrl = "";

    /** 模型标识（传给推理服务的模型名） */
    private String modelName = "mini-transformer";

    /** 前端显示名称 */
    private String displayName = "Mini Transformer";

    /** 最大上下文 token 数 */
    private int maxContext = 512;

    /** 最大输出 token 数 */
    private int maxOutput = 256;

    /** 是否支持工具调用 */
    private boolean supportsTools = false;

    /** 是否支持流式 */
    private boolean supportsStream = true;
}
