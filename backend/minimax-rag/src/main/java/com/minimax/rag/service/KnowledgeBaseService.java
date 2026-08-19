package com.minimax.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.rag.entity.KnowledgeBase;
import com.minimax.rag.mapper.KnowledgeBaseMapper;
import com.minimax.common.tenant.TenantContext;
import com.minimax.common.tenant.TenantQueryHelper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper mapper;

    public Long create(Long ownerId, String name, String description, String visibility, String tags) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name 必填");
        if (visibility == null) visibility = "private";
        // 校验唯一（带租户过滤）
        LambdaQueryWrapper<KnowledgeBase> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(KnowledgeBase::getOwnerId, ownerId);
        checkWrapper.eq(KnowledgeBase::getName, name);
        TenantQueryHelper.applyTenantFilter(checkWrapper, KnowledgeBase::getTenantId);
        checkWrapper.last("LIMIT 1");
        KnowledgeBase exist = mapper.selectOne(checkWrapper);
        if (exist != null) {
            throw new IllegalArgumentException("已存在同名知识库: " + name);
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setOwnerId(ownerId);
        kb.setTenantId(TenantContext.currentTenantId());
        kb.setName(name);
        kb.setDescription(description);
        kb.setVisibility(visibility);
        kb.setTags(tags);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        mapper.insert(kb);
        return kb.getId();
    }

    public KnowledgeBase get(Long id, Long ownerId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getId, id);
        TenantQueryHelper.applyTenantFilter(wrapper, KnowledgeBase::getTenantId);
        wrapper.last("LIMIT 1");
        KnowledgeBase kb = mapper.selectOne(wrapper);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) return null;
        // 私有: 必须 owner 是自己; 公开: 所有人可看
        if ("private".equals(kb.getVisibility()) && !kb.getOwnerId().equals(ownerId)) {
            return null;
        }
        return kb;
    }

    public List<KnowledgeBase> listByOwner(Long ownerId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getOwnerId, ownerId);
        TenantQueryHelper.applyTenantFilter(wrapper, KnowledgeBase::getTenantId);
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return mapper.selectList(wrapper);
    }

    public List<KnowledgeBase> listPublic() {
        return mapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getVisibility, "public")
                        .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    public boolean delete(Long id, Long ownerId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getId, id);
        TenantQueryHelper.applyTenantFilter(wrapper, KnowledgeBase::getTenantId);
        wrapper.last("LIMIT 1");
        KnowledgeBase kb = mapper.selectOne(wrapper);
        if (kb == null) return false;
        if (!kb.getOwnerId().equals(ownerId)) return false;
        mapper.deleteById(id);
        return true;
    }

    /** V5.33 Day 23: 更新知识库（元数据编辑） */
    public KnowledgeBase updateKb(Long id, Long ownerId, Map<String, String> patch) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getId, id);
        TenantQueryHelper.applyTenantFilter(wrapper, KnowledgeBase::getTenantId);
        wrapper.last("LIMIT 1");
        KnowledgeBase kb = mapper.selectOne(wrapper);
        if (kb == null) throw new IllegalArgumentException("知识库不存在: " + id);
        if (!kb.getOwnerId().equals(ownerId)) throw new SecurityException("无权修改此知识库");
        if (patch.containsKey("name") && !patch.get("name").isBlank()) {
            kb.setName(patch.get("name"));
        }
        if (patch.containsKey("description")) {
            kb.setDescription(patch.get("description"));
        }
        if (patch.containsKey("visibility") && (patch.get("visibility").equals("public") || patch.get("visibility").equals("private"))) {
            kb.setVisibility(patch.get("visibility"));
        }
        if (patch.containsKey("tags")) {
            kb.setTags(patch.get("tags"));
        }
        mapper.updateById(kb);
        return kb;
    }

    public void incDocCount(Long id, int delta) {
        if (delta != 0) mapper.incDocCount(id, delta);
    }

    public void incChunkCount(Long id, int delta) {
        if (delta != 0) mapper.incChunkCount(id, delta);
    }

    /**
     * V6.8.2: 校验用户是否有权访问指定知识库.
     * 公开库任何人都能访问；私有库必须是创建者.
     * @throws SecurityException 无权访问
     */
    public void verifyAccess(Long kbId, Long userId) {
        if (kbId == null) return;  // null kbId = 跨库检索，跳过校验
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getId, kbId);
        TenantQueryHelper.applyTenantFilter(wrapper, KnowledgeBase::getTenantId);
        wrapper.last("LIMIT 1");
        KnowledgeBase kb = mapper.selectOne(wrapper);
        if (kb == null) throw new SecurityException("知识库不存在: " + kbId);
        if ("private".equals(kb.getVisibility()) && !kb.getOwnerId().equals(userId)) {
            throw new SecurityException("无权访问此知识库: " + kbId);
        }
    }
}
