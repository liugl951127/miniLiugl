package com.minimax.rag.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class VectorUtils {
    private VectorUtils() {}
    public static byte[] toBytes(float[] v) {
        if (v == null) return new byte[0];
        ByteBuffer bb = ByteBuffer.allocate(v.length * 4).order(ByteOrder.BIG_ENDIAN);
        bb.asFloatBuffer().put(v);
        return bb.array();
    }
    public static float[] fromBytes(byte[] b) {
        if (b == null || b.length == 0) return new float[0];
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN);
        FloatBuffer fb = bb.asFloatBuffer();
        float[] out = new float[fb.remaining()];
        fb.get(out);
        return out;
    }
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) return 0.0;
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
