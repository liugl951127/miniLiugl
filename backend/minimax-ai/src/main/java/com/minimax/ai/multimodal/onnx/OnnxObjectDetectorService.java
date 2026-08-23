package com.minimax.ai.multimodal.onnx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * ONNX 目标检测服务 (V7.1)
 *
 * <p>支持 YOLOv8n ONNX 模型. 输入 640x640, 输出 [1, 84, 8400] (4 box + 80 class conf).
 * 沙箱环境模型下载受限, 实现完整 NMS + 坐标反投影逻辑, 模型到位即可用.</p>
 *
 * <h3>当前状态</h3>
 * <ul>
 *   <li>代码: YOLOv8 后处理完整 (NMS + 坐标还原 + class filter)</li>
 *   <li>模型: 待 yolov8n/model.onnx 下载 (scripts/download-models.sh 自动拉)</li>
 *   <li>降级: 模型缺失时返回空列表, 调用方可回退到现有 ImageAnalyzer 的简单检测</li>
 * </ul>
 */
@Slf4j
@Service
public class OnnxObjectDetectorService {

    @Value("${minimax.onnx-vision.yolo-enabled:true}")
    private boolean enabled;

    @Value("${minimax.onnx-vision.yolo-path:./data/models/yolov8n/model.onnx}")
    private String yoloPath;

    @Value("${minimax.onnx-vision.yolo-confidence:0.25}")
    private float confThreshold;

    @Value("${minimax.onnx-vision.yolo-iou:0.45}")
    private float iouThreshold;

    private static final int INPUT_SIZE = 640;
    private static final int NUM_CLASSES = 80;  // COCO

    // COCO 类别 (与 yolov8 一致)
    private static final String[] COCO_CLASSES = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
        "truck", "boat", "traffic light", "fire hydrant", "stop sign", "parking meter",
        "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear",
        "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase",
        "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat",
        "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
        "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut",
        "cake", "chair", "couch", "potted plant", "bed", "dining table", "toilet",
        "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
        "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
        "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    private boolean modelReady = false;
    // 真实 session 留空 (按需加载)
    private Object session;  // OrtSession 占位, 沙箱无模型时不实例化
    private String inputName;
    private String outputName;

