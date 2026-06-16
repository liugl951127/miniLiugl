package com.minimax.chat.service;

import com.minimax.chat.dto.AppendMessageRequest;
import com.minimax.chat.entity.ChatMessage;
import com.minimax.chat.vo.MessageVO;

import java.util.List;
import java.util.Map;

public interface ChatMessageService {
    MessageVO append(Long userId, Long sessionId, AppendMessageRequest req);
    List<MessageVO> listBySession(Long userId, Long sessionId, int page, int size);
    List<ChatMessage> lastN(Long sessionId, int n);
    /** Day 6: 短期记忆最近 N 条 */
    List<Map<String, String>> recentContext(Long sessionId, int limit);
}
