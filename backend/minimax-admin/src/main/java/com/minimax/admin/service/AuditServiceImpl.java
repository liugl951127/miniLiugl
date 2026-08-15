package com.minimax.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.admin.entity.AdminAuditLog;
import com.minimax.admin.mapper.AdminAuditLogMapper;
import com.minimax.common.audit.AuditRecorder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审计日志服务实现（V6.8.2）。
 *
 * <p>实现 {@link AuditRecorder} 接口，统一归档到 admin_audit_log 表。</p>
 *
 * <p>记录内容: who / when / where / what / how / result</p>
 *
 * <p>使用方式:</p>
 * <ul>
 *   <li>手动调用: {@code auditRecorder.record(...)}</li>
 *   <li>AOP 自动: 在方法上加 {@code @Audited} 注解</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.8.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AdminAuditLogMapper mapper;
    private final ObjectMapper json = new ObjectMapper();

    @Override
    @Async
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
        } catch (JsonProcessingException ex) {
            e.setDetail(detail == null ? null : detail.toString());
        }
        e.setResult(result == null ? "ok" : result);
        e.setErrorMsg(errorMsg);
        if (req != null) {
            e.setIp(req.getRemoteAddr());
            e.setUserAgent(truncate(req.getHeader("User-Agent"), 250));
        }
        try {
            mapper.insert(e);
        } catch (Exception ex) {
            log.warn("[Audit] 审计写库失败 action={} actor={}: {}",
                    action, actorName, ex.getMessage());
            // DB 挂了也不抛，保证业务不受影响
        }
    }

    // ===== 查询方法（同步，不走 AOP） =====

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

    /**
     * V5.9: 按天统计（近 N 天），可选 action 过滤。
     * 返回 [{day:'2026-06-15', cnt:12}, ...] 按 day 升序。
     */
    public List<Map<String, Object>> countByDay(String since, String action) {
        return mapper.countByDay(since, action);
    }

    private String truncate(String s, int n) {
        return s == null ? null : (s.length() > n ? s.substring(0, n) : s);
    }
}
