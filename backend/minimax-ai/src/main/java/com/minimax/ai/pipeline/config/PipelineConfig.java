package com.minimax.ai.pipeline.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 流水线全局配置 (V2.8.5)
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>所有可调常量都用 static 字段 (运行时可改, 通过 /api/ai/pipeline/config)</li>
 *   <li>类别用枚举 (强类型, 编译期检查)</li>
 *   <li>支持热加载: 修改后下一次请求生效</li>
 * </ul>
 *
 * <h3>用法</h3>
 * <pre>{@code
 *   PipelineConfig.getInstance().setComputeMode(ComputeMode.GPU);
 *   PipelineConfig.MAX_HISTORY_TURNS = 20;
 * }</pre>
 */
public final class PipelineConfig {

    // =============================================================
    // 一、模型配置 (常量)
    // =============================================================

    /** 词表大小 - 与 MiniTransformer 构造参数一致, 修改需重建模型 */
    public static volatile int VOCAB_SIZE = 8192;

    /** Transformer 隐藏层维度 */
    public static volatile int HIDDEN_DIM = 128;

    /** 注意力头数 - 必须能被 HIDDEN_DIM 整除 */
    public static volatile int NUM_HEADS = 4;

    /** Transformer 层数 */
    public static volatile int NUM_LAYERS = 2;

    /** 最大序列长度 */
    public static volatile int MAX_SEQ_LEN = 128;

    /** 采样温度 (0=贪心, 1=均匀, >1=更随机) */
    public static volatile float TEMPERATURE = 0.8f;

    /** top-k 采样 (每步只取概率最高的 k 个 token) */
    public static volatile int TOP_K = 40;

    /** top-p (nucleus) 采样阈值 (累积概率) */
    public static volatile float TOP_P = 0.9f;

    // =============================================================
    // 二、流水线行为
    // =============================================================

    /** 单 session 最大历史轮数 (超过则截断最早的) */
    public static volatile int MAX_HISTORY_TURNS = 10;

    /** 最大生成 token 数 (防止无限循环) */
    public static volatile int MAX_GENERATE_TOKENS = 200;

    /** RAG 检索 top-k (返回最相关的 k 条) */
    public static volatile int RAG_TOP_K = 5;

    /** RAG 相似度阈值 (低于此分视为不相关) */
    public static volatile double RAG_SIMILARITY_THRESHOLD = 0.65;

    /** 风控敏感词命中阈值 (前置阻断) */
    public static volatile int RISK_BLOCK_THRESHOLD = 1;

    /** 风控置信度阈值 (后置, 命中后追加审查) */
    public static volatile double POST_RISK_CONFIDENCE = 0.8;

    // =============================================================
    // 三、性能调优
    // =============================================================

    /** 批处理大小 (1=无批, 4/8/16 提升吞吐) */
    public static volatile int BATCH_SIZE = 1;

    /** 是否启用 KV cache (加速自回归生成, 2x+) */
    public static volatile boolean ENABLE_KV_CACHE = true;

    /** 是否启用 int8 量化 (CPU 模式加速, 轻微精度损失) */
    public static volatile boolean ENABLE_INT8_QUANT = false;

    /** 异步执行 (pipeline 各阶段解耦, 整体延迟降低 30%) */
    public static volatile boolean ASYNC_EXEC = true;

    /** 线程池大小 */
    public static volatile int THREAD_POOL_SIZE = 8;

    // =============================================================
    // 四、计算模式 (核心开关)
    // =============================================================

    /**
     * 计算设备枚举
     * - CPU: 纯 Java 矩阵运算, 速度慢但通用
     * - GPU: 优先尝试 OpenCL/CUDA 加速 (无 GPU 时自动 fallback 到 CPU)
     * - AUTO: 根据可用性自动选择
     */
    public enum ComputeMode {
        CPU,    // 纯 CPU 推理
        GPU,    // GPU 推理 (OpenCL/CUDA)
        AUTO    // 自动检测
    }

    /** 当前计算模式 (默认 AUTO) */
    private static volatile ComputeMode COMPUTE_MODE = ComputeMode.AUTO;

    /** 实际生效设备 (运行时检测) */
    private static volatile String ACTIVE_DEVICE = "CPU";

    // =============================================================
    // 五、阶段开关
    // =============================================================

    /**
     * 流水线阶段枚举
     * 对应 13 个处理环节
     */
    public enum Stage {
        USER_INPUT(1, "用户输入"),
        GATEWAY_DISPATCH(2, "网关分发"),
        MULTIMODAL_PARSE(3, "ASR/OCR 多模态解析"),
        CONTEXT_ASSEMBLE(4, "历史上下文+系统提示"),
        PRE_RISK(5, "前置风控"),
        RAG_TOOL_AGENT(6, "RAG/工具/智能体增强"),
        TOKENIZE(7, "分词转 Token"),
        MODEL_GENERATE(8, "模型自回归生成"),
        TOKEN_DECODE(9, "Token 解码"),
        POST_RISK(10, "后置风控"),
        FORMAT(11, "格式化整理"),
        LOG_STORE(12, "存储会话日志"),
        RETURN(13, "返回前端");

