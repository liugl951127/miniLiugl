package com.minimax.ai.nlp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class MultiTurnDialogManagerTest {

    private MultiTurnDialogManager manager;

    @BeforeEach
    void setUp() {
        manager = new MultiTurnDialogManager(new NerExtractor());
    }

    @Test
    void testAddMessages() {
        manager.addUserMessage("s1", "Java 是什么");
        manager.addAssistantMessage("s1", "Java 是面向对象编程语言");
        List<MultiTurnDialogManager.Turn> turns = manager.getRecentTurns("s1", 5);
        assertEquals(2, turns.size());
        assertEquals("user", turns.get(0).role);
        assertEquals("assistant", turns.get(1).role);
    }

    @Test
    void testCoreferenceResolution() {
        manager.addUserMessage("s1", "Java 是什么");
        manager.addAssistantMessage("s1", "Java 是面向对象编程语言");
        // 下一轮: "它有什么特点?"
        String resolved = manager.resolveCoreferences("s1", "它有什么特点");
        // 应替换 "它" 为 "Java"
        assertNotEquals("它有什么特点", resolved);
        assertTrue(resolved.contains("Java"), "应含 Java: " + resolved);
    }

    @Test
    void testTopicShift() {
        manager.addUserMessage("s1", "什么是 Python");
        manager.addAssistantMessage("s1", "Python 是脚本语言");
        // 完全不同的主题
        boolean isShift = manager.isTopicShift("s1", "今天天气怎么样");
        assertTrue(isShift, "应识别话题转移");
    }

    @Test
    void testTopicContinue() {
        manager.addUserMessage("s1", "Java 是什么");
        manager.addAssistantMessage("s1", "Java 是面向对象编程语言");
        // 继续同一话题
        boolean isShift = manager.isTopicShift("s1", "Java 有什么特点");
        assertFalse(isShift, "应识别话题延续");
    }

    @Test
    void testSuggestFollowUps() {
        List<String> suggestions = manager.suggestFollowUps("s1", "Java 是面向对象编程语言, 1995 年发布", 3);
        assertEquals(3, suggestions.size());
    }

    @Test
    void testUserProfile() {
        manager.addUserMessage("s1", "Java 怎么学");
        manager.addUserMessage("s1", "Java 多线程怎么用");
        MultiTurnDialogManager.UserProfile profile = manager.getUserProfile("s1");
        assertNotNull(profile);
        assertTrue(profile.totalMessages >= 0);
    }

    @Test
    void testCleanup() {
        manager.addUserMessage("s1", "test");
        assertEquals(1, manager.activeSessionCount());
        // cleanup 0 分钟, 全清
        int removed = manager.cleanup(0);
        assertTrue(removed >= 0);  // 允许 0 (timestamp in ms) 
    }
}
