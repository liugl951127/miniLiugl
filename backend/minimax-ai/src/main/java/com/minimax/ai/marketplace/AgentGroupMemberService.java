package com.minimax.ai.marketplace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AgentGroupMember 业务层 (T1-backend-orchestrator)
 *
 * <p>对外暴露成员 CRUD:
 * <ul>
 *   <li>listByGroupId       — 按群组查全部成员 (启用 + 禁用都返回)</li>
 *   <li>listEnabledByGroupId — 仅启用成员 (编排器调用)</li>
 *   <li>addMember           — 新增成员</li>
 *   <li>updateMember        — 更新成员</li>
 *   <li>removeMember        — 删除成员</li>
 *   <li>reorder             — 批量重排 (按入参顺序重写 position)</li>
 * </ul>
 *
 * @author MiniMax
 * @since T1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentGroupMemberService {

    private final AgentGroupMemberMapper mapper;

    /**
     * 按 groupId 查全部成员 (含 disabled), 按 position ASC 排序
     */
    public List<AgentGroupMember> listByGroupId(Long groupId) {
        return mapper.findAllByGroupId(groupId);
    }

    /**
     * 按 groupId 查启用成员, 编排器使用
     */
    public List<AgentGroupMember> listEnabledByGroupId(Long groupId) {
        return mapper.findByGroupId(groupId);
    }

    /**
     * 查单个成员
     */
    public AgentGroupMember getById(Long memberId) {
        return mapper.selectById(memberId);
    }

    /**
     * 添加成员
     */
    @Transactional
    public AgentGroupMember addMember(Long groupId, AgentGroupMember body) {
        if (body == null) {
            throw new IllegalArgumentException("body 不能为空");
        }
        if (body.getAgentCode() == null || body.getAgentCode().isBlank()) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        AgentGroupMember m = new AgentGroupMember();
        m.setId(null);
        m.setGroupId(groupId);
        m.setAgentCode(body.getAgentCode());
        m.setRole(coerceRole(body.getRole()));
        m.setPosition(body.getPosition() == null ? nextPosition(groupId) : body.getPosition());
        m.setConfigJson(body.getConfigJson() == null ? "" : body.getConfigJson());
        m.setEnabled(body.getEnabled() == null ? 1 : body.getEnabled());
        mapper.insert(m);
        log.info("[group-member-svc] 新增成员: groupId={} agentCode={} role={} pos={}",
                groupId, m.getAgentCode(), m.getRole(), m.getPosition());
        return m;
    }

    /**
     * 更新成员 (按 memberId)
     */
    @Transactional
    public AgentGroupMember updateMember(Long groupId, Long memberId, AgentGroupMember body) {
        AgentGroupMember exist = mapper.selectById(memberId);
        if (exist == null || !Objects.equals(exist.getGroupId(), groupId)) {
            return null;
        }
        if (body.getAgentCode() != null && !body.getAgentCode().isBlank()) {
            exist.setAgentCode(body.getAgentCode());
        }
        if (body.getRole() != null) {
            exist.setRole(coerceRole(body.getRole()));
        }
        if (body.getPosition() != null) {
            exist.setPosition(body.getPosition());
        }
        if (body.getConfigJson() != null) {
            exist.setConfigJson(body.getConfigJson());
        }
        if (body.getEnabled() != null) {
            exist.setEnabled(body.getEnabled());
        }
        // updated_at 由 MetaObjectHandler 写 (本配置只对 createdAt 写了 fill, 这里手动兜底)
        exist.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(exist);
        log.info("[group-member-svc] 更新成员: id={} agentCode={}", exist.getId(), exist.getAgentCode());
        return exist;
    }

    /**
     * 删除成员
     */
    @Transactional
    public boolean removeMember(Long groupId, Long memberId) {
        AgentGroupMember exist = mapper.selectById(memberId);
        if (exist == null || !Objects.equals(exist.getGroupId(), groupId)) {
            return false;
        }
        mapper.deleteById(memberId);
        log.info("[group-member-svc] 删除成员: id={} agentCode={}", exist.getId(), exist.getAgentCode());
        return true;
    }

    /**
     * 重排 - 入参是 [{memberId, position}, ...]
     * 按入参顺序, 从 position=0 开始重新分配
     */
    @Transactional
    public int reorder(Long groupId, List<Map<String, Object>> orders) {
        if (orders == null || orders.isEmpty()) return 0;
        int updated = 0;
        for (int i = 0; i < orders.size(); i++) {
            Map<String, Object> o = orders.get(i);
            Object idObj = o.get("memberId");
            if (idObj == null) continue;
            Long mid = ((Number) idObj).longValue();
            AgentGroupMember exist = mapper.selectById(mid);
            if (exist == null || !Objects.equals(exist.getGroupId(), groupId)) {
                continue;
            }
            Integer pos = o.get("position") == null ? i : ((Number) o.get("position")).intValue();
            exist.setPosition(pos);
            exist.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(exist);
            updated++;
        }
        log.info("[group-member-svc] 重排完成: groupId={} 更新 {} 个", groupId, updated);
        return updated;
    }

    // ---------- helpers ----------

    private String coerceRole(String role) {
        if (role == null || role.isBlank()) return "WORKER";
        String r = role.trim().toUpperCase();
        if (!"MANAGER".equals(r) && !"WORKER".equals(r) && !"CRITIC".equals(r)) {
            throw new IllegalArgumentException("role 必须是 MANAGER | WORKER | CRITIC, 收到: " + role);
        }
        return r;
    }

    private int nextPosition(Long groupId) {
        return mapper.countByGroupId(groupId);
    }
}
