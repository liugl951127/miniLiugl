package com.minimax.pipeline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.exception.BizException;
import com.minimax.pipeline.entity.RuleDefinition;
import com.minimax.pipeline.mapper.RuleDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 规则定义服务 (T1-backend-apis / P0)
 *
 * 5 个端点:
 *   - POST   /api/v1/rule       创建
 *   - GET    /api/v1/rule       列表 (分页 + keyword)
 *   - GET    /api/v1/rule/{id}  详情
 *   - PUT    /api/v1/rule/{id}  更新
 *   - DELETE /api/v1/rule/{id}  软删 (@TableLogic)
 *
 * @since V7.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleDefinitionMapper ruleMapper;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 创建规则 (前端编辑后保存到后端)
     *
     * @param name     规则名
     * @param jsonBody JSON DSL 字符串
     * @param scope    GLOBAL/TENANT/USER
     * @param enabled  0/1
     * @param userId   创建人 (从 @RequestHeader X-User-Id 拿)
     */
    @Transactional
    public Long create(String name, String jsonBody, String scope, Integer enabled, Long userId) {
        validateJson(jsonBody);
        if (name == null || name.isBlank()) {
            throw new BizException("规则名不能为空");
        }
        RuleDefinition rule = new RuleDefinition();
        rule.setName(name.trim());
        rule.setJsonContent(jsonBody);
        rule.setScope(scope == null || scope.isBlank() ? "GLOBAL" : scope);
        rule.setEnabled(enabled == null ? 1 : (enabled == 0 ? 0 : 1));
        rule.setCreatedBy(userId);
        ruleMapper.insert(rule);
        log.info("[Rule] created id={} name='{}' by userId={}", rule.getId(), rule.getName(), userId);
        return rule.getId();
    }

    /**
     * 规则列表 (分页 + 按 name 模糊搜索)
     */
    public Page<RuleDefinition> list(int page, int size, String keyword) {
        LambdaQueryWrapper<RuleDefinition> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.like(RuleDefinition::getName, keyword.trim());
        }
        q.orderByDesc(RuleDefinition::getUpdatedAt);
        return ruleMapper.selectPage(new Page<>(page, size), q);
    }

    /**
     * 规则详情
     */
    public RuleDefinition getById(Long id) {
        RuleDefinition r = ruleMapper.selectById(id);
        if (r == null) {
            throw new BizException("规则不存在: " + id);
        }
        return r;
    }

    /**
     * 更新规则 (部分字段允许为 null 表示不更新)
     */
    @Transactional
    public void update(Long id, String name, String jsonBody, String scope, Integer enabled, Long userId) {
        RuleDefinition exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw new BizException("规则不存在: " + id);
        }
        if (jsonBody != null && !jsonBody.isBlank()) {
            validateJson(jsonBody);
            exist.setJsonContent(jsonBody);
        }
        if (name != null && !name.isBlank()) {
            exist.setName(name.trim());
        }
        if (scope != null && !scope.isBlank()) {
            exist.setScope(scope);
        }
        if (enabled != null) {
            exist.setEnabled(enabled == 0 ? 0 : 1);
        }
        // updatedAt 由 MetaObjectHandler 自动填充
        ruleMapper.updateById(exist);
        log.info("[Rule] updated id={} by userId={}", id, userId);
    }

    /**
     * 软删 (依赖 @TableLogic, 实际更新 deleted=1)
     */
    @Transactional
    public void delete(Long id, Long userId) {
        RuleDefinition exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw new BizException("规则不存在: " + id);
        }
        ruleMapper.deleteById(id);
        log.info("[Rule] soft-deleted id={} by userId={}", id, userId);
    }

    /**
     * 内部 helper: 校验 JSON 格式
     */
    private void validateJson(String s) {
        if (s == null || s.isBlank()) {
            throw new BizException("规则 JSON 不能为空");
        }
        try {
            json.readValue(s, Map.class);
        } catch (Exception e) {
            throw new BizException("规则 JSON 格式错误: " + e.getMessage());
        }
    }
}
