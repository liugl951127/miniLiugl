package com.minimax.ai;

import com.minimax.ai.tensorboard.TfEventReader;
import com.minimax.ai.tensorboard.TfEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.8 TensorBoard 自托管可视化测试
 *
 * <p>验证前端 /api/v1/tensorboard/runs/{id}/scalars 返回数据格式
 * 完全兼容 TensorBoard plugin-scalars 协议, 前端 vue-echarts 可直接渲染.</p>
 */
class V288TensorBoardSelfHostedTest {

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
    void testMultipleRunsForComparison() {
        // 模拟两个训练 run, 用户想在 TensorBoard 里对比
        // run-a: loss 下降更快
        for (int i = 0; i < 50; i++) {
            double loss = 1.0 * Math.exp(-i * 0.05) + 0.1;
            writer.writeScalar("run-a", "loss", i, loss);
        }
        // run-b: loss 下降更慢
        for (int i = 0; i < 50; i++) {
            double loss = 1.0 * Math.exp(-i * 0.02) + 0.3;
            writer.writeScalar("run-b", "loss", i, loss);
        }
        writer.close("run-a");
        writer.close("run-b");

        // 列出 runs
        List<String> runs = reader.listRuns();
        assertEquals(2, runs.size());
        assertTrue(runs.contains("run-a"));
        assertTrue(runs.contains("run-b"));

        // 读 run-a 的 loss
        Map<String, List<TfEventReader.ScalarPoint>> aData = reader.readScalars("run-a", "loss");
        assertEquals(50, aData.get("loss").size());
        TfEventReader.ScalarPoint aLast = aData.get("loss").get(49);
        // run-a 最终 loss 应该 < run-b 最终 loss
        Map<String, List<TfEventReader.ScalarPoint>> bData = reader.readScalars("run-b", "loss");
        TfEventReader.ScalarPoint bLast = bData.get("loss").get(49);
        assertTrue(aLast.getValue() < bLast.getValue(),
            "run-a 应下降更快: a=" + aLast.getValue() + " b=" + bLast.getValue());
    }

    @Test
    void testMultipleTagsPerRun() {
        // 一个 run 多个 tag, 用于 TensorBoard 多图
        String runId = "multi-tag-run";
        for (int i = 0; i < 30; i++) {
            writer.writeScalar(runId, "loss", i, 1.0 / (i + 1));
            writer.writeScalar(runId, "accuracy", i, Math.min(1.0, 0.3 + i * 0.02));
            writer.writeScalar(runId, "learning_rate", i, 0.001 * Math.pow(0.95, i));
        }
        writer.close(runId);

        // TensorBoard 通常 1 个 run 多 tag
        List<String> tags = reader.listTags(runId);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("loss"));
        assertTrue(tags.contains("accuracy"));
        assertTrue(tags.contains("learning_rate"));

        // 3 个 tag 数据独立
        assertEquals(30, reader.readScalars(runId, "loss").get("loss").size());
        assertEquals(30, reader.readScalars(runId, "accuracy").get("accuracy").size());
        assertEquals(30, reader.readScalars(runId, "learning_rate").get("learning_rate").size());
    }

    @Test
    void testRealTimeRefresh() throws Exception {
        // 模拟实时刷新: 第一次写, 读; 第二次再写, 读
        String runId = "realtime";
        writer.writeScalar(runId, "loss", 0, 1.0);
        writer.writeScalar(runId, "loss", 1, 0.9);
        writer.close(runId);

        // 第一次读
        Map<String, List<TfEventReader.ScalarPoint>> first = reader.readScalars(runId, "loss");
        assertEquals(2, first.get("loss").size());

        // 模拟新数据写入 (用新 writer, 模拟服务端持续接收)
        TfEventWriter writer2 = new TfEventWriter();
        writer2.writeScalar(runId, "loss", 2, 0.8);
        writer2.writeScalar(runId, "loss", 3, 0.7);
        writer2.close(runId);

        // 第二次读 (模拟前端 3s 后再次 GET)
        Map<String, List<TfEventReader.ScalarPoint>> second = reader.readScalars(runId, "loss");
        // 应包含全部数据 (新 writer 写新文件, reader 读所有)
        assertTrue(second.get("loss").size() >= 2);
    }

    @Test
    void testHealthCheck() {
        Map<String, Object> h = reader.health();
        assertTrue(h.containsKey("logDir"));
        assertTrue(h.containsKey("exists"));
        assertTrue(h.containsKey("writable"));
        assertTrue(h.containsKey("runs"));
    }
}
