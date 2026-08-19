package com.minimax.pipeline.feign;

import com.minimax.common.feign.pipeline.FunctionToolDTO;
import com.minimax.common.feign.pipeline.SkillApprovalDTO;
import com.minimax.common.feign.pipeline.ToolResultDTO;
import com.minimax.pipeline.function_ext.entity.FunctionTool;
import com.minimax.pipeline.function_ext.entity.SkillApproval;

/**
 * Pipeline → Agent 的 DTO 映射器（V6.8.1）
 *
 * 放在 pipeline 模块内，避免 common 依赖 pipeline 实体。
 * Controller 层使用此类做 entity → shared DTO 的转换。
 */
public final class PipelineFeignMapper {

    private PipelineFeignMapper() {}

    // ==================== FunctionTool ====================

    public static FunctionToolDTO toDTO(FunctionTool e) {
        if (e == null) return null;
        FunctionToolDTO dto = new FunctionToolDTO();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setDisplayName(e.getDisplayName());
        dto.setDescription(e.getDescription());
        dto.setCategory(e.getCategory());
        dto.setScope(e.getScope());
        dto.setOwnerId(e.getOwnerId());
        dto.setParameters(e.getParameters());
        dto.setEndpoint(e.getEndpoint());
        dto.setHttpMethod(e.getHttpMethod());
        dto.setEnabled(e.getEnabled());
        dto.setTags(e.getTags());
        dto.setRiskLevel(e.getRiskLevel());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    // ==================== SkillApproval ====================

    public static SkillApprovalDTO toDTO(SkillApproval e) {
        if (e == null) return null;
        SkillApprovalDTO dto = new SkillApprovalDTO();
        dto.setId(e.getId());
        dto.setTaskId(e.getTaskId());
        dto.setUserId(e.getUserId());
        dto.setUsername(e.getUsername());
        dto.setToolName(e.getToolName());
        dto.setRiskLevel(e.getRiskLevel());
        dto.setGoal(e.getGoal());
        dto.setToolParams(e.getToolParams());
        dto.setStatus(e.getStatus());
        dto.setApproverId(e.getApproverId());
        dto.setApproverName(e.getApproverName());
        dto.setReason(e.getReason());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    // ==================== ToolResult ====================

    public static ToolResultDTO toDTO(com.minimax.pipeline.function_ext.executor.ToolExecutor.ToolResult r) {
        if (r == null) return null;
        return new ToolResultDTO(r.ok(), r.result(), r.durationMs());
    }
}
