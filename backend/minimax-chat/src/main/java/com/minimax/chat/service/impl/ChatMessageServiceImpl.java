package com.minimax.chat.service.impl;

import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.entity.ChatMessage;
import com.minimax.chat.mapper.ChatMessageMapper;
import com.minimax.chat.memory.SessionContextCache;
import com.minimax.chat.service.ChatMessageService;
import com.minimax.chat.service.ChatSessionService;
import com.minimax.chat.vo.MessageVO;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper messageMapper;
    private final ChatSessionService sessionService;
    private final com.minimax.chat.mapper.ChatSessionMapper sessionMapper;
    private final SessionContextCache contextCache;   // Day 6: 短期记忆

    @Override
    @Transactional
    public MessageVO append(Long userId, Long sessionId, AppendMessageRequest req) {
        // 鉴权 + 拿到 session
        sessionService.requireOwned(sessionId, userId);

        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setRole(req.getRole());
        m.setContent(req.getContent());
        m.setTokens(req.getTokens());
        m.setFinishReason(req.getFinishReason());
        m.setErrorMessage(req.getErrorMessage());
        messageMapper.insert(m);

        // 触发 session 计数 + last_message_at
        sessionMapper.bumpMessage(sessionId, LocalDateTime.now());

        // Day 6: 同步进短期记忆（供模型调用时取上下文）
        if (req.getRole() != null && req.getContent() != null) {
            contextCache.append(sessionId, req.getRole(), req.getContent());
        }

        return MessageVO.from(m);
    }

    @Override
    public List<MessageVO> listBySession(Long userId, Long sessionId, int page, int size) {
        // 鉴权
        sessionService.requireOwned(sessionId, userId);
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 50;
        int offset = page * size;
        List<ChatMessage> rows = messageMapper.selectBySessionId(sessionId, offset, size);
        return rows.stream().map(MessageVO::from).toList();
    }

    @Override
    public List<ChatMessage> lastN(Long sessionId, int n) {
        if (n <= 0 || n > 200) n = 20;
        return messageMapper.selectLastN(sessionId, n);
    }

    /** Day 6: 取最近 N 条 context。 */
    @Override
    public List<java.util.Map<String, String>> recentContext(Long sessionId, int limit) {
        return contextCache.recent(sessionId, limit);
    }
}
