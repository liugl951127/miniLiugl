package com.minimax.admin;

import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import com.minimax.admin.entity.AdminAuditLog;
import com.minimax.admin.service.AuditService;
import com.minimax.admin.service.HealthAggregator;
import com.minimax.admin.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 10 集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired AuditService audit;
    @Autowired StatsService stats;
    @Autowired HealthAggregator health;
    @Autowired ServiceClient client;
    @Autowired ServiceEndpoints endpoints;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM admin_audit_log");
    }

    @Test
    void recordAuditLog() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("User-Agent", "test");

        audit.record(1L, "admin", "create_user", "user", "100",
                Map.of("username", "alice"), "ok", null, req);

        var logs = audit.recent(10);
        assertEquals(1, logs.size());
        AdminAuditLog l = logs.get(0);
        assertEquals("create_user", l.getAction());
        assertEquals("user", l.getResourceType());
        assertEquals("100", l.getResourceId());
        assertEquals("ok", l.getResult());
        assertEquals("127.0.0.1", l.getIp());
        assertTrue(l.getDetail().contains("alice"));
    }

    @Test
    void recordAuditLogError() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        audit.record(1L, "admin", "delete_kb", "kb", "999", null, "error", "not found", req);
        var logs = audit.recent(10);
        assertEquals(1, logs.size());
        assertEquals("error", logs.get(0).getResult());
        assertEquals("not found", logs.get(0).getErrorMsg());
    }

    @Test
    void auditByActor() {
        audit.record(1L, "alice", "x", "u", null, null, "ok", null, null);
        audit.record(1L, "alice", "y", "u", null, null, "ok", null, null);
        audit.record(2L, "bob", "z", "u", null, null, "ok", null, null);

        var aliceLogs = audit.byActor(1L, 10);
        assertEquals(2, aliceLogs.size());

        var bobLogs = audit.byActor(2L, 10);
        assertEquals(1, bobLogs.size());
    }

    @Test
    void countByAction() {
        for (int i = 0; i < 3; i++) audit.record(1L, "x", "create_user", "user", null, null, "ok", null, null);
        for (int i = 0; i < 2; i++) audit.record(1L, "x", "delete_user", "user", null, null, "ok", null, null);

        var counts = audit.countByAction(null);
        assertNotNull(counts);
        // 至少 2 个 action
        assertTrue(counts.size() >= 2);
    }

    @Test
    void periodBounds() {
        Map<String, String> p = stats.periodBounds();
        assertNotNull(p.get("today"));
        assertNotNull(p.get("week7d"));
        assertNotNull(p.get("month30d"));
    }

    @Test
    void opsStats() {
        audit.record(1L, "x", "create_user", "user", null, null, "ok", null, null);
        var s = stats.opsStats();
        assertNotNull(s.get("today"));
        assertNotNull(s.get("last7d"));
        assertNotNull(s.get("last30d"));
        assertNotNull(s.get("byResourceType"));
    }

    @Test
    void healthAggregateAllDown() {
        // 配置指向不存在的端口
        endpoints.toString(); // not changed at runtime
        var agg = health.aggregate();
        assertNotNull(agg);
        // 6 个服务都 DOWN (沙箱里没启其他服务)
        assertEquals("0/6 UP", agg.get("summary"));
        assertFalse((Boolean) agg.get("allUp"));
    }

    @Test
    void errorRespMethod() {
        var r = client.errorResp("nope");
        assertEquals(1500, r.get("code"));
    }
}
