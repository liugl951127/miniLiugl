package com.minimax.ai.model;

// DataInputStream: 二进制输入流 (用于加载模型权重)
import java.io.DataInputStream;
// DataOutputStream: 二进制输出流 (用于保存模型权重)
import java.io.DataOutputStream;
// IOException: IO 异常
import java.io.IOException;
// Arrays: 数组工具 (fill 用)
import java.util.Arrays;
// Random: 随机数 (权重初始化)
import java.util.Random;

/**
 * 单个 Transformer Block (V6.0 详细注释版)
 *
 * <h2>架构</h2>
 * <pre>
 *   x
 *   │
 *   ├── LayerNorm1 ──→ Self-Attention ──→ ⊕ → residual
 *   │                                       │
 *   │                                       ▼
 *   │                                  LayerNorm2
 *   │                                       │
 *   │                                       ▼
 *   │                                       FFN (hidden → 4*hidden → hidden)
 *   │                                       │
 *   │                                       ▼
 *   └────────────────────────────────── ⊕ → output
 * </pre>
 *
 * <h2>Pre-Norm vs Post-Norm</h2>
 * 本实现用 Pre-Norm (LayerNorm 在 Attention/FFN 之前), 现代主流:
 *   - 训练更稳定 (深层网络)
 *   - 不需要 warmup
 *   - 论文: "On Layer Normalization in the Transformer Architecture" (2020)
 *
 * <h2>Self-Attention 机制</h2>
 * <pre>
 *   Q = x · Wq + bq      (Query: 查询向量)
 *   K = x · Wk + bk      (Key:  键向量)
 *   V = x · Wv + bv      (Value: 值向量)
 *
 *   score(i, j) = (Q[i] · K[j]) / sqrt(headDim)    (点积 + 缩放)
 *   mask(i, j) = -inf if j > i else 0              (因果 mask, 看不到未来)
 *   attn = softmax(score + mask)                    (按行 softmax)
 *   out[i] = Σ_j attn[i, j] · V[j]                  (加权求和)
 *
 *   output = out · Wo + bo                          (输出投影)
 * </pre>
 *
 * <h2>Multi-Head 优势</h2>
 * 单个 head 只能关注一种模式, 多 head 并行关注不同子空间:
 *   - Head 1: 主谓关系
 *   - Head 2: 长距离依赖
 *   - Head 3: 句法结构
 *   - ... (取决于训练)
 *
 * <h2>FFN 作用</h2>
 * 两层全连接 + ReLU 激活:
 *   - 增加非线性
 *   - 相当于 "key-value memory" (论文 "Transformer Feed-Forward Layers Are Key-Value Memories")
 *   - 维度 hidden → 4*hidden → hidden
 *
 * <h2>参数规模 (hidden=64, heads=4)</h2>
 *   - Attention: 4 * 64*64 + 4*64 = 16640
 *   - FFN: 64*256 + 256*64 + 256 + 64 = 24896
 *   - LayerNorm: 2 * (64+64) = 256
 *   - Total: ~41792 参数 / block
 *//**
 * TransformerBlock (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * ML 模型 - TransformerBlock.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 TransformerBlock 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */

public class TransformerBlock {

    // ============== 超参数 ==============
    // hiddenDim: 隐藏层维度 (词向量维度)
    private final int hiddenDim;
    // numHeads: 注意力头数 (必须能整除 hiddenDim)
    private final int numHeads;
    // headDim: 每个 head 的维度 (= hiddenDim / numHeads)
    private final int headDim;

    // ============== Self-Attention 参数 ==============
    // Wq: Query 投影矩阵 [hiddenDim x hiddenDim]
    private final double[][] Wq;
    // Wk: Key 投影矩阵
    private final double[][] Wk;
    // Wv: Value 投影矩阵
    private final double[][] Wv;
    // Wo: Output 投影矩阵 (多头拼接后再投影回 hiddenDim)
    private final double[][] Wo;
    // bq/bk/bv/bo: 对应偏置向量 (默认全 0)
    private final double[] bq, bk, bv, bo;

