package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Marketplace 服务 (V2.9.0)
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>用户上传 Agent (definition JSON + 描述 + 标签)</li>
 *   <li>浏览市场 (按分类/评分/下载数过滤)</li>
 *   <li>详情/搜索</li>
 *   <li>评分/评论 (1-5 星)</li>
 *   <li>使用次数统计</li>
 *   <li>审核状态机 (PENDING → APPROVED → PUBLISHED)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.9.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceMapper mapper;
    private final AgentRatingMapper ratingMapper;
    private final ObjectMapper objectMapper;

    /**
     * 上传 Agent
     */
    @Transactional
    public MarketplaceAgent upload(String name, String description, String category, String icon,
                                    Long authorId, String authorName, String definitionJson,
                                    String version, String visibility, String tags, String capabilities) {
        // 1. 验证 definition JSON
        try {
            objectMapper.readTree(definitionJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Agent 定义 JSON 格式错误: " + e.getMessage());
        }
        // 2. 生成 agentKey
        String agentKey = generateKey(name, authorId);
        // 3. 默认状态
        String status = "PUBLIC".equals(visibility) ? "PENDING" : "PUBLISHED";

        MarketplaceAgent agent = MarketplaceAgent.builder()
                .agentKey(agentKey)
                .name(name)
                .description(description)
                .category(category == null ? "CUSTOM" : category)
                .icon(icon == null ? "🤖" : icon)
                .authorId(authorId)
                .authorName(authorName)
                .definitionJson(definitionJson)
                .version(version == null ? "1.0.0" : version)
                .visibility(visibility == null ? "PRIVATE" : visibility)
                .status(status)
                .usageCount(0L)
                .avgRating(0.0)
                .ratingCount(0L)
                .tags(tags)
                .capabilities(capabilities)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        mapper.insert(agent);
        log.info("[marketplace] Agent 上传 authorId={} key={} visibility={}", authorId, agentKey, visibility);
        return agent;
    }

    /**
     * 浏览市场 (已发布)
     */
    public List<MarketplaceAgent> browse(String category, String keyword, String sortBy, int limit) {
        if (limit <= 0) limit = 50;
        if (limit > 200) limit = 200;
        QueryWrapper<MarketplaceAgent> qw = new QueryWrapper<>();
        qw.eq("status", "PUBLISHED").eq("visibility", "PUBLIC");
        if (category != null && !category.isEmpty() && !"ALL".equals(category)) {
            qw.eq("category", category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("name", keyword).or().like("description", keyword));
        }
        // 排序
        if ("rating".equals(sortBy)) {
            qw.orderByDesc("avgRating");
        } else if ("usage".equals(sortBy)) {
            qw.orderByDesc("usageCount");
        } else {
            qw.orderByDesc("publishedAt");
        }
        qw.last("LIMIT " + limit);
        return mapper.selectList(qw);
    }

    /**
     * 详情
     */
    public MarketplaceAgent detail(String agentKey) {
        return mapper.selectOne(new QueryWrapper<MarketplaceAgent>().eq("agentKey", agentKey));
    }

    /**
     * 评分
     */
    @Transactional
    public void rate(String agentKey, Long userId, String username, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在 1-5 之间");
        }
        // 1. 检查 agent 存在
        MarketplaceAgent agent = detail(agentKey);
        if (agent == null) throw new IllegalArgumentException("Agent 不存在");

        // 2. 查找已评分
        AgentRating existing = ratingMapper.findUserRating(agentKey, userId);
        if (existing != null) {
            existing.setRating(rating);
            existing.setComment(comment);
            existing.setCreatedAt(LocalDateTime.now());
            ratingMapper.updateById(existing);
        } else {
            AgentRating r = AgentRating.builder()
                    .agentKey(agentKey)
                    .userId(userId)
                    .username(username)
                    .rating(rating)
                    .comment(comment)
                    .createdAt(LocalDateTime.now())
                    .build();
            ratingMapper.insert(r);
        }
        // 3. 更新聚合
        mapper.updateRatingStats(agentKey);
    }

    /**
     * 获取评分列表
     */
    public List<AgentRating> ratings(String agentKey, int limit) {
        if (limit <= 0) limit = 20;
        return ratingMapper.findByAgentKey(agentKey, limit);
    }

    /**
     * 记录使用 (增加计数)
     */
    public void recordUsage(String agentKey) {
        mapper.incrementUsage(agentKey);
    }

    /**
     * 审核
     */
    @Transactional
    public boolean approve(String agentKey, boolean approve, String reason) {
        MarketplaceAgent agent = detail(agentKey);
        if (agent == null) return false;
        String newStatus = approve ? "PUBLISHED" : "REJECTED";
        agent.setStatus(newStatus);
        agent.setUpdatedAt(LocalDateTime.now());
        if (approve) {
            agent.setPublishedAt(LocalDateTime.now());
        }
        mapper.updateById(agent);
        log.info("[marketplace] Agent 审核 key={} decision={} reason={}", agentKey, newStatus, reason);
        return true;
    }

    /**
     * 作者的 Agent
     */
    public List<MarketplaceAgent> myAgents(Long authorId) {
        return mapper.selectList(
            new QueryWrapper<MarketplaceAgent>().eq("authorId", authorId)
                .orderByDesc("createdAt"));
    }

    /**
     * 统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = mapper.selectCount(null);
        long published = mapper.selectCount(new QueryWrapper<MarketplaceAgent>().eq("status", "PUBLISHED"));
        long pending = mapper.selectCount(new QueryWrapper<MarketplaceAgent>().eq("status", "PENDING"));
        long totalUsage = mapper.selectList(null).stream()
            .mapToLong(MarketplaceAgent::getUsageCount).sum();
        result.put("total", total);
        result.put("published", published);
        result.put("pending", pending);
        result.put("totalUsage", totalUsage);
        return result;
    }

    /**
     * 生成 agentKey: 名字-slug-短hash
     */
    private String generateKey(String name, Long authorId) {
        String slug = name.toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("^-|-$", "");
        if (slug.length() > 30) slug = slug.substring(0, 30);
        int hash = Math.abs((name + authorId + System.nanoTime()).hashCode()) % 100000;
        return slug + "-" + hash;
    }
}
