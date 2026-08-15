package com.minimax.agent.controller;

import com.minimax.agent.service.ExternalAgentService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Agent 内部 API (V7.0)
 *
 * 供内部服务调用 (AI模块 → Agent模块), 沙箱模式下 bypass API Key 鉴权。
 * 使用 AuthenticatedUser (由 H2localMockAuthFilter 注入) 获取 userId。
 */
@Slf4j
@Tag(name = "Agent 内部 API (h2local)")
@RestController
@RequestMapping("/internal/agent")
@RequiredArgsConstructor
public class AgentInternalController {

    private final ExternalAgentService externalAgentService;

    /**
     * 同步运行 Agent (内部调用)
     * Flow②: AI模块 → Agent模块, 传递 session上下文 + 知识库信息
     */
    @Operation(summary = "同步运行 Agent (内部调用, bypass API Key)")
    @PostMapping("/run")
    public Result<Map<String, Object>> run(@RequestBody Map<String, Object> req,
                                           @RequestParam(defaultValue = "1") Long userId) {
        String agentId = (String) req.get("agentId");
        String goal = (String) req.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) req.get("tools");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) req.get("params");

        log.info("[Agent/Internal] userId={} agentId={} goal={}", userId, agentId, goal);
        try {
            Map<String, Object> result = externalAgentService.runSync(userId, agentId, goal, tools, params);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("[Agent/Internal] 运行失败: {}", e.getMessage(), e);
            return Result.fail(500, "Agent 执行失败: " + e.getMessage());
        }
    }

    /**
     * SSE 流式运行 Agent (内部调用)
     * Flow②: 支持流式返回 agent 思考过程
     */
    @Operation(summary = "SSE流式运行 Agent (内部调用)")
    @PostMapping(value = "/run-stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@RequestBody Map<String, Object> req,
                               @RequestParam(defaultValue = "1") Long userId) {
        String agentId = (String) req.get("agentId");
        String goal = (String) req.get("goal");
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) req.get("tools");
        return externalAgentService.runStream(userId, agentId, goal, tools);
    }
}
