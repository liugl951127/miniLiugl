package com.minimax.agent;

import com.minimax.agent.service.AgentService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentServiceTest {

    @Test
    void failResultContainsSteps() {
        AgentService.AgentResult r = AgentService.AgentResult.fail("no tools",
                java.util.List.of(), 0, java.util.Set.of());
        assertFalse(r.success());
        assertEquals(0, r.rounds());
    }

    @Test
    void okResultHoldsAnswer() {
        AgentService.AgentResult r = AgentService.AgentResult.ok("完成",
                java.util.List.of(), 3, java.util.Set.of("calculator"), 100);
        assertTrue(r.success());
        assertEquals(3, r.rounds());
        assertTrue(r.toolsUsed().contains("calculator"));
        assertEquals(100, r.durationMs());
    }
}
