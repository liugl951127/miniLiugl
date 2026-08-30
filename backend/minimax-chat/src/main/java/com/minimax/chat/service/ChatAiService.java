package com.minimax.chat.service;

import com.minimax.chat.dto.AiGenerateRequest;
import com.minimax.common.sdk.LlmClient;

public interface ChatAiService {
    LlmClient.LlmResult generateAiReply(Long sessionId, Long userId, AiGenerateRequest req);
}
