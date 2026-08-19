package com.minimax.analytics.feign;

import com.minimax.common.feign.model.ChatRequestDTO;
import com.minimax.common.feign.model.ChatResponseDTO;
import com.minimax.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign 客户端：analytics → model LLM 对话
 *
 * V6.8.1: 替代 Maven 编译依赖 minimax-model。
 * 路由: POST /api/v1/models/internal/chat → lb://minimax-model
 */
@FeignClient(
        name = "minimax-model",
        contextId = "modelChatClient",
        path = "/api/v1/models"
)
public interface ModelChatClient {

    /**
     * 内部 chat 端点（无用户认证，供服务间调用）
     * POST /api/v1/models/internal/chat
     */
    @PostMapping("/internal/chat")
    Result<ChatResponseDTO> chat(
            @RequestParam(required = false) Long userId,
            @RequestParam ChatRequestDTO request
    );
}
