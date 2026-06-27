package com.minimax.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimax.model.dto.TrainingTaskDTO;
import com.minimax.model.entity.TrainingTask;
import com.minimax.model.mapper.TrainingTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** 训练任务服务 (Day 23 — mock 模拟训练过程) */
@Slf4j
@Service
public class TrainingService extends ServiceImpl<TrainingTaskMapper, TrainingTask> {

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
    task.setnLayer(dto.getNLayer() != null ? dto.getNLayer() : 12);
    task.setnHead(dto.getNHead() != null ? dto.getNHead() : 12);
    task.setnEmbd(dto.getNEmbd() != null ? dto.getNEmbd() : 768);
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

      t.setCurrentIter(current);
      t.setProgress(progress);
      t.setCurrentLoss(Math.round(loss * 1000.0) / 1000.0);
      t.setUpdatedAt(LocalDateTime.now());
      baseMapper.updateById(t);

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
}
