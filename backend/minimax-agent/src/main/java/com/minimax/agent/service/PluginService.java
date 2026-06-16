package com.minimax.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.agent.entity.Plugin;
import com.minimax.agent.mapper.PluginMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * V2.4: 插件市场服务
 *
 * - 列出已发布插件
 * - 用户安装/启用/禁用
 * - 评分
 * - 搜索/分类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginMapper mapper;

    public List<Plugin> listAll(String category) {
        if (category == null || category.isBlank()) {
            return mapper.selectList(
                    new LambdaQueryWrapper<Plugin>()
                            .eq(Plugin::getEnabled, 1)
                            .orderByDesc(Plugin::getDownloads));
        }
        return mapper.selectList(
                new LambdaQueryWrapper<Plugin>()
                        .eq(Plugin::getCategory, category)
                        .eq(Plugin::getEnabled, 1)
                        .orderByDesc(Plugin::getDownloads));
    }

    public List<Plugin> listByOwner(Long ownerId) {
        return mapper.selectList(
                new LambdaQueryWrapper<Plugin>()
                        .eq(Plugin::getOwnerId, ownerId)
                        .orderByDesc(Plugin::getUpdatedAt));
    }

    public Plugin get(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 发布插件 (用户提交)。
     */
    public Long publish(Long ownerId, String name, String displayName, String description,
                        String version, String author, String category,
                        String entry, String pluginType, String config) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name 必填");
        if (mapper.selectCount(
                new LambdaQueryWrapper<Plugin>().eq(Plugin::getName, name)) > 0) {
            throw new IllegalArgumentException("插件名已存在: " + name);
        }
        Plugin p = new Plugin();
        p.setName(name);
        p.setDisplayName(displayName == null ? name : displayName);
        p.setDescription(description);
        p.setVersion(version == null ? "1.0.0" : version);
        p.setAuthor(author);
        p.setCategory(category == null ? "general" : category);
        p.setScope("user");
        p.setOwnerId(ownerId);
        p.setEntry(entry);
        p.setPluginType(pluginType == null ? "class" : pluginType);
        p.setConfig(config);
        p.setEnabled(1);
        p.setDownloads(0);
        p.setRating(new java.math.BigDecimal("0.00"));
        mapper.insert(p);
        return p.getId();
    }

    public boolean incrementDownload(Long id) {
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        p.setDownloads(p.getDownloads() == null ? 1 : p.getDownloads() + 1);
        mapper.updateById(p);
        return true;
    }

    public boolean rate(Long id, double score) {
        if (score < 0 || score > 5) return false;
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        // 简化: 直接覆盖; 生产: 加权平均
        p.setRating(new java.math.BigDecimal(score));
        mapper.updateById(p);
        return true;
    }

    public boolean setEnabled(Long id, boolean enabled) {
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        p.setEnabled(enabled ? 1 : 0);
        mapper.updateById(p);
        return true;
    }

    public boolean delete(Long id, Long ownerId) {
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        if (!"user".equals(p.getScope())) return false;  // 系统插件不能删
        if (!p.getOwnerId().equals(ownerId)) return false;
        mapper.deleteById(id);
        return true;
    }
}
