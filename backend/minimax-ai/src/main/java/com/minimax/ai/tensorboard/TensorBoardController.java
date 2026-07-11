package com.minimax.ai.tensorboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * TensorBoard 兼容 HTTP API (V2.8.7)
 *
 * <p>实现 TensorBoard HTTP API 子集, 让前端 / WandB / 其他工具可直接读取.</p>
 *
 * <h3>端点</h3>
 * <pre>
 *   GET  /tensorboard/health                       健康检查
 *   GET  /tensorboard/runs                         列出所有 run
 *   GET  /tensorboard/runs/{run}/tags              列出 tag
 *   GET  /tensorboard/runs/{run}/scalars           读取 scalar (兼容 TF plugin)
 *   GET  /tensorboard/runs/{run}/scalars/{tag}     读取单个 tag
 *   POST /tensorboard/runs/{run}/scalars/{tag}     写入 scalar (供训练回调)
 *   GET  /tensorboard/runs/{run}/events            原始 events.json (for WandB)
 * </pre>
 *
 * <h3>TF plugin 兼容</h3>
 * <p>/runs/{run}/scalars 返回格式参考 <code>tensorboard-plugin-scalars</code>:</p>
 * <pre>
 *   [
 *     {
 *       "run": "run-001",
 *       "tag": "loss",
 *       "points": [
 *         { "step": 1, "wall_time": 1234567890.0, "value": 0.5 },
 *         ...
 *       ]
 *     }
 *   ]
 * </pre>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@RestController
@RequestMapping("/api/v1/tensorboard")
@RequiredArgsConstructor
public class TensorBoardController {

    private final TfEventReader reader;
    private final TfEventWriter writer;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "data", reader.health(),
            "version", "V2.8.7"
        ));
    }

    @GetMapping("/runs")
    public ResponseEntity<Map<String, Object>> listRuns() {
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "data", reader.listRuns()
        ));
    }

    @GetMapping("/runs/{runId}/tags")
    public ResponseEntity<Map<String, Object>> listTags(@PathVariable String runId) {
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "data", reader.listTags(runId)
        ));
    }

    /**
     * TF plugin 兼容: 返回所有 tag 的标量
     */
    @GetMapping(value = "/runs/{runId}/scalars", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> readScalars(@PathVariable String runId) {
        Map<String, List<TfEventReader.ScalarPoint>> all = reader.readScalars(runId, null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<TfEventReader.ScalarPoint>> e : all.entrySet()) {
            List<Map<String, Object>> points = new ArrayList<>();
            for (TfEventReader.ScalarPoint p : e.getValue()) {
                points.add(Map.of(
                    "step", p.getStep(),
                    "wall_time", p.getWallTime(),
                    "value", p.getValue()
                ));
            }
            result.add(Map.of(
                "run", runId,
                "tag", e.getKey(),
                "points", points
            ));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 读取单个 tag
     */
    @GetMapping("/runs/{runId}/scalars/{tag}")
    public ResponseEntity<Map<String, Object>> readScalar(
            @PathVariable String runId,
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int last) {
        List<TfEventReader.ScalarPoint> points = last > 0
            ? reader.readLastN(runId, tag, last)
            : reader.readScalars(runId, tag).getOrDefault(tag, Collections.emptyList());
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "data", Map.of(
                "run", runId,
                "tag", tag,
                "points", points,
                "count", points.size()
            )
        ));
    }

    /**
     * 写入 scalar (供 TrainingTracker 调用)
     */
    @PostMapping("/runs/{runId}/scalars/{tag}")
    public ResponseEntity<Map<String, Object>> writeScalar(
            @PathVariable String runId,
            @PathVariable String tag,
            @RequestBody Map<String, Object> body) {
        long step = ((Number) body.getOrDefault("step", 0)).longValue();
        double value = ((Number) body.getOrDefault("value", 0)).doubleValue();
        writer.writeScalar(runId, tag, step, value);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "written",
            "data", Map.of("run", runId, "tag", tag, "step", step, "value", value)
        ));
    }

    /**
     * 写入文本
     */
    @PostMapping("/runs/{runId}/text/{tag}")
    public ResponseEntity<Map<String, Object>> writeText(
            @PathVariable String runId,
            @PathVariable String tag,
            @RequestBody Map<String, Object> body) {
        long step = ((Number) body.getOrDefault("step", 0)).longValue();
        String text = (String) body.getOrDefault("text", "");
        writer.writeText(runId, tag, step, text);
        return ResponseEntity.ok(Map.of("code", 0, "message", "written"));
    }

    /**
     * events.json (WandB 兼容)
     */
    @GetMapping("/runs/{runId}/events")
    public ResponseEntity<Map<String, Object>> eventsJson(@PathVariable String runId) {
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "data", reader.readScalars(runId, null)
        ));
    }
}
