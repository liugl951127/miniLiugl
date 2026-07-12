package com.minimax.ai.training;

import com.minimax.ai.entity.TrainingCheckpoint;
import com.minimax.ai.entity.TrainingJob;
import com.minimax.ai.entity.TrainingMetric;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 训练可视化 REST API + SSE 实时 (V3.2.0)
 *
 * <p>API 列表 (统一 /api/v1/ai/training 前缀):
 * <ul>
 *   <li>POST   /create                创建训练任务</li>
 *   <li>GET    /list                  列出所有任务</li>
 *   <li>GET    /{taskId}              查任务详情</li>
 *   <li>POST   /{taskId}/start        开始</li>
 *   <li>POST   /{taskId}/metric       上报指标 (高频)</li>
 *   <li>POST   /{taskId}/complete     完成</li>
 *   <li>POST   /{taskId}/fail         失败</li>
 *   <li>POST   /{taskId}/cancel       取消</li>
 *   <li>GET    /{taskId}/history      训练曲线历史</li>
 *   <li>GET    /{taskId}/smoothed     EMA 平滑曲线</li>
 *   <li>GET    /{taskId}/stream       SSE 实时订阅</li>
 *   <li>POST   /{taskId}/checkpoint   保存 checkpoint</li>
 *   <li>GET    /{taskId}/checkpoints  列 checkpoint</li>
 *   <li>GET    /{taskId}/best         最佳 checkpoint</li>
 *   <li>GET    /checkpoint/{id}/download  下载 checkpoint</li>
 * </ul>
 */
@Tag(name = "训练可视化")
@RestController
@RequestMapping("/api/v1/ai/training")
@RequiredArgsConstructor
public class TrainingVizController {

    private final TrainingVizService service;

