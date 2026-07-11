package com.minimax.ai.controller;

import com.minimax.ai.codegen.ProjectCodeGenerator;
import com.minimax.ai.codegen.ProjectPackager;
import com.minimax.ai.dto.CodeGenRequest;
import com.minimax.ai.dto.CodeGenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 项目直接下载 Controller (V2.8.4)
 *
 * <p>浏览器直接访问即可下载 ZIP, 无需 Base64 解码.</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 *   GET /api/ai/project/download?projectName=minimax-erp&version=1.0.0&type=spring-boot
 *   → 直接返回 application/zip 流
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/project")
@RequiredArgsConstructor
@Tag(name = "项目下载", description = "直接下载生成的项目 ZIP")
public class ProjectDownloadController {

    private final ProjectCodeGenerator codeGenerator;
    private final ProjectPackager packager;

    @GetMapping("/download")
    @Operation(summary = "下载生成的项目 ZIP")
    public ResponseEntity<byte[]> downloadProject(
            @RequestParam(defaultValue = "minimax-app") String projectName,
            @RequestParam(defaultValue = "1.0.0") String version,
            @RequestParam(defaultValue = "spring-boot") String type,
            @RequestParam(defaultValue = "com.minimax.app") String packageName,
            @RequestParam(defaultValue = "mysql") String database) {

        try {
            log.info("[download] project={} version={} type={}", projectName, version, type);

            // 1. 生成代码
            CodeGenRequest req = new CodeGenRequest();
            req.setProjectName(projectName);
            req.setPackageName(packageName);
            req.setProjectType(type);
            req.setDatabase(database);

            CodeGenResponse resp = codeGenerator.generate(req);

            // 2. 打包
            byte[] zipBytes = packager.packageAsZip(resp, projectName, version);

            // 3. 返回 ZIP 流
            String filename = URLEncoder.encode(projectName + "-" + version + ".zip", StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(zipBytes.length);
            headers.add("X-Project-Name", projectName);
            headers.add("X-Project-Version", version);
            headers.add("X-File-Count", String.valueOf(resp.getFiles() != null ? resp.getFiles().size() : 0));

            return ResponseEntity.ok().headers(headers).body(zipBytes);
        } catch (Exception e) {
            log.error("[download] failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST 形式 (参数多时用)
     */
    @PostMapping("/download")
    @Operation(summary = "下载 (POST)")
    public ResponseEntity<byte[]> downloadProjectPost(@RequestBody Map<String, Object> params) {
        return downloadProject(
                strOr(params, "projectName", "minimax-app"),
                strOr(params, "version", "1.0.0"),
                strOr(params, "type", "spring-boot"),
                strOr(params, "packageName", "com.minimax.app"),
                strOr(params, "database", "mysql"));
    }

    private String strOr(Map<String, Object> m, String k, String d) {
        Object v = m.get(k);
        return v == null ? d : v.toString();
    }
}
