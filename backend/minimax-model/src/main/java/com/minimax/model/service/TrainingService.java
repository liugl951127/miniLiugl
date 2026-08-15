package com.minimax.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimax.model.dto.TrainingTaskDTO;
import com.minimax.model.entity.TrainingMetric;
import com.minimax.model.entity.TrainingTask;
import com.minimax.model.mapper.TrainingMetricMapper;
import com.minimax.model.mapper.TrainingTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** 训练任务服务 (V6.8+ — 真实指标持久化) */
@Slf4j
@Service
public class TrainingService extends ServiceImpl<TrainingTaskMapper, TrainingTask> {

    @Autowired(required = false)
    private TrainingMetricMapper metricMapper;

  /** 可训练的基座模型列表 */
  private static final List<Map<String, String>> TRAINABLE_MODELS = List.of(
    Map.of("code", "MiniGPT (小型 Transformer)", "name", "MiniGPT-S", "params", "7M"),
    Map.of("code", "GPT-2 Small", "name", "GPT-2-Small", "params", "124M"),
    Map.of("code", "BERT Base", "name", "BERT-Base", "params", "110M"),
    Map.of("code", "T5 Small", "name", "T5-Small", "params", "60M"),
    Map.of("code", "Llama2-7B (需要GPU)", "name", "Llama2-7B", "params", "6.7B")
  );

  /** 正在模拟运行的任务 */
  private final Map<Long, AtomicInteger> runningSims = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  public List<Map<String, String>> listModels() { return TRAINABLE_MODELS; }

  public TrainingTask createTask(TrainingTaskDTO dto) {
    TrainingTask task = new TrainingTask();
    task.setUserId(dto.getUserId() != null ? dto.getUserId() : 1L);
    task.setModelName(dto.getModelName());
    task.setCorpusPath(dto.getCorpusPath());
    task.setNLayer(dto.getNLayer() != null ? dto.getNLayer() : 12);
    task.setNHead(dto.getNHead() != null ? dto.getNHead() : 12);
    task.setNEmbd(dto.getNEmbd() != null ? dto.getNEmbd() : 768);
    task.setBlockSize(dto.getBlockSize() != null ? dto.getBlockSize() : 128);
    task.setMaxIters(dto.getMaxIters() != null ? dto.getMaxIters() : 100);
    task.setBatchSize(dto.getBatchSize() != null ? dto.getBatchSize() : 32);
    task.setLearningRate(dto.getLearningRate() != null ? dto.getLearningRate() : 0.0003);
    task.setStatus(TrainingTask.STATUS_PENDING);
    task.setProgress(0);
    task.setCurrentIter(0);
    task.setCurrentLoss(4.6);
    baseMapper.insert(task);
    log.info("训练任务创建: id={} model={}", task.getId(), task.getModelName());
    return task;
  }

  public List<TrainingTask> listByUser(Long userId) {
    return baseMapper.selectList(
      new LambdaQueryWrapper<TrainingTask>()
        .eq(TrainingTask::getUserId, userId)
        .orderByDesc(TrainingTask::getCreatedAt)
    );
  }

  public TrainingTask getById(Long id) { return baseMapper.selectById(id); }

  public boolean cancel(Long id) {
    TrainingTask t = baseMapper.selectById(id);
    if (t == null) return false;
    if (TrainingTask.STATUS_TRAINING.equals(t.getStatus())) {
      runningSims.remove(id);
      t.setStatus(TrainingTask.STATUS_FAILED);
      t.setErrorMessage("用户手动取消");
      t.setCompletedAt(LocalDateTime.now());
      baseMapper.updateById(t);
      return true;
    }
    return false;
  }

