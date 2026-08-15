package com.minimax.chat.service;

import com.minimax.chat.dto.CreateSessionRequest;
import com.minimax.chat.dto.UpdateSessionRequest;
import com.minimax.chat.entity.ChatSession;
import com.minimax.chat.vo.SessionVO;

import java.util.List;

public interface ChatSessionService {
    SessionVO create(Long userId, CreateSessionRequest req);
    List<SessionVO> listByUser(Long userId, Integer status);
    SessionVO detail(Long id, Long userId);
    SessionVO update(Long id, Long userId, UpdateSessionRequest req);
    void archive(Long id, Long userId);
    ChatSession requireOwned(Long id, Long userId);
}
