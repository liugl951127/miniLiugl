package com.minimax.ai.audit;

import com.minimax.common.audit.AuditRecorder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NoOp AuditRecorder implementation for standalone mode.
 *
 * <p>AI 独立运行模式下不依赖 admin 服务的审计实现，
 * 提供空实现以满足 AuditAspect 的依赖注入。</p>
 */
@Slf4j
@Component
public class NoOpAuditRecorder implements AuditRecorder {

    @Override
    public void record(Long actorId, String actorName, String action, String resourceType,
                       String resourceId, Map<String, Object> detail, String result,
                       String errorMsg, HttpServletRequest req) {
        log.debug("[NoOpAuditRecorder] skip audit: action={}, resourceType={}, resourceId={}",
                action, resourceType, resourceId);
    }
}
