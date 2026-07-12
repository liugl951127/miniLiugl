package com.minimax.ai.framework.group;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 一句话自动生成智能体群 (V3.4.2) 单元测试
 */
class AutoAgentGroupGeneratorTest {

    /**
     * 测试 1: 写作意图 → 写作团队
     */
    @Test
    @DisplayName("1. '写一份季报' → 写作团队 (PIPELINE)")
    void testWriteIntent() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("写一份季报");
        assertEquals("WRITING_TEAM", group.template().name());
        assertEquals(GroupStrategy.PIPELINE, group.strategy());
        assertTrue(group.members().size() >= 3, "应至少 3 个成员");
    }

    /**
     * 测试 2: 分析意图 → 分析团队
     */
    @Test
    @DisplayName("2. '分析用户行为' → 分析团队 (SWARM)")
    void testAnalyzeIntent() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("分析用户行为");
        assertEquals("ANALYST_TEAM", group.template().name());
        assertEquals(GroupStrategy.SWARM, group.strategy());
    }

    /**
     * 测试 3: 编码意图 → 编码团队
     */
    @Test
    @DisplayName("3. '写代码' → 编码团队 (PIPELINE)")
    void testCodeIntent() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("用 Java 写代码");
        assertEquals("CODER_TEAM", group.template().name());
    }

    /**
     * 测试 4: 投票意图 → 投票委员会
     */
    @Test
    @DisplayName("4. '投票决策' → 投票委员会 (VOTE)")
    void testVoteIntent() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("投票决策选哪个");
        assertEquals("VOTE_COUNCIL", group.template().name());
        assertEquals(GroupStrategy.VOTE, group.strategy());
    }

    /**
     * 测试 5: 辩论意图 → 辩论小组
     */
    @Test
    @DisplayName("5. '对比两种方案' → 辩论小组 (DEBATE)")
    void testDebateIntent() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("对比两种方案");
        assertEquals("DEBATE_PANEL", group.template().name());
        assertEquals(GroupStrategy.DEBATE, group.strategy());
    }

    /**
     * 测试 6: 群组必含 MANAGER
     */
    @Test
    @DisplayName("6. 群组必含 MANAGER")
    void testContainsManager() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("写一份文档");
        long mgrCount = group.members().stream().filter(m -> m.getRole() == GroupRole.MANAGER).count();
        assertEquals(1, mgrCount, "群组应含 1 个 MANAGER");
    }

    /**
     * 测试 7: 群组 ID 唯一
     */
    @Test
    @DisplayName("7. 连续生成 → 不同 groupId")
    void testGroupIdUnique() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        String id1 = g.generate("写一份文档").groupId();
        String id2 = g.generate("写一份文档").groupId();
        assertNotEquals(id1, id2);
    }

    /**
     * 测试 8: 模板列表
     */
    @Test
    @DisplayName("8. 模板列表 (>=6 个)")
    void testTemplateList() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        assertTrue(g.listTemplates().size() >= 6);
    }

    /**
     * 测试 9: 直接按模板生成
     */
    @Test
    @DisplayName("9. 按模板名直接生成")
    void testGenerateFromTemplate() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generateFromTemplate("WRITING_TEAM", "测试");
        assertEquals("WRITING_TEAM", group.template().name());
    }

    /**
     * 测试 10: 未知模板抛错
     */
    @Test
    @DisplayName("10. 未知模板抛 IllegalArgumentException")
    void testUnknownTemplate() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        assertThrows(IllegalArgumentException.class,
                () -> g.generateFromTemplate("UNKNOWN", "x"));
    }

    /**
     * 测试 11: 空文本 → 默认写作团队
     */
    @Test
    @DisplayName("11. 空文本 → fallback 写作团队")
    void testEmptyFallback() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group1 = g.generate("");
        assertEquals("WRITING_TEAM", group1.template().name());
        AutoAgentGroupGenerator.GeneratedGroup group2 = g.generate(null);
        assertEquals("WRITING_TEAM", group2.template().name());
    }

    /**
     * 测试 12: 群组权重
     */
    @Test
    @DisplayName("12. 群组权重 (MANAGER=2.0)")
    void testWeights() {
        AutoAgentGroupGenerator g = new AutoAgentGroupGenerator();
        AutoAgentGroupGenerator.GeneratedGroup group = g.generate("写一份文档");
        GroupMember mgr = group.members().stream()
                .filter(m -> m.getRole() == GroupRole.MANAGER).findFirst().orElseThrow();
        assertEquals(2.0, mgr.getWeight(), 0.001, "MANAGER 权重应为 2.0");
    }
}
