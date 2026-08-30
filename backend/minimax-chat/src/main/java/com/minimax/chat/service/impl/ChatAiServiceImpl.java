package com.minimax.chat.service.impl;

import com.minimax.chat.dto.AiGenerateRequest;
import com.minimax.chat.service.ChatAiService;
import com.minimax.common.sdk.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chat AI 调用服务 (V9.0) — 接入 LLM Gateway
 *
 * 之前: 前端直接调 minimax-ai 的 /api/v1/multimodal/chat-qwen, 没 source
 * 现在: chat 服务后端调 LlmClient (内部 HTTP minimax-ai 的 Gateway),
 *      自动 cloud→local 兜底, 显式返回 source
 *
 * 前端: 调用 /api/v1/sessions/{id}/ai-generate, 拿到 {content, source, model, durationMs}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatAiServiceImpl implements ChatAiService {

    private final LlmClient llmClient;

    @Override
    public LlmClient.LlmResult generateAiReply(Long sessionId, Long userId, AiGenerateRequest req) {
        log.info("[ChatAI] session={} user={} 调用 LLM Gateway", sessionId, userId);

        // 组装 messages: system + history + current
        List<Map<String, String>> messages = new ArrayList<>();

        // system prompt (可由前端传)
        if (req.getSystem() != null && !req.getSystem().isBlank()) {
            messages.add(Map.of("role", "system", "content", req.getSystem()));
        } else {
            // 默认 system
            messages.add(Map.of("role", "system", "content",
                "你是 MiniMax 智能助手, 友好专业, 回答简洁。"));
        }

        // 历史 (如果有, 按 user/assistant 顺序)
        if (req.getHistory() != null) {
            for (var m : req.getHistory()) {
                if (m.getRole() != null && m.getContent() != null) {
                    messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
                }
            }
        }

        // 当前用户消息
        messages.add(Map.of("role", "user", "content", req.getContent()));

        // 调 LlmClient (内部走 cloud→local 兜底)
        LlmClient.LlmResult result = llmClient.chat(messages);
        log.info("[ChatAI] session={} source={} model={} {}ms available={}",
            sessionId, result.source(), result.model(), result.durationMs(), result.available());
        return result;
    }
}
