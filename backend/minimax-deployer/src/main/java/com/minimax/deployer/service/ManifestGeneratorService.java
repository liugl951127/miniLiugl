package com.minimax.deployer.service;

import com.minimax.deployer.entity.ForgeAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Manifest 生成服务 (V4.1)
 *
 * V4.1 重构: 直接接收 List<ForgeAgent> 和 Map<String,Object> config,
 * 不再 parse JSON 字符串 (V4.0 那个 parseAgents/parseDeployConfig 是垃圾)
 *
 * 输入参数类型化, 调用方不再需要序列化为 JSON。
 */
@Service
@Slf4j
public class ManifestGeneratorService {

    /** 部署目标枚举 */
    public enum Target { K8S, GITOPS, DOCKER, EDGE, CLOUD }

    /**
     * 生成所有 manifest 文件
     *
     * @param agents  智能体列表 (来自 forge_agent 子表)
     * @param config  部署配置 (replicas / cpu / memory / autoscale / registry / namespace)
     * @param target  部署目标
     * @param version 版本号 (semver)
     * @return 文件路径 → 文件内容
     */
    public Map<String, String> generateAll(List<ForgeAgent> agents, Map<String, Object> config, Target target, String version) {
        Objects.requireNonNull(agents, "agents 必填");
        Objects.requireNonNull(target, "target 必填");
        Map<String, Object> cfg = config != null ? config : Map.of();

        Map<String, String> result = new LinkedHashMap<>();
        for (ForgeAgent a : agents) {
            String safeName = toSafeName(a.getName());
            result.put("dockerfile/" + safeName + ".Dockerfile", renderDockerfile(a, version));
            if (target == Target.K8S || target == Target.GITOPS || target == Target.CLOUD) {
                result.put("k8s/" + safeName + "-deployment.yaml", renderK8sDeployment(a, cfg, version, safeName));
                result.put("k8s/" + safeName + "-service.yaml", renderK8sService(safeName));
                result.put("k8s/" + safeName + "-configmap.yaml", renderK8sConfigMap(a, safeName));
            }
            if (target == Target.EDGE) {
                result.put("edge/" + safeName + "-deploy.sh", renderEdgeDeployScript(a, cfg, safeName, version));
            }
        }
        if (agents.size() > 1 && (target == Target.K8S || target == Target.GITOPS || target == Target.CLOUD)) {
            result.put("k8s/ingress.yaml", renderK8sIngress(agents));
        }
        log.info("[Manifest] 生成 {} 个 manifest 文件 (target={}, agents={})", result.size(), target, agents.size());
        return result;
    }

