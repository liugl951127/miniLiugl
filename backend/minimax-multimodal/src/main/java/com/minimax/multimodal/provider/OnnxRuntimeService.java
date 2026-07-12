package com.minimax.multimodal.provider;

import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

/**
 * ONNX Runtime 推理服务 (V3.1.0 真实模型加载)
 *
 * <p>职责:
 *   - 加载 .onnx 模型文件 (OrtSession)
 *   - 图像预处理 (resize + normalize + NCHW)
 *   - 推理运行 (session.run)
 *   - 输出解析 (支持分类 / 特征向量 / 文本描述多种输出)
 *
 * <h3>支持的模型类型</h3>
 * <ul>
 *   <li>分类: 输出 [1, num_classes] float[][] (e.g. MobileNet/ResNet/CLIP zero-shot)</li>
 *   <li>特征: 输出 [1, embed_dim] float[] (e.g. CLIP image encoder)</li>
 *   <li>检测: 输出 [1, N, 5] (x,y,w,h,conf) (e.g. YOLO)</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * OrtEnvironment 全局单例 (线程安全).
 * OrtSession 非线程安全, 用 ThreadLocal<OrtSession> 模式.
 * Session 创建开销 ~50ms-1s, 首次创建缓存.
 */
@Slf4j
public class OnnxRuntimeService {

    /** 全局环境 (单例, 线程安全) */
    final OrtEnvironment env;

    /** 线程本地 Session (OrtSession 非线程安全) */
    private final ThreadLocal<OrtSession> sessionTL = new ThreadLocal<>();

    /** 模型文件路径 (已加载) */
    private final String modelPath;

    /** 缓存 session (按线程) */
    private volatile boolean isClosed = false;

    /**
     * 构造
     *
     * @param modelPath .onnx 模型文件路径
     */
    public OnnxRuntimeService(String modelPath) {
        // 1. 初始化环境
        this.env = OrtEnvironment.getEnvironment();
        this.modelPath = modelPath;
        log.info("[onnx-svc] 初始化: modelPath={}", modelPath);
    }

    /**
     * 获取当前线程的 Session (懒加载, package-private for test)
     */
    OrtSession getSession() throws OrtException {
        // 1. 已关闭?
        if (isClosed) {
            throw new IllegalStateException("service closed");
        }
        // 2. 线程本地有?
        OrtSession session = sessionTL.get();
        if (session != null) return session;
        // 3. 新建 (耗时 ~50ms-1s)
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
        sessionTL.set(session);
        log.info("[onnx-svc] 新 session: {}", modelPath);
        return session;
    }