    // ============== FFN 参数 ==============
    // W1: 第一层权重 [hiddenDim x 4*hiddenDim] (升维)
    private final double[][] W1;
    // W2: 第二层权重 [4*hiddenDim x hiddenDim] (降维)
    private final double[][] W2;
    // b1/b2: 对应偏置
    private final double[] b1, b2;

    // ============== LayerNorm 参数 ==============
    // gamma1/beta1: 第一个 LayerNorm 的缩放/偏移
    private final double[] gamma1, beta1;
    // gamma2/beta2: 第二个 LayerNorm
    private final double[] gamma2, beta2;

    // ============== 工具 ==============
    // 随机数生成器 (权重初始化用)
    private final Random random = new Random();

    /**
     * 构造器: 初始化所有参数
     *
     * @param hiddenDim 隐藏层维度 (e.g. 64)
     * @param numHeads  注意力头数 (e.g. 4)
     * @throws IllegalArgumentException 当 hiddenDim 不能被 numHeads 整除
     */
    public TransformerBlock(int hiddenDim, int numHeads) {
        this.hiddenDim = hiddenDim;
        this.numHeads = numHeads;
        // headDim = hiddenDim / numHeads
        // 例: 64 / 4 = 16
        this.headDim = hiddenDim / numHeads;
        // 整除校验 (否则 attention 计算会错位)
        if (headDim * numHeads != hiddenDim) {
            throw new IllegalArgumentException("hiddenDim must be divisible by numHeads");
        }

        // ====== 初始化 Attention 参数 (4 个权重 + 4 个偏置) ======
        // He 初始化: scale = sqrt(2 / fan_in)
        Wq = init(hiddenDim, hiddenDim);
        Wk = init(hiddenDim, hiddenDim);
        Wv = init(hiddenDim, hiddenDim);
        Wo = init(hiddenDim, hiddenDim);
        bq = zeros(hiddenDim);
        bk = zeros(hiddenDim);
        bv = zeros(hiddenDim);
        bo = zeros(hiddenDim);

        // ====== 初始化 FFN 参数 (升维 → 降维) ======
        // hidden → 4*hidden (升维, 增加表达能力)
        W1 = init(hiddenDim, hiddenDim * 4);
        // 4*hidden → hidden (降维, 输出原维度)
        W2 = init(hiddenDim * 4, hiddenDim);
        b1 = zeros(hiddenDim * 4);
        b2 = zeros(hiddenDim);

        // ====== 初始化 LayerNorm 参数 ======
        // gamma 初始为 1 (不缩放), beta 初始为 0 (不偏移)
        gamma1 = ones(hiddenDim);
        beta1 = zeros(hiddenDim);
        gamma2 = ones(hiddenDim);
        beta2 = zeros(hiddenDim);
    }

