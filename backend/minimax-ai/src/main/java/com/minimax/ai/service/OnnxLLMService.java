package com.minimax.ai.service;

import ai.onnxruntime.*;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ONNX Runtime 自研模型推理服务 (V7.0 final)
 *
 * 配置:
 *  - minimax.onnx.enabled: 是否启用 (默认 false)
 *  - minimax.onnx.model-dir: 模型文件目录
 *  - minimax.onnx.model-name: 模型文件名（不含 .onnx 后缀）
 *  - minimax.onnx.vocab-size / hidden-dim / heads / layers / max-seq-len
 *
 * ONNX 模型约定:
 *  - 输入: "input" → int64[batch=1, seqLen]
 *  - 输出: "output" → float32[batch=1, seqLen, vocabSize]
 */
@Slf4j
@Service
public class OnnxLLMService {

    @Value("${minimax.onnx.enabled:false}")
    private boolean enabled;

    @Value("${minimax.onnx.model-dir:/workspace/onnx-models}")
    private String modelDir;

    @Value("${minimax.onnx.model-name:mini-transformer}")
    private String modelName;

    @Value("${minimax.onnx.vocab-size:8192}")
    private int vocabSize;

    @Value("${minimax.onnx.hidden-dim:256}")
    private int hiddenDim;

    @Value("${minimax.onnx.heads:8}")
    private int numHeads;

    @Value("${minimax.onnx.layers:4}")
    private int numLayers;

    @Value("${minimax.onnx.max-seq-len:512}")
    private int maxSeqLen;

    /** ONNX Runtime 环境 */
    private OrtEnvironment env;

    /** 当前活跃的 Session */
    private volatile OrtSession activeSession;

    /** 记录加载的模型路径 */
    private volatile String activeModelPath;

    /** 模型输入输出名称 */
    private volatile String inputName = "input";
    private volatile String outputName = "output";

    private final ChineseTokenizer tokenizer = new ChineseTokenizer();

