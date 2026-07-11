package com.minimax.ws.collab;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ws.entity.CollabMessage;
import com.minimax.ws.entity.CollabParticipant;
import com.minimax.ws.entity.CollabRoom;
import com.minimax.ws.mapper.CollabMessageMapper;
import com.minimax.ws.mapper.CollabParticipantMapper;
import com.minimax.ws.mapper.CollabRoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 协作核心服务 (V2.8.7)
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>房间生命周期管理 (创建/加入/离开/关闭)</li>
 *   <li>参与者状态管理 (心跳/光标/在线状态)</li>
 *   <li>消息持久化 (聊天/编辑/事件)</li>
 *   <li>内存中的实时连接表 (roomId+userId -> WebSocketSession)</li>
 * </ul>
 *
 * <h3>并发模型</h3>
 * <ul>
 *   <li>WebSocket 连接: ConcurrentHashMap (roomId -> List of Sessions)</li>
 *   <li>数据库: 行级锁 + 乐观 CAS</li>
 *   <li>消息: 异步持久化, 不阻塞广播</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabService {

    private final CollabRoomMapper roomMapper;
    private final CollabParticipantMapper participantMapper;
    private final CollabMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 房间内活跃连接: roomId -> List<WebSocketSession>
     * (WebSocketHandler 直接操作此结构)
     */
    public final Map<String, List<org.springframework.web.socket.WebSocketSession>> roomSessions
            = new ConcurrentHashMap<>();

    /**
     * 短码生成器 (8 位字母数字, 便于分享)
     */
    private String generateShortRoomId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 创建房间
     *
     * @param name        房间名
     * @param type        类型: AI_CHAT/DOC/TRAINING/DASHBOARD/CODE
     * @param ownerId     创建者
     * @param ownerName   创建者名
     * @param isPublic    是否公开
     * @param maxCount    最大人数
     * @return 新房间
     */
    @Transactional
    public CollabRoom createRoom(String name, String type, Long ownerId, String ownerName,
                                  boolean isPublic, int maxCount) {
        // 1. 生成唯一短码 (重试 5 次)
        String roomId;
        int retry = 0;
        do {
            roomId = generateShortRoomId();
            CollabRoom exist = roomMapper.selectOne(
                new QueryWrapper<CollabRoom>().eq("roomId", roomId));
            if (exist == null) break;
        } while (++retry < 5);

        if (retry >= 5) {
            throw new RuntimeException("房间号生成失败, 请重试");
        }

        // 2. 插入房间
        CollabRoom room = CollabRoom.builder()
                .roomId(roomId)
                .name(name == null ? "未命名房间" : name)
                .type(type == null ? "AI_CHAT" : type)
                .ownerId(ownerId)
                .ownerName(ownerName)
                .isPublic(isPublic ? 1 : 0)
                .maxParticipants(maxCount > 0 ? maxCount : 50)
                .status("ACTIVE")
                .currentParticipants(0)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();
        roomMapper.insert(room);

        // 3. 创建者自动加入
        joinRoom(roomId, ownerId, ownerName, ownerName, null, "OWNER");

        log.info("[collab] 房间创建 roomId={} owner={} type={}", roomId, ownerName, type);
        return room;
    }

    /**
     * 用户加入房间
     *
     * @return true=成功, false=已存在或房间已满
     */
    @Transactional
    public boolean joinRoom(String roomId, Long userId, String username, String nickname,
                             String avatar, String role) {
        // 1. 检查房间状态
        CollabRoom room = roomMapper.selectOne(
            new QueryWrapper<CollabRoom>().eq("roomId", roomId));
        if (room == null || !"ACTIVE".equals(room.getStatus())) {
            return false;
        }

        // 2. 检查是否已加入
        CollabParticipant exist = participantMapper.findActiveParticipant(roomId, userId);
        if (exist != null) {
            // 重新激活
            exist.setStatus("ONLINE");
            exist.setLastHeartbeat(LocalDateTime.now());
            participantMapper.updateById(exist);
            return true;
        }

        // 3. 检查容量
        if (room.getCurrentParticipants() >= room.getMaxParticipants()) {
            return false;
        }

        // 4. 创建参与记录
        CollabParticipant p = CollabParticipant.builder()
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .avatar(avatar)
                .role(role == null ? "EDITOR" : role)
                .status("ONLINE")
                .joinedAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .build();
        participantMapper.insert(p);

        // 5. 增加房间人数
        roomMapper.updateParticipantCount(roomId, +1);

        // 6. 广播加入事件
        broadcastSystemEvent(roomId, "JOIN", userId, username, nickname, null);
        return true;
    }

    /**
     * 用户离开房间
     */
    @Transactional
    public void leaveRoom(String roomId, Long userId) {
        CollabParticipant p = participantMapper.findActiveParticipant(roomId, userId);
        if (p == null) return;

        participantMapper.markLeave(roomId, userId);
        roomMapper.updateParticipantCount(roomId, -1);

        broadcastSystemEvent(roomId, "LEAVE", userId, p.getUsername(), p.getNickname(), null);
        log.info("[collab] 用户离开 roomId={} userId={}", roomId, userId);
    }

    /**
     * 关闭房间 (仅 owner)
     */
    @Transactional
    public boolean closeRoom(String roomId, Long userId) {
        CollabRoom room = roomMapper.selectOne(
            new QueryWrapper<CollabRoom>().eq("roomId", roomId));
        if (room == null) return false;
        if (!room.getOwnerId().equals(userId)) return false;

        roomMapper.closeRoom(roomId);
        // 关闭所有参与者
        List<CollabParticipant> participants = participantMapper.findOnlineByRoomId(roomId);
        for (CollabParticipant p : participants) {
            participantMapper.markLeave(roomId, p.getUserId());
        }
        log.info("[collab] 房间关闭 roomId={} by userId={}", roomId, userId);
        return true;
    }

    /**
     * 心跳更新
     */
    public void heartbeat(String roomId, Long userId, Integer cursorX, Integer cursorY, String selectionId) {
        participantMapper.updateCursor(roomId, userId, cursorX, cursorY, selectionId);
    }

    /**
     * 持久化聊天消息
     */
    public CollabMessage saveMessage(String roomId, Long userId, String username, String nickname,
                                       String type, String content, String metadata, String clientMsgId) {
        CollabMessage msg = CollabMessage.builder()
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .type(type)
                .content(content)
                .metadata(metadata)
                .clientMsgId(clientMsgId)
                .broadcast(1)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(msg);
        return msg;
    }

    /**
     * 查询聊天历史
     */
    public List<CollabMessage> getChatHistory(String roomId, int limit) {
        if (limit <= 0) limit = 50;
        if (limit > 500) limit = 500;
        return messageMapper.findChatHistory(roomId, limit);
    }

    /**
     * 查询房间参与者
     */
    public List<CollabParticipant> getParticipants(String roomId, boolean onlineOnly) {
        return onlineOnly
            ? participantMapper.findOnlineByRoomId(roomId)
            : participantMapper.findByRoomId(roomId);
    }

    /**
     * 查询房间信息
     */
    public CollabRoom getRoom(String roomId) {
        return roomMapper.selectOne(
            new QueryWrapper<CollabRoom>().eq("roomId", roomId));
    }

    /**
     * 查询所有公开房间
     */
    public List<CollabRoom> listPublicRooms(int limit) {
        if (limit <= 0) limit = 50;
        return roomMapper.selectList(
            new QueryWrapper<CollabRoom>()
                .eq("isPublic", 1)
                .eq("status", "ACTIVE")
                .orderByDesc("lastActivityAt")
                .last("LIMIT " + limit));
    }

    /**
     * 广播系统事件 (内部用)
     */
    private void broadcastSystemEvent(String roomId, String type, Long userId, String username,
                                        String nickname, Object metadata) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "SYSTEM_EVENT");
            event.put("event", type);
            event.put("userId", userId);
            event.put("username", username);
            event.put("nickname", nickname);
            event.put("timestamp", System.currentTimeMillis());
            if (metadata != null) event.put("metadata", metadata);

            // 同步到消息历史
            String content = String.format("[%s] %s %s", type, nickname != null ? nickname : username,
                type.equals("JOIN") ? "加入了房间" : "离开了房间");
            saveMessage(roomId, null, "system", "System", type, content,
                objectMapper.writeValueAsString(event), null);
        } catch (Exception e) {
            log.warn("[collab] 广播系统事件失败: {}", e.getMessage());
        }
    }
}
