package com.minimax.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.ai.constants.CollabInviteConstants;
import com.minimax.ai.entity.CollabInvite;
import com.minimax.ai.mapper.CollabInviteMapper;
import com.minimax.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 协作房间邀请服务 (T1-backend-apis / P0)
 *
 * 3 个端点:
 *   - POST /api/v1/collab/rooms/{id}/invite      发送邀请
 *   - GET  /api/v1/collab/invites                我发起的邀请
 *   - GET  /api/v1/collab/invites/received        我收到的邀请
 *
 * 前端对接: views/collab/Index.vue inviteMember()
 *
 * @since V7.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabInviteService {

    private final CollabInviteMapper inviteMapper;

    /**
     * 发送邀请
     *
     * @param roomId    房间 ID
     * @param email     被邀请邮箱
     * @param inviterId 发起邀请用户 ID (从 X-User-Id)
     * @return inviteId (前端用作 toast 提示 / 调试)
     */
    @Transactional
    public Long sendInvite(Long roomId, String email, Long inviterId) {
        if (roomId == null) {
            throw new BizException("房间 ID 不能为空");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BizException("邮箱格式不合法: " + email);
        }
        CollabInvite inv = new CollabInvite();
        inv.setRoomId(roomId);
        inv.setInviterId(inviterId);
        inv.setInviteeEmail(email.trim().toLowerCase());
        inv.setToken(generateToken());
        inv.setStatus(CollabInviteConstants.STATUS_PENDING);
        inv.setExpiresAt(LocalDateTime.now().plusDays(CollabInviteConstants.DEFAULT_EXPIRE_DAYS));
        inviteMapper.insert(inv);
        log.info("[CollabInvite] sent: roomId={} email={} inviterId={} inviteId={} token={}",
                roomId, email, inviterId, inv.getId(), inv.getToken());
        return inv.getId();
    }

    /**
     * 当前用户 (按 inviterId) 的邀请列表
     */
    public List<CollabInvite> listMyInvites(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return inviteMapper.selectList(
                new LambdaQueryWrapper<CollabInvite>()
                        .eq(CollabInvite::getInviterId, userId)
                        .orderByDesc(CollabInvite::getCreatedAt)
                        .last("LIMIT 100"));
    }

    /**
     * 我收到的邀请 (按 inviteeUserId)
     */
    public List<CollabInvite> listInvitesForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return inviteMapper.selectList(
                new LambdaQueryWrapper<CollabInvite>()
                        .eq(CollabInvite::getInviteeUserId, userId)
                        .orderByDesc(CollabInvite::getCreatedAt)
                        .last("LIMIT 100"));
    }

    private String generateToken() {
        return CollabInviteConstants.TOKEN_PREFIX
                + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, CollabInviteConstants.TOKEN_BODY_LENGTH);
    }
}
