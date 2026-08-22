package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协作房间邀请 (T1-backend-apis / P0)
 *
 * 用于 collab/Index.vue 的"邀请"按钮持久化。
 * 真实场景:
 *   1. 用户在房间点击"邀请"
 *   2. 前端输入邮箱, 提交到后端
 *   3. 后端生成 token, 状态 PENDING
 *   4. 邮件链接点击 → 前端跳到 /collab/accept?token=...
 *   5. 后端校验 token 改状态 ACCEPTED
 *
 * @since V7.2
 */
@Data
@TableName("collab_invite")
public class CollabInvite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;
    private Long inviterId;
    private String inviteeEmail;

    /** 已注册用户可填; 邮箱邀请时为空 */
    private Long inviteeUserId;

    /** 邀请唯一 token, 用于接受时校验 */
    private String token;

    /** PENDING / ACCEPTED / EXPIRED */
    private String status;

    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
