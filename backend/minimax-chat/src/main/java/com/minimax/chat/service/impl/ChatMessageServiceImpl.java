package com.minimax.chat.service.impl;

import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.entity.ChatMessage;
import com.minimax.chat.mapper.ChatMessageMapper;
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
}