    /**
     * 创建任务
     */
    @Operation(summary = "创建训练任务")
    @PostMapping("/create")
    public Result<TrainingJob> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String model = (String) body.getOrDefault("model", "unknown");
        int totalEpochs = ((Number) body.getOrDefault("totalEpochs", 10)).intValue();
        String config = (String) body.getOrDefault("config", "{}");
        Long ownerId = body.get("ownerId") == null ? null : ((Number) body.get("ownerId")).longValue();
        String tags = (String) body.getOrDefault("tags", "");
        return Result.ok(service.createJob(name, model, totalEpochs, config, ownerId, tags));
    }

    /**
     * 列出所有任务
     */
    @Operation(summary = "列出训练任务")
    @GetMapping("/list")
    public Result<List<TrainingJob>> list(@RequestParam(required = false) String status) {
        return Result.ok(status == null ? service.listAll() : service.listByStatus(status));
    }

    /**
     * 查任务详情
     */
    @Operation(summary = "查训练任务详情")
    @GetMapping("/{taskId}")
    public Result<TrainingJob> get(@PathVariable String taskId) {
        TrainingJob job = service.findJob(taskId);
        if (job == null) return Result.fail(404, "任务不存在");
        return Result.ok(job);
    }

    /**
     * 开始
     */
    @PostMapping("/{taskId}/start")
    public Result<Void> start(@PathVariable String taskId) {
        service.startJob(taskId);
        return Result.ok();
    }

    /**
     * 上报指标
     */
    @PostMapping("/{taskId}/metric")
    public Result<Void> metric(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
        int epoch = ((Number) body.getOrDefault("epoch", 0)).intValue();
        int step = ((Number) body.getOrDefault("step", 0)).intValue();
        double loss = ((Number) body.getOrDefault("loss", 0)).doubleValue();
        double valLoss = ((Number) body.getOrDefault("valLoss", 0)).doubleValue();
        double accuracy = ((Number) body.getOrDefault("accuracy", 0)).doubleValue();
        double lr = ((Number) body.getOrDefault("lr", 0)).doubleValue();
        long elapsedMs = ((Number) body.getOrDefault("elapsedMs", 0)).longValue();
        service.recordMetric(taskId, epoch, step, loss, valLoss, accuracy, lr, elapsedMs);
        return Result.ok();
    }

    /**
     * 完成
     */
    @PostMapping("/{taskId}/complete")
    public Result<Void> complete(@PathVariable String taskId) {
        service.completeJob(taskId);
        return Result.ok();
    }

    /**
     * 失败
     */
    @PostMapping("/{taskId}/fail")
    public Result<Void> fail(@PathVariable String taskId, @RequestBody(required = false) Map<String, String> body) {
        String error = body == null ? null : body.get("error");
        service.failJob(taskId, error);
        return Result.ok();
    }

    /**
     * 取消
     */
    @PostMapping("/{taskId}/cancel")
    public Result<Void> cancel(@PathVariable String taskId) {
        service.cancelJob(taskId);
        return Result.ok();
    }

    /**
     * 训练曲线历史
     */
    @GetMapping("/{taskId}/history")
    public Result<List<Map<String, Object>>> history(@PathVariable String taskId,
                                                      @RequestParam(defaultValue = "false") boolean smoothed,
                                                      @RequestParam(defaultValue = "0.3") double alpha) {
        List<TrainingMetric> h = service.getHistory(taskId);
        List<Double> losses = h.stream().map(TrainingMetric::getLoss).collect(Collectors.toList());
        List<Double> valLosses = h.stream().map(TrainingMetric::getValLoss).collect(Collectors.toList());
        List<Double> accs = h.stream().map(TrainingMetric::getAccuracy).collect(Collectors.toList());
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("steps", h.stream().map(m -> m.getStep() != null ? m.getStep() : 0).collect(Collectors.toList()));
        chart.put("epochs", h.stream().map(m -> m.getEpoch() != null ? m.getEpoch() : 0).collect(Collectors.toList()));
        chart.put("loss", losses);
        chart.put("valLoss", valLosses);
        chart.put("accuracy", accs);
        if (smoothed) {
            chart.put("lossSmoothed", service.ema(losses, alpha));
            chart.put("valLossSmoothed", service.ema(valLosses, alpha));
        }
        return Result.ok(List.of(chart));
    }

    /**
     * EMA 平滑 (单独 API)
     */
    @PostMapping("/ema")
    public Result<List<Double>> ema(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) body.get("values");
        double alpha = ((Number) body.getOrDefault("alpha", 0.3)).doubleValue();
        List<Double> values = raw.stream().map(Number::doubleValue).collect(Collectors.toList());
        return Result.ok(service.ema(values, alpha));
    }

    /**
     * SSE 实时订阅
     */
    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String taskId) {
        return service.subscribeStream(taskId);
    }

    /**
     * 保存 checkpoint (multipart 上传文件)
     */
    @PostMapping("/{taskId}/checkpoint")
    public Result<TrainingCheckpoint> saveCheckpoint(@PathVariable String taskId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam String name,
                                                     @RequestParam int epoch,
                                                     @RequestParam int step,
                                                     @RequestParam(required = false, defaultValue = "0") double valLoss,
                                                     @RequestParam(required = false, defaultValue = "0") double accuracy,
                                                     @RequestParam(required = false, defaultValue = "manual") String tags) {
        try {
            TrainingCheckpoint ckpt = service.saveCheckpoint(taskId, name, epoch, step,
                    file.getBytes(), valLoss, accuracy, tags);
            return Result.ok(ckpt);
        } catch (Exception e) {
            return Result.fail(500, "保存失败: " + e.getMessage());
        }
    }

    /**
     * 列 checkpoint
     */
    @GetMapping("/{taskId}/checkpoints")
    public Result<List<TrainingCheckpoint>> listCheckpoints(@PathVariable String taskId) {
        return Result.ok(service.listCheckpoints(taskId));
    }

    /**
     * 最佳 checkpoint
     */
    @GetMapping("/{taskId}/best")
    public Result<TrainingCheckpoint> bestCheckpoint(@PathVariable String taskId) {
        TrainingCheckpoint c = service.getBestCheckpoint(taskId);
        if (c == null) return Result.fail(404, "无 checkpoint");
        return Result.ok(c);
    }

    /**
     * 下载 checkpoint
     */
    @GetMapping("/checkpoint/{checkpointId}/download")
    public org.springframework.core.io.Resource downloadCheckpoint(@PathVariable String checkpointId) {
        byte[] data = service.loadCheckpoint(checkpointId);
        return new org.springframework.core.io.ByteArrayResource(data) {
            @Override public String getFilename() { return checkpointId + ".bin"; }
        };
    }
}
