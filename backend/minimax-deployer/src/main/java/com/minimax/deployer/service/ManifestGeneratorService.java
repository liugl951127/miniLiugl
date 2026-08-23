package com.minimax.deployer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manifest 生成服务 (V2.0)
 *
 * 基于 Freemarker 模板, 为每个智能体生成:
 *  1. Dockerfile        - 容器镜像构建
 *  2. K8s Deployment    - 容器编排
 *  3. K8s Service       - 服务暴露
 *  4. K8s ConfigMap     - 配置注入
 *
 * 支持 4 部署目标:
 *  - DOCKER: 仅 Dockerfile
 *  - K8S: Dockerfile + K8s manifest
 *  - CLOUD: Dockerfile + 厂商特定配置 (ACK/TKE/EKS)
 *  - EDGE: Dockerfile + 远程 SSH 部署脚本
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ManifestGeneratorService {

    private final ObjectMapper objectMapper;

    /** 解析智能体 JSON */
    public List<Map<String, Object>> parseAgents(String agentsJson) {
        try {
            return objectMapper.readValue(agentsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("parseAgents 失败, 返回空: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 解析部署配置 JSON */
    public Map<String, Object> parseDeployConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /** 生成所有 manifest (按目标) */
    public Map<String, String> generateAll(String agentsJson, String configJson, String target, String version) {
        List<Map<String, Object>> agents = parseAgents(agentsJson);
        Map<String, Object> config = parseDeployConfig(configJson);
        Map<String, String> result = new LinkedHashMap<>();

        for (Map<String, Object> agent : agents) {
            String name = (String) agent.get("name");
            String safeName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            String dockerfile = renderDockerfile(agent, version);
            result.put("dockerfile/" + safeName + ".Dockerfile", dockerfile);

            if ("K8S".equalsIgnoreCase(target) || "CLOUD".equalsIgnoreCase(target)) {
                result.put("k8s/" + safeName + "-deployment.yaml",
                    renderK8sDeployment(agent, config, version, safeName));
                result.put("k8s/" + safeName + "-service.yaml",
                    renderK8sService(agent, safeName));
                result.put("k8s/" + safeName + "-configmap.yaml",
                    renderK8sConfigMap(agent, safeName));
            }

            if ("EDGE".equalsIgnoreCase(target)) {
                result.put("edge/" + safeName + "-deploy.sh",
                    renderEdgeDeployScript(agent, config, safeName, version));
            }
        }

        // K8s Ingress (如果多 agent)
        if (agents.size() > 1 && ("K8S".equalsIgnoreCase(target) || "CLOUD".equalsIgnoreCase(target))) {
            result.put("k8s/ingress.yaml", renderK8sIngress(agents));
        }

        return result;
    }

    /** 渲染 Dockerfile */
    private String renderDockerfile(Map<String, Object> agent, String version) {
        String name = (String) agent.get("name");
        String model = (String) agent.getOrDefault("model", "Qwen2.5-7B");
        return """
            # Agent Forge 自动生成 - %s (v%s)
            FROM minimax/base-agent:v6.8 AS base

            LABEL maintainer="agent-forge@minimax.io" \\
                  version="%s" \\
                  agent.name="%s"

            # 复制智能体配置
            COPY prompts/ /app/prompts/
            COPY config/ /app/config/

            # 设置环境变量
            ENV AGENT_NAME=%s \\
                AGENT_MODEL=%s \\
                AGENT_VERSION=%s \\
                JAVA_OPTS="-Xmx512m -Xms256m"

            # 健康检查
            HEALTHCHECK --interval=30s --timeout=5s --retries=3 \\
                CMD curl -f http://localhost:8080/health || exit 1

            EXPOSE 8080
            ENTRYPOINT ["java", "-jar", "/app/agent.jar"]
            """.formatted(name, version, version, name, model, version);
    }

    /** 渲染 K8s Deployment */
    private String renderK8sDeployment(Map<String, Object> agent, Map<String, Object> config, String version, String safeName) {
        Integer replicas = (Integer) config.getOrDefault("replicas", 2);
        Integer cpu = (Integer) config.getOrDefault("cpu", 500);
        Integer memory = (Integer) config.getOrDefault("memory", 1024);
        Boolean autoscale = (Boolean) config.getOrDefault("autoscale", false);

        String hpaBlock = "";
        if (Boolean.TRUE.equals(autoscale)) {
            Integer min = (Integer) config.getOrDefault("min", 2);
            Integer max = (Integer) config.getOrDefault("max", 8);
            hpaBlock = """
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
                """.formatted(safeName, safeName, min, max);
        }

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
                      image: registry.minimax.io/agent-forge/%s:%s
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
            """.formatted(
                safeName, safeName, version,
                replicas, safeName, safeName,
                safeName, safeName, version,
                (String) agent.get("name"), (String) agent.getOrDefault("model", "Qwen2.5-7B"),
                cpu, memory, cpu * 2, memory * 2,
                hpaBlock
            );
    }

    /** 渲染 K8s Service */
    private String renderK8sService(Map<String, Object> agent, String safeName) {
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

    /** 渲染 K8s ConfigMap */
    private String renderK8sConfigMap(Map<String, Object> agent, String safeName) {
        String prompt = (String) agent.getOrDefault("prompt", "你是" + agent.get("name"));
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
            """.formatted(safeName, agent.get("name"), agent.get("role"),
                prompt.replace("\n", "\n                  "));
    }

    /** 渲染 K8s Ingress */
    private String renderK8sIngress(List<Map<String, Object>> agents) {
        StringBuilder paths = new StringBuilder();
        for (var a : agents) {
            String name = (String) a.get("name");
            String safe = name.toLowerCase().replaceAll("[^a-z0-9]", "");
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

    /** 渲染边缘部署脚本 */
    private String renderEdgeDeployScript(Map<String, Object> agent, Map<String, Object> config, String safeName, String version) {
        return """
            #!/bin/bash
            # Agent Forge 边缘部署脚本 - %s (v%s)
            set -e

            AGENT_NAME="%s"
            IMAGE="registry.minimax.io/agent-forge/%s:%s"
            CONTAINER_NAME="agent-${AGENT_NAME}"

            echo "🚀 部署 $${AGENT_NAME} 到边缘节点..."

            # 1. 拉取镜像
            docker pull $${IMAGE}

            # 2. 停止旧容器
            docker stop $${CONTAINER_NAME} 2>/dev/null || true
            docker rm $${CONTAINER_NAME} 2>/dev/null || true

            # 3. 启动新容器
            docker run -d \\
              --name $${CONTAINER_NAME} \\
              --restart unless-stopped \\
              -p 8080:8080 \\
              -e AGENT_NAME=$${AGENT_NAME} \\
              $${IMAGE}

            # 4. 健康检查
            sleep 10
            if curl -sf http://localhost:8080/health > /dev/null; then
              echo "✅ $${AGENT_NAME} 部署成功"
            else
              echo "❌ 健康检查失败, 回滚..."
              docker stop $${CONTAINER_NAME}
              exit 1
            fi
            """.formatted(agent.get("name"), version, agent.get("name"), safeName, version);
    }
}
