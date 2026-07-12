package com.minimax.ws.controller;

import com.minimax.ws.collab.CollabService;
import com.minimax.ws.collab.CrdtEngine;
import com.minimax.ws.entity.CollabMessage;
import com.minimax.ws.entity.CollabParticipant;
import com.minimax.ws.entity.CollabRoom;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协作 REST API 控制器 (V2.8.7 实时协作)
 *
 * <h3>端点</h3>
 * <pre>
 *   POST   /api/v1/collab/rooms                      创建房间
 *   GET    /api/v1/collab/rooms/{roomId}             查询房间
 *   GET    /api/v1/collab/rooms/public               公开房间列表
 *   DELETE /api/v1/collab/rooms/{roomId}             关闭房间
 *   GET    /api/v1/collab/rooms/{roomId}/participants 查询参与者
 *   GET    /api/v1/collab/rooms/{roomId}/messages    聊天历史
 * </pre>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@RestController
@RequestMapping("/api/v1/collab")
@RequiredArgsConstructor
public class CollabController {

    private final CollabService collabService;
    private final CrdtEngine crdtEngine;

    /**
     * 创建协作房间
     */
    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest req) {
        CollabRoom room = collabService.createRoom(
                req.getName(),
                req.getType(),
                req.getOwnerId(),
                req.getOwnerName(),
                req.isPublic(),
                req.getMaxParticipants()
        );
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "创建成功",
                "data", room
        ));
    }

    /**
     * 查询房间
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoom(@PathVariable String roomId) {
        CollabRoom room = collabService.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "房间不存在", "data", null));
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "ok", "data", room));
    }

    /**
     * 公开房间列表
     */
    @GetMapping("/rooms/public")
    public ResponseEntity<Map<String, Object>> listPublicRooms(
            @RequestParam(defaultValue = "50") int limit) {
        List<CollabRoom> rooms = collabService.listPublicRooms(limit);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", rooms,
                "total", rooms.size()
        ));
    }

    /**
     * 关闭房间 (仅 owner)
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> closeRoom(
            @PathVariable String roomId,
            @RequestParam Long userId) {
        boolean ok = collabService.closeRoom(roomId, userId);
        return ResponseEntity.ok(Map.of(
                "code", ok ? 0 : 403,
                "message", ok ? "已关闭" : "权限不足或房间不存在"
        ));
    }

    /**
     * 查询参与者
     */
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<Map<String, Object>> getParticipants(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "true") boolean onlineOnly) {
        List<CollabParticipant> list = collabService.getParticipants(roomId, onlineOnly);
        Map<String, Object> data = new HashMap<>();
        data.put("participants", list);
        data.put("total", list.size());
        return ResponseEntity.ok(Map.of("code", 0, "message", "ok", "data", data));
    }

    /**
     * 聊天历史
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        List<CollabMessage> messages = collabService.getChatHistory(roomId, limit);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", messages,
                "total", messages.size()
        ));
    }

    /**
     * V2.8.8: CRDT 文档快照 (供客户端初始同步)
     */
    @GetMapping("/rooms/{roomId}/doc")
    public ResponseEntity<Map<String, Object>> getDocSnapshot(@PathVariable String roomId) {
        Map<String, Object> snapshot = crdtEngine.snapshot(roomId);
        String text = crdtEngine.renderText(roomId);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", Map.of(
                    "snapshot", snapshot,
                    "text", text,
                    "length", text.length()
                )
        ));
    }

    /**
     * V2.8.8: 应用 CRDT op (HTTP 模式, 供不支持 WebSocket 的客户端)
     */
    @PostMapping("/rooms/{roomId}/doc/ops")
    public ResponseEntity<Map<String, Object>> applyDocOps(
            @PathVariable String roomId,
            @RequestBody java.util.List<CrdtEngine.CrdtOperation> ops) {
        CrdtEngine.DocState state = crdtEngine.applyBatch(roomId, ops);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", Map.of(
                    "version", state.getVersion(),
                    "text", crdtEngine.renderText(roomId)
                )
        ));
    }

    // ============= DTO =============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoomRequest {
        private String name;
        private String type;             // AI_CHAT/DOC/TRAINING/DASHBOARD/CODE
        private Long ownerId;
        private String ownerName;
        private boolean isPublic;
        private int maxParticipants;     // 默认 50
    }
}
