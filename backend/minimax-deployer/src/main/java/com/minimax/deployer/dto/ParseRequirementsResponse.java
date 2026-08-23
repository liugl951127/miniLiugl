package com.minimax.deployer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 需求解析响应 DTO (V2.0)
 *
 * LLM 解析后输出, 包含项目元数据 + 推荐智能体 + 工作流。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParseRequirementsResponse {

    /** 解析后的项目元数据 */
    private Map<String, Object> extracted;

    /** 推荐智能体列表 */
    private List<Map<String, Object>> agents;

    /** 工作流步骤 */
    private List<Map<String, Object>> workflow;

    /** 总 token 数 (用于计费) */
    private Integer totalTokens;

    /** 解析耗时 (毫秒) */
    private Long durationMs;

    /** 使用的模型 */
    private String model;
}
