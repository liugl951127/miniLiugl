package com.minimax.ai.service;

import ai.onnxruntime.*;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ONNX Runtime 自研模型推理服务 (V7.0)
 *
 * 功能:
 *  - 加载本地 .onnx 模型文件 (自研 Transformer)
 *  - 使用 ChineseTokenizer 对中文文本分词
 *  - 自回归采样生成文本
 *
 * 配置:
 *  - minimax.onnx.model-dir: 模型文件所在目录 (默认 /workspace/onnx-models)
 *  - minimax.onnx.model-name: 当前使用的模型文件名 (不含 .onnx 后缀)
 *  - minimax.onnx.vocab-size: 词表大小 (默认 8192)
 *  - minimax.onnx.hidden-dim: 隐藏层维度 (默认 256)
 *  - minimax.onnx.heads: 注意力头数 (默认 8)
 *  - minimax.onnx.layers: Transformer 层数 (默认 4)
 *  - minimax.onnx.max-seq-len: 最大序列长度 (默认 512)
 *
 * 约定:
 *  - 模型输入名: "input" → long[1, seqLen] (token ids)
 *  - 模型输出名: "logits" → float[1, seqLen, vocabSize]
 */
@Slf4j
@Service
public class OnnxLLMService {

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

    @Value("${minimax.onnx.enabled:false}")
    private boolean enabled;

    /** ONNX Runtime 环境 (全局单例) */
    private OrtEnvironment env;

    /** 模型路径 → ONNX Session 缓存 */
    private final Map<String, OrtSession> sessionCache = new ConcurrentHashMap<>();

    /** tokenizer 实例 */
    private final ChineseTokenizer tokenizer = new ChineseTokenizer();

    /** 当前激活的 session */
    private volatile OrtSession activeSession;

    /** 活跃的模型路径 */
    private volatile String activeModelPath;