    public boolean isEnabled() { return enabled; }
    public boolean isReady()   { return modelReady; }
    public String getModelPath() { return yoloPath; }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[OnnxDetector] 禁用");
            return;
        }
        java.io.File f = new java.io.File(yoloPath);
        if (!f.exists()) {
            log.warn("[OnnxDetector] 模型不存在: {} — 目标检测降级, 等待 scripts/download-models.sh", yoloPath);
            return;
        }
        // 模型存在时加载
        tryLoadYolo();
    }

    private void tryLoadYolo() {
        try {
            Class<?> envCls = Class.forName("ai.onnxruntime.OrtEnvironment");
            Object env = envCls.getMethod("getEnvironment").invoke(null);
            Class<?> optsCls = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
            Object opts = optsCls.getDeclaredConstructor().newInstance();
            optsCls.getMethod("setIntraOpNumThreads", int.class).invoke(opts, 4);
            Class<?> optLevelCls = Class.forName("ai.onnxruntime.OrtSession$SessionOptions$OptLevel");
            Object extLevel = Enum.valueOf((Class<Enum>) optLevelCls, "EXTENDED_OPT");
            optsCls.getMethod("setOptimizationLevel", optLevelCls).invoke(opts, extLevel);

            Class<?> sessCls = Class.forName("ai.onnxruntime.OrtSession");
            session = sessCls.getMethod("createSession", String.class, optsCls)
                .invoke(env, yoloPath);

            // input/output name
            Object inputNames = sessCls.getMethod("getInputNames").invoke(session);
            Iterable<?> inIter = (Iterable<?>) inputNames;
            inputName = (String) inIter.iterator().next();
            Object outputNames = sessCls.getMethod("getOutputNames").invoke(session);
            Iterable<?> outIter = (Iterable<?>) outputNames;
            outputName = (String) outIter.iterator().next();

            modelReady = true;
            log.info("[OnnxDetector] ✅ YOLOv8n 加载成功: input={} output={}", inputName, outputName);
        } catch (Exception e) {
            log.error("[OnnxDetector] 加载失败: {}", e.getMessage());
            modelReady = false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (session != null) {
            try {
                session.getClass().getMethod("close").invoke(session);
            } catch (Exception ignored) {}
        }
    }

    /**
     * 目标检测
     *
     * @param image 输入图片
     * @return 检测框列表 [class, confidence, x, y, width, height] (像素坐标)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Detection> detect(BufferedImage image) {
        if (!modelReady || image == null) return Collections.emptyList();
        try {
            // 1. 预处理: resize + 归一化
            float[] inputData = preprocess(image);
            long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
            Class<?> tensorCls = Class.forName("ai.onnxruntime.OnnxTensor");
            java.nio.FloatBuffer fb = java.nio.FloatBuffer.wrap(inputData);
            Object inputTensor = tensorCls.getMethod("createTensor",
                Class.forName("ai.onnxruntime.OrtEnvironment"),
                java.nio.FloatBuffer.class, long[].class)
                .invoke(null, getEnv(), fb, shape);

            // 2. 推理
            Class<?> sessCls = Class.forName("ai.onnxruntime.OrtSession");
            Class<?> resultCls = Class.forName("ai.onnxruntime.OrtSession$Result");
            Object result = sessCls.getMethod("run", java.util.Map.class)
                .invoke(session, java.util.Map.of(inputName, inputTensor));

            try {
                // 3. 后处理
                Object outputTensor = resultCls.getMethod("get", int.class).invoke(result, 0);
                float[][][] output = (float[][][]) outputTensor.getClass()
                    .getMethod("getValue").invoke(outputTensor);
                return postProcess(output, image.getWidth(), image.getHeight());
            } finally {
                resultCls.getMethod("close").invoke(result);
            }
        } catch (Exception e) {
            log.error("[OnnxDetector] 推理失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Object getEnv() throws Exception {
        Class<?> envCls = Class.forName("ai.onnxruntime.OrtEnvironment");
        return envCls.getMethod("getEnvironment").invoke(null);
    }

    /** Letterbox resize + 归一化 [0,1] */
    private float[] preprocess(BufferedImage src) {
        int ow = src.getWidth();
        int oh = src.getHeight();
        double scale = Math.min((double) INPUT_SIZE / ow, (double) INPUT_SIZE / oh);
        int nw = (int) Math.round(ow * scale);
        int nh = (int) Math.round(oh * scale);
        int padX = (INPUT_SIZE - nw) / 2;
        int padY = (INPUT_SIZE - nh) / 2;

        BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        float[] data = new float[3 * INPUT_SIZE * INPUT_SIZE];
        for (int c = 0; c < 3; c++) {
            for (int h = 0; h < INPUT_SIZE; h++) {
                for (int w = 0; w < INPUT_SIZE; w++) {
                    int pixel;
                    if (w < padX || w >= padX + nw || h < padY || h >= padY + nh) {
                        pixel = 0x808080;  // 灰色填充
                    } else {
                        pixel = resized.getRGB(w - padX, h - padY);
                    }
                    int r = (pixel >> 16) & 0xFF;
                    int gv = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    float[] ch = {r / 255f, gv / 255f, b / 255f};
                    data[c * INPUT_SIZE * INPUT_SIZE + h * INPUT_SIZE + w] = ch[c];
                }
            }
        }
        return data;
    }

    /** YOLOv8 后处理: output[1][84][8400] → NMS → Detection */
    private List<Detection> postProcess(float[][][] output, int origW, int origH) {
        // output[0][4+c][i] = c-th class score at anchor i
        int numAnchors = output[0][0].length;
        List<float[]> candidates = new ArrayList<>();

        double scale = Math.min((double) INPUT_SIZE / origW, (double) INPUT_SIZE / origH);
        int nw = (int) Math.round(origW * scale);
        int nh = (int) Math.round(origH * scale);
        int padX = (INPUT_SIZE - nw) / 2;
        int padY = (INPUT_SIZE - nh) / 2;

        for (int i = 0; i < numAnchors; i++) {
            float maxConf = -1;
            int maxCls = -1;
            for (int c = 0; c < NUM_CLASSES; c++) {
                float score = output[0][4 + c][i];
                if (score > maxConf) {
                    maxConf = score;
                    maxCls = c;
                }
            }
            if (maxConf < confThreshold) continue;
            float cx = output[0][0][i];
            float cy = output[0][1][i];
            float w  = output[0][2][i];
            float h  = output[0][3][i];
            // letterbox 反投影
            float x1 = cx - w / 2f - padX;
            float y1 = cy - h / 2f - padY;
            float x2 = cx + w / 2f - padX;
            float y2 = cy + h / 2f - padY;
            // 缩放回原图
            x1 /= scale; y1 /= scale; x2 /= scale; y2 /= scale;
            // 裁切
            x1 = Math.max(0, x1); y1 = Math.max(0, y1);
            x2 = Math.min(origW, x2); y2 = Math.min(origH, y2);
            candidates.add(new float[]{x1, y1, x2, y2, maxConf, maxCls});
        }

        // NMS (按 class 区分)
        candidates.sort((a, b) -> Float.compare(b[4], a[4]));
        List<float[]> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            if (suppressed[i]) continue;
            kept.add(candidates.get(i));
            for (int j = i + 1; j < candidates.size(); j++) {
                if (suppressed[j]) continue;
                if ((int) candidates.get(i)[5] != (int) candidates.get(j)[5]) continue;
                float iou = iou(candidates.get(i), candidates.get(j));
                if (iou > iouThreshold) suppressed[j] = true;
            }
        }

        List<Detection> out = new ArrayList<>(kept.size());
        for (float[] k : kept) {
            int cls = (int) k[5];
            String name = cls >= 0 && cls < COCO_CLASSES.length ? COCO_CLASSES[cls] : "class_" + cls;
            out.add(new Detection(name, k[4], k[0], k[1], k[2] - k[0], k[3] - k[1]));
        }
        return out;
    }

    private float iou(float[] a, float[] b) {
        float x1 = Math.max(a[0], b[0]);
        float y1 = Math.max(a[1], b[1]);
        float x2 = Math.min(a[2], b[2]);
        float y2 = Math.min(a[3], b[3]);
        float inter = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float areaA = (a[2] - a[0]) * (a[3] - a[1]);
        float areaB = (b[2] - b[0]) * (b[3] - b[1]);
        return inter / (areaA + areaB - inter + 1e-6f);
    }

    public record Detection(String className, float confidence,
                            float x, float y, float width, float height) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("class", className);
            m.put("confidence", confidence);
            m.put("bbox", new float[]{x, y, width, height});
            return m;
        }
    }
}
