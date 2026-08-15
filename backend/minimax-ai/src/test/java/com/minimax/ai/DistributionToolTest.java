package com.minimax.ai;

import com.minimax.ai.tool.builtin.DistributionTool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DistributionTool 测试 (V2.7)
 */
class DistributionToolTest {

    @Test
    void testQuantile() throws Exception {
        DistributionTool tool = new DistributionTool(null, null);
        Method m = DistributionTool.class.getDeclaredMethod("quantile", List.class, double.class);
        m.setAccessible(true);
        List<Double> sorted = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(1.0, (Double) m.invoke(tool, sorted, 0.0), 0.001);
        assertEquals(3.0, (Double) m.invoke(tool, sorted, 0.5), 0.001);
        assertEquals(5.0, (Double) m.invoke(tool, sorted, 1.0), 0.001);
        // Q1 ≈ 2
        double q1 = (Double) m.invoke(tool, sorted, 0.25);
        assertTrue(q1 > 1.5 && q1 < 2.5, "Q1 should be near 2: " + q1);
    }

    @Test
    void testQuantileEmpty() throws Exception {
        DistributionTool tool = new DistributionTool(null, null);
        Method m = DistributionTool.class.getDeclaredMethod("quantile", List.class, double.class);
        m.setAccessible(true);
        assertEquals(0.0, (Double) m.invoke(tool, new ArrayList<>(), 0.5), 0.001);
    }

    @Test
    void testGetCode() {
        DistributionTool tool = new DistributionTool(null, null);
        assertEquals("data.analyze.distribution", tool.getCode());
    }
}
