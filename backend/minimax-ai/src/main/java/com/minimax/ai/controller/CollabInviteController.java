package com.minimax.ai.controller;

import com.minimax.ai.entity.CollabInvite;
import com.minimax.ai.service.CollabInviteService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 协作房间邀请 Controller (T1-backend-apis / P0)
 *
 * 2 个端点 (修复 views/collab/Index.vue inviteMember() 的 mock 行为):
 * <ul>
 *   <li>POST /api/v1/collab/rooms/{id}/invite  发送邀请, 返回 { inviteId, token, status }</li>
 *   <li>GET  /api/v1/collab/invites           当前用户的邀请列表</li>
 * </ul>
 *
 * @since V7.2
 */
@Tag(name = "协作邀请 (V7.2 P0)")
@RestController
@RequestMapping("/api/v1/collab")
@RequiredArgsConstructor
public class CollabInviteController {

    private final CollabInviteService collabInviteService;

    @Operation(summary = "邀请成员加入房间")
    @PostMapping("/rooms/{id}/invite")
    public Result<Map<String, Object>> invite(@PathVariable("id") Long roomId,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                               @RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        Long inviteId = collabInviteService.sendInvite(roomId, email, userId);
        return Result.ok(Map.of(
                "inviteId", inviteId,
                "roomId", roomId,
                "email", email,
                "status", "PENDING",
                "message", "邀请已发出, 等待对方接受"
        ));
    }

    @Operation(summary = "我的邀请列表 (按 X-User-Id)")
    @GetMapping("/invites")
    public Result<List<CollabInvite>> myInvites(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(collabInviteService.listMyInvites(userId));
    }

    @Operation(summary = "我收到的邀请 (按 X-User-Id 当 inviteeUserId)")
    @GetMapping("/invites/received")
    public Result<List<CollabInvite>> receivedInvites(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(collabInviteService.listInvitesForUser(userId));
    }
}
