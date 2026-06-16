package com.minimax.memory;

import com.minimax.memory.longterm.VectorUtils;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VectorUtilsTest {

    @Test
    void bytesRoundTrip() {
        float[] v = {1.0f, 2.5f, -3.14f, 0.0f, 100.123f};
        byte[] b = VectorUtils.toBytes(v);
        float[] back = VectorUtils.fromBytes(b);
        assertArrayEquals(v, back, 1e-6f);
    }

    @Test
    void cosineIdentical() {
        float[] v = {1, 2, 3, 4, 5};
        assertEquals(1.0, VectorUtils.cosine(v, v), 1e-6);
    }

    @Test
    void cosineOrthogonal() {
        float[] a = {1, 0, 0};
        float[] b = {0, 1, 0};
        assertEquals(0.0, VectorUtils.cosine(a, b), 1e-6);
    }

    @Test
    void cosineOpposite() {
        float[] a = {1, 0, 0};
        float[] b = {-1, 0, 0};
        assertEquals(-1.0, VectorUtils.cosine(a, b), 1e-6);
    }

    @Test
    void cosineEmpty() {
        assertEquals(0.0, VectorUtils.cosine(null, new float[]{1, 2}));
        assertEquals(0.0, VectorUtils.cosine(new float[0], new float[]{1, 2}));
    }

    @Test
    void cosineDimensionMismatch() {
        float[] a = {1, 2, 3};
        float[] b = {1, 2};
        assertEquals(0.0, VectorUtils.cosine(a, b));
    }

    @Test
    void cosineRandom() {
        Random r = new Random(42);
        float[] a = new float[128];
        float[] b = new float[128];
        for (int i = 0; i < 128; i++) { a[i] = r.nextFloat(); b[i] = r.nextFloat(); }
        double sim = VectorUtils.cosine(a, b);
        assertTrue(sim >= -1.0 && sim <= 1.0, "cosine in [-1,1], got " + sim);
    }
}
