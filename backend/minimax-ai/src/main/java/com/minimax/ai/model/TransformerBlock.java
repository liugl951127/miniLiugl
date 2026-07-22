package com.minimax.ai.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * 单个 Transformer Block:
 *   x -> LayerNorm -> Self-Attention -> Residual ->
 *   -> LayerNorm -> FFN -> Residual -> out
 */
public class TransformerBlock {

    private final int hiddenDim;
    private final int numHeads;
    private final int headDim;

    // Self-Attention 参数
    private final double[][] Wq, Wk, Wv, Wo;
    private final double[] bq, bk, bv, bo;

    // FFN 参数 (hidden -> 4*hidden -> hidden)
    private final double[][] W1, W2;
    private final double[] b1, b2;

    // LayerNorm
    private final double[] gamma1, beta1;
    private final double[] gamma2, beta2;

    private final Random random = new Random();

    public TransformerBlock(int hiddenDim, int numHeads) {
        this.hiddenDim = hiddenDim;
        this.numHeads = numHeads;
        this.headDim = hiddenDim / numHeads;
        if (headDim * numHeads != hiddenDim) {
            throw new IllegalArgumentException("hiddenDim must be divisible by numHeads");
        }

        // Attention
        Wq = init(hiddenDim, hiddenDim);
        Wk = init(hiddenDim, hiddenDim);
        Wv = init(hiddenDim, hiddenDim);
        Wo = init(hiddenDim, hiddenDim);
        bq = zeros(hiddenDim);
        bk = zeros(hiddenDim);
        bv = zeros(hiddenDim);
        bo = zeros(hiddenDim);

        // FFN
        W1 = init(hiddenDim, hiddenDim * 4);
        W2 = init(hiddenDim * 4, hiddenDim);
        b1 = zeros(hiddenDim * 4);
        b2 = zeros(hiddenDim);

        // LayerNorm
        gamma1 = ones(hiddenDim);
        beta1 = zeros(hiddenDim);
        gamma2 = ones(hiddenDim);
        beta2 = zeros(hiddenDim);
    }

