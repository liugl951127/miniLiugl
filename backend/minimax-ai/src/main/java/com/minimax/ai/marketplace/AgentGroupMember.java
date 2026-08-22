package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体群成员 (T1-backend-orchestrator)
 *
 * <p>与 {@link AgentGroup} 多对一 (group_id)。
 *
 * <h3>字段</h3>
 * <ul>
 *   <li>agentCode - Agent 业务标识 (如 "echo-writer")</li>
 *   <li>role - MANAGER / WORKER / CRITIC</li>
 *   <li>position - 排序, PIPELINE 策略按此顺序执行</li>
 *   <li>configJson - 成员级配置 (capability/weight/systemPrompt/...)</li>
 *   <li>enabled - 0 禁用 / 1 启用</li>
 * </ul>
 *
 * @author MiniMax
 * @since T1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_group_member")
public class AgentGroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 AgentGroup.id (主键 ID, 不是 groupId 业务 ID) */
    @TableField("group_id")
    private Long groupId;

    /** Agent 业务代码 */
    @TableField("agent_code")
    private String agentCode;

    /** 角色: MANAGER | WORKER | CRITIC */
    @TableField("role")
    private String role;

    /** 顺序 */
    @TableField("position")
    private Integer position;

    /** 配置 JSON (capability/weight/systemPrompt 等) */
    @TableField("config_json")
    private String configJson;

    /** 0 禁用 / 1 启用 */
    @TableField("enabled")
    private Integer enabled;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
