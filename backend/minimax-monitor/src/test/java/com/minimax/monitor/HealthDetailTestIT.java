package com.minimax.monitor;

import com.minimax.monitor.health.HealthDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HealthDetailTestIT {

    @Autowired HealthDetailService health;

    @Test
    void deepCheckReturnsAllSections() {
        Map<String, Object> r = health.deepCheck();
        assertNotNull(r);
        assertTrue(r.containsKey("database"));
        assertTrue(r.containsKey("jvm"));
        assertTrue(r.containsKey("disk"));
        assertTrue(r.containsKey("thread"));
        assertTrue(r.containsKey("system"));
        assertTrue(r.containsKey("overall"));
    }

    @Test
    void databaseUp() {
        Map<String, Object> r = health.checkDatabase();
        assertEquals("UP", r.get("status"));
        assertNotNull(r.get("latencyMs"));
    }

    @Test
    void jvmHasHeap() {
        Map<String, Object> r = health.checkJvm();
        assertNotNull(r.get("heap"));
        Map<?,?> heap = (Map<?,?>) r.get("heap");
        assertNotNull(heap.get("usedMB"));
        assertNotNull(heap.get("maxMB"));
    }

    @Test
    void diskHasUsagePercent() {
        Map<String, Object> r = health.checkDisk();
        assertNotNull(r.get("usagePercent"));
        assertTrue(r.get("usagePercent") instanceof Number);
    }

    @Test
    void threadCountPositive() {
        Map<String, Object> r = health.checkThread();
        long total = ((Number) r.get("total")).longValue();
        assertTrue(total > 0, "should have at least 1 thread");
    }

    @Test
    void systemHasCpu() {
        Map<String, Object> r = health.checkSystem();
        assertNotNull(r.get("availableProcessors"));
    }
}
