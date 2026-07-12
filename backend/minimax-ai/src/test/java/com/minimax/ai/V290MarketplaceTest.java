package com.minimax.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.marketplace.AgentRating;
import com.minimax.ai.marketplace.AgentRatingMapper;
import com.minimax.ai.marketplace.MarketplaceAgent;
import com.minimax.ai.marketplace.MarketplaceMapper;
import com.minimax.ai.marketplace.MarketplaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2.9.0 Agent Marketplace 测试
 */
class V290MarketplaceTest {

    private MarketplaceService service;
    private MarketplaceMapper marketplaceMapper;
    private AgentRatingMapper ratingMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        marketplaceMapper = mock(MarketplaceMapper.class);
        ratingMapper = mock(AgentRatingMapper.class);
        objectMapper = new ObjectMapper();
        service = new MarketplaceService(marketplaceMapper, ratingMapper, objectMapper);
    }

    @Test
    void testUpload_Basic() {
        when(marketplaceMapper.insert(any())).thenReturn(1);
        String defJson = "{\"capabilities\":[\"a\",\"b\"],\"tools\":[],\"systemPrompt\":\"hi\"}";

        MarketplaceAgent agent = service.upload(
            "test agent", "desc", "CUSTOM", "🤖",
            1L, "alice", defJson, "1.0.0", "PRIVATE", "tag1,tag2", "a,b");

        assertNotNull(agent);
        assertNotNull(agent.getAgentKey());
        assertTrue(agent.getAgentKey().contains("test-agent"));
        assertEquals("test agent", agent.getName());
        assertEquals("PUBLISHED", agent.getStatus()); // PRIVATE 直接发布
        assertEquals(0L, agent.getUsageCount());
    }

    @Test
    void testUpload_Public_RequiresApproval() {
        when(marketplaceMapper.insert(any())).thenReturn(1);
        String defJson = "{\"capabilities\":[]}";

        MarketplaceAgent agent = service.upload(
            "public agent", "public", "CUSTOM", null,
            1L, "alice", defJson, null, "PUBLIC", null, null);

        assertEquals("PENDING", agent.getStatus()); // 公开需审核
        assertEquals("1.0.0", agent.getVersion()); // 默认版本
        assertEquals("🤖", agent.getIcon()); // 默认图标
    }

    @Test
    void testUpload_InvalidJson() {
        assertThrows(Exception.class, () -> {
            service.upload("bad", "d", "CUSTOM", "🤖",
                1L, "alice", "{invalid json}", "1.0.0", "PRIVATE", null, null);
        });
    }

    @Test
    void testBrowse() {
        List<MarketplaceAgent> agents = Arrays.asList(
            mkAgent("a-1", "Travel", "TRAVEL", 4.8, 100L, "PUBLISHED", "PUBLIC"),
            mkAgent("a-2", "Code", "PRODUCTIVITY", 4.5, 50L, "PUBLISHED", "PUBLIC"),
            mkAgent("a-3", "Private", "CUSTOM", 4.0, 10L, "PUBLISHED", "PRIVATE")
        );
        when(marketplaceMapper.selectList(any())).thenReturn(agents);

        List<MarketplaceAgent> result = service.browse(null, null, null, 50);
        // Mock 没过滤, 直接返回所有
        assertEquals(3, result.size());
    }

    @Test
    void testRate_NewUser() {
        MarketplaceAgent agent = mkAgent("a-1", "A", "CUSTOM", 0, 0, "PUBLISHED", "PUBLIC");
        when(marketplaceMapper.selectOne(any())).thenReturn(agent);
        when(ratingMapper.findUserRating(any(), any())).thenReturn(null);
        when(ratingMapper.insert(any())).thenReturn(1);
        when(marketplaceMapper.updateRatingStats(any())).thenReturn(1);

        service.rate("a-1", 1L, "alice", 5, "great!");

        // 验证调用了 insert
        org.mockito.Mockito.verify(ratingMapper).insert(any());
        org.mockito.Mockito.verify(marketplaceMapper).updateRatingStats("a-1");
    }

    @Test
    void testRate_UpdateExisting() {
        MarketplaceAgent agent = mkAgent("a-1", "A", "CUSTOM", 4.0, 5, "PUBLISHED", "PUBLIC");
        AgentRating existing = AgentRating.builder()
            .agentKey("a-1").userId(1L).rating(3).build();
        when(marketplaceMapper.selectOne(any())).thenReturn(agent);
        when(ratingMapper.findUserRating(any(), any())).thenReturn(existing);
        when(ratingMapper.updateById(any())).thenReturn(1);
        when(marketplaceMapper.updateRatingStats(any())).thenReturn(1);

        service.rate("a-1", 1L, "alice", 5, "better");

        org.mockito.Mockito.verify(ratingMapper).updateById(any());
    }

    @Test
    void testRate_Invalid() {
        MarketplaceAgent agent = mkAgent("a-1", "A", "CUSTOM", 0, 0, "PUBLISHED", "PUBLIC");
        when(marketplaceMapper.selectOne(any())).thenReturn(agent);
        assertThrows(IllegalArgumentException.class, () -> {
            service.rate("a-1", 1L, "alice", 6, "bad");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            service.rate("a-1", 1L, "alice", 0, "bad");
        });
    }

    @Test
    void testRate_AgentNotFound() {
        when(marketplaceMapper.selectOne(any())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> {
            service.rate("nope", 1L, "alice", 5, "");
        });
    }

    @Test
    void testApprove() {
        MarketplaceAgent agent = mkAgent("a-1", "A", "CUSTOM", 0, 0, "PENDING", "PUBLIC");
        when(marketplaceMapper.selectOne(any())).thenReturn(agent);
        when(marketplaceMapper.updateById(any())).thenReturn(1);

        boolean ok = service.approve("a-1", true, "looks good");
        assertTrue(ok);
    }

    @Test
    void testApprove_NotFound() {
        when(marketplaceMapper.selectOne(any())).thenReturn(null);
        boolean ok = service.approve("nope", true, "");
        assertFalse(ok);
    }

    @Test
    void testRecordUsage() {
        when(marketplaceMapper.incrementUsage(any())).thenReturn(1);
        service.recordUsage("a-1");
        org.mockito.Mockito.verify(marketplaceMapper).incrementUsage("a-1");
    }

    private MarketplaceAgent mkAgent(String key, String name, String cat, double rating, long usage, String status, String vis) {
        return MarketplaceAgent.builder()
            .id(1L).agentKey(key).name(name).description("d").category(cat).icon("🤖")
            .authorId(1L).authorName("alice")
            .definitionJson("{}").version("1.0.0")
            .visibility(vis).status(status)
            .usageCount(usage).avgRating(rating).ratingCount(usage > 0 ? 5L : 0L)
            .tags("t1").capabilities("c1")
            .build();
    }
}
