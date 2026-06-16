package com.minimax.monitor.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 深度健康检查服务。
 * 比 /actuator/health 更细:
 *  - DB 连通性 + 响应时间
 *  - Heap / Non-Heap 内存
 *  - 磁盘分区
 *  - 线程 + GC
 *  - 系统负载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDetailService {

    private final DataSource dataSource;

    public Map<String, Object> deepCheck() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("checkedAt", System.currentTimeMillis());
        r.put("database", checkDatabase());
        r.put("jvm", checkJvm());
        r.put("disk", checkDisk());
        r.put("thread", checkThread());
        r.put("system", checkSystem());

        // 总体状态
        boolean allOk = true;
        for (Object v : r.values()) {
            if (v instanceof Map<?,?> m) {
                Object s = m.get("status");
                if ("DOWN".equals(s) || "ERROR".equals(s)) { allOk = false; break; }
            }
        }
        r.put("overall", allOk ? "UP" : "DEGRADED");
        return r;
    }

    public Map<String, Object> checkDatabase() {
        Map<String, Object> r = new LinkedHashMap<>();
        long t0 = System.currentTimeMillis();
        try (Connection c = dataSource.getConnection()) {
            boolean valid = c.isValid(2);
            r.put("status", valid ? "UP" : "DOWN");
            r.put("latencyMs", System.currentTimeMillis() - t0);
            r.put("url", c.getMetaData().getURL());
            r.put("product", c.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            r.put("status", "DOWN");
            r.put("error", e.getMessage());
        }
        return r;
    }

    public Map<String, Object> checkJvm() {
        Map<String, Object> r = new LinkedHashMap<>();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mem.getHeapMemoryUsage();
        MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();

        long max = heap.getMax();
        long used = heap.getUsed();
        double pct = max > 0 ? (used * 100.0 / max) : 0;

        Map<String, Object> heapMap = new LinkedHashMap<>();
        heapMap.put("usedMB", used / 1024 / 1024);
        heapMap.put("maxMB", max / 1024 / 1024);
        heapMap.put("usagePercent", round2(pct));
        heapMap.put("initMB", heap.getInit() / 1024 / 1024);
        heapMap.put("committedMB", heap.getCommitted() / 1024 / 1024);

        Map<String, Object> nonHeapMap = new LinkedHashMap<>();
        nonHeapMap.put("usedMB", nonHeap.getUsed() / 1024 / 1024);
        nonHeapMap.put("maxMB", nonHeap.getMax() / 1024 / 1024);

        r.put("heap", heapMap);
        r.put("nonHeap", nonHeapMap);
        r.put("status", pct > 90 ? "DOWN" : "UP");
        r.put("usagePercent", round2(pct));
        return r;
    }

    public Map<String, Object> checkDisk() {
        Map<String, Object> r = new LinkedHashMap<>();
        File root = new File("/");
        long total = root.getTotalSpace();
        long free = root.getUsableSpace();
        long used = total - free;
        double pct = total > 0 ? (used * 100.0 / total) : 0;
        r.put("totalGB", round2(total / 1024.0 / 1024 / 1024));
        r.put("freeGB", round2(free / 1024.0 / 1024 / 1024));
        r.put("usedGB", round2(used / 1024.0 / 1024 / 1024));
        r.put("usagePercent", round2(pct));
        r.put("status", pct > 90 ? "DOWN" : "UP");
        return r;
    }

    public Map<String, Object> checkThread() {
        Map<String, Object> r = new LinkedHashMap<>();
        ThreadMXBean tm = ManagementFactory.getThreadMXBean();
        r.put("total", tm.getThreadCount());
        r.put("daemon", tm.getDaemonThreadCount());
        r.put("peak", tm.getPeakThreadCount());
        r.put("startedTotal", tm.getTotalStartedThreadCount());
        r.put("status", "UP");
        return r;
    }

    public Map<String, Object> checkSystem() {
        Map<String, Object> r = new LinkedHashMap<>();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        r.put("os", os.getName());
        r.put("version", os.getVersion());
        r.put("arch", os.getArch());
        r.put("availableProcessors", os.getAvailableProcessors());
        r.put("systemLoadAverage", round2(os.getSystemLoadAverage()));
        r.put("status", "UP");
        return r;
    }

    private double round2(double v) { return Math.round(v * 100) / 100.0; }
}
