package com.minimax.pipeline.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DAG 校验结果 (V5.32)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagValidationVo {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private List<List<String>> executionOrder;     // [[n1, n2], [n3]] 拓扑层
    private Map<String, Integer> inDegree;          // 入度表
}
