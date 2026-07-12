package com.minimax.auth.controller;

import com.minimax.auth.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知管理接口。
 */
@Tag(name = "通知管理")
@RestController
// V1.9.1: 改为 /auth/notifications (放在 auth 模块里, 跟随 auth 路由)
@RequestMapping("/api/v1/auth/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "通知列表（分页）")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", notificationService.list(userId, page, size)
        ));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读数量")
    public ResponseEntity<?> unreadCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", notificationService.unreadCount(userId)
        ));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public ResponseEntity<?> markRead(
            @Parameter(description = "通知 ID") @PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "已标记已读"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部已读")
    public ResponseEntity<?> markAllRead(HttpServletRequest request) {
        Long userId = getUserId(request);
        int count = notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "已全部已读", "data", count));
    }

    @DeleteMapping
    @Operation(summary = "清空通知")
    public ResponseEntity<?> clear(HttpServletRequest request) {
        Long userId = getUserId(request);
        int count = notificationService.clear(userId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "已清空", "data", count));
    }

    // 从请求属性中取 userId（gateway 透传）
    private Long getUserId(HttpServletRequest request) {
        Object uid = request.getAttribute("userId");
        if (uid instanceof Long) return (Long) uid;
        if (uid instanceof Integer) return ((Integer) uid).longValue();
        if (uid instanceof String) return Long.parseLong((String) uid);
        return null;
    }
}