    /**
     * He 初始化 (又称 Kaiming 初始化)
     *
     * <h2>原理</h2>
     * 随机权重 ~ N(0, σ²), 其中 σ = sqrt(2 / fan_in)
     * 目的: 让每层输出方差 ≈ 1, 避免梯度消失/爆炸
     *
     * <h2>对比</h2>
     * - Xavier (Glorot): σ = sqrt(1 / fan_in), 适合 sigmoid/tanh
     * - He: σ = sqrt(2 / fan_in), 适合 ReLU
     *
     * @param rows 矩阵行数 (fan_out)
     * @param cols 矩阵列数 (fan_in)
     * @return 随机权重矩阵
     */
    private double[][] init(int rows, int cols) {
        double[][] m = new double[rows][cols];
        // He init scale
        double scale = Math.sqrt(2.0 / rows);
        // 填充高斯分布
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // nextGaussian() 返回 N(0, 1) 的随机数
                m[i][j] = random.nextGaussian() * scale;
            }
        }
        return m;
    }

    /**
     * 全 0 向量 (用于偏置)
     */
    private double[] zeros(int n) {
        return new double[n];
    }

    /**
     * 全 1 向量 (LayerNorm 的 gamma 初值)
     */
    private double[] ones(int n) {
        double[] v = new double[n];
        Arrays.fill(v, 1.0);
        return v;
    }

    // ============== Forward Pass ==============
    /**
     * 前向传播: 输入 [seqLen, hiddenDim] → 输出 [seqLen, hiddenDim]
     *
     * <h2>两步计算</h2>
     * <pre>
     *   步骤 1: normed = LayerNorm(x)
     *           attn = SelfAttention(normed)
     *           h1 = x + attn  (残差连接)
     *
     *   步骤 2: normed2 = LayerNorm(h1)
     *           ffn = FFN(normed2)
     *           out = h1 + ffn  (残差连接)
     * </pre>
     *
     * @param x 输入 [seqLen, hiddenDim]
     * @return 输出 [seqLen, hiddenDim]
     */
    public double[][] forward(double[][] x) {
        int seqLen = x.length;

        // ====== 步骤 1: Pre-Norm + Self-Attention + Residual ======
        // 1.1 LayerNorm 归一化输入 (稳定训练)
        double[][] normed = layerNorm(x, gamma1, beta1);
        // 1.2 Self-Attention 计算 (见下方详细)
        double[][] attnOut = selfAttention(normed);
        // 1.3 残差连接: out = x + attn(x)
        //    作用: 保留原始信息, 缓解梯度消失
        double[][] afterAttn = add(x, attnOut);

        // ====== 步骤 2: Pre-Norm + FFN + Residual ======
        // 2.1 第二次 LayerNorm
        double[][] normed2 = layerNorm(afterAttn, gamma2, beta2);
        // 2.2 FFN (前馈网络)
        double[][] ffnOut = ffn(normed2);
        // 2.3 第二次残差连接
        return add(afterAttn, ffnOut);
    }

    // ============== Self-Attention ==============
    /**
     * 多头自注意力 (Multi-Head Self-Attention)
     *
     * <h2>步骤</h2>
     * <pre>
     *   1. Q, K, V = x · Wq/Wk/Wv + b  (3 次矩阵乘法)
     *   2. 拆成 numHeads 个子空间
     *   3. 每个 head 独立计算 attention (并行)
     *   4. 拼接所有 head 输出
     *   5. Wo 投影回 hiddenDim
     * </pre>
     *
     * <h2>复杂度</h2>
     * - 时间: O(seqLen² · hiddenDim)
     * - 空间: O(seqLen² · numHeads) (attention 矩阵)
     *
     * @param x 输入 [seqLen, hiddenDim]
     * @return 输出 [seqLen, hiddenDim]
     */
    private double[][] selfAttention(double[][] x) {
        int seqLen = x.length;

        // ====== 1. Q, K, V 投影 (3 次矩阵乘法) ======
        // Q[i] = x[i] · Wq + bq  (query: "我要找什么")
        // K[i] = x[i] · Wk + bk  (key:   "我代表什么")
        // V[i] = x[i] · Wv + bv  (value: "我提供什么信息")
        double[][] Q = matmul(x, Wq, bq);
        double[][] K = matmul(x, Wk, bk);
        double[][] V = matmul(x, Wv, bv);

        // ====== 2. 多头拆分 (按 headDim 切分维度) ======
        // 例: hiddenDim=64, numHeads=4 → headDim=16
        //     head 0: dim 0-15
        //     head 1: dim 16-31
        //     head 2: dim 32-47
        //     head 3: dim 48-63
        double[][] output = new double[seqLen][hiddenDim];
        // 缩放因子 1/sqrt(headDim)
        // 作用: 防止 Q·K 点积过大导致 softmax 梯度消失
        // 解释: Q, K ~ N(0, 1), Q·K ~ N(0, headDim), 缩放后 ~ N(0, 1)
        double scale = 1.0 / Math.sqrt(headDim);

        // ====== 3. 每个 head 独立计算 (并行) ======
        // V3.5.15+ 用 Java parallel streams 并行化
        // 每个 head 写入 output 的不同 offset, 无写冲突
        int[] headOffsets = new int[numHeads];
        // 预计算每个 head 在 output 中的起始位置
        for (int h = 0; h < numHeads; h++) headOffsets[h] = h * headDim;

        // 并行 forEach: 多个 head 同时计算
        java.util.stream.IntStream.range(0, numHeads).parallel().forEach(h -> {
            int offset = headOffsets[h];
            // ===== 3.1 计算 attention 分数 (Q · K^T) =====
            // scores[i][j] = Q[i] · K[j] / sqrt(headDim)
            // 含义: 第 i 个 token 对第 j 个 token 的关注度
            double[][] scores = new double[seqLen][seqLen];
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < seqLen; j++) {
                    // 点积 Q[i] · K[j] (只在该 head 的子空间内)
                    double s = 0;
                    for (int d = 0; d < headDim; d++) {
                        s += Q[i][offset + d] * K[j][offset + d];
                    }
                    scores[i][j] = s * scale;  // 缩放
                }
            }
            // ===== 3.2 因果 Mask (语言模型只能看过去) =====
            // 把 j > i 的位置设成 -inf, softmax 后概率为 0
            for (int i = 0; i < seqLen; i++) {
                for (int j = i + 1; j < seqLen; j++) {
                    scores[i][j] = -1e9;  // 数值上 -inf, 避免 softmax NaN
                }
            }
            // ===== 3.3 Softmax (按行, 转为概率分布) =====
            // 公式: softmax(x_i) = e^x_i / Σ e^x_j
            for (int i = 0; i < seqLen; i++) {
                // 数值稳定: 减去最大值 (e^max 不会爆)
                double max = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < seqLen; j++) if (scores[i][j] > max) max = scores[i][j];
                // 计算 exp
                double sum = 0;
                for (int j = 0; j < seqLen; j++) {
                    scores[i][j] = Math.exp(scores[i][j] - max);
                    sum += scores[i][j];
                }
                // 归一化 (概率和为 1)
                for (int j = 0; j < seqLen; j++) scores[i][j] /= sum;
            }
            // ===== 3.4 加权求和 V =====
            // output[i] = Σ_j scores[i][j] · V[j]
            for (int i = 0; i < seqLen; i++) {
                for (int d = 0; d < headDim; d++) {
                    double s = 0;
                    for (int j = 0; j < seqLen; j++) {
                        s += scores[i][j] * V[j][offset + d];
                    }
                    output[i][offset + d] = s;
                }
            }
        });

        // ====== 4. 输出投影 (多头拼接 → hiddenDim) ======
        return matmul(output, Wo, bo);
    }

    // ============== FFN ==============
    /**
     * 前馈网络 (Feed-Forward Network)
     *
     * <h2>结构</h2>
     * <pre>
     *   FFN(x) = ReLU(x · W1 + b1) · W2 + b2
     * </pre>
     *
     * <h2>维度变化</h2>
     *   hiddenDim → 4*hiddenDim → hiddenDim
     *   (升维增加表达能力, 降维输出原维度)
     *
     * <h2>ReLU</h2>
     *   ReLU(x) = max(0, x)
     *   引入非线性, 否则多层线性等价于单层
     *
     * @param x 输入 [seqLen, hiddenDim]
     * @return 输出 [seqLen, hiddenDim]
     */
    private double[][] ffn(double[][] x) {
        int seqLen = x.length;
        // 1. 第一层: hiddenDim → 4*hiddenDim (升维)
        double[][] h = matmul(x, W1, b1);
        // 2. ReLU 激活 (逐元素)
        for (int i = 0; i < seqLen; i++) {
            for (int j = 0; j < h[0].length; j++) {
                // ReLU: max(0, x)
                h[i][j] = Math.max(0, h[i][j]);
            }
        }
        // 3. 第二层: 4*hiddenDim → hiddenDim (降维)
        return matmul(h, W2, b2);
    }

    // ============== 矩阵运算 ==============
    /**
     * 矩阵乘法 + 偏置
     *
     * <h2>公式</h2>
     *   result[i][j] = Σ_k x[i][k] · w[k][j] + b[j]
     *
     * <h2>复杂度</h2>
     *   时间: O(rows · cols · common)
     *   空间: O(rows · cols)
     *
     * @param x 输入 [rows x common]
     * @param w 权重 [common x cols]
     * @param b 偏置 [cols]
     * @return 输出 [rows x cols]
     */
    private double[][] matmul(double[][] x, double[][] w, double[] b) {
        int rows = x.length;          // 输入行数 (seqLen)
        int cols = w[0].length;       // 输出列数 (输出维度)
        int common = w.length;        // 公共维度 (输入维度)
        double[][] result = new double[rows][cols];
        // 三重循环: O(rows × cols × common)
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double sum = b != null ? b[j] : 0;  // 偏置
                for (int k = 0; k < common; k++) {
                    sum += x[i][k] * w[k][j];
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

    // ============== 残差连接 ==============
    /**
     * 元素级加法 (残差连接)
     *
     * <h2>公式</h2>
     *   out[i][j] = a[i][j] + b[i][j]
     *
     * <h2>为什么用残差</h2>
     *   - 缓解梯度消失 (深层网络训练)
     *   - 保留原始信息 (x 可以直接"穿透"到输出)
     *   - 类比: y = x + F(x), F(x) 学的是"残差"
     *
     * @param a 输入 A
     * @param b 输入 B (要求同维度)
     * @return A + B
     */
    private double[][] add(double[][] a, double[][] b) {
        int rows = a.length;
        int cols = a[0].length;
        double[][] result = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = a[i][j] + b[i][j];
            }
        }
        return result;
    }

    // ============== Layer Normalization ==============
    /**
     * Layer Normalization
     *
     * <h2>公式</h2>
     *   LayerNorm(x) = γ · (x - μ) / sqrt(σ² + ε) + β
     *
     *   μ = mean(x)         (均值)
     *   σ² = var(x)          (方差)
     *   γ = gamma (learnable scale)
     *   β = beta  (learnable shift)
     *   ε = 1e-6 (数值稳定)
     *
     * <h2>vs Batch Norm</h2>
     *   - LayerNorm: 跨特征 (每个 token 独立归一化)
     *   - BatchNorm: 跨样本 (每个特征独立归一化)
     *   - Transformer 用 LayerNorm, 不依赖 batch size
     *
     * @param x 输入 [seqLen, hiddenDim]
     * @param gamma 缩放参数 [hiddenDim]
     * @param beta  偏移参数 [hiddenDim]
     * @return 归一化后 [seqLen, hiddenDim]
     */
    private double[][] layerNorm(double[][] x, double[] gamma, double[] beta) {
        int rows = x.length;
        int cols = x[0].length;
        double[][] result = new double[rows][cols];
        // 对每个 token (行) 独立归一化
        for (int i = 0; i < rows; i++) {
            // 1. 计算均值
            double mean = 0;
            for (int j = 0; j < cols; j++) mean += x[i][j];
            mean /= cols;
            // 2. 计算方差
            double variance = 0;
            for (int j = 0; j < cols; j++) {
                double d = x[i][j] - mean;
                variance += d * d;
            }
            variance /= cols;
            // 3. 归一化 + 缩放/偏移
            double std = Math.sqrt(variance + 1e-6);  // +1e-6 防除 0
            for (int j = 0; j < cols; j++) {
                result[i][j] = gamma[j] * (x[i][j] - mean) / std + beta[j];
            }
        }
        return result;
    }

    // ============== 模型持久化 (后续可扩展) ==============
    /**
     * 保存模型权重到二进制流
     * (占位, 暂未启用)
     */
    public void save(DataOutputStream out) throws IOException {
        // TODO: 实现权重序列化
    }

    /**
     * 从二进制流加载模型权重
     * (占位, 暂未启用)
     */
    public void load(DataInputStream in) throws IOException {
        // TODO: 实现权重反序列化
    }
}
