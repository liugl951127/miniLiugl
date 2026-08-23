package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("collab_member")
public class CollabMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("collab_id")
    private Long collabId;
    @TableField("user_id")
    private Long userId;
    @TableField("role")
    private String role;
    @TableField("joined_at")
    private LocalDateTime joinedAt;
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
}
