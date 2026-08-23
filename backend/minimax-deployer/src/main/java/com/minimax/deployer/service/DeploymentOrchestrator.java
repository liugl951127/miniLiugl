package com.minimax.deployer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 部署编排服务 (V2.0)
 *
 * 协调整个部署流程:
 *  1. 镜像构建 (模拟 30s)
 *  2. 镜像推送 (模拟 15s)
 *  3. 集群部署 (模拟 45s)
 *  4. 健康检查 (模拟 10s)
 *  5. 流量接入 (模拟 5s)
 *
 * 通过 SSE (Server-Sent Events) 实时推送部署状态给前端。
 *
 * 当前为模拟实现, 生产环境需要:
 *  - 镜像构建: docker build / Kaniko
 *  - 镜像推送: docker push / 阿里云 ACR
 *  - 集群部署: kubectl apply / Helm / ArgoCD
 *  - 健康检查: HTTP probe / kubectl wait
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentOrchestrator {

    private final ForgeDeploymentMapper deploymentMapper;
    private final ForgeReleaseMapper releaseMapper;
    private final ManifestGeneratorService manifestGenerator;
    private final ObjectMapper objectMapper;

    /** SSE 推送器: deploymentId -> List<SseEmitter> */
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** 部署阶段定义 */
    private static final List<Map<String, Object>> STAGE_TEMPLATE = List.of(
        Map.of("name", "代码校验",      "desc", "检查智能体配置合法性",  "duration", 2),
        Map.of("name", "构建镜像",      "desc", "Docker build, N 个镜像并行", "duration", 35),
        Map.of("name", "镜像推送",      "desc", "推送到镜像仓库",          "duration", 22),
        Map.of("name", "创建命名空间",   "desc", "namespace 创建",            "duration", 2),
        Map.of("name", "应用配置",      "desc", "ConfigMap / Secret",       "duration", 5),
        Map.of("name", "部署 Pod",      "desc", "Deployment 创建",           "duration", 30),
        Map.of("name", "健康检查",      "desc", "Liveness / Readiness",     "duration", 10),
        Map.of("name", "流量接入",      "desc", "Service / Ingress",         "duration", 5)
    );

    /**
     * 注册 SSE 监听器
     */
    public SseEmitter subscribe(Long deploymentId) {
        SseEmitter emitter = new SseEmitter(0L);  // 永不超时
        emitters.computeIfAbsent(deploymentId, k -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(deploymentId, emitter));
        emitter.onTimeout(() -> removeEmitter(deploymentId, emitter));
        emitter.onError(e -> removeEmitter(deploymentId, emitter));
        return emitter;
    }

    private void removeEmitter(Long deploymentId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(deploymentId);
        if (list != null) list.remove(emitter);
    }

    /**
     * 启动部署 (异步)
     */
    @Async
    public void startDeployment(Long releaseId) {
        log.info("[Deploy] 开始部署 release={}", releaseId);

        ForgeRelease release = releaseMapper.selectById(releaseId);
        if (release == null) {
            log.error("[Deploy] release {} 不存在", releaseId);
            return;
        }

        // 1. 创建部署实例
        ForgeDeployment deployment = ForgeDeployment.builder()
            .releaseId(releaseId)
            .instanceName("deploy-" + System.currentTimeMillis())
            .stages("[]")
            .logs("")
            .status("PENDING")
            .target(release.getDeployTarget())
            .namespace("agent-forge")
            .desiredReplicas(release.getReplicas() != null ? release.getReplicas() : 2)
            .runningReplicas(0)
            .startedAt(LocalDateTime.now())
            .build();
        deploymentMapper.insert(deployment);

        // 2. 更新 release 状态
        release.setStatus("DEPLOYING");
        releaseMapper.updateById(release);

        // 3. 异步执行部署流程
        executeStages(deployment.getId(), deployment.getDesiredReplicas());
    }

    private void executeStages(Long deploymentId, Integer desiredReplicas) {
        List<Map<String, Object>> stages = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (int i = 0; i < STAGE_TEMPLATE.size(); i++) {
            Map<String, Object> stageTemplate = STAGE_TEMPLATE.get(i);
            Map<String, Object> stage = new LinkedHashMap<>(stageTemplate);
            stage.put("status", "running");
            stage.put("index", i);
            stage.put("duration", 0);
            stages.add(stage);

            // 推送阶段开始
            pushUpdate(deploymentId, "stage_start", Map.of(
                "stage", stage, "index", i, "stages", stages
            ));

            // 模拟阶段执行
            int duration = ((Number) stageTemplate.get("duration")).intValue();
            for (int t = 0; t < duration; t++) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                addLog(logs, "INFO", String.format("[%s] 进行中... %ds/%ds",
                    stageTemplate.get("name"), t + 1, duration));
                pushUpdate(deploymentId, "log", Map.of(
                    "time", formatTime(), "level", "INFO",
                    "text", logs.get(logs.size() - 1)
                ));
            }

            // 完成本阶段
            stage.put("status", "done");
            stage.put("duration", duration);
            addLog(logs, "INFO", "✅ 阶段完成: " + stageTemplate.get("name"));
            pushUpdate(deploymentId, "stage_done", Map.of(
                "stage", stage, "stages", stages
            ));
        }

        // 4. 全部完成
        long totalDuration = (System.currentTimeMillis() - start) / 1000;
        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deploymentId)
            .set("status", "RUNNING")
            .set("running_replicas", desiredReplicas)
            .set("finished_at", LocalDateTime.now())
            .set("stages", toJson(stages))
            .set("logs", String.join("\n", logs)));

        // 5. 更新 release 状态为 ACTIVE
        ForgeDeployment deploy = deploymentMapper.selectById(deploymentId);
        releaseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeRelease>()
            .eq("id", deploy.getReleaseId())
            .set("status", "ACTIVE")
            .set("deployed_at", LocalDateTime.now())
            .set("deploy_duration", (int) totalDuration));

        pushUpdate(deploymentId, "done", Map.of(
            "status", "RUNNING",
            "duration", totalDuration,
            "message", "部署完成"
        ));

        log.info("[Deploy] release={} 部署完成, duration={}s", deploy.getReleaseId(), totalDuration);
    }

    private void pushUpdate(Long deploymentId, String event, Object data) {
        List<SseEmitter> list = emitters.get(deploymentId);
        if (list == null) return;
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name(event).data(data));
            } catch (IOException ex) {
                removeEmitter(deploymentId, e);
            }
        }
    }

    private void addLog(List<String> logs, String level, String text) {
        logs.add(String.format("[%s] [%s] %s", formatTime(), level, text));
    }

    private String formatTime() {
        return LocalDateTime.now().toString().substring(11, 19);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    /**
     * 回滚到指定 release
     */
    public void rollback(Long currentReleaseId, Long targetReleaseId) {
        log.info("[Rollback] 从 release={} 回滚到 release={}", currentReleaseId, targetReleaseId);

        // 1. 标记当前为 ROLLED_BACK
        releaseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeRelease>()
            .eq("id", currentReleaseId)
            .set("status", "ROLLED_BACK"));

        // 2. 重新部署目标版本
        startDeployment(targetReleaseId);
    }
}
