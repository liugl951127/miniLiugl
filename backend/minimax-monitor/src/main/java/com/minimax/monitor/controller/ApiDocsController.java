package com.minimax.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * V5.11: API 文档聚合入口.
 *
 * 路径:
 *   GET /monitor/api-docs      重定向到 /api-docs.html (static 聚合页)
 *
 * static 资源位于:
 *   backend/minimax-monitor/src/main/resources/static/api-docs.html
 *
 * 聚合页通过 iframe + tab 切换嵌入 13 个微服务的 knife4j UI:
 *   - /doc.html                      (Gateway 自身)
 *   - /api/v1/{module}/doc.html      (走 gateway :8080 → lb://minimax-{module}/doc.html)
 *
 * @since V5.11
 */
@Tag(name = "API 文档")
@Controller
public class ApiDocsController {

    @Operation(summary = "V5.11 API 文档聚合入口 (重定向到 static/api-docs.html)")
    @GetMapping("/monitor/api-docs")
    public String apiDocs() {
        // Spring 自动从 classpath:/static/api-docs.html 找
        return "forward:/api-docs.html";
    }
}
