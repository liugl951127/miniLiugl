package com.minimax.ai.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.TrainingCheckpoint;
import com.minimax.ai.entity.TrainingJob;
import com.minimax.ai.entity.TrainingMetric;
import com.minimax.ai.mapper.TrainingCheckpointMapper;
import com.minimax.ai.mapper.TrainingJobMapper;
import com.minimax.ai.mapper.TrainingMetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 训练可视化服务 (V3.2.0)
 *
 * <p>职责:
 *   - TrainingJob 持久化 (CRUD)
 *   - 训练曲线 (按 taskId 查 metric 历史)
 *   - EMA 平滑 (前端可直接调)
 *   - Checkpoint 管理 (保存/查询/恢复)
 *
 * <h3>Checkpoint 文件系统</h3>
 * <p>默认存到 {@code ${MINIMAX_MODEL_DIR:-/var/minimax/models}/checkpoints/{taskId}/{checkpointId}/}
 * <p>实际生产用 Arrow / HDF5 序列化, 这里简化用 mock bytes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingVizService {

    private final TrainingJobMapper jobMapper;
    private final TrainingMetricMapper metricMapper;
    private final TrainingCheckpointMapper checkpointMapper;
    private final TrainingStream stream;
    private final ObjectMapper json = new ObjectMapper();

    /** Checkpoint 根目录 */
    @Value("${minimax.model.dir:/var/minimax/models}")
    private String modelDir;

    // ============= TrainingJob CRUD =============

    /**
     * 创建任务
     */
    public TrainingJob createJob(String name, String model, int totalEpochs, String config, Long ownerId, String tags) {
        // 1. 构造实体
        TrainingJob job = new TrainingJob();
        job.setTaskId("train-" + System.currentTimeMillis() + "-" + Math.abs(name.hashCode() % 10000));
        job.setName(name);
        job.setModel(model);
        job.setStatus("PENDING");
        job.setTotalEpochs(totalEpochs);
        job.setCurrentEpoch(0);
        job.setCurrentStep(0);
        job.setStartTimeMs(System.currentTimeMillis());
        job.setEndTimeMs(0L);
        job.setConfig(config);
        job.setOwnerId(ownerId);
        job.setTags(tags);
        job.setTotalSteps(0);
        // 2. 入库
        jobMapper.insert(job);
        log.info("[train-viz] 创建任务: {} ({})", job.getTaskId(), name);
        // 3. 广播状态
        stream.broadcastStatus(job.getTaskId(), "PENDING");
        return job;
    }

    /**
     * 开始任务
     */
    public void startJob(String taskId) {
        jobMapper.updateStatus(taskId, "RUNNING", null, null);
        stream.broadcastStatus(taskId, "RUNNING");
    }

    /**
     * 记录指标 (训练中每 N step 调一次)
     */
    public void recordMetric(String taskId, int epoch, int step, double loss, double valLoss, double accuracy, double lr, long elapsedMs) {
        // 1. 写历史表
        TrainingMetric m = new TrainingMetric();
        m.setTaskId(taskId);
        m.setEpoch(epoch);
        m.setStep(step);
        m.setLoss(loss);
        m.setValLoss(valLoss);
        m.setAccuracy(accuracy);
        m.setLearningRate(lr);
        m.setElapsedMs(elapsedMs);
        metricMapper.insert(m);
        // 2. 更新 job 最新值
        jobMapper.updateLatestMetric(taskId, epoch, step, loss, valLoss, accuracy);
        // 3. SSE 广播
        stream.broadcastMetric(taskId, epoch, step, loss, valLoss, accuracy);
    }

    /**
     * 完成任务
     */
    public void completeJob(String taskId) {
        jobMapper.updateStatus(taskId, "COMPLETED", System.currentTimeMillis(), null);
        stream.broadcastStatus(taskId, "COMPLETED");
    }

    /**
     * 失败任务
     */
    public void failJob(String taskId, String error) {
        jobMapper.updateStatus(taskId, "FAILED", System.currentTimeMillis(), error);
        stream.broadcastStatus(taskId, "FAILED");
    }

    /**
     * 取消任务
     */
    public void cancelJob(String taskId) {
        jobMapper.updateStatus(taskId, "CANCELLED", System.currentTimeMillis(), null);
        stream.broadcastStatus(taskId, "CANCELLED");
    }

    // ============= Query =============

    public TrainingJob findJob(String taskId) { return jobMapper.findByTaskId(taskId); }
    public List<TrainingJob> listAll() { return jobMapper.selectList(null); }
    public List<TrainingJob> listByStatus(String status) { return jobMapper.findByStatus(status); }
    public List<TrainingMetric> getHistory(String taskId) { return metricMapper.findByTaskId(taskId); }
    public List<TrainingMetric> getRecent(String taskId, int limit) { return metricMapper.findRecent(taskId, limit); }

    /**
     * EMA 平滑 (公开方法, 给前端直接调)
     *
     * <p>应用: 训练 loss 曲线平滑, 去除 batch 噪点
     */
    public List<Double> ema(List<Double> values, double alpha) {
        if (values == null || values.isEmpty()) return List.of();
        List<Double> out = new ArrayList<>(values.size());
        double prev = values.get(0);
        for (double v : values) {
            prev = alpha * v + (1 - alpha) * prev;
            out.add(prev);
        }
        return out;
    }

    // ============= Checkpoint =============

    /**
     * 保存 checkpoint
     *
     * @param bytes 模型字节 (mock: 实际为 ONNX / Arrow / HDF5)
     * @return checkpointId
     */
    public TrainingCheckpoint saveCheckpoint(String taskId, String name, int epoch, int step,
                                              byte[] bytes, double valLoss, double accuracy, String tags) {
        // 1. 路径
        String checkpointId = "ckpt-" + UUID.randomUUID().toString().substring(0, 8);
        File dir = new File(modelDir, "checkpoints/" + taskId + "/" + checkpointId);
        dir.mkdirs();
        // 2. 写文件
        File f = new File(dir, "model.bin");
        try {
            Files.write(f.toPath(), bytes);
        } catch (IOException e) {
            throw new RuntimeException("写 checkpoint 失败: " + e.getMessage(), e);
        }
        // 3. SHA256
        String sha256 = sha256(bytes);
        // 4. 入库
        TrainingCheckpoint ckpt = new TrainingCheckpoint();
        ckpt.setTaskId(taskId);
        ckpt.setCheckpointId(checkpointId);
        ckpt.setName(name);
        ckpt.setEpoch(epoch);
        ckpt.setStep(step);
        ckpt.setEpoch(epoch);
        ckpt.setFilePath(f.getAbsolutePath());
        ckpt.setSizeBytes((long) bytes.length);
        ckpt.setSha256(sha256);
        ckpt.setValLoss(valLoss);
        ckpt.setAccuracy(accuracy);
        ckpt.setTags(tags);
        checkpointMapper.insert(ckpt);
        log.info("[train-viz] 保存 checkpoint: {} ({} bytes, sha256={})", checkpointId, bytes.length, sha256.substring(0, 8));
        // 5. 广播
        stream.broadcastCheckpoint(taskId, checkpointId, name, epoch);
        return ckpt;
    }

    /**
     * 读 checkpoint 字节 (用于恢复训练或下载)
     */
    public byte[] loadCheckpoint(String checkpointId) {
        // 1. 查元信息
        TrainingCheckpoint ckpt = checkpointMapper.findByCheckpointId(checkpointId);
        if (ckpt == null) return new byte[0];
        // 2. 读文件
        try {
            return Files.readAllBytes(Paths.get(ckpt.getFilePath()));
        } catch (IOException e) {
            log.error("[train-viz] 读 checkpoint 失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    public List<TrainingCheckpoint> listCheckpoints(String taskId) { return checkpointMapper.findByTaskId(taskId); }
    public TrainingCheckpoint getBestCheckpoint(String taskId) { return checkpointMapper.findBestByTaskId(taskId); }

    /**
     * SSE 订阅
     */
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribeStream(String taskId) {
        return stream.subscribe(taskId);
    }

    /**
     * SHA256
     */
    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "(error)";
        }
    }
}
