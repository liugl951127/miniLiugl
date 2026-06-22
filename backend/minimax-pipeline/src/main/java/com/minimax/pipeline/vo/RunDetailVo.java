package com.minimax.pipeline.vo;

import com.minimax.pipeline.entity.PipelineNodeLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 执行详情 VO (V5.32) - 含每节点状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunDetailVo {
    private Long id;
    private Long workflowId;
    private String workflowName;
    private String status;
    private Long triggerBy;
    private String triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private List<PipelineNodeLog> nodeLogs;        // 每节点日志
    private Map<String, Object> resultSummary;    // 输出节点行数/路径
}
