package com.minimax.memory.longterm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 向量工具：float[] ↔ byte[] (BLOB 存储) + 余弦相似度。
 */
public final class VectorUtils {

    private VectorUtils() {}

    public static byte[] toBytes(float[] vec) {
        if (vec == null) return new byte[0];
        ByteBuffer bb = ByteBuffer.allocate(vec.length * 4).order(ByteOrder.BIG_ENDIAN);
        bb.asFloatBuffer().put(vec);
        return bb.array();
    }

    public static float[] fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new float[0];
        if (bytes.length % 4 != 0) throw new IllegalArgumentException("byte length must be multiple of 4");
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        FloatBuffer fb = bb.asFloatBuffer();
        float[] out = new float[fb.remaining()];
        fb.get(out);
        return out;
    }

    /**
     * 余弦相似度。范围 [-1, 1]，越大越相似。
     * 任一向量为 null/空 → 0。
     */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return 0.0;
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
}
