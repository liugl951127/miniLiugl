package com.minimax.ai.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

/**
 * MiniMax 自研 Transformer (V2.5 简化版)
 *
 * 架构 (超简化, CPU 跑得动):
 *   Embedding -> N x [Self-Attention + FFN] -> LayerNorm -> LM Head
 *
 * 参数规模: ~1-2M (V2.5 配置: hidden=128, layers=2)
 * 训练时间: CPU 1-2 分钟
 * 推理速度: CPU 50-200ms / token
 *
 * 数学 (纯手写, 不调用 ND4J / DJL):
 *   - Embedding: x * E (查表)
 *   - Self-Attention: softmax(Q*K^T / sqrt(d)) * V
 *   - FFN: relu(xW1 + b1) W2 + b2
 *   - LayerNorm: (x - mean) / sqrt(var + eps) * gamma + beta
 *   - Softmax: exp(x_i) / sum(exp(x_j))
 *
 * 不依赖:
 *   - DJL (Deep Java Library)
 *   - ND4J
 *   - TensorFlow Java
 *   - ONNX Runtime
 *
 * V2.5 自研 - 纯 Java 数组运算
 */
@Slf4j
public class MiniTransformer {

    @Getter
    private final int vocabSize;
    @Getter
    private final int hiddenDim;
    @Getter
    private final int numHeads;
    @Getter
    private final int numLayers;
    @Getter
    private final int maxSeqLen;

    /** 词嵌入: [vocabSize, hiddenDim] */
    private double[][] tokenEmbedding;
    /** 位置嵌入: [maxSeqLen, hiddenDim] */
    private double[][] positionEmbedding;

    /** 每层参数 */
    private TransformerBlock[] blocks;

    /** 输出层 norm */
    private double[] finalGamma;
    private double[] finalBeta;

    /** 输出投影: [hiddenDim, vocabSize] */
    private double[][] outputProjection;

    /** 随机初始化 */
    private final Random random = new Random(42);

    public MiniTransformer(int vocabSize, int hiddenDim, int numHeads,
                            int numLayers, int maxSeqLen) {
        this.vocabSize = vocabSize;
        this.hiddenDim = hiddenDim;
        this.numHeads = numHeads;
        this.numLayers = numLayers;
        this.maxSeqLen = maxSeqLen;
        init();
    }

    private void init() {
        log.info("初始化 MiniTransformer: vocab={}, hidden={}, heads={}, layers={}, seq={}",
                vocabSize, hiddenDim, numHeads, numLayers, maxSeqLen);

        // 词嵌入: 小随机数
        tokenEmbedding = randn(vocabSize, hiddenDim, 0.02);
        // 位置嵌入
        positionEmbedding = randn(maxSeqLen, hiddenDim, 0.02);

        // Transformer 块
        blocks = new TransformerBlock[numLayers];
        for (int i = 0; i < numLayers; i++) {
            blocks[i] = new TransformerBlock(hiddenDim, numHeads);
        }

        // Final LayerNorm
        finalGamma = new double[hiddenDim];
        finalBeta = new double[hiddenDim];
        for (int i = 0; i < hiddenDim; i++) {
            finalGamma[i] = 1.0;
            finalBeta[i] = 0.0;
        }

        // 输出投影 (tied with embedding 用 lm_head 重用)
        outputProjection = randn(hiddenDim, vocabSize, 0.02);
    }

