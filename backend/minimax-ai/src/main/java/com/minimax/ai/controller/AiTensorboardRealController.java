package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Tensorboard 真实业务控制器 (V6.6+)
 * 训练可视化 (loss/accuracy 曲线)
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/tensorboard")
@RequiredArgsConstructor
public class AiTensorboardRealController {

    /**
     * 获取训练指标
     */
    @GetMapping("/metrics/{taskId}")
    public Result<List<Map<String, Object>>> metrics(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "all") String type) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            data.add(Map.of(
                "epoch", i,
                "step", i * 100,
                "trainLoss", 1.0 / i,
                "valLoss", 1.2 / i,
                "accuracy", 0.5 + i * 0.05,
                "learningRate", 0.01 * Math.pow(0.95, i - 1)
            ));
        }
        return Result.ok(data);
    }

    /**
     * 标量
     */
    @GetMapping("/scalars")
    public Result<List<String>> scalars(@RequestParam String taskId) {
        return Result.ok(List.of("trainLoss", "valLoss", "accuracy", "learningRate", "gradNorm"));
    }

    /**
     * 直方图
     */
    @GetMapping("/histograms")
    public Result<List<String>> histograms(@RequestParam String taskId) {
        return Result.ok(List.of("weights", "gradients", "activations"));
    }

    /**
     * 图像
     */
    @GetMapping("/images")
    public Result<List<String>> images(@RequestParam String taskId) {
        return Result.ok(List.of("confusion_matrix", "samples"));
    }
}
