package com.minimax.model.init;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 自研 ONNX 模型配置属性 (V7.2)
 *
 * 绑定 application-common.yml 中的 minimax.self-models 配置节。
 * 配置驱动，无硬编码模型数据。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minimax.self-models")
public class SelfModelsProperties {

    /** 是否启用自研模型加载 */
    private boolean enabled = true;

    /** ONNX 模型文件所在目录（扫描 .onnx 文件自动发现） */
    private String baseDir = "/opt/minimax/models";

    /** 自研 provider 元信息 */
    private Provider provider = new Provider();

    /** 显式模型定义列表 */
    private List<ModelDef> models = new ArrayList<>();

    @Data
    public static class Provider {
        private String code = "onnx";
        private String name = "自研训练模型";
        private String description = "平台自主训练，通过 ONNX 文件加载";
    }

    @Data
    public static class ModelDef {
        /** 模型代码，对应 .onnx 文件名（不含后缀） */
        private String code;
        /** 前端显示名称 */
        private String displayName;
        /** 模型描述 */
        private String description = "";
        /** 是否支持视觉（多模态） */
        private boolean supportsVision = false;
        /** 是否支持流式输出 */
        private boolean supportsStream = true;
        /** 最大上下文 token 数 */
        private int maxContext = 4096;
        /** 最大输出 token 数 */
        private int maxOutput = 1024;
        /** 输入价格（元/千token），默认 0 */
        private BigDecimal inputPrice = BigDecimal.ZERO;
        /** 输出价格（元/千token），默认 0 */
        private BigDecimal outputPrice = BigDecimal.ZERO;
    }
}
