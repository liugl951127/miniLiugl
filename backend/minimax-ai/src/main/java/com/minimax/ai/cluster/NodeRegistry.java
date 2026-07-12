package com.minimax.ai.cluster;

import com.minimax.ai.entity.ClusterNode;
import com.minimax.ai.mapper.ClusterNodeMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 节点注册器 (V3.3.0 集群联邦)
 *
 * <p>职责:
 *   - 节点注册 (启动时调一次)
 *   - 心跳续约 (每 10s 一次, @Scheduled)
 *   - 离线检测 (最后心跳 > 30s → OFFLINE)
 *   - 节点能力查询 (按能力/负载选节点)
 *
 * <h3>心跳机制</h3>
 * <pre>
 *   节点启动 → register()
 *   每 10s → heartbeat() (上报 CPU/内存/任务数)
 *   检测: lastHeartbeat > 30s 前 → 标 OFFLINE
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeRegistry {

    private final ClusterNodeMapper mapper;

    /** 当前节点 ID (实例唯一) */
    private final String currentNodeId = "node-" + UUID.randomUUID().toString().substring(0, 8);
    /** 节点名 */
    @Value("${minimax.cluster.name:auto-detect}")
    private String nodeName;
    /** host:port */
    @Value("${minimax.cluster.address:auto}")
    private String address;
    /** 区域 */
    @Value("${minimax.cluster.region:default}")
    private String region;
    /** 能力 */
    @Value("${minimax.cluster.capabilities:llm,vlm}")
    private String capabilities;
    /** 总核心 */
    @Value("${minimax.cluster.cores:0}")
    private int cores;
    /** 总内存 MB */
    @Value("${minimax.cluster.memory:0}")
    private long memoryMb;

    /** 内存缓存: 节点列表 (避免频繁查库) */
    private volatile List<ClusterNode> cachedNodes = List.of();
    /** 缓存时间 */
    private volatile long cacheExpireAt = 0;

    /**
     * 启动时注册
     */
    @PostConstruct
    public void init() {
        // 1. 自动检测 (如果没有显式配置)
        if (cores <= 0) cores = Runtime.getRuntime().availableProcessors();
        if (memoryMb <= 0) memoryMb = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        if ("auto".equals(address)) {
            address = "localhost:" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        }
        if ("auto-detect".equals(nodeName)) {
            try { nodeName = java.net.InetAddress.getLocalHost().getHostName(); }
            catch (Exception e) { nodeName = "unknown"; }
        }
        // 2. 写入注册表
        ClusterNode n = new ClusterNode();
        n.setNodeId(currentNodeId);
        n.setName(nodeName);
        n.setAddress(address);
        n.setRegion(region);
        n.setZone("default");
        n.setCapabilities(capabilities);
        n.setTotalCores(cores);
        n.setTotalMemoryMb(memoryMb);
        n.setTotalGpus(0);
        n.setCpuUsage(0.0);
        n.setMemoryUsage(0.0);
        n.setGpuUsage(0.0);
        n.setActiveTasks(0);
        n.setStatus("ACTIVE");
        n.setIsLeader(false);
        n.setLastHeartbeat(LocalDateTime.now());
        n.setStartedAt(LocalDateTime.now());
        try {
            // 3. 启动时清掉同 nodeId 旧记录 (重启场景)
            ClusterNode old = mapper.findByNodeId(currentNodeId);
            if (old != null) {
                mapper.deleteById(old.getId());
            }
            mapper.insert(n);
            log.info("[cluster] 节点注册成功: {} ({}), {} 核, {} MB", currentNodeId, address, cores, memoryMb);
        } catch (Exception e) {
            log.warn("[cluster] 注册失败 (可能无 DB): {}", e.getMessage());
        }
    }

    /**
     * 关闭时标离线
     */
    @PreDestroy
    public void destroy() {
        try { mapper.markOffline(currentNodeId); } catch (Exception ignored) {}
        log.info("[cluster] 节点离线: {}", currentNodeId);
    }

    /**
     * 每 10s 心跳一次
     */
    @Scheduled(fixedRate = 10_000)
    public void heartbeat() {
        try {
            // 1. 实时指标
            double cpu = readCpuUsage();
            double mem = readMemoryUsage();
            int activeTasks = 0;
            // 2. 写心跳
            mapper.heartbeat(currentNodeId, cpu, mem, 0, activeTasks);
            // 3. 失效缓存
            invalidateCache();
        } catch (Exception e) {
            log.debug("[cluster] 心跳失败: {}", e.getMessage());
        }
    }

    /**
     * 每 5s 离线检测
     */
    @Scheduled(fixedRate = 5_000)
    public void detectOffline() {
        try {
            // 1. 找 ACTIVE 但心跳超时的节点
            List<ClusterNode> active = mapper.findActive();
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
            for (ClusterNode n : active) {
                if (n.getLastHeartbeat() != null && n.getLastHeartbeat().isBefore(threshold)) {
                    mapper.markOffline(n.getNodeId());
                    log.info("[cluster] 节点离线: {} (最后心跳 {})", n.getNodeId(), n.getLastHeartbeat());
                }
            }
        } catch (Exception e) {
            log.debug("[cluster] 离线检测失败: {}", e.getMessage());
        }
    }

    // ============= 查询 API =============

    public String currentNodeId() { return currentNodeId; }

    public List<ClusterNode> listAll() {
        // 缓存 5s
        if (System.currentTimeMillis() < cacheExpireAt) return cachedNodes;
        cachedNodes = mapper.selectList(null);
        cacheExpireAt = System.currentTimeMillis() + 5000;
        return cachedNodes;
    }

    public List<ClusterNode> listActive() {
        return listAll().stream().filter(n -> "ACTIVE".equals(n.getStatus())).collect(Collectors.toList());
    }

    public ClusterNode getById(String nodeId) { return mapper.findByNodeId(nodeId); }

    public void invalidateCache() { cacheExpireAt = 0; }

    /**
     * 按能力选节点 (least load)
     */
    public ClusterNode selectByCapability(String capability) {
        return listActive().stream()
                .filter(n -> n.getCapabilities() != null && n.getCapabilities().contains(capability))
                .min(Comparator.comparingDouble(n -> {
                    double load = (n.getCpuUsage() == null ? 0 : n.getCpuUsage()) * 0.5
                                + (n.getMemoryUsage() == null ? 0 : n.getMemoryUsage()) * 0.3
                                + (n.getGpuUsage() == null ? 0 : n.getGpuUsage()) * 0.2;
                    return load;
                }))
                .orElse(null);
    }

    // ============= 内部工具 =============

    private double readCpuUsage() {
        // 简化: 用 Runtime 估算
        return Math.min(1.0, Thread.activeCount() / (double) Math.max(1, cores));
    }

    private double readMemoryUsage() {
        long total = Runtime.getRuntime().maxMemory();
        long used = total - Runtime.getRuntime().freeMemory();
        return (double) used / total;
    }
}
