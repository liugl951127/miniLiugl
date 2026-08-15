package com.minimax.ai;

import com.minimax.ai.training.TrainingTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainingTrackerTest {

    @Test
    void testCreateTask() {
        TrainingTracker t = new TrainingTracker();
        String id = t.createTask("test", "mini", 5, "{}");
        assertNotNull(id);
        assertTrue(id.startsWith("train-"));
        assertEquals(1, t.listAll().size());
    }

    @Test
    void testTaskLifecycle() {
        TrainingTracker t = new TrainingTracker();
        String id = t.createTask("t", "m", 3, "{}");
        assertEquals("PENDING", t.get(id).status);

        t.start(id);
        assertEquals("RUNNING", t.get(id).status);

        t.record(id, new TrainingTracker.MetricPoint(1, 10, 5.0, 5.2, 0.3, 0.01, 100));
        t.record(id, new TrainingTracker.MetricPoint(2, 20, 4.0, 4.5, 0.4, 0.01, 200));
        t.record(id, new TrainingTracker.MetricPoint(3, 30, 3.0, 3.5, 0.5, 0.01, 300));

        assertEquals(3, t.get(id).history.size());
        assertEquals(3, t.get(id).currentEpoch);

        t.complete(id);
        assertEquals("COMPLETED", t.get(id).status);
        assertTrue(t.get(id).endTimeMs > 0);
    }

    @Test
    void testFail() {
        TrainingTracker t = new TrainingTracker();
        String id = t.createTask("t", "m", 3, "{}");
        t.fail(id, "out of memory");
        assertEquals("FAILED", t.get(id).status);
        assertEquals("out of memory", t.get(id).error);
    }

    @Test
    void testGetHistory() {
        TrainingTracker t = new TrainingTracker();
        String id = t.createTask("t", "m", 2, "{}");
        t.record(id, new TrainingTracker.MetricPoint(1, 10, 5.0, 5.0, 0.0, 0.01, 0));
        t.record(id, new TrainingTracker.MetricPoint(2, 20, 4.0, 4.0, 0.0, 0.01, 0));
        assertEquals(2, t.getHistory(id).size());
    }

    @Test
    void testRemove() {
        TrainingTracker t = new TrainingTracker();
        String id = t.createTask("t", "m", 1, "{}");
        assertTrue(t.remove(id));
        assertNull(t.get(id));
        assertEquals(0, t.listAll().size());
    }

    @Test
    void testEma() {
        List<Double> v = List.of(5.0, 4.0, 3.0, 2.0, 1.0);
        List<Double> smoothed = TrainingTracker.ema(v, 0.5);
        assertEquals(5, smoothed.size());
        // EMA: 5 -> 5 -> 4.5 -> 3.75 -> 2.875 -> 1.9375
        assertEquals(5.0, smoothed.get(0), 0.01);
        assertTrue(smoothed.get(4) < 5.0);
        assertTrue(smoothed.get(4) > 1.0);
    }

    @Test
    void testEmaEmpty() {
        assertTrue(TrainingTracker.ema(List.of(), 0.1).isEmpty());
    }

    @Test
    void testMetricPointToMap() {
        TrainingTracker.MetricPoint p = new TrainingTracker.MetricPoint(1, 100, 4.5, 4.6, 0.5, 0.01, 1000);
        var m = p.toMap();
        assertEquals(1, m.get("epoch"));
        assertEquals(100, m.get("step"));
        assertEquals(4.5, (Double) m.get("loss"), 0.01);
        assertTrue(m.containsKey("perplexity"));
    }

    @Test
    void testTaskInfoToMap() {
        TrainingTracker t = new TrainingTracker();
        String id = t.createTask("task", "model", 5, "config");
        t.record(id, new TrainingTracker.MetricPoint(1, 10, 4.0, 4.0, 0.5, 0.01, 100));
        var m = t.get(id).toMap();
        assertEquals("task", m.get("name"));
        assertEquals(5, m.get("totalEpochs"));
        assertEquals(1, m.get("pointCount"));
    }

    @Test
    void testListAllSorted() throws InterruptedException {
        TrainingTracker t = new TrainingTracker();
        String a = t.createTask("a", "m", 1, "{}");
        Thread.sleep(10);
        String b = t.createTask("b", "m", 1, "{}");
        Thread.sleep(10);
        String c = t.createTask("c", "m", 1, "{}");
        var list = t.listAll();
        assertEquals(3, list.size());
        assertEquals(c, list.get(0).taskId);  // 最新
        assertEquals(a, list.get(2).taskId);  // 最旧
    }
}