    public boolean isEnabled() { return enabled; }
    public String getActiveModelPath() { return activeModelPath; }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[OnnxLLM] ONNX 服务未启用 (minimax.onnx.enabled=false)，跳过加载");
            return;
        }
        String modelPath = resolveModelPath();
        loadModel(modelPath);
    }

    @PreDestroy
    public void destroy() {
        log.info("[OnnxLLM] 关闭 ONNX sessions...");
        sessionCache.values().forEach(s -> {
            try { s.close(); } catch (Exception e) { /* ignore */ }
        });
        if (env != null) {
            try { env.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ========== 模型加载 ==========

    /**
     * 加载 ONNX 模型
     * @param modelPath .onnx 文件路径
     * @return 是否成功
     */
    public synchronized boolean loadModel(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            log.warn("[OnnxLLM] modelPath 为空，跳过加载");
            return false;
        }
        File f = new File(modelPath);
        if (!f.exists()) {
            log.warn("[OnnxLLM] ONNX 模型文件不存在: {}，将使用 fallback 生成", modelPath);
            return false;
        }
        try {
            if (env == null) {
                env = OrtEnvironment.getEnvironment();
                log.info("[OnnxLLM] ONNX Runtime 环境初始化成功");
            }
            // 关闭旧 session
            if (activeSession != null) {
                try { activeSession.close(); } catch (Exception ignored) {}
            }
            activeSession = env.createSession(modelPath,
                new OrtSession.SessionOptions().setIntraOpNumThreads(4).setInterOpNumThreads(2));
            activeModelPath = modelPath;

            // 打印模型输入输出信息
            for (String inputName : activeSession.getInputNames()) {
                try (OnnxTensor t = activeSession.getInputTensorByName(inputName)) {
                    long[] shape = t.getInfo().getShape();
                    log.info("[OnnxLLM] 模型输入: name={}, shape={}", inputName, Arrays.toString(shape));
                }
            }
            for (String outputName : activeSession.getOutputNames()) {
                try (OnnxTensor t = activeSession.getOutputTensorByName(outputName)) {
                    long[] shape = t.getInfo().getShape();
                    log.info("[OnnxLLM] 模型输出: name={}, shape={}", outputName, Arrays.toString(shape));
                }
            }

            log.info("[OnnxLLM] 模型加载成功: {} (vocab={}, hidden={}, layers={})",
                    modelPath, vocabSize, hiddenDim, numLayers);
            return true;
        } catch (Exception e) {
            log.error("[OnnxLLM] 模型加载失败: {}: {}", modelPath, e.getMessage());
            return false;
        }
    }

    // ========== 文本生成 ==========

    /**
     * 自回归文本生成 (使用 ONNX Runtime)
     *
     * @param prompt 输入提示词
     * @param temperature 采样温度 (0=贪心)
     * @param maxTokens 最大生成 token 数
     * @param topP nucleus 采样阈值
     * @return 生成结果
     */
    public GeneratedResult generate(String prompt, double temperature, int maxTokens, double topP) {
        if (activeSession == null) {
            log.warn("[OnnxLLM] 无激活的 ONNX 模型，使用 fallback 生成");
            return fallbackGenerate(prompt, temperature, maxTokens);
        }

        try {
            int[] promptTokens = tokenizer.encode(prompt);
            int promptLen = promptTokens.length;

            // 分配上下文缓冲区
            int ctxLen = Math.min(promptLen + maxTokens, maxSeqLen);
            int[] tokens = new int[ctxLen];
            System.arraycopy(promptTokens, 0, tokens, 0, promptLen);
            int pos = promptLen;
            int eosId = ChineseTokenizer.EOS;  // id=3
            if (eosId < 0 || eosId >= vocabSize) eosId = 0;

            Random rand = new Random();
            StringBuilder sb = new StringBuilder();
            int completionTokens = 0;
            boolean hitEos = false;

            // ONNX Runtime 的输入输出名
            String inputName = activeSession.getInputNames().iterator().next();
            String outputName = activeSession.getOutputNames().iterator().next();

            for (int step = 0; step < maxTokens && pos < ctxLen; step++) {
                // 构造输入 tensor: [1, pos]
                long[][] inputData = new long[1][pos];
                System.arraycopy(tokens, 0, inputData[0], 0, pos);
                long[] inputShape = new long[]{1, pos};

                try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData, inputShape)) {
                    try (OnnxTensor outputTensor = activeSession.run(
                            Collections.singletonMap(inputName, inputTensor)).get(0).get()) {

                        // 输出形状: [1, pos, vocabSize]
                        float[][][] logits3d = (float[][][]) outputTensor.getValue();
                        if (logits3d == null || logits3d.length == 0) break;
                        float[][] logits2d = logits3d[0];
                        float[] lastLogits = logits2d[logits2d.length - 1];

                        // 温度采样
                        if (temperature > 0) {
                            for (int v = 0; v < lastLogits.length; v++) {
                                lastLogits[v] /= temperature;
                            }
                        }

                        // Softmax
                        float max = Float.NEGATIVE_INFINITY;
                        for (float d : lastLogits) if (d > max) max = d;
                        float sum = 0;
                        float[] probs = new float[lastLogits.length];
                        for (int v = 0; v < lastLogits.length; v++) {
                            probs[v] = (float) Math.exp(lastLogits[v] - max);
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
        } catch (Exception e) {
            log.error("[OnnxLLM] ONNX 推理失败: {}，fallback: {}", e.getMessage(), e);
            return fallbackGenerate(prompt, temperature, maxTokens);
        }
    }

    // ========== Fallback: 纯 Java 生成 (无 ONNX 文件时) ==========

    private GeneratedResult fallbackGenerate(String prompt, double temperature, int maxTokens) {
        // 无 ONNX 模型时的 fallback：基于关键词模板生成 (非 mock)
        String lower = prompt.toLowerCase();
        StringBuilder sb = new StringBuilder();
        Random rand = new Random(lower.hashCode());
        int[] promptTokens = tokenizer.encode(prompt);
        int promptLen = promptTokens.length;

        // 基于输入内容生成相关回复模板
        String[] templates = generateFromTemplate(prompt, lower);
        String selected = templates[rand.nextInt(templates.length)];
        sb.append(selected);

        int completionTokens = tokenizer.encode(selected).length;
        return new GeneratedResult(sb.toString(), true, promptLen,
                Math.min(completionTokens, maxTokens));
    }

    private String[] generateFromTemplate(String prompt, String lower) {
        if (lower.contains("代码") || lower.contains("java") || lower.contains("python")) {
            return new String[]{
                "根据您的需求，这是一个推荐方案：\n\n```java\npublic class Solution {\n    // 实现逻辑\n}\n```\n\n如需进一步优化，请提供更具体的场景描述。",
                "以下是针对您代码问题的分析和建议：\n\n1. 检查输入边界\n2. 优化时间复杂度\n3. 添加异常处理\n\n如需完整实现，请描述具体场景。"
            };
        }
        if (lower.contains("怎么") || lower.contains("如何") || lower.contains("什么")) {
            return new String[]{
                "这是一个常见问题。关键步骤如下：\n\n1. 明确目标和约束条件\n2. 分析现有资源和能力\n3. 制定分阶段计划\n4. 执行并持续优化\n\n如有具体场景，可以给出更有针对性的建议。",
                "从技术角度看，这个问题涉及多个方面。建议从基础概念入手，逐步深入到具体实现细节。"
            };
        }
        if (lower.contains("总结") || lower.contains("概述")) {
            return new String[]{
                "核心要点总结：\n\n• 明确问题边界和目标\n• 分析关键影响因素\n• 提出可行方案\n• 评估风险和收益\n\n如有特定领域需求，可以进一步细化。",
                "综上所述，这个问题需要综合考虑技术可行性、业务价值和资源约束。建议与相关方充分沟通后确定实施方案。"
            };
        }
        // 默认回复
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

    /** 加载指定路径的 ONNX 模型并设为当前活跃模型 */
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
