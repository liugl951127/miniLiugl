package com.minimax.agent.feign;

import com.minimax.common.feign.pipeline.SkillApprovalDTO;
import com.minimax.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端：agent → pipeline 工具审批服务
 *
 * HIGH/CRITICAL 工具调用需审批后才能执行。
 * 路由：POST/GET /api/v1/skill-approval/** → lb://minimax-pipeline
 */
@FeignClient(
        name = "minimax-pipeline",
        contextId = "skillApprovalClient",
        path = "/api/v1/skill-approval"
)
public interface SkillApprovalClient {

    /**
     * 提交审批（Agent 自动调用，或用户手动发起）
     * POST /api/v1/skill-approval/submit
     */
    @PostMapping("/submit")
    Result<SkillApprovalDTO> submit(@RequestBody Map<String, Object> body);

    /**
     * 审批通过
     * POST /api/v1/skill-approval/{id}/approve
     */
    @PostMapping("/{id}/approve")
    Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body);

    /**
     * 审批拒绝
     * POST /api/v1/skill-approval/{id}/reject
     */
    @PostMapping("/{id}/reject")
    Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body);

    /**
     * 按 taskId 查最新审批记录
     * GET /api/v1/skill-approval/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    Result<SkillApprovalDTO> getByTask(@PathVariable String taskId);

    /**
     * 我的待审批列表
     * GET /api/v1/skill-approval/pending?userId=xxx
     */
    @GetMapping("/pending")
    Result<List<SkillApprovalDTO>> getMyPending(@RequestParam(required = false) Long userId);

    /**
     * 所有待审批（管理员视角）
     * GET /api/v1/skill-approval/pending/all
     */
    @GetMapping("/pending/all")
    Result<List<SkillApprovalDTO>> getAllPending();

    /**
     * 审批历史
     * GET /api/v1/skill-approval/history?userId=xxx&page=1&size=20
     */
    @GetMapping("/history")
    Result<List<SkillApprovalDTO>> getHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
