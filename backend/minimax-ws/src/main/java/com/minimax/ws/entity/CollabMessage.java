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
 * 协作消息 (V2.8.7 实时协作)
 *
 * <p>房间内的实时消息: 聊天 / 编辑操作 / 光标移动 / 状态变化 / 系统事件</p>
 *
 * <h3>消息类型</h3>
 * <ul>
 *   <li>CHAT: 聊天文字</li>
 *   <li>EDIT: 文档编辑 (CRDT 操作)</li>
 *   <li>CURSOR: 光标位置更新</li>
 *   <li>SELECTION: 选中区域变化</li>
 *   <li>JOIN: 用户加入</li>
 *   <li>LEAVE: 用户离开</li>
 *   <li>AI: AI 生成结果</li>
 *   <li>SYSTEM: 系统通知</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collab_message")
public class CollabMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 房间 ID */
    @TableField("room_id")
    private String roomId;

    /** 发送者用户 ID (SYSTEM 消息为 null) */
    @TableField("user_id")
    private Long userId;

    /** 发送者用户名 */
    @TableField("username")
    private String username;

    /** 发送者昵称 */
    @TableField("nickname")
    private String nickname;

    /** 消息类型: CHAT / EDIT / CURSOR / SELECTION / JOIN / LEAVE / AI / SYSTEM */
    @TableField("type")
    private String type;

    /** 消息内容 (聊天文字 / CRDT op JSON / 光标坐标 JSON) */
    @TableField("content")
    private String content;

    /** 额外元数据 (JSON) */
    @TableField("metadata")
    private String metadata;

    /** 客户端消息 ID (用于去重) */
    @TableField("client_msg_id")
    private String clientMsgId;

    /** 是否广播到所有参与者 */
    @TableField("broadcast")
    private Integer broadcast;

    /** 发送时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
