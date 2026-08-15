package com.minimax.agent.service;

import com.minimax.agent.entity.CollabMember;
import com.minimax.agent.entity.CollabSession;
import com.minimax.agent.mapper.CollabMemberMapper;
import com.minimax.agent.mapper.CollabSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 协作会话的持久化层 (与内存 WebSocket 层 CollabService 分离)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabDbService {

    private final CollabSessionMapper sessionMapper;
    private final CollabMemberMapper memberMapper;

    public Long createSession(Long ownerId, String title, Integer maxUsers) {
        CollabSession s = new CollabSession();
        s.setSessionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        s.setOwnerId(ownerId);
        s.setTitle(title == null ? "新协作" : title);
        s.setMaxUsers(maxUsers == null ? 10 : maxUsers);
        s.setStatus("active");
        sessionMapper.insert(s);

        CollabMember owner = new CollabMember();
        owner.setCollabId(s.getId());
        owner.setUserId(ownerId);
        owner.setRole("owner");
        owner.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(owner);
        return s.getId();
    }

    public boolean joinSession(Long collabId, Long userId, String role) {
        CollabSession s = sessionMapper.selectById(collabId);
        if (s == null) return false;
        if (!"active".equals(s.getStatus())) return false;
        CollabMember exist = memberMapper.selectByUser(collabId, userId);
        if (exist != null) return true;
        CollabMember m = new CollabMember();
        m.setCollabId(collabId);
        m.setUserId(userId);
        m.setRole(role == null ? "editor" : role);
        m.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(m);
        return true;
    }

    public boolean closeSession(Long collabId, Long userId) {
        CollabSession s = sessionMapper.selectById(collabId);
        if (s == null) return false;
        if (!s.getOwnerId().equals(userId)) return false;
        s.setStatus("closed");
        s.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(s);
        return true;
    }

    public CollabSession findBySessionId(String sessionId) {
        return sessionMapper.selectBySessionId(sessionId);
    }
}
