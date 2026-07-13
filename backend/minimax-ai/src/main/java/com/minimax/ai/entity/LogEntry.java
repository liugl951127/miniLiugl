package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Raft 日志条目 (V3.5.0)
 *
 * <p>Raft 算法核心数据结构: 每个日志条目包含
 * <ul>
 *   <li>term: 创建时的任期号</li>
 *   <li>logIndex: 在该节点日志中的位置 (从 1 开始)</li>
 *   <li>command: 业务命令 (JSON 字符串, e.g. "{type:\"config_update\",key:\"...\"}")</li>
 *   <li>committed: 是否已提交 (多数派写入)</li>
 * </ul>
 *
 * <p>所有节点按 (term, logIndex) 顺序应用, 保证状态机最终一致
 */
@Data
@TableName("raft_log")
public class LogEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任期号 (从 1 开始, 每次选举增 1) */
    private Long term;

    /** 日志索引 (从 1 开始, 严格递增) */
    private Long logIndex;

    /** 节点 ID (产生该日志的节点, 唯一) */
    private String nodeId;

    /** 业务命令 (JSON) */
    private String command;

    /** 是否已提交 */
    private Boolean committed;

    /** 提交时间 */
    private LocalDateTime committedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