  /** 启动模拟训练 (每个 iter 耗时约 300ms) */
  @Async
  public void startSimulation(Long taskId) {
    TrainingTask t = baseMapper.selectById(taskId);
    if (t == null || !TrainingTask.STATUS_PENDING.equals(t.getStatus())) return;

    t.setStatus(TrainingTask.STATUS_TRAINING);
    baseMapper.updateById(t);

    AtomicInteger iter = new AtomicInteger(0);
    AtomicInteger simProgress = new AtomicInteger(0);
    runningSims.put(taskId, iter);

    int totalIters = t.getMaxIters();
    double baseLoss = 4.6;

    scheduler.scheduleAtFixedRate(() -> {
      if (!runningSims.containsKey(taskId)) return;
      int current = iter.incrementAndGet();
      int progress = Math.min(100, (current * 100) / totalIters);
      double loss = Math.max(0.3, baseLoss * Math.exp(-0.03 * current) + Math.random() * 0.2);
      double accuracy = Math.min(0.99, 0.2 + 0.7 * (1 - Math.exp(-0.05 * current)));
      int gpuUtil = Math.min(98, 65 + (current * 10 / totalIters));
      double vramGb = 12.0 + (current * 6.0 / totalIters);

      t.setCurrentIter(current);
      t.setProgress(progress);
      t.setCurrentLoss(Math.round(loss * 1000.0) / 1000.0);
      t.setUpdatedAt(LocalDateTime.now());
      baseMapper.updateById(t);

      // V6.8+: 持久化每步指标到 training_metric 表
      if (metricMapper != null) {
        try {
          TrainingMetric m = new TrainingMetric();
          m.setTaskId(taskId);
          m.setIter(current);
          m.setLoss(Math.round(loss * 1000.0) / 1000.0);
          m.setAccuracy(Math.round(accuracy * 1000.0) / 1000.0);
          m.setProgress(progress);
          m.setCreatedAt(LocalDateTime.now());
          metricMapper.insert(m);
        } catch (Exception e) {
          log.warn("指标记录失败: {}", e.getMessage());
        }
      }

      log.debug("训练进度: taskId={} iter={}/{} loss={} progress={}%",
        taskId, current, totalIters, t.getCurrentLoss(), progress);

      if (current >= totalIters) {
        runningSims.remove(taskId);
        t.setStatus(TrainingTask.STATUS_COMPLETED);
        t.setProgress(100);
        t.setCompletedAt(LocalDateTime.now());
        t.setCurrentLoss(Math.round(loss * 1000.0) / 1000.0);
        baseMapper.updateById(t);
        log.info("训练完成: taskId={} final_loss={}", taskId, t.getCurrentLoss());
      }
    }, 300, 300, TimeUnit.MILLISECONDS);
  }

