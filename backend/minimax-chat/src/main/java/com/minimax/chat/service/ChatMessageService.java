package com.minimax.chat.service;

import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.entity.ChatMessage;
import com.minimax.chat.vo.MessageVO;

import java.util.List;

public interface ChatMessageService {
    MessageVO append(Long userId, Long sessionId, AppendMessageRequest req);
    List<MessageVO> listBySession(Long userId, Long sessionId, int page, int size);
    List<ChatMessage> lastN(Long sessionId, int n);
}
