package com.minimax.ai.controller;

import com.minimax.ai.framework.agent.Agent;
import com.minimax.ai.framework.agent.Agent.AgentContext;
import com.minimax.ai.framework.agent.Agent.AgentResult;
import com.minimax.ai.framework.agent.AgentRegistry;
import com.minimax.ai.framework.memory.MemoryStore;
import com.minimax.ai.framework.permission.Permission;
import com.minimax.ai.framework.permission.PermissionGate;
import com.minimax.ai.framework.tool.ProductSearchTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MiniMax AI 框架 Controller (V2.8.6)
 *
 * <h3>API</h3>
 * <ul>
 *   <li>POST /api/ai/framework/agents/execute       - 执行指定 Agent</li>
 *   <li>POST /api/ai/framework/agents/route          - 自动路由 + 执行</li>
 *   <li>GET  /api/ai/framework/agents                - 列出所有 Agent</li>
 *   <li>POST /api/ai/framework/permission/grant     - 授权</li>
 *   <li>POST /api/ai/framework/permission/revoke    - 撤销</li>
 *   <li>GET  /api/ai/framework/permission/list      - 列出已授权</li>
 *   <li>GET  /api/ai/framework/memory/stats         - 记忆统计</li>
 *   <li>POST /api/ai/framework/memory/clear         - 清除记忆</li>
 *   <li>GET  /api/ai/framework/products/search      - 商品搜索 (无 Agent)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/framework")
@RequiredArgsConstructor
@Tag(name = "MiniMax AI 框架", description = "自研类 LangChain4j/Spring AI 框架")
public class FrameworkController {

    private final AgentRegistry agentRegistry;
    private final PermissionGate permissionGate;
    private final MemoryStore memoryStore;
    private final ProductSearchTool productSearchTool;

    // ============================================
    // Agent 端点
    // ============================================

    @PostMapping("/agents/execute")
    @Operation(summary = "执行指定 Agent")
    public AgentResult executeAgent(@RequestParam String agentName,
                                     @RequestBody AgentRequest request) {
        Agent agent = agentRegistry.get(agentName);
        if (agent == null) {
            AgentResult r = new AgentResult();
            r.success = false;
            r.errorMessage = "Agent not found: " + agentName;
            return r;
        }
        AgentContext ctx = new AgentContext();
        ctx.sessionId = request.sessionId;
        ctx.userId = request.userId;
        ctx.userQuery = request.query;
        ctx.userLat = request.userLat;
        ctx.userLng = request.userLng;
        ctx.userCity = request.userCity;
        return agent.execute(ctx);
    }

    @PostMapping("/agents/route")
    @Operation(summary = "自动路由到合适 Agent 并执行")
    public AgentResult routeAndExecute(@RequestBody AgentRequest request) {
        Agent agent = agentRegistry.route(request.query);
        if (agent == null) {
            AgentResult r = new AgentResult();
            r.success = false;
            r.errorMessage = "No agents registered";
            return r;
        }
        AgentContext ctx = new AgentContext();
        ctx.sessionId = request.sessionId != null ? request.sessionId : "session-" + System.currentTimeMillis();
        ctx.userId = request.userId;
        ctx.userQuery = request.query;
        ctx.userLat = request.userLat;
        ctx.userLng = request.userLng;
        ctx.userCity = request.userCity;
        return agent.execute(ctx);
    }

    @GetMapping("/agents")
    @Operation(summary = "列出所有 Agent")
    public List<Map<String, Object>> listAgents() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Agent a : agentRegistry.list()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", a.getName());
            m.put("description", a.getDescription());
            m.put("capabilities", a.getCapabilities());
            m.put("requiredPermissions",
                  a.getRequiredPermissions().stream().map(Permission::getCode).toList());
            m.put("tools", a.getTools().keySet());
            list.add(m);
        }
        return list;
    }

    // ============================================
    // 权限端点
    // ============================================

    @PostMapping("/permission/grant")
    @Operation(summary = "用户授权")
    public Map<String, Object> grant(@RequestParam String sessionId,
                                       @RequestBody List<String> permissionCodes) {
        permissionGate.grant(sessionId, permissionCodes);
        return Map.of("sessionId", sessionId, "granted", permissionCodes);
    }

    @PostMapping("/permission/revoke")
    @Operation(summary = "撤销单个权限")
    public Map<String, Object> revoke(@RequestParam String sessionId,
                                        @RequestParam String permissionCode) {
        permissionGate.revoke(sessionId, permissionCode);
        return Map.of("sessionId", sessionId, "revoked", permissionCode);
    }

    @PostMapping("/permission/revoke-all")
    @Operation(summary = "撤销会话所有权限")
    public Map<String, Object> revokeAll(@RequestParam String sessionId) {
        permissionGate.revokeAll(sessionId);
        return Map.of("sessionId", sessionId, "revokedAll", true);
    }

    @GetMapping("/permission/list")
    @Operation(summary = "查看已授权权限")
    public Map<String, Object> listPermissions(@RequestParam String sessionId) {
        Set<String> granted = permissionGate.listGranted(sessionId);
        return Map.of("sessionId", sessionId, "granted", granted);
    }

    // ============================================
    // 记忆端点
    // ============================================

    @GetMapping("/memory/stats")
    @Operation(summary = "记忆存储统计")
    public Map<String, Object> memoryStats() {
        return memoryStore.stats();
    }

    @PostMapping("/memory/clear")
    @Operation(summary = "清除短期记忆")
    public Map<String, Object> clearMemory(@RequestParam String sessionId) {
        memoryStore.clearShortTerm(sessionId);
        return Map.of("sessionId", sessionId, "cleared", true);
    }

    // ============================================
    // 商品 (无 Agent 直接调用)
    // ============================================

    @GetMapping("/products/search")
    @Operation(summary = "直接搜索商品 (无 Agent)")
    public Map<String, Object> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") int maxPrice,
            @RequestParam(required = false, defaultValue = "10") int topK) {
        return productSearchTool.execute(null, Map.of(
                "keyword", keyword == null ? "" : keyword,
                "category", category == null ? "" : category,
                "maxPrice", maxPrice,
                "topK", topK
        ));
    }

    /** Agent 请求 DTO */
    @lombok.Data
    public static class AgentRequest {
        public String sessionId;
        public Long userId;
        public String query;
        public Double userLat;
        public Double userLng;
        public String userCity;
    }
}
