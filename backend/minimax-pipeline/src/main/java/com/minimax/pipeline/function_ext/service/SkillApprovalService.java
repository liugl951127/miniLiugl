package com.minimax.pipeline.function_ext.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimax.pipeline.function_ext.entity.SkillApproval;
import com.minimax.pipeline.function_ext.mapper.SkillApprovalMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 审批服务 (V6.8.1+)
 *
 * HIGH/CRITICAL 工具调用需用户审批后才能执行。
 *
 * @since 2026-08-12
 */
@Slf4j
@Service
public class SkillApprovalService extends ServiceImpl<SkillApprovalMapper, SkillApproval> {

    // ==================== 提交审批 ====================

    public SkillApproval submit(String taskId, Long userId, String username,
                                String toolName, String riskLevel,
                                String goal, String toolParams) {
        SkillApproval record = new SkillApproval();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setUsername(username != null ? username : "user-" + userId);
        record.setToolName(toolName);
        record.setRiskLevel(riskLevel != null ? riskLevel : SkillApproval.RISK_HIGH);
        record.setGoal(goal);
        record.setToolParams(toolParams);
        record.setStatus(SkillApproval.STATUS_PENDING);
        baseMapper.insert(record);
        log.info("[Approval] 提交审批: taskId={} tool={} risk={} by userId={}",
                taskId, toolName, riskLevel, userId);
        return record;
    }

    // ==================== 审批 ====================

    public boolean approve(Long approvalId, Long approverId, String approverName, String reason) {
        SkillApproval record = baseMapper.selectById(approvalId);
        if (record == null) return false;
        if (!SkillApproval.STATUS_PENDING.equals(record.getStatus())) {
            log.warn("[Approval] 审批已处理: id={} status={}", approvalId, record.getStatus());
            return false;
        }
        record.setStatus(SkillApproval.STATUS_APPROVED);
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setReason(reason);
        record.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(record);
        log.info("[Approval] 审批通过: id={} tool={} by approver={}", approvalId, record.getToolName(), approverName);
        return true;
    }

    public boolean reject(Long approvalId, Long approverId, String approverName, String reason) {
        SkillApproval record = baseMapper.selectById(approvalId);
        if (record == null) return false;
        if (!SkillApproval.STATUS_PENDING.equals(record.getStatus())) {
            log.warn("[Approval] 审批已处理: id={} status={}", approvalId, record.getStatus());
            return false;
        }
        record.setStatus(SkillApproval.STATUS_REJECTED);
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setReason(reason);
        record.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(record);
        log.info("[Approval] 审批拒绝: id={} tool={} by approver={} reason={}",
                approvalId, record.getToolName(), approverName, reason);
        return true;
    }

    // ==================== 查询 ====================

    /** 查某用户的待审批列表 */
    public List<SkillApproval> getPendingByUser(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<SkillApproval>()
                .eq(SkillApproval::getUserId, userId)
                .eq(SkillApproval::getStatus, SkillApproval.STATUS_PENDING)
                .orderByDesc(SkillApproval::getCreatedAt));
    }

    /** 查所有人的待审批 (管理员) */
    public List<SkillApproval> getPendingAll() {
        return baseMapper.selectList(new LambdaQueryWrapper<SkillApproval>()
                .eq(SkillApproval::getStatus, SkillApproval.STATUS_PENDING)
                .orderByDesc(SkillApproval::getCreatedAt));
    }

    /** 按 taskId 查待审批记录 */
    public SkillApproval findPendingByTask(String taskId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SkillApproval>()
                .eq(SkillApproval::getTaskId, taskId)
                .eq(SkillApproval::getStatus, SkillApproval.STATUS_PENDING));
    }

    /** 某任务的最新审批记录 */
    public SkillApproval findLatestByTask(String taskId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SkillApproval>()
                .eq(SkillApproval::getTaskId, taskId)
                .orderByDesc(SkillApproval::getCreatedAt)
                .last("LIMIT 1"));
    }

    /** 某人所有的审批历史 */
    public List<SkillApproval> getHistoryByUser(Long userId, int page, int size) {
        return baseMapper.selectList(new LambdaQueryWrapper<SkillApproval>()
                .eq(SkillApproval::getUserId, userId)
                .orderByDesc(SkillApproval::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size));
    }
}
