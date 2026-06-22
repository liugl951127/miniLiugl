package com.minimax.pipeline.vo;

import com.minimax.pipeline.dto.WorkflowDto;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流详情 VO (V5.32) - 含 definition
 */
@Data
public class WorkflowDetailVo {
    private Long id;
    private String name;
    private String description;
    private Integer version;
    private Integer status;
    private WorkflowDto definition;     // nodes + edges + viewport
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
