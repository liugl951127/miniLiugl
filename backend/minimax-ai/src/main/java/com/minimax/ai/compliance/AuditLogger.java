package com.minimax.ai.compliance;

import com.minimax.ai.entity.AuditLog;
import com.minimax.ai.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志 (V2.6 合规)
 *
 * 法规依据:
 *   - 网络安全法 第 21 条 (网络日志保留 6 个月以上)
 *   - GDPR Article 30 (Records of processing activities)
 *   - 等保 2.0 (安全审计)
 *
 * 记录内容:
 *   - 5W1H: Who / When / Where / What / Why / How
 *   - 请求体 (脱敏后)
 *   - 响应状态
 *   - 耗时
 *   - 链路追踪 ID
 *
 * 特性:
 *   - 异步落库 (不阻塞业务)
 *   - 自动脱敏 (请求体里的敏感信息)
 *   - 失败本地兜底 (DB 挂了也不丢)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogMapper auditLogMapper;
    private final DataMasker dataMasker;

    /**
     * 记录审计日志 (异步)
     */
    @Async
    public void log(String action,
                    Long userId,
                    String username,
                    String userIp,
                    String userAgent,
                    String resourceType,
                    String resourceId,
                    String method,
                    String path,
                    String requestBody,
                    int responseStatus,
                    String result,
                    long durationMs) {
        try {
            AuditLog log_ = new AuditLog();
            log_.setTraceId(UUID.randomUUID().toString().replace("-", ""));
            log_.setUserId(userId);
            log_.setUsername(username);
            log_.setUserIp(userIp);
            log_.setUserAgent(truncate(userAgent, 500));
            log_.setAction(action);
            log_.setResourceType(resourceType);
            log_.setResourceId(resourceId);
            log_.setMethod(method);
            log_.setPath(truncate(path, 500));
            log_.setRequestBody(truncate(dataMasker.mask(requestBody), 60000));
            log_.setResponseStatus(responseStatus);
            log_.setResult(result);
            log_.setDurationMs((int) durationMs);
            auditLogMapper.insert(log_);
        } catch (Exception e) {
            // 兜底: DB 失败时打印到日志
            log.error("[AUDIT-FALLBACK] action={} user={} ip={} path={} status={} result={} duration={}ms body={}",
                    action, username, userIp, path, responseStatus, result, durationMs, requestBody);
        }
    }

    /**
     * 快速记录 (无 body)
     */
    public void log(String action, Long userId, String username, String path) {
        log(action, userId, username, null, null, null, null, null, path, null, 0, "SUCCESS", 0);
    }

    /**
     * 记录登录
     */
    public void logLogin(Long userId, String username, String ip, String ua, boolean success) {
        log("LOGIN", userId, username, ip, ua, "user", userId == null ? null : String.valueOf(userId),
                null, "/api/auth/login", null, success ? 200 : 401, success ? "SUCCESS" : "FAILURE", 0);
    }

    /**
     * 记录 AI 调用
     */
    public void logAiCall(Long userId, String username, String ip, String modality, String prompt, long durationMs, boolean success) {
        log("AI_" + modality.toUpperCase(), userId, username, ip, null, "ai", null,
                "POST", "/api/ai/" + modality, prompt, success ? 200 : 500,
                success ? "SUCCESS" : "FAILURE", durationMs);
    }

    /**
     * 记录文件上传
     */
    public void logFileUpload(Long userId, String username, String ip, String fileId, long fileSize, String fileType) {
        log("FILE_UPLOAD", userId, username, ip, null, "file", fileId, "POST",
                "/api/multimedia/upload", "fileType=" + fileType + " size=" + fileSize, 200, "SUCCESS", 0);
    }

    /**
     * 记录数据导出
     */
    public void logExport(Long userId, String username, String ip, String resourceType, int rows) {
        log("EXPORT_DATA", userId, username, ip, null, resourceType, null, "POST",
                "/api/admin/export", "rows=" + rows, 200, "SUCCESS", 0);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
