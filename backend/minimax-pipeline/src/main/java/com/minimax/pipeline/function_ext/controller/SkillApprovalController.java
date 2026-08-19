package com.minimax.pipeline.function_ext.controller;

import com.minimax.common.feign.pipeline.SkillApprovalDTO;
import com.minimax.common.result.Result;
import com.minimax.pipeline.feign.PipelineFeignMapper;
import com.minimax.pipeline.function_ext.entity.SkillApproval;
import com.minimax.pipeline.function_ext.service.SkillApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Skill/工具 审批 API — 供 agent 服务通过 HTTP 调用
 *
 * 原在 minimax-agent，现移至 minimax-pipeline（逻辑归属 pipeline 模块）。
 * agent 通过 FeignClient → /api/v1/skill-approval/** 调用。
 *
 * @since 2026-08-20
 */
@Slf4j
@Tag(name = "Skill审批", description = "HIGH/CRITICAL 工具执行审批流")
@RestController
@RequestMapping("/api/v1/skill-approval")
@RequiredArgsConstructor
public class SkillApprovalController {

    private final SkillApprovalService approvalService;

    // ==================== 提交审批 ====================

    @PostMapping("/submit")
    @Operation(summary = "提交审批 (Agent 自动调用，或用户手动发起)")
    public Result<SkillApprovalDTO> submit(@RequestBody Map<String, Object> body) {
        Long userId = parseUserId(body.get("userId"));
        String username = (String) body.getOrDefault("username", "user-" + userId);
        String taskId = (String) body.getOrDefault("taskId", "task-" + System.currentTimeMillis());
        String toolName = (String) body.get("toolName");
        String riskLevel = (String) body.getOrDefault("riskLevel", SkillApproval.RISK_HIGH);
        String goal = (String) body.get("goal");
        String toolParams = body.get("toolParams") != null ? body.get("toolParams").toString() : null;

        if (toolName == null || toolName.isBlank()) {
            return Result.fail("toolName 不能为空");
        }
        SkillApproval record = approvalService.submit(taskId, userId, username, toolName, riskLevel, goal, toolParams);
        return Result.ok(PipelineFeignMapper.toDTO(record));
    }

    // ==================== 审批 ====================

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approverId = parseUserId(body.get("approverId"));
        String approverName = (String) body.getOrDefault("approverName", "approver-" + approverId);
        String reason = (String) body.getOrDefault("reason", "");
        boolean ok = approvalService.approve(id, approverId, approverName, reason);
        return ok ? Result.ok() : Result.fail("审批不存在或已处理");
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approverId = parseUserId(body.get("approverId"));
        String approverName = (String) body.getOrDefault("approverName", "approver-" + approverId);
        String reason = (String) body.getOrDefault("reason", "拒绝执行");
        boolean ok = approvalService.reject(id, approverId, approverName, reason);
        return ok ? Result.ok() : Result.fail("审批不存在或已处理");
    }

    // ==================== 查询 ====================

    @GetMapping("/pending")
    @Operation(summary = "我的待审批列表 (申请人视角)")
    public Result<List<SkillApprovalDTO>> getMyPending(@RequestParam(required = false) Long userId) {
        return Result.ok(approvalService.getPendingByUser(userId).stream()
                .map(PipelineFeignMapper::toDTO).toList());
    }

    @GetMapping("/pending/all")
    @Operation(summary = "所有待审批 (管理员视角)")
    public Result<List<SkillApprovalDTO>> getAllPending() {
        return Result.ok(approvalService.getPendingAll().stream()
                .map(PipelineFeignMapper::toDTO).toList());
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "查任务最新审批状态")
    public Result<SkillApprovalDTO> getByTask(@PathVariable String taskId) {
        SkillApproval record = approvalService.findLatestByTask(taskId);
        return record != null ? Result.ok(PipelineFeignMapper.toDTO(record)) : Result.fail("无审批记录: " + taskId);
    }

    @GetMapping("/history")
    @Operation(summary = "我的审批历史")
    public Result<List<SkillApprovalDTO>> getHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(approvalService.getHistoryByUser(userId, page, size).stream()
                .map(PipelineFeignMapper::toDTO).toList());
    }

    // ==================== Utils ====================

    private Long parseUserId(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); }
        catch (Exception e) { return null; }
    }
}
