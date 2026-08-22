package com.minimax.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.ai.entity.TrainedModel;
import com.minimax.ai.service.TrainedModelService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 自研训练模型 Controller (T1-backend-apis / P0)
 *
 * 5 个端点 (修复 views/model/Index.vue 的 5 个 mock 按钮):
 * <ul>
 *   <li>POST   /api/v1/training/models              创建 (saveTrainedModel)</li>
 *   <li>PUT    /api/v1/training/models/{id}/status  启停 (confirmToggleTrained)</li>
 *   <li>POST   /api/v1/training/models/{id}/publish 发布 (publishTrained)</li>
 *   <li>GET    /api/v1/training/models              列表</li>
 *   <li>DELETE /api/v1/training/models/{id}         删除</li>
 *   <li>POST   /api/v1/training/models/{id}/test    测试 (testTrained) — T1-mock-fix 新增</li>
 * </ul>
 *
 * @since V7.2
 */
@Tag(name = "训练模型管理 (V7.2 P0)")
@RestController
@RequestMapping("/api/v1/training/models")
@RequiredArgsConstructor
public class TrainedModelController {

    private final TrainedModelService trainedModelService;

    @Operation(summary = "创建训练模型")
    @PostMapping
    public Result<Long> create(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                @RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String name = (String) body.get("name");
        BigDecimal accuracy = toBigDecimal(body.get("accuracy"));
        String status = (String) body.get("status");
        return Result.ok(trainedModelService.create(code, name, accuracy, status, userId));
    }

    @Operation(summary = "启停训练模型")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        trainedModelService.changeStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "发布训练模型 (设置 publishedAt)")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        trainedModelService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "训练模型列表 (分页)")
    @GetMapping
    public Result<Page<TrainedModel>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) String status) {
        return Result.ok(trainedModelService.list(page, size, status));
    }

    @Operation(summary = "删除训练模型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        trainedModelService.delete(id, userId);
        return Result.ok();
    }

    /**
     * T1-mock-fix: 模型测试 - POST /api/v1/training/models/{id}/test
     * 返回 { accuracy, latencyMs, sampleOutput }
     */
    @Operation(summary = "测试训练模型 (返回 accuracy / latencyMs / sampleOutput)")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        return Result.ok(trainedModelService.test(id));
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return null; }
    }
}
