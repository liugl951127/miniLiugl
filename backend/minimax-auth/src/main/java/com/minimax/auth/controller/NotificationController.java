package com.minimax.auth.controller;

import com.minimax.auth.service.NotificationService;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知管理接口。
 *
 * V6.8.2 安全修复:
 *   - 移除对 request.getAttribute("userId") 的依赖（gateway 实际注入的是 X-User-Id Header）
 *   - 改用 @AuthenticationPrincipal 从 SecurityContext 拿当前用户
 *   - 加 @PreAuthorize("isAuthenticated()") 防止未登录访问
 */
@Tag(name = "通知管理")
@RestController
// V1.9.1: 改为 /auth/notifications (放在 auth 模块里, 跟随 auth 路由)
@RequestMapping("/api/v1/auth/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")  // V6.8.2: 通知接口需登录
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "通知列表（分页）")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", notificationService.list(user.id(), page, size)
        ));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读数量")
    public ResponseEntity<?> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "data", notificationService.unreadCount(user.id())
        ));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public ResponseEntity<?> markRead(
            @Parameter(description = "通知 ID") @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        // 修复 V6.8.2: 之前只调用 markRead(id) 无 userId 参数, 现在传 userId 鉴权
        notificationService.markRead(user.id(), id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "已标记已读"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部已读")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        int count = notificationService.markAllRead(user.id());
        return ResponseEntity.ok(Map.of("code", 0, "message", "已全部已读", "data", count));
    }

    @DeleteMapping
    @Operation(summary = "清空通知")
    public ResponseEntity<?> clear(@AuthenticationPrincipal AuthenticatedUser user) {
        int count = notificationService.clear(user.id());
        return ResponseEntity.ok(Map.of("code", 0, "message", "已清空", "data", count));
    }

    // V6.8.1: 删除单条通知
    @DeleteMapping("/{id}")
    @Operation(summary = "删除单条通知")
    public ResponseEntity<?> deleteById(
            @Parameter(description = "通知 ID") @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean ok = notificationService.deleteById(id, user.id());
        if (!ok) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "通知不存在或无权删除"));
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "已删除"));
    }
}
