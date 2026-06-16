package com.minimax.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.rag.entity.KnowledgeBase;
import com.minimax.rag.mapper.KnowledgeBaseMapper;
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
        // 校验唯一
        KnowledgeBase exist = mapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getOwnerId, ownerId)
                        .eq(KnowledgeBase::getName, name)
                        .last("LIMIT 1"));
        if (exist != null) {
            throw new IllegalArgumentException("已存在同名知识库: " + name);
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setOwnerId(ownerId);
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
        KnowledgeBase kb = mapper.selectById(id);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) return null;
        // 私有: 必须 owner 是自己; 公开: 所有人可看
        if ("private".equals(kb.getVisibility()) && !kb.getOwnerId().equals(ownerId)) {
            return null;
        }
        return kb;
    }

    public List<KnowledgeBase> listByOwner(Long ownerId) {
        return mapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getOwnerId, ownerId)
                        .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    public List<KnowledgeBase> listPublic() {
        return mapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getVisibility, "public")
                        .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    public boolean delete(Long id, Long ownerId) {
        KnowledgeBase kb = mapper.selectById(id);
        if (kb == null) return false;
        if (!kb.getOwnerId().equals(ownerId)) return false;
        mapper.deleteById(id);
        return true;
    }

    public void incDocCount(Long id, int delta) {
        if (delta != 0) mapper.incDocCount(id, delta);
    }

    public void incChunkCount(Long id, int delta) {
        if (delta != 0) mapper.incChunkCount(id, delta);
    }
}
