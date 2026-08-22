package com.minimax.system.service;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * System 模块业务逻辑 (T-system-module).
 *
 * 目前所有数据都在内存里硬编码 (没有数据库依赖), 方便前端联调。
 * 后续如果需要持久化, 只需把这里替换成 mapper/service 调用。
 */
@Service
public class SystemService {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    // ============== 1. 左侧菜单 ==============
    public List<Map<String, Object>> getMenu() {
        List<Map<String, Object>> menu = new ArrayList<>();

        // 一级菜单: 工作台
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("name", "工作台");
        dashboard.put("path", "/dashboard");
        dashboard.put("icon", "dashboard");
        dashboard.put("children", new ArrayList<>());
        menu.add(dashboard);

        // 一级菜单: 对话
        Map<String, Object> chat = new LinkedHashMap<>();
        chat.put("name", "对话");
        chat.put("path", "/chat");
        chat.put("icon", "chat");
        List<Map<String, Object>> chatChildren = new ArrayList<>();
        chatChildren.add(menuLeaf("新对话", "/chat/new", "message"));
        chatChildren.add(menuLeaf("历史会话", "/chat/history", "history"));
        chat.put("children", chatChildren);
        menu.add(chat);

        // 一级菜单: 模型广场
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", "模型广场");
        model.put("path", "/models");
        model.put("icon", "model");
        List<Map<String, Object>> modelChildren = new ArrayList<>();
        modelChildren.add(menuLeaf("对话模型", "/models/chat", "chat"));
        modelChildren.add(menuLeaf("图像模型", "/models/image", "image"));
        modelChildren.add(menuLeaf("语音模型", "/models/audio", "audio"));
        modelChildren.add(menuLeaf("视频模型", "/models/video", "video"));
        modelChildren.add(menuLeaf("排行榜", "/models/leaderboard", "rank"));
        model.put("children", modelChildren);
        menu.add(model);

        // 一级菜单: 智能体
        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("name", "智能体");
        agent.put("path", "/agent");
        agent.put("icon", "agent");
        List<Map<String, Object>> agentChildren = new ArrayList<>();
        agentChildren.add(menuLeaf("智能体广场", "/agent/market", "market"));
        agentChildren.add(menuLeaf("工作流", "/agent/workflow", "workflow"));
        agentChildren.add(menuLeaf("知识库", "/agent/rag", "rag"));
        agent.put("children", agentChildren);
        menu.add(agent);

        // 一级菜单: 多模态
        Map<String, Object> multimodal = new LinkedHashMap<>();
        multimodal.put("name", "多模态");
        multimodal.put("path", "/multimodal");
        multimodal.put("icon", "multi");
        List<Map<String, Object>> multiChildren = new ArrayList<>();
        multiChildren.add(menuLeaf("图像生成", "/multimodal/imagegen", "image"));
        multiChildren.add(menuLeaf("语音合成", "/multimodal/audio", "audio"));
        multiChildren.add(menuLeaf("音乐生成", "/multimodal/music", "music"));
        multiChildren.add(menuLeaf("视频生成", "/multimodal/video", "video"));
        multimodal.put("children", multiChildren);
        menu.add(multimodal);

        // 一级菜单: 训练
        Map<String, Object> training = new LinkedHashMap<>();
        training.put("name", "训练");
        training.put("path", "/training");
        training.put("icon", "training");
        List<Map<String, Object>> trainingChildren = new ArrayList<>();
        trainingChildren.add(menuLeaf("训练任务", "/training/tasks", "task"));
        trainingChildren.add(menuLeaf("训练看板", "/training/dashboard", "dashboard"));
        trainingChildren.add(menuLeaf("TensorBoard", "/training/tensorboard", "tb"));
        training.put("children", trainingChildren);
        menu.add(training);

        // 一级菜单: 监控
        Map<String, Object> monitor = new LinkedHashMap<>();
        monitor.put("name", "监控");
        monitor.put("path", "/monitor");
        monitor.put("icon", "monitor");
        List<Map<String, Object>> monitorChildren = new ArrayList<>();
        monitorChildren.add(menuLeaf("服务状态", "/monitor/services", "service"));
        monitorChildren.add(menuLeaf("告警", "/monitor/alerts", "alert"));
        monitorChildren.add(menuLeaf("日志", "/monitor/logs", "log"));
        monitor.put("children", monitorChildren);
        menu.add(monitor);

        // 一级菜单: 管理
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("name", "管理");
        admin.put("path", "/admin");
        admin.put("icon", "admin");
        List<Map<String, Object>> adminChildren = new ArrayList<>();
        adminChildren.add(menuLeaf("用户管理", "/admin/users", "user"));
        adminChildren.add(menuLeaf("API Key", "/admin/apikey", "key"));
        adminChildren.add(menuLeaf("统计分析", "/admin/stats", "stats"));
        admin.put("children", adminChildren);
        menu.add(admin);

        return menu;
    }

