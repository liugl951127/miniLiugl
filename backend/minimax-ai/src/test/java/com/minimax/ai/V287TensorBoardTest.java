package com.minimax.ai;

import com.minimax.ai.tensorboard.TfEventReader;
import com.minimax.ai.tensorboard.TfEventWriter;
import com.minimax.ai.training.TrainingTracker;
import com.minimax.ai.training.TrainingTracker.MetricPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.7 TensorBoard 协议测试
 *
 * <p>验证:</p>
 * <ol>
 *   <li>TfEventWriter 写入 .tfevents 文件 (TensorBoard 可读格式)</li>
 *   <li>TfEventReader 解析回 scalar points</li>
 *   <li>TrainingTracker 自动写入 events (集成)</li>
 *   <li>完整往返: 写 → 读 → 校验</li>
 * </ol>
 *
 * @author MiniMax
 */
class V287TensorBoardTest {

    @TempDir
    Path tempDir;

    private TfEventWriter writer;
    private TfEventReader reader;

    @BeforeEach
    void setup() throws Exception {
        // 设置临时 log 目录
        System.setProperty("MINIMAX_LOG_DIR", tempDir.toString());
        // 重新创建 writer/reader (读取环境变量)
        writer = new TfEventWriter();
        reader = new TfEventReader();
    }

    @Test
    void testWriteAndReadScalar() {
        String runId = "test-run-1";
        // 写入 100 个 step 的 loss
        for (int step = 0; step < 100; step++) {
            double loss = 1.0 / (step + 1);
            writer.writeScalar(runId, "loss", step, loss);
        }
        writer.close(runId);

        // 读回
        Map<String, List<TfEventReader.ScalarPoint>> result = reader.readScalars(runId, null);
        assertNotNull(result);
        assertTrue(result.containsKey("loss"), "应包含 loss tag");
        List<TfEventReader.ScalarPoint> points = result.get("loss");
        assertEquals(100, points.size(), "应有 100 个点");

        // 验证值正确
        TfEventReader.ScalarPoint p0 = points.get(0);
        assertEquals(0, p0.getStep());
        assertEquals(1.0, p0.getValue(), 0.001);

        TfEventReader.ScalarPoint p99 = points.get(99);
        assertEquals(99, p99.getStep());
        assertEquals(1.0 / 100, p99.getValue(), 0.001);

        // 验证 step 升序
        for (int i = 1; i < points.size(); i++) {
            assertTrue(points.get(i).getStep() > points.get(i - 1).getStep());
        }
    }

    @Test
    void testMultipleTags() {
        String runId = "multi-tag";
        writer.writeScalar(runId, "loss", 0, 1.0);
        writer.writeScalar(runId, "loss", 1, 0.5);
        writer.writeScalar(runId, "accuracy", 0, 0.5);
        writer.writeScalar(runId, "accuracy", 1, 0.8);
        writer.writeScalar(runId, "learning_rate", 0, 0.001);
        writer.close(runId);

        List<String> tags = reader.listTags(runId);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("loss"));
        assertTrue(tags.contains("accuracy"));
        assertTrue(tags.contains("learning_rate"));

        // 过滤单 tag
        Map<String, List<TfEventReader.ScalarPoint>> lossOnly = reader.readScalars(runId, "loss");
        assertEquals(1, lossOnly.size());
        assertEquals(2, lossOnly.get("loss").size());
    }

    @Test
    void testLastN() {
        String runId = "lastn";
        for (int i = 0; i < 50; i++) {
            writer.writeScalar(runId, "metric", i, i * 0.1);
        }
        writer.close(runId);

        List<TfEventReader.ScalarPoint> last10 = reader.readLastN(runId, "metric", 10);
        assertEquals(10, last10.size());
        assertEquals(40, last10.get(0).getStep());
        assertEquals(49, last10.get(9).getStep());
    }

    @Test
    void testFileFormat() throws Exception {
        String runId = "format";
        writer.writeScalar(runId, "test", 0, 0.5);
        writer.close(runId);

        // 验证文件存在
        Path tfeventsFile = tempDir.resolve(runId)
            .resolve("events.tfevents." + System.currentTimeMillis() + ".*");
        // 列出目录所有文件
        List<Path> files = Files.list(tempDir.resolve(runId))
            .filter(p -> p.getFileName().toString().startsWith("events.tfevents"))
            .toList();
        assertEquals(1, files.size(), "应生成 1 个 events.tfevents 文件");

        // 验证文件头 magic
        byte[] header = new byte[8];
        try (var in = Files.newInputStream(files.get(0))) {
            in.readNBytes(header, 0, 8);
        }
        // magic = 0xA55A0001 = [0x01, 0x00, 0x5A, 0xA5, 0x00, 0x00, 0x00, 0x00] (小端)
        // 高 4 字节应该是 0xA55A0001
        int magic = ((header[4] & 0xFF) << 24) | ((header[5] & 0xFF) << 16)
                  | ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
        assertEquals(0xA55A0001, magic, "TensorBoard magic 应为 0xA55A0001");
    }

    @Test
    void testTrainingTrackerIntegration() {
        // 创建带 TfEventWriter 的 tracker
        TrainingTracker tracker = new TrainingTracker();
        org.springframework.test.util.ReflectionTestUtils.setField(
            tracker, "tfEventWriter", writer);

        // 创建任务 + 记录
        String taskId = tracker.createTask("test-task", "minimax-7b", 10, "{\"lr\":0.001}");
        tracker.start(taskId);

        for (int step = 0; step < 20; step++) {
            tracker.record(taskId, new MetricPoint(
                0, step,
                1.0 / (step + 1),    // loss
                1.2 / (step + 1),    // val_loss
                0.5 + step * 0.02,   // accuracy
                0.001,                // lr
                100L * step           // elapsed
            ));
        }
        tracker.complete(taskId);

        // 验证 events.tfevents 已生成
        Map<String, List<TfEventReader.ScalarPoint>> result = reader.readScalars(taskId, null);
        assertNotNull(result);
        assertTrue(result.containsKey("loss"));
        assertTrue(result.containsKey("val_loss"));
        assertTrue(result.containsKey("accuracy"));
        assertTrue(result.containsKey("learning_rate"));

        // 验证点数
        assertEquals(20, result.get("loss").size());
        assertEquals(20, result.get("accuracy").size());
    }

    @Test
    void testHealth() {
        Map<String, Object> h = reader.health();
        assertNotNull(h);
        assertTrue(h.containsKey("logDir"));
        assertTrue(h.containsKey("exists"));
        assertTrue(h.containsKey("writable"));
        assertTrue((Boolean) h.get("exists"));
    }

    @Test
    void testListRuns() {
        writer.writeScalar("run-a", "loss", 0, 0.5);
        writer.writeScalar("run-b", "loss", 0, 0.3);
        writer.writeScalar("run-c", "loss", 0, 0.7);
        writer.close("run-a");
        writer.close("run-b");
        writer.close("run-c");

        List<String> runs = reader.listRuns();
        assertTrue(runs.size() >= 3, "应至少 3 个 run");
        assertTrue(runs.contains("run-a"));
    }
}
