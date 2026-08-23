package com.minimax.deployer.gitops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * ArgoCD REST API 客户端 (V5.0)
 *
 * 真实调用 ArgoCD HTTP API:
 *  - GET  /api/v1/applications/{name}     查询 Application 状态
 *  - POST /api/v1/applications            创建 Application
 *  - POST /api/v1/applications/{name}/sync  触发同步
 *
 * 认证: Bearer Token (argocd-token)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArgoCdClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agent-forge.argocd.server:}")
    private String argoServer;

    @Value("${agent-forge.argocd.token:}")
    private String argoToken;

    @Value("${agent-forge.argocd.project:default}")
    private String project;

    /**
     * 查询 Application 状态
     */
    public ApplicationStatus queryStatus(String appName) {
        if (argoServer == null || argoServer.isBlank()) {
            throw new ArgoCdException("ArgoCD server 未配置 (agent-forge.argocd.server)");
        }
        String url = argoServer + "/api/v1/applications/" + appName;
        log.info("[ArgoCD] GET {}", url);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new ArgoCdException("ArgoCD 返回 " + resp.getStatusCode());
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode status = root.path("status");
            return new ApplicationStatus(
                appName,
                status.path("sync").path("status").asText("Unknown"),
                status.path("health").path("status").asText("Unknown"),
                status.path("sync").path("revision").asText("n/a")
            );
        } catch (org.springframework.web.client.RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ArgoCdException("ArgoCD API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 触发 Application 同步
     */
    public void triggerSync(String appName) {
        if (argoServer == null || argoServer.isBlank()) {
            throw new ArgoCdException("ArgoCD server 未配置");
        }
        String url = argoServer + "/api/v1/applications/" + appName + "/sync";
        log.info("[ArgoCD] POST {} (触发 sync)", url);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("revision", "HEAD");
            body.put("prune", true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new ArgoCdException("ArgoCD sync 失败: " + resp.getStatusCode());
            }
        } catch (org.springframework.web.client.RestClientException e) {
            throw new ArgoCdException("ArgoCD sync 异常: " + e.getMessage(), e);
        }
    }

    private HttpEntity<Void> authEntity() {
        HttpHeaders h = new HttpHeaders();
        if (argoToken != null && !argoToken.isBlank()) {
            h.setBearerAuth(argoToken);
        }
        return new HttpEntity<>(h);
    }

    public record ApplicationStatus(String name, String syncStatus, String health, String revision) {}

    public static class ArgoCdException extends RuntimeException {
        public ArgoCdException(String msg) { super(msg); }
        public ArgoCdException(String msg, Throwable cause) { super(msg, cause); }
    }
}