    private Map<String, Object> menuLeaf(String name, String path, String icon) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("name", name);
        leaf.put("path", path);
        leaf.put("icon", icon);
        return leaf;
    }

    // ============== 2. 平台信息 ==============
    public Map<String, Object> getPlatformInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "MiniMax Platform");
        info.put("displayName", "MiniMax 大模型平台");
        info.put("version", "1.0.0");
        info.put("buildTime", "2026-08-22 08:32:53");
        info.put("javaVersion", System.getProperty("java.version", "17"));
        info.put("osName", System.getProperty("os.name", "linux"));
        info.put("osArch", System.getProperty("os.arch", "amd64"));
        info.put("jvmVendor", System.getProperty("java.vendor", "Unknown"));
        info.put("serverCount", 14);
        info.put("modules", new String[]{
                "minimax-common", "minimax-gateway", "minimax-ws", "minimax-auth",
                "minimax-chat", "minimax-model", "minimax-rag", "minimax-admin",
                "minimax-multimodal", "minimax-monitor", "minimax-agent",
                "minimax-analytics", "minimax-pipeline", "minimax-ai", "minimax-system"
        });
        info.put("uptime", getUptimeString());
        return info;
    }

    // ============== 3. 公告 ==============
    public List<Map<String, Object>> getAnnouncements() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> a1 = new LinkedHashMap<>();
        a1.put("id", 1L);
        a1.put("title", "MiniMax 平台 V1.0.0 正式发布");
        a1.put("content", "MiniMax 大模型平台 V1.0.0 正式上线, 集成对话/智能体/多模态/训练全流程能力。");
        a1.put("level", "info");
        a1.put("createdAt", "2026-08-22 08:00:00");
        list.add(a1);

        Map<String, Object> a2 = new LinkedHashMap<>();
        a2.put("id", 2L);
        a2.put("title", "新增 minimax-system 系统服务模块");
        a2.put("content", "新增系统通用模块 minimax-system, 提供菜单/平台信息/公告/健康检查/心跳等基础能力。");
        a2.put("level", "success");
        a2.put("createdAt", "2026-08-22 08:30:00");
        list.add(a2);

        Map<String, Object> a3 = new LinkedHashMap<>();
        a3.put("id", 3L);
        a3.put("title", "网关路由升级到 Nacos LoadBalancer");
        a3.put("content", "所有路由 uri 从硬编码 IP 改为 lb://minimax-{module}, 支持 Nacos 服务发现 + 负载均衡。");
        a3.put("level", "warning");
        a3.put("createdAt", "2026-08-21 18:00:00");
        list.add(a3);

        return list;
    }

    // ============== 4. 健康状态 ==============
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("uptime", getUptimeString());
        health.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        health.put("now", TS_FMT.format(Instant.now()));
        health.put("jvm", jvmInfo());

        List<Map<String, Object>> services = new ArrayList<>();
        services.add(serviceHealth("minimax-gateway", "UP", "7080"));
        services.add(serviceHealth("minimax-auth", "UP", "8081"));
        services.add(serviceHealth("minimax-chat", "UP", "8082"));
        services.add(serviceHealth("minimax-model", "UP", "8083"));
        services.add(serviceHealth("minimax-rag", "UP", "8084"));
        services.add(serviceHealth("minimax-admin", "UP", "8085"));
        services.add(serviceHealth("minimax-monitor", "UP", "8089"));
        services.add(serviceHealth("minimax-agent", "UP", "8090"));
        services.add(serviceHealth("minimax-analytics", "UP", "8091"));
        services.add(serviceHealth("minimax-pipeline", "UP", "8092"));
        services.add(serviceHealth("minimax-ai", "UP", "8093"));
        services.add(serviceHealth("minimax-multimodal", "UP", "8094"));
        services.add(serviceHealth("minimax-ws", "UP", "8087"));
        services.add(serviceHealth("minimax-system", "UP", "9086"));
        health.put("services", services);

        health.put("totalServices", services.size());
        health.put("healthyServices", services.size());
        return health;
    }

    private Map<String, Object> serviceHealth(String name, String status, String port) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("name", name);
        s.put("status", status);
        s.put("port", port);
        s.put("checkedAt", TS_FMT.format(Instant.now()));
        return s;
    }

    private Map<String, Object> jvmInfo() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("totalMemoryMb", rt.totalMemory() / 1024 / 1024);
        jvm.put("freeMemoryMb", rt.freeMemory() / 1024 / 1024);
        jvm.put("maxMemoryMb", rt.maxMemory() / 1024 / 1024);
        jvm.put("availableProcessors", rt.availableProcessors());
        return jvm;
    }

    private String getUptimeString() {
        long ms = ManagementFactory.getRuntimeMXBean().getUptime();
        long sec = ms / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%dh %dm %ds", h, m, s);
    }

    // ============== 5. ping/pong ==============
    public Map<String, Object> ping() {
        Map<String, Object> pong = new LinkedHashMap<>();
        pong.put("pong", true);
        pong.put("ts", System.currentTimeMillis());
        pong.put("tsHuman", TS_FMT.format(Instant.now()));
        return pong;
    }
}
