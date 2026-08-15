package com.minimax.ai.framework;

import com.minimax.ai.framework.agent.*;
import com.minimax.ai.framework.memory.MemoryStore;
import com.minimax.ai.framework.permission.PermissionGate;
import com.minimax.ai.framework.tool.*;
import com.minimax.ai.pipeline.stage.Tokenizer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 框架启动器 (V2.8.6)
 *
 * <h3>职责</h3>
 * Spring 启动后, 自动:
 * <ul>
 *   <li>注册所有业务 Agent</li>
 *   <li>注入依赖 (MemoryStore, PermissionGate, Tokenizer)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrameworkBootstrap {

    private final AgentRegistry agentRegistry;
    private final ShoppingAgent shoppingAgent;
    private final HotelAgent hotelAgent;
    private final EntertainmentAgent entertainmentAgent;
    private final MemoryStore memoryStore;
    private final PermissionGate permissionGate;
    private final Tokenizer tokenizer;

    @PostConstruct
    public void init() {
        // 注入依赖
        shoppingAgent.withDependencies(memoryStore, permissionGate, tokenizer);
        hotelAgent.withDependencies(memoryStore, permissionGate, tokenizer);
        entertainmentAgent.withDependencies(memoryStore, permissionGate, tokenizer);

        // 注册 Agent
        agentRegistry.register(shoppingAgent);
        agentRegistry.register(hotelAgent);
        agentRegistry.register(entertainmentAgent);

        log.info("[framework] bootstrap complete: {} agents registered", agentRegistry.list().size());
    }
}
