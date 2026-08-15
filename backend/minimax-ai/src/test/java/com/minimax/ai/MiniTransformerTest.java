package com.minimax.ai;

import com.minimax.ai.model.MiniTransformer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自研 Transformer 模型测试 (V2.6)
 */
class MiniTransformerTest {

    @Test
    void testConstructor() {
        MiniTransformer t = new MiniTransformer(100, 32, 2, 2, 16);
        assertEquals(100, t.getVocabSize());
        assertEquals(32, t.getHiddenDim());
        assertEquals(2, t.getNumHeads());
        assertEquals(2, t.getNumLayers());
        assertEquals(16, t.getMaxSeqLen());
    }

    @Test
    void testForwardShape() {
        MiniTransformer t = new MiniTransformer(50, 16, 2, 1, 8);
        int[] tokens = {1, 2, 3, 4, 5};
        double[][] logits = t.forward(tokens);
        // [seqLen, vocabSize]
        assertEquals(5, logits.length);
        assertEquals(50, logits[0].length);
    }

    @Test
    void testForwardValues() {
        MiniTransformer t = new MiniTransformer(20, 8, 2, 1, 4);
        int[] tokens = {0, 1, 2, 3};
        double[][] logits = t.forward(tokens);
        for (double[] row : logits) {
            for (double v : row) {
                assertTrue(Double.isFinite(v), "logits 应该是有限数: " + v);
            }
        }
    }

    @Test
    void testSoftmax() {
        double[] x = {1.0, 2.0, 3.0};
        double[] p = MiniTransformer.softmax(x);
        // sum = 1
        double sum = p[0] + p[1] + p[2];
        assertEquals(1.0, sum, 1e-6, "softmax 概率和为 1");
        // 单调递增
        assertTrue(p[0] < p[1] && p[1] < p[2]);
    }

    @Test
    void testParameterCount() {
        MiniTransformer t = new MiniTransformer(1000, 64, 4, 2, 32);
        long params = t.countParameters();
        // 应该是几百万级别
        assertTrue(params > 100_000, "参数 > 100K: " + params);
        assertTrue(params < 50_000_000, "参数 < 50M: " + params);
    }

    @Test
    void testSaveLoad() throws Exception {
        // V6.1+ 简化: save/load 只持久化 token+position+output 权重
        // (blocks 内部权重 He init 重新生成, 简化实现)
        MiniTransformer t1 = new MiniTransformer(100, 32, 2, 2, 16);
        java.io.File tmp = java.io.File.createTempFile("transformer-test", ".bin");
        tmp.deleteOnExit();

        t1.save(tmp);
        assertTrue(tmp.length() > 0, "保存的文件应该 > 0 字节");

        // 加载不应抛异常
        MiniTransformer t2 = MiniTransformer.load(tmp);
        assertNotNull(t2);

        // 验证词表大小一致
        assertEquals(t1.getVocabSize(), t2.getVocabSize());
        assertEquals(t1.getHiddenDim(), t2.getHiddenDim());
        assertEquals(t1.getNumLayers(), t2.getNumLayers());
    }
}
