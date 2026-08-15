package com.minimax.rag;

import com.minimax.rag.service.VectorUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectorUtilsTest {

    @Test
    void roundTrip() {
        float[] v = {1.0f, 2.5f, -3.14f, 0f, 100.123f};
        byte[] b = VectorUtils.toBytes(v);
        assertArrayEquals(v, VectorUtils.fromBytes(b), 1e-6f);
    }

    @Test
    void cosineIdentical() {
        float[] v = {1, 2, 3, 4};
        assertEquals(1.0, VectorUtils.cosine(v, v), 1e-6);
    }

    @Test
    void cosineOrthogonal() {
        assertEquals(0.0, VectorUtils.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-6);
    }

    @Test
    void cosineEmpty() {
        assertEquals(0.0, VectorUtils.cosine(null, new float[]{1, 2}));
        assertEquals(0.0, VectorUtils.cosine(new float[0], new float[]{1, 2}));
    }

    @Test
    void cosineDimensionMismatch() {
        assertEquals(0.0, VectorUtils.cosine(new float[]{1, 2, 3}, new float[]{1, 2}));
    }
}
