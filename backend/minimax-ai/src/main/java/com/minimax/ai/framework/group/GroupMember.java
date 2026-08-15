package com.minimax.ai.framework.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群成员 (V3.0.3)
 *
 * <p>每个 Agent 在群组中的身份卡
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    /** 成员 ID (UUID) */
    private String memberId;
    /** 群组 ID */
    private String groupId;
    /** Agent 名 (唯一) */
    private String agentName;
    /** 角色 */
    private GroupRole role;
    /** 权重 (投票时用, 越大越有话语权) */
    private double weight;
    /** Agent 能力描述 (用于 manager 分配任务) */
    private String capability;
    /** 顺序 (PIPELINE 策略用) */
    private int order;

    /**
     * 默认成员工厂
     */
    public static GroupMember worker(String groupId, String agentName, String capability) {
        return GroupMember.builder()
                .groupId(groupId)
                .agentName(agentName)
                .role(GroupRole.WORKER)
                .weight(1.0)
                .capability(capability)
                .order(0)
                .build();
    }

    public static GroupMember manager(String groupId, String agentName) {
        return GroupMember.builder()
                .groupId(groupId)
                .agentName(agentName)
                .role(GroupRole.MANAGER)
                .weight(2.0)
                .capability("coordinator")
                .order(-1)
                .build();
    }

    public static GroupMember critic(String groupId, String agentName) {
        return GroupMember.builder()
                .groupId(groupId)
                .agentName(agentName)
                .role(GroupRole.CRITIC)
                .weight(1.5)
                .capability("reviewer")
                .order(99)
                .build();
    }
}
