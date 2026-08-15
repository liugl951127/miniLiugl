package com.minimax.admin.service;

import com.minimax.admin.entity.AdminAuditLog;
import com.minimax.common.audit.AuditRecorder;

import java.util.List;
import java.util.Map;

/**
 * 审计服务接口（V6.8.2）。
 *
 * <p>继承 {@link AuditRecorder}，增加查询方法。</p>
 *
 * @author MiniMax
 * @since V6.8.2
 */
public interface AuditService extends AuditRecorder {

    /**
     * 查询最近 N 条审计日志。
     *
     * @param limit 条数（1-500）
     * @return 审计日志列表
     */
    List<AdminAuditLog> recent(int limit);

    /**
     * 按操作人查询审计日志。
     *
     * @param actorId 操作人 ID
     * @param limit   条数（1-200）
     * @return 审计日志列表
     */
    List<AdminAuditLog> byActor(Long actorId, int limit);

    /**
     * 按天统计审计日志数量。
     *
     * @param since  起始日期（如 "2026-06-01"）
     * @param action 操作类型过滤（可为 null）
     * @return 按天统计结果 [{day, cnt}, ...]
     */
    List<Map<String, Object>> countByDay(String since, String action);

    /**
     * 按操作类型统计审计日志数量。
     *
     * @param since 起始日期（可为 null）
     * @return 统计结果 [{action, cnt}, ...]
     */
    List<Map<String, Object>> countByAction(String since);

    /**
     * 按资源类型统计审计日志数量。
     *
     * @param since 起始日期（可为 null）
     * @return 统计结果 [{resourceType, cnt}, ...]
     */
    List<Map<String, Object>> countByResourceType(String since);
}
