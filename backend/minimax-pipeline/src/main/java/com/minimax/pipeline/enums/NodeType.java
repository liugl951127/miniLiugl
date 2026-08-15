package com.minimax.pipeline.enums;

import lombok.Getter;

/**
 * 节点类型 (V5.32) - 13 种
 *
 * INPUT (3): MYSQL_INPUT / FILE_INPUT / API_INPUT
 * TRANSFORM (8): FILTER / SELECT / JOIN / AGGREGATE / SORT / LIMIT / UNION / DISTINCT
 * OUTPUT (3): DB_OUTPUT / FILE_OUTPUT / REPORT_OUTPUT
 */
@Getter
public enum NodeType {

    // ===== INPUT =====
    MYSQL_INPUT(0, "MySQL 输入", ""),
    FILE_INPUT(0, "文件输入", ""),
    API_INPUT(0, "API 输入", ""),

    // ===== TRANSFORM =====
    FILTER(1, "过滤", "condition"),
    SELECT(1, "列投影", "columns"),
    JOIN(2, "连接", "leftKey,rightKey,type"),
    AGGREGATE(1, "聚合", "groupBy,aggregations"),
    SORT(1, "排序", "orders"),
    LIMIT(1, "限制", "limit"),
    UNION(2, "合并", "type"),
    DISTINCT(1, "去重", "columns"),

    // ===== OUTPUT =====
    DB_OUTPUT(1, "数据库输出", "datasourceId,table"),
    FILE_OUTPUT(1, "文件输出", "format,path"),
    REPORT_OUTPUT(1, "报告输出", "title");

    private final int inputArity;
    private final String label;
    private final String configHint;

    NodeType(int inputArity, String label, String configHint) {
        this.inputArity = inputArity;
        this.label = label;
        this.configHint = configHint;
    }

    public boolean isInput() { return this == MYSQL_INPUT || this == FILE_INPUT || this == API_INPUT; }
    public boolean isOutput() { return this == DB_OUTPUT || this == FILE_OUTPUT || this == REPORT_OUTPUT; }
}