    /**
     * 图像预处理: 解码 → resize 224x224 → 归一化 → NCHW float tensor
     *
     * <p>归一化参数: ImageNet mean/std
     *   mean = [0.485, 0.456, 0.406]
     *   std  = [0.229, 0.224, 0.225]
     */
    public float[][][][] preprocessImage(byte[] imageBytes, int targetSize) throws Exception {
        // 1. 解码图像
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (src == null) {
            throw new IllegalArgumentException("无效的图像数据");
        }
        // 2. Resize (保持比例, 短边缩放, 中心裁剪)
        BufferedImage resized = resizeAndCrop(src, targetSize);
        // 3. 准备 NCHW 数组
        int channels = 3;
        float[][][][] tensor = new float[1][channels][targetSize][targetSize];
        // 4. ImageNet 归一化参数
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std  = {0.229f, 0.224f, 0.225f};
        // 5. 遍历像素 + 归一化
        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                int rgb = resized.getRGB(x, y);
                // 6. 拆 RGB (0xAARRGGBB)
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 7. 归一化: (pixel/255 - mean) / std
                tensor[0][0][y][x] = (r / 255.0f - mean[0]) / std[0];
                tensor[0][1][y][x] = (g / 255.0f - mean[1]) / std[1];
                tensor[0][2][y][x] = (b / 255.0f - mean[2]) / std[2];
            }
        }
        return tensor;
    }

    /**
     * Resize + 中心裁剪到 targetSize x targetSize
     */
    private BufferedImage resizeAndCrop(BufferedImage src, int targetSize) {
        // 1. 等比缩放 (短边 → targetSize)
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.max((double) targetSize / w, (double) targetSize / h);
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);
        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, 0, 0, newW, newH, null);
        g2d.dispose();
        // 2. 中心裁剪
        int x = (newW - targetSize) / 2;
        int y = (newH - targetSize) / 2;
        return scaled.getSubimage(x, y, targetSize, targetSize);
    }

    /**
     * 推理: 图像 → 分类概率
     *
     * <p>返回 Map: { "topClass": "dog", "topProb": 0.85, "allProbs": [...] }
     *
     * @param imageBytes  原始图像字节
     * @param inputName   输入节点名 (默认 "input")
     * @param inputSize   输入尺寸 (默认 224)
     * @return 推理结果
     */
    public Map<String, Object> classify(byte[] imageBytes, String inputName, int inputSize) throws Exception {
        // 1. 预处理
        float[][][][] preprocessed = preprocessImage(imageBytes, inputSize);
        String inName = (inputName == null || inputName.isEmpty()) ? "input" : inputName;
        // 2. 创建输入 tensor
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, preprocessed)) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(inName, inputTensor);
            // 3. 推理
            try (OrtSession.Result result = getSession().run(inputs)) {
                // 4. 解析输出
                OnnxValue out = result.get(0);
                Object val = out.getValue();
                // 5. 期望 float[][] (logits)
                if (!(val instanceof float[][])) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "不支持的输出类型: " + val.getClass().getSimpleName());
                    return err;
                }
                float[][] logits = (float[][]) val;
                // 6. softmax
                float[] probs = softmax(logits[0]);
                // 7. 找 top class
                int topIdx = 0;
                float topProb = probs[0];
                for (int i = 1; i < probs.length; i++) {
                    if (probs[i] > topProb) {
                        topIdx = i;
                        topProb = probs[i];
                    }
                }
                // 8. 组装结果
                Map<String, Object> res = new LinkedHashMap<>();
                res.put("topClassIdx", topIdx);
                res.put("topProb", topProb);
                res.put("allProbs", probs);
                res.put("numClasses", probs.length);
                return res;
            }
        }
    }

    /**
     * 推理: 图像 → 特征向量
     */
    public float[] embed(byte[] imageBytes, String inputName, int inputSize) throws Exception {
        float[][][][] preprocessed = preprocessImage(imageBytes, inputSize);
        String inName = (inputName == null || inputName.isEmpty()) ? "input" : inputName;
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, preprocessed)) {
            Map<String, OnnxTensor> inputs = Map.of(inName, inputTensor);
            try (OrtSession.Result result = getSession().run(inputs)) {
                Object val = result.get(0).getValue();
                if (val instanceof float[][]) {
                    return ((float[][]) val)[0];
                } else if (val instanceof float[]) {
                    return (float[]) val;
                }
                return new float[0];
            }
        }
    }

    /**
     * softmax: e^x_i / sum(e^x_j)
     */
    private float[] softmax(float[] logits) {
        // 1. 数值稳定: 减去最大值
        float max = Float.NEGATIVE_INFINITY;
        for (float x : logits) if (x > max) max = x;
        // 2. exp
        float[] exp = new float[logits.length];
        float sum = 0;
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        // 3. 归一化
        float[] probs = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probs[i] = exp[i] / sum;
        }
        return probs;
    }

    /**
     * 列出模型输入/输出元信息
     */
    public Map<String, Object> modelMetadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        try {
            OrtSession s = getSession();
            // 输入信息
            List<Map<String, Object>> inputs = new ArrayList<>();
            for (NodeInfo i : s.getInputInfo().values()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", i.getName());
                info.put("type", i.getInfo().toString());
                inputs.add(info);
            }
            // 输出信息
            List<Map<String, Object>> outputs = new ArrayList<>();
            for (NodeInfo o : s.getOutputInfo().values()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", o.getName());
                info.put("type", o.getInfo().toString());
                outputs.add(info);
            }
            meta.put("inputs", inputs);
            meta.put("outputs", outputs);
        } catch (OrtException e) {
            meta.put("error", e.getMessage());
        }
        return meta;
    }

    /**
     * 关闭 (释放 session 和环境)
     */
    public void close() {
        isClosed = true;
        // 关闭当前线程的 session
        OrtSession s = sessionTL.get();
        if (s != null) {
            try {
                s.close();
            } catch (OrtException e) {
                log.warn("[onnx-svc] 关闭 session 失败: {}", e.getMessage());
            }
            sessionTL.remove();
        }
    }

    /**
     * 检查模型文件是否可加载
     */
    public static boolean isValidModel(String modelPath) {
        File f = new File(modelPath);
        return f.exists() && f.isFile() && f.length() > 0;
    }
}
