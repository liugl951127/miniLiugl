package com.minimax.ai.tool.builtin;

import com.minimax.ai.codegen.ProjectCodeGenerator;
import com.minimax.ai.codegen.ProjectPackager;
import com.minimax.ai.dto.CodeGenRequest;
import com.minimax.ai.dto.CodeGenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 企业级 Java 项目生成工具 (V2.8.4)
 *
 * <h3>功能</h3>
 * <ul>
 *   <li>生成完整 Spring Boot 项目源码</li>
 *   <li>打包为 ZIP (Base64 编码) 直接下载</li>
 *   <li>含: Docker / K8s / CI / CD / SQL / 运维脚本 / 文档</li>
 * </ul>
 *
 * <h3>调用示例</h3>
 * <pre>{@code
 * POST /api/ai/admin/tools/java.project.gen/invoke
 * {
 *   "projectName": "minimax-erp",
 *   "version": "1.0.0",
 *   "type": "spring-boot",
 *   "packageName": "com.minimax.erp",
 *   "database": "mysql",
 *   "features": ["auth", "rbac", "redis", "monitor"]
 * }
 *
 * → { success: true, fileCount: 60, zipBase64: "...", sizeBytes: 12345, downloadName: "minimax-erp-1.0.0.zip" }
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class JavaProjectGenTool extends AbstractSimpleTool {

    private final ProjectCodeGenerator codeGenerator;
    private final ProjectPackager packager;

    @Override
    public String getCode() { return "java.project.gen"; }

    @Override
    public String getName() { return "Java 企业项目生成"; }

    @Override
    public String getDescription() { return "生成完整可部署的 Spring Boot 项目 ZIP, 含 Docker/K8s/CI/SQL/运维脚本"; }

    @Override
    public String getCategory() { return "code"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) throws Exception {
        String projectName = strOrDefault(input, "projectName", "minimax-app");
        String version = strOrDefault(input, "version", "1.0.0");
        String packageName = strOrDefault(input, "packageName", "com.minimax." + projectName.toLowerCase().replace("-", ""));
        String type = strOrDefault(input, "type", "spring-boot");
        String database = strOrDefault(input, "database", "mysql");

        // 1. 生成项目源码
        CodeGenRequest req = new CodeGenRequest();
        req.setProjectName(projectName);
        req.setPackageName(packageName);
        req.setProjectType(type);
        req.setDatabase(database);

        CodeGenResponse resp = codeGenerator.generate(req);
        if (resp == null || resp.getFiles() == null) {
            throw new IllegalStateException("代码生成失败, 返回为空");
        }

        // 2. 打包为 ZIP
        byte[] zipBytes = packager.packageAsZip(resp, projectName, version);

        // 3. Base64 编码 (便于 JSON 传输)
        String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);

        // 4. 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectName", projectName);
        result.put("version", version);
        result.put("packageName", packageName);
        result.put("type", type);
        result.put("database", database);
        result.put("fileCount", resp.getFiles() != null ? resp.getFiles().size() : 0);
        result.put("sizeBytes", zipBytes.length);
        result.put("sizeKB", zipBytes.length / 1024);
        result.put("zipBase64", zipBase64);
        result.put("downloadName", projectName + "-" + version + ".zip");
        result.put("fileTree", buildFileTree(resp));
        result.put("category", "code");
        result.put("usage", "前端可用 window.atob 解码后 Blob 下载, 或后端直接返回 application/zip 流");

        return result;
    }

    private String strOrDefault(Map<String, Object> input, String key, String defaultVal) {
        Object v = input.get(key);
        return v != null ? v.toString() : defaultVal;
    }

    /** 构建文件树概览 (前 20 个) */
    private Object buildFileTree(CodeGenResponse resp) {
        if (resp.getFiles() == null) return java.util.List.of();
        return resp.getFiles().keySet().stream()
                .sorted()
                .limit(20)
                .toList();
    }
}
