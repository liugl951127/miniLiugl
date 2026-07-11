package com.minimax.ai.framework.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 注册表 + 路由 (V2.8.6)
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>注册所有业务 Agent</li>
 *   <li>根据用户查询自动选择最匹配的 Agent</li>
 *   <li>提供 Agent 列表 API</li>
 * </ul>
 *
 * <h3>路由算法</h3>
 * 1. 按 capability 标签匹配 (权重)
 * 2. 按 Agent 名称/描述关键词匹配
 * 3. 默认: 第一个 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRegistry {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final List<Agent> agentList = new ArrayList<>();

    /**
     * 注册 Agent (Spring 启动后调用)
     */
    public void register(Agent agent) {
        agents.put(agent.getName(), agent);
        agentList.add(agent);
        log.info("[agent-registry] registered: {} - {}", agent.getName(), agent.getDescription());
    }

    /**
     * 获取 Agent
     */
    public Agent get(String name) {
        return agents.get(name);
    }

    /**
     * 列出所有 Agent
     */
    public List<Agent> list() {
        return new ArrayList<>(agentList);
    }

    /**
     * 自动路由: 根据查询找最合适的 Agent
     */
    public Agent route(String userQuery) {
        if (userQuery == null || userQuery.isEmpty()) {
            return agentList.isEmpty() ? null : agentList.get(0);
        }
        String lower = userQuery.toLowerCase();
        Agent best = null;
        int bestScore = -1;
        for (Agent a : agentList) {
            int score = scoreAgent(a, lower);
            log.info("SCORE {} = {} for '{}'", a.getName(), score, userQuery);
            if (score > bestScore) {
                bestScore = score;
                best = a;
            }
        }
        log.info("BEST {} (score={})", best != null ? best.getName() : "null", bestScore);
        return best != null ? best : (agentList.isEmpty() ? null : agentList.get(0));
    }

    /**
     * 评分 Agent 与查询的匹配度
     */
    private int scoreAgent(Agent agent, String lowerQuery) {
        int score = 0;
        // 1. capability 匹配 (中英双查)
        for (String cap : agent.getCapabilities()) {
            if (lowerQuery.contains(cap.toLowerCase()) || lowerQuery.contains(cap)) score += 3;
        }
        // 2. 名称匹配
        if (lowerQuery.contains(agent.getName().toLowerCase()) || lowerQuery.contains(agent.getName())) score += 5;
        // 3. 描述中每个 2 字符以上子串做滑动窗口
        if (agent.getDescription() != null) {
            String desc = agent.getDescription();
            // 提取所有 2-char 子串
            for (int i = 0; i + 2 <= desc.length(); i++) {
                String sub = desc.substring(i, i + 2);
                if (lowerQuery.contains(sub)) {
                    score += 1;
                }
            }
        }
        return score;
    }
}
