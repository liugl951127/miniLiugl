package com.minimax.ai;

import com.minimax.ai.tensorboard.TfEventReader;
import com.minimax.ai.tensorboard.TfEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.9 TensorBoard 统计分析测试
 *
 * <p>验证 computeStats / computeHistogram / compareRunsStats 三个新方法.</p>
 */
class V289TensorBoardStatsTest {

    @TempDir
    Path tempDir;

    private TfEventWriter writer;
    private TfEventReader reader;

    @BeforeEach
    void setup() {
        System.setProperty("MINIMAX_LOG_DIR", tempDir.toString());
        writer = new TfEventWriter();
        reader = new TfEventReader();
    }

    @Test
    void testStats_Basic() {
        String run = "stats-run-1";
        // 写入 1.0, 2.0, ..., 10.0
        for (int i = 1; i <= 10; i++) {
            writer.writeScalar(run, "loss", i, (double) i);
        }
        writer.close(run);

        Map<String, Object> stats = reader.computeStats(run, "loss");
        assertEquals(run, stats.get("runId"));
        assertEquals("loss", stats.get("tag"));
        assertEquals(10, stats.get("count"));
        assertEquals(1.0, (double) stats.get("min"), 0.001);
        assertEquals(10.0, (double) stats.get("max"), 0.001);
        assertEquals(5.5, (double) stats.get("mean"), 0.001);
        assertEquals(5.5, (double) stats.get("median"), 0.001);
        // std
        assertTrue(((Number) stats.get("std")).doubleValue() > 0);
        // percentiles
        assertNotNull(stats.get("p25"));
        assertNotNull(stats.get("p75"));
        assertNotNull(stats.get("p95"));
    }

    @Test
    void testStats_Empty() {
        Map<String, Object> stats = reader.computeStats("nonexistent", "loss");
        assertEquals(0, stats.get("count"));
    }

    @Test
    void testStats_SingleValue() {
        String run = "single";
        writer.writeScalar(run, "constant", 0, 5.0);
        writer.writeScalar(run, "constant", 1, 5.0);
        writer.writeScalar(run, "constant", 2, 5.0);
        writer.close(run);

        Map<String, Object> stats = reader.computeStats(run, "constant");
        assertEquals(3, stats.get("count"));
        assertEquals(5.0, (double) stats.get("mean"), 0.001);
        assertEquals(0.0, (double) stats.get("std"), 0.001);
    }

    @Test
    void testHistogram_Basic() {
        String run = "hist-run";
        // 100 个点, 0-99
        for (int i = 0; i < 100; i++) {
            writer.writeScalar(run, "metric", i, (double) i);
        }
        writer.close(run);

        Map<String, Object> h = reader.computeHistogram(run, "metric", 10);
        assertEquals(10, ((int[]) h.get("counts")).length);
        assertEquals(11, ((double[]) h.get("bins")).length);
        // 总数应等于 100
        int total = 0;
        for (int c : (int[]) h.get("counts")) total += c;
        assertEquals(100, total);
    }

    @Test
    void testHistogram_AllSameValue() {
        String run = "const-hist";
        for (int i = 0; i < 50; i++) {
            writer.writeScalar(run, "v", i, 3.14);
        }
        writer.close(run);

        Map<String, Object> h = reader.computeHistogram(run, "v", 10);
        // min == max, 所有点都进 bin 0
        int[] counts = (int[]) h.get("counts");
        assertEquals(50, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    void testHistogram_BinsLimit() {
        String run = "bins-limit";
        for (int i = 0; i < 50; i++) writer.writeScalar(run, "v", i, (double) i);
        writer.close(run);

        // bins > 100 会被截到 100
        Map<String, Object> h = reader.computeHistogram(run, "v", 200);
        assertEquals(100, ((int[]) h.get("counts")).length);
    }

    @Test
    void testCompareRuns() {
        // 3 个 run, 同 tag
        for (int r = 1; r <= 3; r++) {
            String runId = "run-" + r;
            for (int i = 0; i < 20; i++) {
                double loss = 1.0 / (i + r);
                writer.writeScalar(runId, "loss", i, loss);
            }
            writer.close(runId);
        }

        List<Map<String, Object>> stats = reader.compareRunsStats(
            List.of("run-1", "run-2", "run-3"), "loss");
        assertEquals(3, stats.size());
        for (int i = 0; i < 3; i++) {
            assertEquals("run-" + (i + 1), stats.get(i).get("runId"));
            assertEquals(20, stats.get(i).get("count"));
        }
    }

    @Test
    void testPercentile_Interpolation() {
        // 测试插值: q=0.5 落在中间, 应插值
        String run = "perc-run";
        for (int i = 1; i <= 5; i++) writer.writeScalar(run, "v", i, (double) i);
        writer.close(run);

        Map<String, Object> stats = reader.computeStats(run, "v");
        // 中位数 q=0.5, 1,2,3,4,5 位置 0.5*4=2, 整数 -> 3
        assertEquals(3.0, (double) stats.get("median"), 0.001);
    }
}
