package com.minimax.ai.framework.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 群组消息 (V3.0.3)
 *
 * <p>Agent 之间通过 {@link GroupMessageBus} 传递消息
 * <p>每条消息含: 发送者, 接收者 (空=广播), 类型, 载荷
 *
 * <h3>消息类型</h3>
 * <ul>
 *   <li>TASK — 任务派发 (manager → worker)</li>
 *   <li>RESULT — 任务结果 (worker → manager)</li>
 *   <li>FEEDBACK — 评论 (critic → worker)</li>
 *   <li>BROADCAST — 广播 (任何 → 所有人)</li>
 *   <li>SHUTDOWN — 终止 (manager → 所有人)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessage {

    /** 消息 ID (UUID) */
    private String id;
    /** 发送者 (Agent 名) */
    private String from;
    /** 接收者 (空=广播) */
    private String to;
    /** 消息类型 */
    private Type type;
    /** 主题 (任务名 / 评论点) */
    private String subject;
    /** 文本内容 */
    private String content;
    /** 附加数据 (结构化) */
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    /** 时间戳 (ms) */
    private long timestamp;
    /** 引用的任务 ID (用于关联) */
    private String taskId;

    public enum Type {
        TASK, RESULT, FEEDBACK, BROADCAST, SHUTDOWN
    }
}