  /**
   * 获取训练历史指标 (用于 loss 曲线图)
   */
  public Map<String, Object> getHistory(Long taskId) {
    TrainingTask t = baseMapper.selectById(taskId);
    if (t == null) return Map.of();

    List<TrainingMetric> metrics = Collections.emptyList();
    if (metricMapper != null) {
      try {
        metrics = metricMapper.selectList(
                new LambdaQueryWrapper<TrainingMetric>()
                        .eq(TrainingMetric::getTaskId, taskId)
                        .orderByAsc(TrainingMetric::getIter)
        );
      } catch (Exception e) {
        log.warn("读取训练历史失败: {}", e.getMessage());
      }
    }

    // 如果没有历史指标，从当前任务状态重建
    if (metrics.isEmpty() && t.getCurrentIter() != null && t.getCurrentIter() > 0) {
      // 模拟生成历史数据（从当前 iter 往前重建）
      List<Map<String, Object>> pseudoHistory = new ArrayList<>();
      int total = t.getCurrentIter();
      for (int i = 1; i <= total; i++) {
        double loss = Math.max(0.3, 4.6 * Math.exp(-0.03 * i) + 0.1);
        pseudoHistory.add(Map.of("iter", i, "loss", Math.round(loss * 1000.0) / 1000.0));
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("task", toTaskMap(t));
      result.put("points", pseudoHistory);
      result.put("finalLoss", t.getCurrentLoss());
      result.put("totalIters", total);
      result.put("source", "reconstructed");
      return result;
    }

    List<Map<String, Object>> points = metrics.stream().<Map<String, Object>>map(m ->
            Map.of("iter", m.getIter(), "loss", m.getLoss(),
                    "accuracy", m.getAccuracy(), "progress", m.getProgress())
    ).toList();

    double minLoss = points.stream().mapToDouble(p -> ((Number) p.get("loss")).doubleValue()).min().orElse(0);
    double maxLoss = points.stream().mapToDouble(p -> ((Number) p.get("loss")).doubleValue()).max().orElse(0);
    double finalLoss = points.isEmpty() ? t.getCurrentLoss() : ((Number) points.get(points.size() - 1).get("loss")).doubleValue();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("task", toTaskMap(t));
    result.put("points", points);
    result.put("minLoss", minLoss);
    result.put("maxLoss", maxLoss);
    result.put("finalLoss", finalLoss);
    result.put("totalIters", t.getCurrentIter() != null ? t.getCurrentIter() : 0);
    result.put("source", "recorded");
    return result;
  }

  private Map<String, Object> toTaskMap(TrainingTask t) {
    return Map.of(
            "id", t.getId(),
            "name", t.getModelName(),
            "status", t.getStatus(),
            "progress", t.getProgress(),
            "currentLoss", t.getCurrentLoss(),
            "currentIter", t.getCurrentIter() != null ? t.getCurrentIter() : 0,
            "maxIters", t.getMaxIters() != null ? t.getMaxIters() : 100
    );
  }

  /**
   * V6.8.1: 获取训练日志 (从 training_metric 重建)
   * 每条日志 = 每轮 iter 的 loss / accuracy / GPU 利用率
   */
  public List<Map<String, Object>> getLogs(Long taskId) {
    TrainingTask t = baseMapper.selectById(taskId);
    if (t == null) return List.of();

    List<Map<String, Object>> logs = new ArrayList<>();

    // 记录任务启动日志
    logs.add(Map.of(
            "time", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "",
            "level", "INFO",
            "msg", "训练任务已创建: " + t.getModelName() + ", maxIters=" + t.getMaxIters()
    ));

    // 从 metric 表读每轮记录
    if (metricMapper != null) {
      try {
        List<TrainingMetric> metrics = metricMapper.selectList(
                new LambdaQueryWrapper<TrainingMetric>()
                        .eq(TrainingMetric::getTaskId, taskId)
                        .orderByAsc(TrainingMetric::getIter)
        );
        for (TrainingMetric m : metrics) {
          logs.add(Map.of(
                  "time", m.getCreatedAt() != null ? m.getCreatedAt().toString() : "",
                  "level", "INFO",
                  "msg", String.format(
                          "[Iter %d/%d] loss=%.4f acc=%.2f%% progress=%d%%",
                          m.getIter(), t.getMaxIters(),
                          m.getLoss(), m.getAccuracy() * 100, m.getProgress()
                  ),
                  "iter", m.getIter(),
                  "loss", m.getLoss(),
                  "accuracy", m.getAccuracy(),
                  "progress", m.getProgress()
          ));
        }
      } catch (Exception e) {
        log.warn("读取训练日志失败: {}", e.getMessage());
      }
    }

    // 任务完成/失败日志
    if (TrainingTask.STATUS_COMPLETED.equals(t.getStatus())) {
      logs.add(Map.of(
              "time", t.getCompletedAt() != null ? t.getCompletedAt().toString() : "",
              "level", "INFO",
              "msg", "训练完成! final_loss=" + t.getCurrentLoss()
      ));
    } else if (TrainingTask.STATUS_FAILED.equals(t.getStatus())) {
      logs.add(Map.of(
              "time", t.getCompletedAt() != null ? t.getCompletedAt().toString() : "",
              "level", "ERROR",
              "msg", "训练失败: " + (t.getErrorMessage() != null ? t.getErrorMessage() : "未知原因")
      ));
    }

    return logs;
  }
}
