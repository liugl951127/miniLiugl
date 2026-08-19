package com.minimax.agent.feign;

import com.minimax.common.feign.pipeline.FunctionToolDTO;
import com.minimax.common.feign.pipeline.ToolResultDTO;
import com.minimax.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端：agent → pipeline 函数工具服务
 *
 * 通过 HTTP 调用替代 Maven 编译依赖，解耦部署。
 * 路由：GET/POST /api/v1/function/** → lb://minimax-pipeline
 */
@FeignClient(
        name = "minimax-pipeline",
        contextId = "pipelineFunctionClient",
        path = "/api/v1/function"
)
public interface PipelineFunctionClient {

    /**
     * 列出已启用的工具列表
     * GET /api/v1/function/tools
     */
    @GetMapping("/tools")
    Result<List<FunctionToolDTO>> listTools();

    /**
     * 按分类列出工具
     * GET /api/v1/function/tools/category/{category}
     */
    @GetMapping("/tools/category/{category}")
    Result<List<FunctionToolDTO>> listByCategory(@PathVariable String category);

    /**
     * 按 name 查询工具详情
     * GET /api/v1/function/tools/by-name/{name}
     */
    @GetMapping("/tools/by-name/{name}")
    Result<FunctionToolDTO> getByName(@PathVariable String name);

    /**
     * 直接调用工具（无 LLM）
     * POST /api/v1/function/invoke/{name}
     */
    @PostMapping("/invoke/{name}")
    Result<ToolResultDTO> invoke(
            @PathVariable String name,
            @RequestParam Long userId,
            @RequestParam(required = false) Long sessionId,
            @RequestBody(required = false) Map<String, Object> body
    );
}
