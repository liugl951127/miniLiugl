package com.minimax.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.admin.entity.AdminAuditLog;
import com.minimax.admin.mapper.AdminAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 统一操作审计。
 * 所有 admin 操作 (改密/启停用户/调限流/删 KB) 都应调用此服务记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AdminAuditLogMapper mapper;
    private final ObjectMapper json = new ObjectMapper();

    @Transactional
    public void record(Long actorId, String actorName, String action, String resourceType,
                       String resourceId, Map<String, Object> detail, String result,
                       String errorMsg, HttpServletRequest req) {
        AdminAuditLog e = new AdminAuditLog();
        e.setActorId(actorId);
        e.setActorName(actorName);
        e.setAction(action);
        e.setResourceType(resourceType);
        e.setResourceId(resourceId);
        try {
            e.setDetail(detail == null ? null : json.writeValueAsString(detail));
        } catch (Exception ex) {
            e.setDetail(detail == null ? null : detail.toString());
        }
        e.setResult(result == null ? "ok" : result);
        e.setErrorMsg(errorMsg);
        if (req != null) {
            e.setIp(req.getRemoteAddr());
            e.setUserAgent(truncate(req.getHeader("User-Agent"), 250));
        }
        try { mapper.insert(e); }
        catch (Exception ex) { log.warn("审计写库失败: {}", ex.getMessage()); }
    }

    public List<AdminAuditLog> recent(int limit) {
        if (limit <= 0 || limit > 500) limit = 50;
        return mapper.selectRecent(limit);
    }

    public List<AdminAuditLog> byActor(Long actorId, int limit) {
        if (limit <= 0 || limit > 200) limit = 20;
        return mapper.selectByActor(actorId, limit);
    }

    public List<Map<String, Object>> countByAction(String since) {
        return mapper.countByAction(since);
    }

    public List<Map<String, Object>> countByResourceType(String since) {
        return mapper.countByResourceType(since);
    }

    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
