package com.minimax.agent.controller;

import com.minimax.agent.feign.SkillApprovalClient;
import com.minimax.common.feign.pipeline.SkillApprovalDTO;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Skill/工具 审批 API — V6.8.1+ 重构版
 *
 * 底层通过 Feign 代理到 minimax-pipeline 服务。
 * 业务逻辑和数据存储在 pipeline 模块，agent 只做 HTTP 转发。
 *
 * 路由: /api/v1/skill-approval/** (gateway → minimax-agent → minimax-pipeline)
 *
 * @since 2026-08-20 重构
 */
@Slf4j
@Tag(name = "Skill审批", description = "HIGH/CRITICAL 工具执行审批流")
@RestController
@RequestMapping("/api/v1/skill-approval")
@RequiredArgsConstructor
public class SkillApprovalController {

    private final SkillApprovalClient approvalClient;

    // ==================== 提交审批 ====================

    @PostMapping("/submit")
    @Operation(summary = "提交审批 (Agent 自动调用，或用户手动发起)")
    public Result<SkillApprovalDTO> submit(@RequestBody Map<String, Object> body) {
        return approvalClient.submit(body);
    }

    // ==================== 审批 ====================

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return approvalClient.approve(id, body);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return approvalClient.reject(id, body);
    }

    // ==================== 查询 ====================

    @GetMapping("/pending")
    @Operation(summary = "我的待审批列表 (申请人视角)")
    public Result<List<SkillApprovalDTO>> getMyPending(@RequestParam(required = false) Long userId) {
        return approvalClient.getMyPending(userId);
    }

    @GetMapping("/pending/all")
    @Operation(summary = "所有待审批 (管理员视角)")
    public Result<List<SkillApprovalDTO>> getAllPending() {
        return approvalClient.getAllPending();
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "查任务最新审批状态")
    public Result<SkillApprovalDTO> getByTask(@PathVariable String taskId) {
        return approvalClient.getByTask(taskId);
    }

    @GetMapping("/history")
    @Operation(summary = "我的审批历史")
    public Result<List<SkillApprovalDTO>> getHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return approvalClient.getHistory(userId, page, size);
    }
}