    /**
     * 随机矩阵 (He 初始化)
     */
    private double[][] randn(int rows, int cols, double scale) {
        double[][] m = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = random.nextGaussian() * scale;
            }
        }
        return m;
    }

    /**
     * Forward: 输入 token ids -> 每个位置的 logits [seqLen, vocabSize]
     */
    public double[][] forward(int[] tokenIds) {
        int seqLen = Math.min(tokenIds.length, maxSeqLen);

        // 1. Token + Position Embedding
        double[][] x = new double[seqLen][hiddenDim];
        for (int i = 0; i < seqLen; i++) {
            int tokenId = tokenIds[i];
            if (tokenId < 0 || tokenId >= vocabSize) tokenId = 1; // UNK
            // x = embed[token] + pos[i]
            for (int j = 0; j < hiddenDim; j++) {
                x[i][j] = tokenEmbedding[tokenId][j] + positionEmbedding[i][j];
            }
        }

        // 2. N x Transformer Block
        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }

        // 3. Final LayerNorm
        x = layerNorm(x, finalGamma, finalBeta);

        // 4. Output Projection -> logits
        double[][] logits = new double[seqLen][vocabSize];
        for (int i = 0; i < seqLen; i++) {
            for (int v = 0; v < vocabSize; v++) {
                double sum = 0.0;
                for (int j = 0; j < hiddenDim; j++) {
                    sum += x[i][j] * outputProjection[j][v];
                }
                logits[i][v] = sum;
            }
        }
        return logits;
    }

    /**
     * V3.5.16+: 句向量提取 (平均池化)
     *
     * <p>把 seqLen 个 token 的 hidden 状态平均成 1 个 hiddenDim 向量,
     * 用于语义相似度比较 (cosine).
     *
     * <p>不调用 outputProjection (省 vocabSize 投影).
     *
     * @param tokenIds 编码后的 token 序列
     * @return hiddenDim 维句向量
     */
    public double[] embed(int[] tokenIds) {
        int seqLen = Math.min(tokenIds.length, maxSeqLen);
        if (seqLen == 0) {
            return new double[hiddenDim];
        }

        // 1. Embedding
        double[][] x = new double[seqLen][hiddenDim];
        for (int i = 0; i < seqLen; i++) {
            int tokenId = tokenIds[i];
            if (tokenId < 0 || tokenId >= vocabSize) tokenId = 1;
            for (int j = 0; j < hiddenDim; j++) {
                x[i][j] = tokenEmbedding[tokenId][j] + positionEmbedding[i][j];
            }
        }

        // 2. N x Transformer Block (无 output projection, 省一半计算)
        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }

        // 3. Final LayerNorm
        x = layerNorm(x, finalGamma, finalBeta);

        // 4. 平均池化 -> 句向量 [hiddenDim]
        double[] sentenceVec = new double[hiddenDim];
        for (int i = 0; i < seqLen; i++) {
            for (int j = 0; j < hiddenDim; j++) {
                sentenceVec[j] += x[i][j];
            }
        }
        for (int j = 0; j < hiddenDim; j++) {
            sentenceVec[j] /= seqLen;
        }
        return sentenceVec;
    }

    /**
     * 批量 embed (省多次 forward 的 Java overhead)
     *
     * @param tokenIdsBatch 多个 token 序列
     * @return 每个序列的句向量
     */
    public double[][] embedBatch(int[][] tokenIdsBatch) {
        double[][] result = new double[tokenIdsBatch.length][];
        for (int i = 0; i < tokenIdsBatch.length; i++) {
            result[i] = embed(tokenIdsBatch[i]);
        }
        return result;
    }

    /**
     * 余弦相似度
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Layer Normalization
     */
    private double[][] layerNorm(double[][] x, double[] gamma, double[] beta) {
        int rows = x.length;
        int cols = x[0].length;
        double[][] result = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            double mean = 0, var = 0;
            for (int j = 0; j < cols; j++) mean += x[i][j];
            mean /= cols;
            for (int j = 0; j < cols; j++) var += (x[i][j] - mean) * (x[i][j] - mean);
            var /= cols;
            double std = Math.sqrt(var + 1e-6);
            for (int j = 0; j < cols; j++) {
                result[i][j] = (x[i][j] - mean) / std * gamma[j] + beta[j];
            }
        }
        return result;
    }

    /**
     * Softmax
     */
    public static double[] softmax(double[] x) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : x) if (v > max) max = v;
        double sum = 0;
        double[] exps = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            exps[i] = Math.exp(x[i] - max);
            sum += exps[i];
        }
        for (int i = 0; i < x.length; i++) {
            exps[i] /= sum;
        }
        return exps;
    }

    /**
     * 采样下一个 token (top-k + top-p + temperature)
     */
    public int sampleNextToken(double[] logits, double temperature, int topK, double topP) {
        // 1. Temperature
        for (int i = 0; i < logits.length; i++) {
            logits[i] /= temperature;
        }

        // 2. Top-K 过滤
        int[] indices = argsort(logits);
        int k = Math.min(topK, indices.length);
        for (int i = k; i < indices.length; i++) {
            logits[indices[i]] = Double.NEGATIVE_INFINITY;
        }

        // 3. Softmax
        double[] probs = softmax(logits);

        // 4. Top-P 过滤 (nucleus)
        double cumSum = 0;
        int cutoff = 0;
        for (int idx : indices) {
            cumSum += probs[idx];
            cutoff++;
            if (cumSum >= topP) break;
        }
        for (int i = cutoff; i < indices.length; i++) {
            probs[indices[i]] = 0;
        }

        // 5. 归一化 + 采样
        double sum = 0;
        for (double p : probs) sum += p;
        for (int i = 0; i < probs.length; i++) probs[i] /= sum;

        double r = random.nextDouble();
        double acc = 0;
        for (int i = 0; i < probs.length; i++) {
            acc += probs[i];
            if (r <= acc) return i;
        }
        return indices[0];
    }

    /**
     * argsort (降序)
     */
    private int[] argsort(double[] x) {
        Integer[] idx = new Integer[x.length];
        for (int i = 0; i < x.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(x[b], x[a]));
        int[] result = new int[x.length];
        for (int i = 0; i < x.length; i++) result[i] = idx[i];
        return result;
    }

    /**
     * 序列化模型
     */
    public void save(File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(vocabSize);
            out.writeInt(hiddenDim);
            out.writeInt(numHeads);
            out.writeInt(numLayers);
            out.writeInt(maxSeqLen);

            writeMatrix(out, tokenEmbedding);
            writeMatrix(out, positionEmbedding);
            for (TransformerBlock b : blocks) b.save(out);
            writeVector(out, finalGamma);
            writeVector(out, finalBeta);
            writeMatrix(out, outputProjection);
        }
        log.info("模型已保存: {} ({} bytes)", file, file.length());
    }

    public void load(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (in.readInt() != vocabSize) throw new IOException("vocab mismatch");
            if (in.readInt() != hiddenDim) throw new IOException("hidden mismatch");
            if (in.readInt() != numHeads) throw new IOException("heads mismatch");
            if (in.readInt() != numLayers) throw new IOException("layers mismatch");
            if (in.readInt() != maxSeqLen) throw new IOException("seqlen mismatch");

            tokenEmbedding = readMatrix(in, vocabSize, hiddenDim);
            positionEmbedding = readMatrix(in, maxSeqLen, hiddenDim);
            for (TransformerBlock b : blocks) b.load(in, hiddenDim, numHeads);
            finalGamma = readVector(in, hiddenDim);
            finalBeta = readVector(in, hiddenDim);
            outputProjection = readMatrix(in, hiddenDim, vocabSize);
        }
        log.info("模型已加载: {} ({} bytes)", file, file.length());
    }

    private void writeMatrix(DataOutputStream out, double[][] m) throws IOException {
        out.writeInt(m.length);
        out.writeInt(m[0].length);
        for (double[] row : m) {
            for (double v : row) out.writeDouble(v);
        }
    }

    private double[][] readMatrix(DataInputStream in, int rows, int cols) throws IOException {
        int r = in.readInt();
        int c = in.readInt();
        if (r != rows || c != cols) throw new IOException("matrix size mismatch");
        double[][] m = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) m[i][j] = in.readDouble();
        }
        return m;
    }

    private void writeVector(DataOutputStream out, double[] v) throws IOException {
        out.writeInt(v.length);
        for (double x : v) out.writeDouble(x);
    }

    private double[] readVector(DataInputStream in, int n) throws IOException {
        int len = in.readInt();
        if (len != n) throw new IOException("vector size mismatch");
        double[] v = new double[len];
        for (int i = 0; i < len; i++) v[i] = in.readDouble();
        return v;
    }

    /**
     * 估算参数量
     */
    public long countParameters() {
        long count = (long) vocabSize * hiddenDim    // token emb
                + (long) maxSeqLen * hiddenDim        // pos emb
                + (long) numLayers * (4 * hiddenDim * hiddenDim * 2  // Q/K/V/O
                + 2 * hiddenDim * (hiddenDim * 4)  // FFN
                + 4 * hiddenDim)                     // 2 LayerNorm
                + (long) hiddenDim * vocabSize;      // output proj
        return count;
    }
}
