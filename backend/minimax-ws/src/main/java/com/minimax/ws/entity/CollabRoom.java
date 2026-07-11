package com.minimax.ws.entity;

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
 * 协作房间实体 (V2.8.7 实时协作)
 *
 * <p>用于多人协同工作: AI 对话 / 文档编辑 / 训练监控 / 仪表盘协作</p>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>每个房间有唯一 roomId, 用户通过 roomId 加入</li>
 *   <li>type 区分协作场景: AI_CHAT, DOC, TRAINING, DASHBOARD, CODE</li>
 *   <li>ownerId 创建者, isPublic 控制可见性</li>
 *   <li>maxParticipants 上限 (默认 50)</li>
 *   <li>lastActivityAt 用于清理空闲房间</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collab_room")
public class CollabRoom {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 房间唯一标识 (UUID, 短码便于分享) */
    @TableField("roomId")
    private String roomId;

    /** 房间名称 */
    @TableField("name")
    private String name;

    /** 房间类型: AI_CHAT / DOC / TRAINING / DASHBOARD / CODE */
    @TableField("type")
    private String type;

    /** 创建者用户 ID */
    @TableField("ownerId")
    private Long ownerId;

    /** 创建者用户名 (冗余便于查询) */
    @TableField("ownerName")
    private String ownerName;

    /** 房间描述 */
    @TableField("description")
    private String description;

    /** 是否公开 (公开房间任何用户可加入) */
    @TableField("isPublic")
    private Integer isPublic;

    /** 最大参与人数 */
    @TableField("maxParticipants")
    private Integer maxParticipants;

    /** 状态: ACTIVE / CLOSED */
    @TableField("status")
    private String status;

    /** 当前参与人数 (冗余, 实时更新) */
    @TableField("currentParticipants")
    private Integer currentParticipants;

    /** 创建时间 */
    @TableField("createdAt")
    private LocalDateTime createdAt;

    /** 最后活跃时间 */
    @TableField("lastActivityAt")
    private LocalDateTime lastActivityAt;

    /** 关闭时间 */
    @TableField("closedAt")
    private LocalDateTime closedAt;
}
