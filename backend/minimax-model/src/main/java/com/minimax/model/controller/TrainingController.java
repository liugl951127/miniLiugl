package com.minimax.model.controller;

import com.minimax.common.result.Result;
import com.minimax.model.dto.TrainingTaskDTO;
import com.minimax.model.entity.TrainingTask;
import com.minimax.model.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 训练任务 Controller (Day 23) */
@Tag(name = "模型训练", description = "训练任务管理 + 实时指标")
@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class TrainingController {

  private final TrainingService trainingService;

  @GetMapping("/models")
  @Operation(summary = "可训练模型列表")
  public Result listModels() {
    return Result.ok(trainingService.listModels());
  }

  @PostMapping("/tasks")
  @Operation(summary = "创建训练任务")
  public Result createTask(@RequestBody TrainingTaskDTO dto, @AuthenticationPrincipal Long userId) {
    if (dto.getUserId() == null) dto.setUserId(userId);
    TrainingTask task = trainingService.createTask(dto);
    trainingService.startSimulation(task.getId());
    return Result.ok(task);
  }

  @GetMapping("/tasks")
  @Operation(summary = "我的训练任务列表")
  public Result listTasks(@AuthenticationPrincipal Long userId) {
    return Result.ok(trainingService.listByUser(userId));
  }

  @GetMapping("/tasks/{id}")
  @Operation(summary = "查询任务详情")
  public Result getTask(@PathVariable Long id) {
    TrainingTask t = trainingService.getById(id);
    return t != null ? Result.ok(t) : Result.fail("任务不存在");
  }

  @PostMapping("/tasks/{id}/cancel")
  @Operation(summary = "取消训练任务")
  public Result cancel(@PathVariable Long id) {
    boolean ok = trainingService.cancel(id);
    return ok ? Result.ok(null) : Result.fail("无法取消(任务不存在或已完成)");
  }
}
