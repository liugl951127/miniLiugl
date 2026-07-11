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
 * 协作参与者 (V2.8.7 实时协作)
 *
 * <p>记录谁加入了哪个房间, 加入时间, 离开时间, 角色 (OWNER/EDITOR/VIEWER)</p>
 *
 * <h3>角色</h3>
 * <ul>
 *   <li>OWNER: 创建者, 全部权限 (关闭房间, 踢人)</li>
 *   <li>EDITOR: 可编辑, 可发言</li>
 *   <li>VIEWER: 只读旁观</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collab_participant")
public class CollabParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 房间 ID */
    @TableField("roomId")
    private String roomId;

    /** 用户 ID */
    @TableField("userId")
    private Long userId;

    /** 用户名 (冗余) */
    @TableField("username")
    private String username;

    /** 用户昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像 URL */
    @TableField("avatar")
    private String avatar;

    /** 角色: OWNER / EDITOR / VIEWER */
    @TableField("role")
    private String role;

    /** 当前光标 X 坐标 (编辑器场景, 像素) */
    @TableField("cursorX")
    private Integer cursorX;

    /** 当前光标 Y 坐标 */
    @TableField("cursorY")
    private Integer cursorY;

    /** 当前选中元素 ID (DOM 元素或消息 ID) */
    @TableField("selectionId")
    private String selectionId;

    /** 在线状态: ONLINE / AWAY / OFFLINE */
    @TableField("status")
    private String status;

    /** 加入时间 */
    @TableField("joinedAt")
    private LocalDateTime joinedAt;

    /** 离开时间 (null = 在线) */
    @TableField("leftAt")
    private LocalDateTime leftAt;

    /** 最后心跳时间 */
    @TableField("lastHeartbeat")
    private LocalDateTime lastHeartbeat;
}
