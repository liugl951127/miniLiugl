package com.minimax.ai.multimodal.onnx;

import ai.onnxruntime.*;
import com.minimax.ai.multimodal.classifier.ImageNetLabels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * ONNX Runtime ResNet50 图像分类服务 (V7.1 多模态升级)
 *
 * <p>支持本地 ONNX ResNet50 推理, 1000 类 ImageNet 分类. 配置启动后从
 * <code>{modelDir}/resnet50/model.onnx</code> 加载.</p>
 *
 * <h3>模型规格</h3>
 * <ul>
 *   <li>输入: float32[1, 3, 224, 224] - NCHW, BGR, 减均值 [0.485, 0.456, 0.406] / std [0.229, 0.224, 0.225]</li>
 *   <li>输出: float32[1, 1000] - 1000 类 logits (需 softmax)</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <pre>
 * minimax.onnx-vision:
 *   enabled: true
 *   resnet50-path: ${ONNX_RESNET50_PATH:./data/models/resnet50/model.onnx}
 * </pre>
 *
 * <h3>降级策略</h3>
 * <p>当模型未加载或推理失败, 返回空列表 + null score, 由调用方决定是否走
 * 外部 API (OpenAI Vision / DeepSeek VL).</p>
 */
@Slf4j
@Service
public class OnnxResNet50Service {

    @Value("${minimax.onnx-vision.enabled:true}")
    private boolean enabled;

    @Value("${minimax.onnx-vision.resnet50-path:./data/models/resnet50/model.onnx}")
    private String resnet50Path;

    @Value("${minimax.onnx-vision.top-k:5}")
    private int defaultTopK;

    @Value("${minimax.onnx-vision.threads:4}")
    private int threads;

    private OrtEnvironment env;
    private OrtSession session;
    private String inputName;
    private String outputName;

    private static final int INPUT_SIZE = 224;
    // ImageNet mean (RGB order)
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD  = {0.229f, 0.224f, 0.225f};

