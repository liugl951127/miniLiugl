package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private Long collabId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
}