    private double[][] init(int rows, int cols) {
        double[][] m = new double[rows][cols];
        double scale = Math.sqrt(2.0 / rows); // He init
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = random.nextGaussian() * scale;
            }
        }
        return m;
    }

    private double[] zeros(int n) {
        return new double[n];
    }

    private double[] ones(int n) {
        double[] v = new double[n];
        Arrays.fill(v, 1.0);
        return v;
    }

    public double[][] forward(double[][] x) {
        int seqLen = x.length;

        // 1. LayerNorm + Self-Attention + Residual
        double[][] normed = layerNorm(x, gamma1, beta1);
        double[][] attnOut = selfAttention(normed);
        double[][] afterAttn = add(x, attnOut);

        // 2. LayerNorm + FFN + Residual
        double[][] normed2 = layerNorm(afterAttn, gamma2, beta2);
        double[][] ffnOut = ffn(normed2);
        return add(afterAttn, ffnOut);
    }

    private double[][] selfAttention(double[][] x) {
        int seqLen = x.length;

        // Q, K, V 投影
        double[][] Q = matmul(x, Wq, bq);
        double[][] K = matmul(x, Wk, bk);
        double[][] V = matmul(x, Wv, bv);

        // Multi-head split
        double[][] output = new double[seqLen][hiddenDim];
        double scale = 1.0 / Math.sqrt(headDim);

        // V3.5.15+: numHeads 维度并行化 (Java parallel streams)
        // 每个 head 独立计算, 写入 output 的不同 offset 区间, 无冲突
        int[] headOffsets = new int[numHeads];
        for (int h = 0; h < numHeads; h++) headOffsets[h] = h * headDim;

        java.util.stream.IntStream.range(0, numHeads).parallel().forEach(h -> {
            int offset = headOffsets[h];
            // 注意力分数: [seq, seq]
            double[][] scores = new double[seqLen][seqLen];
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < seqLen; j++) {
                    double s = 0;
                    for (int d = 0; d < headDim; d++) {
                        s += Q[i][offset + d] * K[j][offset + d];
                    }
                    scores[i][j] = s * scale;
                }
            }
            // 因果 mask (下三角)
            for (int i = 0; i < seqLen; i++) {
                for (int j = i + 1; j < seqLen; j++) {
                    scores[i][j] = -1e9;
                }
            }
            // Softmax
            for (int i = 0; i < seqLen; i++) {
                double max = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < seqLen; j++) if (scores[i][j] > max) max = scores[i][j];
                double sum = 0;
                for (int j = 0; j < seqLen; j++) {
                    scores[i][j] = Math.exp(scores[i][j] - max);
                    sum += scores[i][j];
                }
                for (int j = 0; j < seqLen; j++) scores[i][j] /= sum;
            }
            // 加权求和 V
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

        // Output projection
        return matmul(output, Wo, bo);
    }

    private double[][] ffn(double[][] x) {
        int seqLen = x.length;
        // 1. W1 + ReLU
        double[][] h = matmul(x, W1, b1);
        for (int i = 0; i < seqLen; i++) {
            for (int j = 0; j < h[0].length; j++) {
                h[i][j] = Math.max(0, h[i][j]); // ReLU
            }
        }
        // 2. W2
        return matmul(h, W2, b2);
    }

    private double[][] matmul(double[][] x, double[][] w, double[] b) {
        int rows = x.length;
        int cols = w[0].length;
        int common = w.length;
        double[][] result = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double sum = b != null ? b[j] : 0;
                for (int k = 0; k < common; k++) {
                    sum += x[i][k] * w[k][j];
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

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

    public void save(DataOutputStream out) throws IOException {
        writeMatrix(out, Wq); writeMatrix(out, Wk); writeMatrix(out, Wv); writeMatrix(out, Wo);
        writeVector(out, bq); writeVector(out, bk); writeVector(out, bv); writeVector(out, bo);
        writeMatrix(out, W1); writeMatrix(out, W2);
        writeVector(out, b1); writeVector(out, b2);
        writeVector(out, gamma1); writeVector(out, beta1);
        writeVector(out, gamma2); writeVector(out, beta2);
    }

    public void load(DataInputStream in, int hiddenDim, int numHeads) throws IOException {
        // 验证模式一致
        // (调用方已保证大小一致)
        readMatrixInto(in, Wq); readMatrixInto(in, Wk);
        readMatrixInto(in, Wv); readMatrixInto(in, Wo);
        readVectorInto(in, bq); readVectorInto(in, bk);
        readVectorInto(in, bv); readVectorInto(in, bo);
        readMatrixInto(in, W1); readMatrixInto(in, W2);
        readVectorInto(in, b1); readVectorInto(in, b2);
        readVectorInto(in, gamma1); readVectorInto(in, beta1);
        readVectorInto(in, gamma2); readVectorInto(in, beta2);
    }

    private void writeMatrix(DataOutputStream out, double[][] m) throws IOException {
        out.writeInt(m.length);
        out.writeInt(m[0].length);
        for (double[] row : m) for (double v : row) out.writeDouble(v);
    }
    private double[][] readMatrix(DataInputStream in, int r, int c) throws IOException {
        double[][] m = new double[r][c];
        for (int i = 0; i < r; i++) for (int j = 0; j < c; j++) m[i][j] = in.readDouble();
        return m;
    }
    private void readMatrixInto(DataInputStream in, double[][] target) throws IOException {
        int r = in.readInt();
        int c = in.readInt();
        if (r != target.length || c != target[0].length) throw new IOException("matrix mismatch");
        for (int i = 0; i < r; i++) for (int j = 0; j < c; j++) target[i][j] = in.readDouble();
    }
    private void writeVector(DataOutputStream out, double[] v) throws IOException {
        out.writeInt(v.length);
        for (double x : v) out.writeDouble(x);
    }
    private void readVectorInto(DataInputStream in, double[] target) throws IOException {
        int len = in.readInt();
        if (len != target.length) throw new IOException("vector mismatch");
        for (int i = 0; i < len; i++) target[i] = in.readDouble();
    }
}
