package com.minimax.pipeline.dto;

import com.minimax.pipeline.enums.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流 DTO (V5.32)
 *
 * 接收前端画布 JSON: { nodes: [...], edges: [...] }
 */
@Data
public class WorkflowDto {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private List<WorkflowNode> nodes;

    @NotNull
    private List<WorkflowEdge> edges;

    private Map<String, Object> viewport;  // { x, y, zoom }

    @Data
    public static class WorkflowNode {
        @NotBlank
        private String id;
        @NotNull
        private NodeType type;
        private String name;
        private Double x;
        private Double y;
        private Map<String, Object> config;   // 节点配置 JSON
    }

    @Data
    public static class WorkflowEdge {
        @NotBlank
        private String id;
        @NotBlank
        private String from;                   // source node id
        @NotBlank
        private String to;                     // target node id
    }
}
