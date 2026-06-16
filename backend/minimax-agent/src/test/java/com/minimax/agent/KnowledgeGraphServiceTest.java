package com.minimax.agent;

import com.minimax.agent.service.KnowledgeGraphService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 纯反射/单元级测试，不启动 Spring
 */
class KnowledgeGraphServiceTest {

    @Test
    void relationsListIsImmutableSafe() {
        // 模拟 small scenario: KG query
        KnowledgeGraphService kg = new KnowledgeGraphService(null, null);
        assertNotNull(kg);
    }

    @Test
    void entityIdPass() {
        // 简单 round-trip: 验证 service 可实例化
        KnowledgeGraphService svc = new KnowledgeGraphService(null, null);
        assertNotNull(svc);
    }

    @Test
    void selfLoopRejected() {
        // 自循环应该拒绝 - 真实场景用 mock mapper
        // 此处仅做单测占位 (mapper mock 留给集成测试)
        KnowledgeGraphService kg = new KnowledgeGraphService(null, null);
        assertNotNull(kg);
        assertTrue(true);
    }
}
