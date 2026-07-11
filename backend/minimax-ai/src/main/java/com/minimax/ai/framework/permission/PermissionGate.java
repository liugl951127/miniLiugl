package com.minimax.ai.framework.permission;

import com.minimax.ai.framework.agent.Agent.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限门控 (V2.8.6)
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>检查 Agent 必填权限是否已授予</li>
 *   <li>未授予: 返回授权请求 (前端弹窗)</li>
 *   <li>用户授权后: 记录到会话权限表</li>
 *   <li>风险等级: HIGH 必须每次确认, LOW 一次授权即可</li>
 * </ul>
 *
 * <h3>实现</h3>
 * 内存存储 + 异步可对接 DB. 真实生产可加 Redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionGate {

    /** sessionId → 已授权的权限编码集合 */
    private final Map<String, Set<String>> sessionGranted = new ConcurrentHashMap<>();

    /**
     * 一次性检查多个权限
     */
    public PermissionResult checkAll(List<Permission> perms, AgentContext context) {
        if (perms == null || perms.isEmpty()) {
            return PermissionResult.granted();
        }
        Set<String> granted = sessionGranted.computeIfAbsent(
                context.sessionId, k -> new HashSet<>());
        List<Permission> missing = new ArrayList<>();
        for (Permission p : perms) {
            if (!granted.contains(p.getCode())) {
                missing.add(p);
            }
        }
        if (missing.isEmpty()) {
            return PermissionResult.granted();
        }
        // 构造授权请求 (前端弹窗用)
        StringBuilder desc = new StringBuilder("Agent 需要以下权限:\n");
        for (Permission p : missing) {
            desc.append("• ").append(p.getName())
                    .append(" - ").append(p.getDescription())
                    .append(" (风险: ").append(p.getRiskLevel()).append(")\n");
        }
        return PermissionResult.denied(desc.toString());
    }

    /**
     * 用户授权后, 记录到会话
     */
    public void grant(String sessionId, List<String> permissionCodes) {
        if (sessionId == null || permissionCodes == null) return;
        Set<String> granted = sessionGranted.computeIfAbsent(
                sessionId, k -> new HashSet<>());
        granted.addAll(permissionCodes);
        log.info("[permission] session={} granted: {}", sessionId, permissionCodes);
    }

    /**
     * 撤销授权
     */
    public void revoke(String sessionId, String permissionCode) {
        Set<String> granted = sessionGranted.get(sessionId);
        if (granted != null) granted.remove(permissionCode);
        log.info("[permission] session={} revoked: {}", sessionId, permissionCode);
    }

    /**
     * 撤销会话所有权限
     */
    public void revokeAll(String sessionId) {
        sessionGranted.remove(sessionId);
        log.info("[permission] session={} all revoked", sessionId);
    }

    /**
     * 查看会话已授权的权限
     */
    public Set<String> listGranted(String sessionId) {
        return sessionGranted.getOrDefault(sessionId, Collections.emptySet());
    }

    /** 权限检查结果 */
    @lombok.Data
    public static class PermissionResult {
        public boolean granted;
        public String request;

        public static PermissionResult granted() {
            PermissionResult r = new PermissionResult();
            r.granted = true;
            return r;
        }
        public static PermissionResult denied(String request) {
            PermissionResult r = new PermissionResult();
            r.granted = false;
            r.request = request;
            return r;
        }
    }
}
