package com.minimax.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.chat.dto.CreateSessionRequest;
import com.minimax.chat.dto.UpdateSessionRequest;
import com.minimax.chat.entity.ChatSession;
import com.minimax.chat.mapper.ChatSessionMapper;
import com.minimax.chat.service.ChatSessionService;
import com.minimax.chat.vo.SessionVO;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import com.minimax.common.tenant.TenantContext;
import com.minimax.common.tenant.TenantQueryHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper sessionMapper;

    @Override
    @Transactional
    public SessionVO create(Long userId, CreateSessionRequest req) {
        ChatSession s = new ChatSession();
        s.setUserId(userId);
        s.setTitle(req.getTitle());
        s.setModel(req.getModel());
        s.setSystemPrompt(req.getSystemPrompt());
        s.setTemperature(req.getTemperature() != null ? req.getTemperature() : new java.math.BigDecimal("0.70"));
        s.setStatus(1);
        s.setMessageCount(0);
        s.setTenantId(TenantContext.currentTenantId());
        sessionMapper.insert(s);
        return SessionVO.from(s);
    }

    @Override
    public List<SessionVO> listByUser(Long userId, Integer status) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId);
        if (status != null) {
            wrapper.eq(ChatSession::getStatus, status);
        }
        TenantQueryHelper.applyTenantFilter(wrapper, ChatSession::getTenantId);
        wrapper.orderByDesc(ChatSession::getLastMessageAt, ChatSession::getUpdatedAt);
        List<ChatSession> list = sessionMapper.selectList(wrapper);
        return list.stream().map(SessionVO::from).toList();
    }

    @Override
    public SessionVO detail(Long id, Long userId) {
        return SessionVO.from(requireOwned(id, userId));
    }

    @Override
    @Transactional
    public SessionVO update(Long id, Long userId, UpdateSessionRequest req) {
        ChatSession s = requireOwned(id, userId);
        if (req.getTitle() != null)       s.setTitle(req.getTitle());
        if (req.getModel() != null)       s.setModel(req.getModel());
        if (req.getSystemPrompt() != null) s.setSystemPrompt(req.getSystemPrompt());
        if (req.getTemperature() != null) s.setTemperature(req.getTemperature());
        if (req.getStatus() != null)      s.setStatus(req.getStatus());
        sessionMapper.updateById(s);
        return SessionVO.from(s);
    }

    @Override
    @Transactional
    public void archive(Long id, Long userId) {
        int rows = sessionMapper.archiveByIdAndUser(id, userId);
        if (rows == 0) throw new BizException(ResultCode.NOT_FOUND, "会话不存在或无权限");
    }

    @Override
    public ChatSession requireOwned(Long id, Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getId, id);
        wrapper.eq(ChatSession::getUserId, userId);
        TenantQueryHelper.applyTenantFilter(wrapper, ChatSession::getTenantId);
        wrapper.last("LIMIT 1");
        ChatSession s = sessionMapper.selectOne(wrapper);
        if (s == null) throw new BizException(ResultCode.NOT_FOUND, "会话不存在或无权限");
        return s;
    }
}
