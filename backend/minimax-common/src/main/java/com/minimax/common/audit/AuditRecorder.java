package com.minimax.common.audit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 审计记录器接口（V6.8.2）。
 *
 * <p>由 {@link com.minimax.admin.service.AuditServiceImpl} 实现，
 * 统一归档到 admin_audit_log 表。</p>
 *
 * <p>所有 Controller 在执行完操作后调用此接口记录审计日志。</p>
 *
 * @author MiniMax
 * @since V6.8.2
 */
public interface AuditRecorder {

    /**
     * 记录一次操作审计。
     *
     * @param actorId      当前操作用户 ID（可为 null 表示系统操作）
     * @param actorName    当前操作用户名（可为 null）
     * @param action       操作名称，如 CREATE / UPDATE / DELETE / LOGIN / LOGOUT
     * @param resourceType 资源类型，如 User / ApiKey / Prompt / Agent
     * @param resourceId   资源 ID（可为 null）
     * @param detail       详细信息（Map，会 JSON 序列化入库）
     * @param result       执行结果描述，如 "ok" / "error:404" / "exception:NullPointer"
     * @param errorMsg     错误信息（无错传 null）
     * @param req          HTTP 请求（从中提取 IP/UA，可传 null）
     */
    void record(Long actorId, String actorName, String action, String resourceType,
                String resourceId, Map<String, Object> detail, String result,
                String errorMsg, HttpServletRequest req);
}
