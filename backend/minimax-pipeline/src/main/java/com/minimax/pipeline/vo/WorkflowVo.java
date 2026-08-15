package com.minimax.pipeline.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流列表 VO (V5.32)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVo {
    private Long id;
    private String name;
    private String description;
    private Integer version;
    private Integer status;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long runCount;          // 累计执行次数 (V5.32 计算)
    private LocalDateTime lastRunTime;
}