    public boolean isEnabled() { return enabled; }
    public boolean isReady()   { return session != null; }
    public String getModelPath() { return resnet50Path; }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[OnnxResNet50] 禁用 (minimax.onnx-vision.enabled=false)");
            return;
        }
        try {
            File f = new File(resnet50Path);
            if (!f.exists()) {
                log.warn("[OnnxResNet50] 模型文件不存在: {} — 分类功能不可用, 降级到外部 API", resnet50Path);
                return;
            }
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(threads);
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.EXTENDED_OPT);
            session = env.createSession(resnet50Path, opts);

            // 探测输入输出名称
            inputName = session.getInputNames().iterator().next();
            outputName = session.getOutputNames().iterator().next();
            log.info("[OnnxResNet50] ✅ 加载成功: input={} output={} path={}", inputName, outputName, resnet50Path);
        } catch (Exception e) {
            log.error("[OnnxResNet50] 加载失败: {}", e.getMessage(), e);
            session = null;
        }
    }

    @PreDestroy
    public void destroy() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (env != null) env.close(); } catch (Exception ignored) {}
    }

    /**
     * 分类 (top-k)
     *
     * @param image 输入图片
     * @param topK  返回前 K 个结果, 默认 5
     * @return top-k 分类结果, 包含 idx, labelEn, labelCn, probability
     */
    public List<ClassificationResult> classify(BufferedImage image, int topK) {
        if (!isReady() || image == null) {
            return Collections.emptyList();
        }
        if (topK <= 0) topK = defaultTopK;
        try {
            float[] inputData = preprocess(image);
            long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
            OnnxTensor inputTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(inputData), shape);
            try (OrtSession.Result result = session.run(Map.of(inputName, inputTensor))) {
                OnnxTensor outputTensor = (OnnxTensor) result.get(0);
                float[][] output = (float[][]) outputTensor.getValue();
                float[] logits = output[0];

                // softmax
                float[] probs = softmax(logits);
                int[] topKIdx = topKIndices(probs, topK);
                List<ClassificationResult> out = new ArrayList<>(topK);
                for (int idx : topKIdx) {
                    String[] both = ImageNetLabels.getBoth(idx);
                    out.add(new ClassificationResult(idx, both[0], both[1], probs[idx]));
                }
                return out;
            } finally {
                inputTensor.close();
            }
        } catch (Exception e) {
            log.error("[OnnxResNet50] 推理失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 分类 (便捷重载, 用默认 top-k)
     */
    public List<ClassificationResult> classify(BufferedImage image) {
        return classify(image, defaultTopK);
    }

    /**
     * 图像特征向量 (ResNet50 倒数第二层, 2048-dim)
     *
     * <p>复用 preprocess, 但需要模型有 2 个输出. 当前 ResNet50 ONNX model.onnx
     * 只输出 logits, 该方法走 softmax → 取 argmax 类别索引作为"特征" (粗略).</p>
     *
     * <p>如需真·2048 维特征, 需加载带 pre_fc 输出的 ONNX (剪枝模型).</p>
     */
    public float[] extractFeature(BufferedImage image) {
        // 简化: 返回 1000 维概率分布作为图像特征, 用于以文搜图时与 CLIP 维度对齐
        if (!isReady() || image == null) return new float[0];
        try {
            float[] inputData = preprocess(image);
            long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
            OnnxTensor inputTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(inputData), shape);
            try (OrtSession.Result result = session.run(Map.of(inputName, inputTensor))) {
                OnnxTensor outputTensor = (OnnxTensor) result.get(0);
                float[][] output = (float[][]) outputTensor.getValue();
                return softmax(output[0]);
            } finally {
                inputTensor.close();
            }
        } catch (Exception e) {
            log.error("[OnnxResNet50] 特征提取失败: {}", e.getMessage());
            return new float[0];
        }
    }

    // ─── 预处理 ──────────────────────────────────────────

    /**
     * 图像预处理: resize → 中心裁切 → 归一化 → NCHW
     */
    private float[] preprocess(BufferedImage src) {
        BufferedImage resized = resizeKeepRatio(src, INPUT_SIZE, INPUT_SIZE);
        BufferedImage cropped = centerCrop(resized, INPUT_SIZE, INPUT_SIZE);

        float[] data = new float[3 * INPUT_SIZE * INPUT_SIZE];
        int idx = 0;
        for (int c = 0; c < 3; c++) {
            for (int h = 0; h < INPUT_SIZE; h++) {
                for (int w = 0; w < INPUT_SIZE; w++) {
                    int rgb = cropped.getRGB(w, h);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    float[] ch = {r / 255f, g / 255f, b / 255f};
                    data[idx++] = (ch[c] - MEAN[c]) / STD[c];
                }
            }
        }
        return data;
    }

    /** 等比缩放到 (>= target, 后续 centerCrop) */
    private BufferedImage resizeKeepRatio(BufferedImage src, int tw, int th) {
        int ow = src.getWidth();
        int oh = src.getHeight();
        double scale = Math.max((double) tw / ow, (double) th / oh);
        int nw = (int) Math.round(ow * scale);
        int nh = (int) Math.round(oh * scale);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    /** 中心裁切 */
    private BufferedImage centerCrop(BufferedImage src, int tw, int th) {
        int x = (src.getWidth() - tw) / 2;
        int y = (src.getHeight() - th) / 2;
        return src.getSubimage(x, y, tw, th);
    }

    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;
        float sum = 0f;
        float[] exps = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = (float) Math.exp(logits[i] - max);
            sum += exps[i];
        }
        for (int i = 0; i < exps.length; i++) exps[i] /= sum;
        return exps;
    }

    private int[] topKIndices(float[] arr, int k) {
        Integer[] idx = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Float.compare(arr[b], arr[a]));
        int[] out = new int[Math.min(k, idx.length)];
        for (int i = 0; i < out.length; i++) out[i] = idx[i];
        return out;
    }

    // ─── DTO ────────────────────────────────────────────

    public record ClassificationResult(int index, String labelEn, String labelCn, float probability) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", index);
            m.put("labelEn", labelEn);
            m.put("labelCn", labelCn);
            m.put("probability", probability);
            return m;
        }
    }
}
