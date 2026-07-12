package com.minimax.admin;

import com.minimax.admin.entity.AuditLogFull;
import com.minimax.admin.governance.GovernanceService;
import com.minimax.admin.mapper.AuditLogFullMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2.9.0 Admin 治理后台测试
 */
class V290GovernanceTest {

    private GovernanceService service;
    private AuditLogFullMapper mapper;

    @BeforeEach
    void setup() {
        mapper = mock(AuditLogFullMapper.class);
        service = new GovernanceService(mapper);
    }

    private AuditLogFull mkLog(Long userId, String action, String resourceType, String result, String ip, int daysAgo) {
        AuditLogFull log = new AuditLogFull();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResult(result);
        log.setUserIp(ip);
        log.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return log;
    }

    @Test
    void testOverview_Basic() {
        List<AuditLogFull> logs = Arrays.asList(
            mkLog(1L, "CREATE", "USER", "SUCCESS", "192.168.1.1", 0),
            mkLog(2L, "UPDATE", "USER", "SUCCESS", "192.168.1.2", 0),
            mkLog(1L, "DELETE", "USER", "FAILED", "192.168.1.1", 0),
            mkLog(3L, "QUERY", "ORDER", "SUCCESS", "192.168.1.3", 1)
        );
        // first call: count, then list
        when(mapper.selectCount(any())).thenReturn(4L, 3L);
        when(mapper.selectList(any())).thenReturn(logs);

        Map<String, Object> result = service.overview(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertEquals(4L, result.get("totalOps"));
        assertEquals(3L, result.get("successOps"));
        assertEquals(1L, result.get("failOps"));
        assertEquals(3, result.get("uniqueUsers"));
        assertTrue(result.containsKey("topActions"));
        assertTrue(result.containsKey("resourceDistribution"));
    }

    @Test
    void testOverview_Empty() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.overview(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertEquals(0L, result.get("totalOps"));
        assertEquals(0.0, (double) result.get("failRate"), 0.001);
    }

    @Test
    void testTimeline() {
        List<AuditLogFull> logs = Arrays.asList(
            mkLog(1L, "A", "X", "SUCCESS", "1.1.1.1", 0),
            mkLog(1L, "A", "X", "FAILED", "1.1.1.1", 0),
            mkLog(2L, "B", "Y", "SUCCESS", "1.1.1.2", 0)
        );
        when(mapper.selectList(any())).thenReturn(logs);

        List<Map<String, Object>> timeline = service.timeline(
            LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertFalse(timeline.isEmpty());
        // 每个 hour bucket 至少 1 个 total
        for (Map<String, Object> point : timeline) {
            assertTrue((long) point.get("total") >= 1);
        }
    }

    @Test
    void testAnomalies_HighFailUsers() {
        List<AuditLogFull> logs = new ArrayList<>();
        // 用户 1 失败 15 次
        for (int i = 0; i < 15; i++) logs.add(mkLog(1L, "QUERY", "X", "FAILED", "1.1.1.1", 0));
        // 用户 2 失败 3 次
        for (int i = 0; i < 3; i++) logs.add(mkLog(2L, "QUERY", "X", "FAILED", "1.1.1.2", 0));

        when(mapper.selectList(any())).thenReturn(logs);

        Map<String, Object> result = service.anomalies(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> highFail = (List<Map<String, Object>>) result.get("highFailUsers");
        assertEquals(1, highFail.size());
        assertEquals(1L, highFail.get(0).get("userId"));
        assertEquals(15L, highFail.get(0).get("failCount"));
    }

    @Test
    void testAnomalies_UnauthorizedDelete() {
        List<AuditLogFull> logs = Arrays.asList(
            mkLog(1L, "DELETE_USER", "USER", "FAILED", "1.1.1.1", 0),
            mkLog(1L, "DELETE_ORDER", "ORDER", "FAILED", "1.1.1.1", 0),
            mkLog(1L, "QUERY", "X", "FAILED", "1.1.1.1", 0)
        );
        when(mapper.selectList(any())).thenReturn(logs);

        Map<String, Object> result = service.anomalies(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        // 2 次 DELETE_* 失败
        assertEquals(2L, result.get("unauthorizedDeleteAttempts"));
    }

    @Test
    void testCompliance() {
        when(mapper.selectCount(any())).thenReturn(100L, 0L, 1000L);

        Map<String, Object> result = service.compliance();

        assertEquals(5, result.get("total"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.get("checks");
        assertEquals(5, checks.size());
        // 全部 PASS (有数据时)
        assertTrue((long) result.get("pass") >= 4);
        assertTrue((double) result.get("score") > 0);
    }

    @Test
    void testRetention() {
        List<Map<String, Object>> policies = service.retentionPolicies();
        assertEquals(3, policies.size());
        for (Map<String, Object> p : policies) {
            assertNotNull(p.get("name"));
            assertNotNull(p.get("table"));
            assertNotNull(p.get("retentionDays"));
        }
    }
}