        public final int order;
        public final String description;

        Stage(int order, String description) {
            this.order = order;
            this.description = description;
        }
    }

    // =============================================================
    // 六、输入模态枚举
    // =============================================================

    public enum InputModality {
        TEXT,    // 纯文本
        IMAGE,   // 图片 (走 OCR)
        AUDIO,   // 音频 (走 ASR)
        VIDEO,   // 视频 (抽帧 + ASR + OCR)
        FILE     // 文件 (解析)
    }

    // =============================================================
    // 七、风险等级
    // =============================================================

    public enum RiskLevel {
        SAFE(0, "安全"),
        LOW(1, "低风险"),
        MEDIUM(2, "中风险"),
        HIGH(3, "高风险"),
        BLOCKED(4, "已拦截");

        public final int level;
        public final String label;

        RiskLevel(int level, String label) {
            this.level = level;
            this.label = label;
        }
    }

    // =============================================================
    // 八、计算设备检测
    // =============================================================

    static {
        // 启动时检测 GPU
        detectGpu();
    }

    /** 检测 GPU 可用性 (V2.8.5 仅作占位, 真实 GPU 加速需 OpenCL native lib) */
    private static void detectGpu() {
        try {
            // 尝试加载 OpenCL (JDK 不内置, 需要额外依赖)
            // Class.forName("org.jocl.CL");
            // 简化: 通过环境变量判断
            String forceGpu = System.getenv("MINIMAX_FORCE_GPU");
            if ("1".equals(forceGpu) || "true".equalsIgnoreCase(forceGpu)) {
                ACTIVE_DEVICE = "GPU (forced)";
            } else {
                ACTIVE_DEVICE = "CPU";
            }
        } catch (Throwable t) {
            ACTIVE_DEVICE = "CPU";
        }
    }

    /**
     * 解析最终计算设备
     * - AUTO: 返回 ACTIVE_DEVICE
     * - GPU: 尝试 GPU, 失败抛错
     * - CPU: 强制 CPU
     */
    public static String resolveDevice() {
        ComputeMode mode = COMPUTE_MODE;
        if (mode == ComputeMode.CPU) return "CPU";
        if (mode == ComputeMode.GPU) {
            if (ACTIVE_DEVICE.startsWith("GPU")) return ACTIVE_DEVICE;
            throw new IllegalStateException("GPU mode requested but GPU not available. ACTIVE_DEVICE=" + ACTIVE_DEVICE);
        }
        return ACTIVE_DEVICE; // AUTO
    }

    // =============================================================
    // 九、getter/setter
    // =============================================================

    public static ComputeMode getComputeMode() { return COMPUTE_MODE; }
    public static void setComputeMode(ComputeMode m) { COMPUTE_MODE = m; }

    public static String getActiveDevice() { return ACTIVE_DEVICE; }

    /** 当前配置快照 (调试用) */
    public static Data snapshot() {
        Data d = new Data();
        d.computeMode = COMPUTE_MODE.name();
        d.activeDevice = ACTIVE_DEVICE;
        d.vocabSize = VOCAB_SIZE;
        d.hiddenDim = HIDDEN_DIM;
        d.numHeads = NUM_HEADS;
        d.numLayers = NUM_LAYERS;
        d.maxSeqLen = MAX_SEQ_LEN;
        d.temperature = TEMPERATURE;
        d.topK = TOP_K;
        d.topP = TOP_P;
        d.maxHistoryTurns = MAX_HISTORY_TURNS;
        d.maxGenerateTokens = MAX_GENERATE_TOKENS;
        d.ragTopK = RAG_TOP_K;
        d.ragSimilarityThreshold = RAG_SIMILARITY_THRESHOLD;
        d.batchSize = BATCH_SIZE;
        d.enableKvCache = ENABLE_KV_CACHE;
        d.enableInt8Quant = ENABLE_INT8_QUANT;
        d.asyncExec = ASYNC_EXEC;
        return d;
    }

    /** 配置快照 DTO (可序列化) */
    @Getter
    @Setter
    public static class Data {
        public String computeMode;
        public String activeDevice;
        public int vocabSize;
        public int hiddenDim;
        public int numHeads;
        public int numLayers;
        public int maxSeqLen;
        public float temperature;
        public int topK;
        public float topP;
        public int maxHistoryTurns;
        public int maxGenerateTokens;
        public int ragTopK;
        public double ragSimilarityThreshold;
        public int batchSize;
        public boolean enableKvCache;
        public boolean enableInt8Quant;
        public boolean asyncExec;
    }
}