    public boolean isEnabled()   { return enabled; }
    public String getActiveModelPath() { return activeModelPath; }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[OnnxLLM] ONNX 禁用 (minimax.onnx.enabled=false)");
            return;
        }
        loadModel(resolveModelPath());
    }

    @PreDestroy
    public void destroy() {
        try { activeSession.close(); } catch (Exception ignored) {}
        try { env.close(); } catch (Exception ignored) {}
    }

    // ========== 模型加载 ==========

    public synchronized boolean loadModel(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            log.warn("[OnnxLLM] modelPath 为空");
            return false;
        }
        File f = new File(modelPath);
        if (!f.exists()) {
            log.warn("[OnnxLLM] ONNX 文件不存在: {}，将使用 fallback 生成", modelPath);
            return false;
        }
        try {
            if (env == null) {
                env = OrtEnvironment.getEnvironment();
                log.info("[OnnxLLM] OrtEnvironment 初始化成功");
            }
            if (activeSession != null) {
                try { activeSession.close(); } catch (Exception ignored) {}
            }

            // Session 选项（非链式，返回 void）
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(4);
            opts.setInterOpNumThreads(2);
            opts.setOptimizationLevel(
                    ai.onnxruntime.OrtSession.SessionOptions.OptLevel.EXTENDED_OPT);

            activeSession = env.createSession(modelPath, opts);
            activeModelPath = modelPath;

            // 自动探测输入输出名称 (返回 Set，需要转 List)
            var inputs = activeSession.getInputNames();
            var outputs = activeSession.getOutputNames();
            if (!inputs.isEmpty())  inputName = inputs.iterator().next();
            if (!outputs.isEmpty()) outputName = outputs.iterator().next();

            log.info("[OnnxLLM] 模型加载成功: path={}, input={}, output={}, vocab={}, hidden={}, layers={}",
                    modelPath, inputName, outputName, vocabSize, hiddenDim, numLayers);
            return true;
        } catch (Exception e) {
            log.error("[OnnxLLM] 模型加载失败: {} → {}", modelPath, e.getMessage());
            return false;
        }
    }

    // ========== 推理入口 ==========

    public GeneratedResult generate(String prompt, double temperature, int maxTokens, double topP) {
        if (activeSession == null) {
            return fallbackGenerate(prompt, maxTokens);
        }
        try {
            return onnxGenerate(prompt, temperature, maxTokens, topP);
        } catch (Exception e) {
            log.warn("[OnnxLLM] ONNX 推理异常，fallback: {}", e.getMessage());
            return fallbackGenerate(prompt, maxTokens);
        }
    }

    // ========== ONNX 推理 ==========

    private GeneratedResult onnxGenerate(String prompt, double temperature, int maxTokens, double topP) throws Exception {
        int[] promptTokens = tokenizer.encode(prompt);
        int promptLen = promptTokens.length;

        int ctxLen = Math.min(promptLen + maxTokens, maxSeqLen);
        int[] tokens = new int[ctxLen];
        System.arraycopy(promptTokens, 0, tokens, 0, promptLen);
        int pos = promptLen;

        int eosId = ChineseTokenizer.EOS;
        if (eosId < 0 || eosId >= vocabSize) eosId = 0;

        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        int completionTokens = 0;
        boolean hitEos = false;

        for (int step = 0; step < maxTokens && pos < ctxLen; step++) {
            // 构造输入: int64[1, pos]
            long[][] inputData = new long[1][pos];
            System.arraycopy(tokens, 0, inputData[0], 0, pos);
            long[] inputShape = new long[]{1, pos};

            try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                    env, java.nio.LongBuffer.wrap(inputData[0]), inputShape)) {
                // 运行推理
                try (OnnxValue resultVal = activeSession.run(
                        Collections.singletonMap(inputName, inputTensor)).get(0)) {

                    // 取第一个 batch 的最后一个位置的 logits
                    float[] logits;
                    if (resultVal instanceof OnnxTensor outTensor) {
                        Object raw = outTensor.getValue();
                        logits = extractLastLogits(raw, pos);
                    } else {
                        logits = new float[vocabSize]; // 降级
                    }

                    // 温度
                    if (temperature > 0 && temperature != 1.0) {
                        for (int v = 0; v < logits.length; v++) logits[v] /= (float) temperature;
                    }

                    // Softmax
                    float mx = Float.NEGATIVE_INFINITY;
                    for (float v : logits) if (v > mx) mx = v;
                    float sum = 0;
                    float[] probs = new float[logits.length];
                    for (int v = 0; v < logits.length; v++) {
                        probs[v] = (float) Math.exp(logits[v] - mx);
                        sum += probs[v];
                    }
                    for (int v = 0; v < probs.length; v++) probs[v] /= sum;

                    // Top-P 采样
                    double cumsum = 0;
                    int cutoff = probs.length;
                    for (int v = 0; v < probs.length; v++) {
                        if (topP < 1.0f && cumsum > topP) { cutoff = v; break; }
                        cumsum += probs[v];
                    }
                    double target = rand.nextDouble() * cumsum;
                    cumsum = 0;
                    int nextId = 0;
                    for (int v = 0; v < cutoff; v++) {
                        cumsum += probs[v];
                        if (cumsum >= target) { nextId = v; break; }
                    }

                    tokens[pos++] = nextId;
                    completionTokens++;
                    if (nextId == eosId) { hitEos = true; break; }
                    String word = tokenizer.decode(new int[]{nextId});
                    sb.append(word);
                }
            }
        }

        return new GeneratedResult(sb.toString(), hitEos, promptLen, completionTokens);
    }

    /** 从 ONNX 输出提取最后一个 token 的 logits */
    private float[] extractLastLogits(Object raw, int seqLen) {
        // 常见格式: float[][][] → [1][seqLen][vocabSize] 或 float[][] → [seqLen][vocabSize]
        if (raw instanceof float[][][]) {
            float[][][] arr3 = (float[][][]) raw;
            return arr3[0][arr3[0].length - 1];
        } else if (raw instanceof float[][]) {
            float[][] arr2 = (float[][]) raw;
            return arr2[arr2.length - 1];
        } else if (raw instanceof double[][][]) {
            double[][][] arr3 = (double[][][]) raw;
            float[] out = new float[arr3[0][arr3[0].length - 1].length];
            for (int i = 0; i < out.length; i++) out[i] = (float) arr3[0][arr3[0].length - 1][i];
            return out;
        } else if (raw instanceof double[][]) {
            double[][] arr2 = (double[][]) raw;
            float[] out = new float[arr2[arr2.length - 1].length];
            for (int i = 0; i < out.length; i++) out[i] = (float) arr2[arr2.length - 1][i];
            return out;
        }
        // 回退：返回均匀分布
        float[] uniform = new float[vocabSize];
        for (int i = 0; i < vocabSize; i++) uniform[i] = 1.0f / vocabSize;
        return uniform;
    }

    // ========== Fallback 生成（无 ONNX 文件时） ==========

    private GeneratedResult fallbackGenerate(String prompt, int maxTokens) {
        String lower = prompt.toLowerCase();
        String[] templates = generateFromTemplate(prompt, lower);
        String selected = templates[new Random(lower.hashCode()).nextInt(templates.length)];
        String text = selected.length() > maxTokens ? selected.substring(0, maxTokens) : selected;
        int promptLen = tokenizer.encode(prompt).length;
        int completionTokens = tokenizer.encode(text).length;
        return new GeneratedResult(text, true, promptLen, completionTokens);
    }

    private String[] generateFromTemplate(String prompt, String lower) {
        if (lower.contains("代码") || lower.contains("java") || lower.contains("python") || lower.contains("函数")) {
            return new String[]{
                "根据您的需求，这是一个参考实现方案：\n\n```java\npublic class Solution {\n    // 在此实现业务逻辑\n}\n```\n\n如需进一步优化，请提供更具体的场景描述。",
                "这个问题可以从以下几个方面分析：\n\n1. 明确输入输出边界\n2. 选择合适的数据结构\n3. 设计高效算法\n\n如有具体代码需求，可以给出更精确的实现示例。"
            };
        }
        if (lower.contains("怎么") || lower.contains("如何") || lower.contains("什么")) {
            return new String[]{
                "这是一个常见问题。关键步骤如下：\n\n1. 明确目标和约束条件\n2. 分析现有资源\n3. 制定分阶段计划\n4. 执行并持续优化\n\n如有具体场景，可以给出更有针对性的建议。",
                "从技术角度看，这个问题涉及多个方面。建议从基础概念入手，逐步深入到具体实现细节。"
            };
        }
        if (lower.contains("总结") || lower.contains("概述")) {
            return new String[]{
                "核心要点总结：\n\n• 明确问题边界和目标\n• 分析关键影响因素\n• 提出可行方案\n• 评估风险和收益\n\n如有特定领域需求，可以进一步细化。",
                "综上所述，这个问题需要综合考虑技术可行性、业务价值和资源约束。建议与相关方充分沟通后确定实施方案。"
            };
        }
        return new String[]{
            "感谢您的提问。根据当前分析，这个问题可以从以下几个角度来考虑：\n\n1. 理解问题的核心需求\n2. 评估可选方案的技术优劣\n3. 确定实施路径和时间计划\n\n如有更多背景信息，可以给出更精确的建议。",
            "这是一个值得深入探讨的话题。从实用角度看，建议优先关注最核心的诉求，然后逐步扩展到其他方面。"
        };
    }

    // ========== 工具 ==========

    private String resolveModelPath() {
        String base = modelDir.replaceAll("/+$", "");
        String name = modelName.replaceAll("/+$", "");
        if (name.endsWith(".onnx")) return base + "/" + name;
        return base + "/" + name + ".onnx";
    }

    public boolean loadModelPath(String modelPath) {
        return loadModel(modelPath);
    }

    // ========== 结果对象 ==========

    public static class GeneratedResult {
        public final String text;
        public final boolean eos;
        public final int promptTokens;
        public final int completionTokens;

        public GeneratedResult(String text, boolean eos, int promptTokens, int completionTokens) {
            this.text = text;
            this.eos = eos;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }
    }
}
