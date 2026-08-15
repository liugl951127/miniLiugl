package com.minimax.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI 模块 → Agent 模块 HTTP 客户端 (V7.0)
 *
 * Flow②: Chat 发送消息时, 可同时委托 Agent 辅助执行:
 *  1. AI 调用 Agent /internal/agent/run
 *  2. Agent 执行 ReAct 推理 (可调用知识库搜索等工具)
 *  3. Agent 返回执行结果
 *  4. AI 将 Agent 结果注入 LLM prompt
 *
 * h2local 模式: 直连 localhost:8088 (无网关, 无鉴权)
 * 生产模式: 通过网关 /api/v1/agent/external/run
 */
@Slf4j
@Service
public class AgentClient {

    private final RestTemplate agentRestTemplate;

    @Value("${minimax.agent.service-url:http://localhost:8088}")
    private String agentServiceUrl;

    public AgentClient() {
        // 5s connect, 60s read (Agent 执行可能需要较长时间)
        this.agentRestTemplate = new RestTemplateBuilder()
                .connectTimeout(5000)
                .readTimeout(60_000)
                .build();
    }

    /**
     * 同步调用 Agent (阻塞等待结果)
     *
     * @param userId   用户ID
     * @param agentId  Agent ID (如 "assistant-agent")
     * @param goal     用户目标 (从对话上下文提取)
     * @param chatHistory 最近 N 轮对话历史 (用于 Agent 理解上下文)
     * @param kbId     关联知识库 ID (Agent 可搜索)
     * @param kbName   知识库名称
     * @return Agent 执行结果文本, 失败返回 null
     */
    public String callAgentSync(Long userId, String agentId, String goal,
                               List<Map<String, String>> chatHistory,
                               Long kbId, String kbName) {
        if (agentId == null || agentId.isBlank()) {
            log.debug("[AgentClient] agentId 为空，跳过 Agent 调用");
            return null;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("agentId", agentId);
            body.put("goal", buildAgentGoal(goal, chatHistory, kbId, kbName));

            // Agent 可用的工具列表
            body.put("tools", List.of("search_knowledge", "calculator", "web_search"));

            // 额外参数
            JSONObject params = new JSONObject();
            params.put("kbId", kbId);
            params.put("kbName", kbName);
            if (chatHistory != null && !chatHistory.isEmpty()) {
                params.put("chatHistory", chatHistory.subList(
                        Math.max(0, chatHistory.size() - 5), chatHistory.size()));
            }
            body.put("params", params);

            String url = agentServiceUrl + "/internal/agent/run?userId=" + userId;
            log.info("[AgentClient] 调用 Agent: agentId={}, goal={}", agentId, goal);

            String resp = agentRestTemplate.postForObject(url, body, String.class);
            if (resp == null) {
                log.warn("[AgentClient] Agent 返回空");
                return null;
            }

            JSONObject result = JSON.parseObject(resp);
            if (result.getIntValue("code") != 0) {
                log.warn("[AgentClient] Agent 调用失败: {}", result.getString("message"));
                return null;
            }

            JSONObject data = result.getJSONObject("data");
            // Agent 返回 answer (不是 result)
            String agentOutput = data != null ? data.getString("answer") : null;
            log.info("[AgentClient] Agent 执行成功, 输出长度={}", agentOutput != null ? agentOutput.length() : 0);
            return agentOutput;

        } catch (Exception e) {
            log.warn("[AgentClient] Agent 调用异常 (不影响主流程): {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建 Agent 执行目标 (包含上下文信息)
     */
    private String buildAgentGoal(String userGoal, List<Map<String, String>> chatHistory,
                                  Long kbId, String kbName) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户目标: ").append(userGoal).append("\n");

        if (kbId != null && kbName != null) {
            sb.append("关联知识库: ").append(kbName).append(" (ID=").append(kbId).append(")\n");
            sb.append("请先搜索知识库获取相关信息再回答。\n");
        }

        if (chatHistory != null && !chatHistory.isEmpty()) {
            sb.append("\n对话历史:\n");
            int start = Math.max(0, chatHistory.size() - 3);
            for (int i = start; i < chatHistory.size(); i++) {
                Map<String, String> msg = chatHistory.get(i);
                sb.append("- ").append(msg.get("role")).append(": ")
                  .append(msg.get("content")).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 内部 RestTemplate builder (避免引入额外依赖)
     */
    private static class RestTemplateBuilder {
        private int connectTimeout = 5000;
        private int readTimeout = 30_000;

        RestTemplateBuilder connectTimeout(int ms) { this.connectTimeout = ms; return this; }
        RestTemplateBuilder readTimeout(int ms) { this.readTimeout = ms; return this; }

        RestTemplate build() {
            org.springframework.boot.web.client.RestTemplateBuilder b =
                    new org.springframework.boot.web.client.RestTemplateBuilder()
                            .setConnectTimeout(java.time.Duration.ofMillis(connectTimeout))
                            .setReadTimeout(java.time.Duration.ofMillis(readTimeout));
            return b.build();
        }
    }
}