    public static String toSafeName(String name) {
        if (name == null) return "agent";
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String renderDockerfile(ForgeAgent a, String version) {
        return """
            # Agent Forge 自动生成 - %s (v%s)
            FROM minimax/base-agent:v6.8 AS base

            LABEL maintainer="agent-forge@minimax.io" \\
                  version="%s" \\
                  agent.name="%s"

            COPY prompts/ /app/prompts/
            COPY config/ /app/config/

            ENV AGENT_NAME=%s \\
                AGENT_MODEL=%s \\
                AGENT_VERSION=%s \\
                JAVA_OPTS="-Xmx512m -Xms256m"

            HEALTHCHECK --interval=30s --timeout=5s --retries=3 \\
                CMD curl -f http://localhost:8080/health || exit 1

            EXPOSE 8080
            ENTRYPOINT ["java", "-jar", "/app/agent.jar"]
            """.formatted(a.getName(), version, version, a.getName(), a.getName(), nullSafe(a.getModel(), "Qwen2.5-7B"), version);
    }

    private String renderK8sDeployment(ForgeAgent a, Map<String, Object> cfg, String version, String safeName) {
        int replicas = intOr(cfg.get("replicas"), 2);
        int cpu = intOr(cfg.get("cpu"), 500);
        int memory = intOr(cfg.get("memory"), 1024);
        boolean autoscale = boolOr(cfg.get("autoscale"), false);
        int min = intOr(cfg.get("min"), 2);
        int max = intOr(cfg.get("max"), 8);
        String registry = strOr(cfg.get("registry"), "registry.minimax.io/agent-forge");

        String hpaBlock = autoscale ? """
            ---
            apiVersion: autoscaling/v2
            kind: HorizontalPodAutoscaler
            metadata:
              name: %s-hpa
            spec:
              scaleTargetRef:
                apiVersion: apps/v1
                kind: Deployment
                name: %s
              minReplicas: %d
              maxReplicas: %d
              metrics:
                - type: Resource
                  resource:
                    name: cpu
                    target:
                      type: Utilization
                      averageUtilization: 70
            """.formatted(safeName, safeName, min, max) : "";

        return """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: %s
              labels:
                app: %s
                app.kubernetes.io/version: "%s"
                managed-by: agent-forge
            spec:
              replicas: %d
              selector:
                matchLabels:
                  app: %s
              template:
                metadata:
                  labels:
                    app: %s
                spec:
                  containers:
                    - name: %s
                      image: %s/%s:%s
                      ports:
                        - containerPort: 8080
                      env:
                        - name: AGENT_NAME
                          value: "%s"
                        - name: AGENT_MODEL
                          value: "%s"
                      resources:
                        requests:
                          cpu: "%dm"
                          memory: "%dMi"
                        limits:
                          cpu: "%dm"
                          memory: "%dMi"
                      livenessProbe:
                        httpGet:
                          path: /health
                          port: 8080
                        initialDelaySeconds: 30
                        periodSeconds: 10
                      readinessProbe:
                        httpGet:
                          path: /ready
                          port: 8080
                        initialDelaySeconds: 5
                        periodSeconds: 5
            %s
            """.formatted(safeName, safeName, version, replicas, safeName, safeName,
                safeName, registry, safeName, version,
                a.getName(), nullSafe(a.getModel(), "Qwen2.5-7B"),
                cpu, memory, cpu * 2, memory * 2, hpaBlock);
    }

    private String renderK8sService(String safeName) {
        return """
            apiVersion: v1
            kind: Service
            metadata:
              name: %s-svc
              labels:
                app: %s
            spec:
              selector:
                app: %s
              ports:
                - protocol: TCP
                  port: 80
                  targetPort: 8080
              type: ClusterIP
            """.formatted(safeName, safeName, safeName);
    }

    private String renderK8sConfigMap(ForgeAgent a, String safeName) {
        String prompt = a.getDescription() != null ? a.getDescription() : "你是 " + a.getName();
        return """
            apiVersion: v1
            kind: ConfigMap
            metadata:
              name: %s-config
            data:
              agent.prompt.yaml: |
                name: %s
                role: %s
                system: |
                  %s
            """.formatted(safeName, a.getName(), a.getRole(), prompt.replace("\n", "\n                  "));
    }

    private String renderK8sIngress(List<ForgeAgent> agents) {
        StringBuilder paths = new StringBuilder();
        for (var a : agents) {
            String safe = toSafeName(a.getName());
            paths.append("    - path: /").append(safe).append("\n")
                 .append("      pathType: Prefix\n")
                 .append("      backend:\n")
                 .append("        service:\n")
                 .append("          name: ").append(safe).append("-svc\n")
                 .append("          port:\n")
                 .append("            number: 80\n");
        }
        return """
            apiVersion: networking.k8s.io/v1
            kind: Ingress
            metadata:
              name: agent-forge-ingress
              annotations:
                nginx.ingress.kubernetes.io/rewrite-target: /
            spec:
              rules:
                - host: agent-forge.minimax.io
                  http:
                    paths:
            %s
            """.formatted(paths);
    }

    private String renderEdgeDeployScript(ForgeAgent a, Map<String, Object> cfg, String safeName, String version) {
        String registry = strOr(cfg.get("registry"), "registry.minimax.io/agent-forge");
        return """
            #!/bin/bash
            # Agent Forge 边缘部署脚本 - %s (v%s)
            set -e

            AGENT_NAME="%s"
            IMAGE="%s/%s:%s"
            CONTAINER_NAME="agent-${AGENT_NAME}"

            echo "🚀 部署 ${AGENT_NAME} 到边缘节点..."

            docker pull ${IMAGE}
            docker stop ${CONTAINER_NAME} 2>/dev/null || true
            docker rm ${CONTAINER_NAME} 2>/dev/null || true

            docker run -d --name ${CONTAINER_NAME} --restart unless-stopped \\
              -p 8080:8080 -e AGENT_NAME=${AGENT_NAME} ${IMAGE}

            sleep 10
            if curl -sf http://localhost:8080/health > /dev/null; then
              echo "✅ ${AGENT_NAME} 部署成功"
            else
              echo "❌ 健康检查失败, 回滚..."
              docker stop ${CONTAINER_NAME}
              exit 1
            fi
            """.formatted(a.getName(), version, a.getName(), registry, safeName, version);
    }

    // ─── 类型安全辅助 ──────────────────────────────
    private static int intOr(Object v, int def) { return v instanceof Number n ? n.intValue() : def; }
    private static boolean boolOr(Object v, boolean def) { return v instanceof Boolean b ? b : def; }
    private static String strOr(Object v, String def) { return v instanceof String s ? s : def; }
    private static String nullSafe(String s, String def) { return s != null ? s : def; }
}
