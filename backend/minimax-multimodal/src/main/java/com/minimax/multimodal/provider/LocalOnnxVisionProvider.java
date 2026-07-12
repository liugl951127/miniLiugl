package com.minimax.multimodal.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地 ONNX 模型提供者 (V3.1.0 真实模型接入)
 *
 * <p>V3.0.1 框架, V3.1.0 接入真实 ONNX Runtime Java 推理.
 *
 * <p>设计:
 *   - 加载本地 .onnx 模型 (e.g. MobileNetV3, CLIP-ViT-B/32, ResNet50)
 *   - 走 {@link OnnxRuntimeService} 推理管线
 *   - 适合隐私数据 / 离线环境 / 高频调用
 *
 * <h3>启用方法</h3>
 * <ol>
 *   <li>下载预训练 ONNX 模型 (.onnx 文件)</li>
 *   <li>放到 {@code ${MINIMAX_MODEL_DIR:-/var/minimax/models}/vision/}</li>
 *   <li>配置 {@code minimax.multimodal.local.model-file=mobilenetv3.onnx}</li>
 *   <li>配置 {@code minimax.multimodal.local.input-name=input} (默认)</li>
 *   <li>配置 {@code minimax.multimodal.local.input-size=224} (默认)</li>
 * </ol>
 *
 * <h3>线程安全</h3>
 * OnnxRuntimeService 内含 ThreadLocal<OrtSession>, 支持多线程并发.
 */
@Slf4j
@Component
public class LocalOnnxVisionProvider implements MultimodalModelProvider {

    /** Provider 名 */
    private static final String PROVIDER_NAME = "local-onnx";

    /** ONNX 模型目录 */
    @Value("${minimax.multimodal.local.model-dir:/var/minimax/models/vision}")
    private String modelDir;

    /** 模型文件名 */
    @Value("${minimax.multimodal.local.model-file:mobilenetv3.onnx}")
    private String modelFile;

    /** 输入节点名 */
    @Value("${minimax.multimodal.local.input-name:input}")
    private String inputName;

    /** 输入尺寸 (默认 224x224) */
    @Value("${minimax.multimodal.local.input-size:224}")
    private int inputSize;

    /** 推理服务 (懒加载) */
    private volatile OnnxRuntimeService runtimeService;

    /** 是否启用 (模型就绪 + 加载成功) */
    private volatile boolean enabled = false;

    /**
     * 启动时尝试加载模型
     */
    @PostConstruct
    public void init() {
        // 1. 检查路径
        if (modelDir == null || modelFile == null) {
            log.info("[{}] 未配置 modelDir/modelFile, 跳过加载", PROVIDER_NAME);
            return;
        }
        // 2. 检查文件存在
        File f = new File(modelDir, modelFile);
        if (!f.exists() || !f.isFile() || f.length() == 0) {
            log.info("[{}] 模型文件不存在: {} (fallback 到 builtin/mock)", PROVIDER_NAME, f.getAbsolutePath());
            return;
        }
        // 3. 尝试加载
        try {
            this.runtimeService = new OnnxRuntimeService(f.getAbsolutePath());
            this.enabled = true;
            log.info("[{}] 模型加载成功: {} ({} MB)", PROVIDER_NAME, f.getAbsolutePath(), f.length() / 1024 / 1024);
        } catch (Exception e) {
            log.warn("[{}] 模型加载失败: {}, fallback 到 builtin", PROVIDER_NAME, e.getMessage());
            this.enabled = false;
        }
    }

    /**
     * 关闭时释放资源
     */
    @PreDestroy
    public void destroy() {
        if (runtimeService != null) {
            runtimeService.close();
            log.info("[{}] 已关闭", PROVIDER_NAME);
        }
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public String description() {
        return "本地 ONNX 模型 (V3.1.0 真实推理)";
    }

    /**
     * 是否就绪: 模型加载成功
     */
    @Override
    public boolean isReady() {
        return enabled && runtimeService != null;
    }

    /**
     * 调用本地模型描述图像
     */
    @Override
    public String describe(String imageBase64, String mimeType, String prompt) throws Exception {
        // 1. 检查就绪
        if (!isReady()) {
            return "【本地 ONNX】模型未就绪\n" +
                   "路径: " + (modelDir == null ? "(null)" : new File(modelDir, modelFile).getAbsolutePath()) + "\n" +
                   "请下载 ONNX 模型后放置到此目录, 或切换 builtin/openai provider";
        }
        // 2. 解码 base64
        byte[] imageBytes = decodeBase64(imageBase64);
        // 3. 分类推理
        Map<String, Object> result = runtimeService.classify(imageBytes, inputName, inputSize);
        // 4. 提取结果
        if (result.containsKey("error")) {
            return "【本地 ONNX】推理失败: " + result.get("error");
        }
        int topClass = (int) result.getOrDefault("topClassIdx", -1);
        float topProb = (float) result.getOrDefault("topProb", 0f);
        int numClasses = (int) result.getOrDefault("numClasses", 0);
        // 5. 拼装输出
        StringBuilder sb = new StringBuilder();
        sb.append("【本地 ONNX 推理】\n");
        sb.append("模型: ").append(modelFile).append("\n");
        sb.append("Top 类别: ").append(topClass).append("\n");
        sb.append("置信度: ").append(String.format("%.4f", topProb)).append("\n");
        sb.append("类别总数: ").append(numClasses).append("\n");
        if (prompt != null && !prompt.isEmpty()) {
            sb.append("用户提示: ").append(prompt).append("\n");
        }
        return sb.toString();
    }

    /**
     * 解码 base64 (支持 data:image/...;base64, 前缀)
     */
    private byte[] decodeBase64(String base64) {
        // 1. 剥前缀
        String data = base64;
        if (data.contains(",")) {
            data = data.substring(data.indexOf(",") + 1);
        }
        // 2. Base64 解码
        return Base64.getDecoder().decode(data);
    }

    /** inspect 委托 */
    @Override
    public Map<String, Object> inspect(String imageBase64) {
        return ImageInspector.inspect(imageBase64);
    }

    /**
     * 模型元信息
     */
    public Map<String, Object> modelInfo() {
        Map<String, Object> info = new HashMap<>();
        // 1. 路径信息
        if (modelDir == null || modelFile == null) {
            info.put("path", "(not configured)");
            info.put("exists", false);
            info.put("sizeBytes", 0);
            info.put("ready", false);
            return info;
        }
        File f = new File(modelDir, modelFile);
        info.put("path", f.getAbsolutePath());
        info.put("exists", f.exists());
        info.put("sizeBytes", f.exists() ? f.length() : 0);
        info.put("ready", isReady());
        // 2. 模型结构信息 (加载成功时)
        if (isReady() && runtimeService != null) {
            try {
                info.put("metadata", runtimeService.modelMetadata());
                info.put("inputName", inputName);
                info.put("inputSize", inputSize);
            } catch (Exception e) {
                info.put("metadataError", e.getMessage());
            }
        }
        return info;
    }
}
