package com.minimax.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimax.ai.datasource.MultiDataSourceManager;
import com.minimax.ai.dto.CodeGenRequest;
import com.minimax.ai.dto.CodeGenResponse;
import com.minimax.ai.codegen.ProjectCodeGenerator;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.mapper.AiToolMapper;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.tool.AiToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI 工具管理 + 数据源管理 (V2.5 企业级)
 *
 * 工具管理:
 *   GET    /api/ai/admin/tools              列表
 *   GET    /api/ai/admin/tools/{code}       详情
 *   POST   /api/ai/admin/tools              新增
 *   PUT    /api/ai/admin/tools/{id}         修改
 *   DELETE /api/ai/admin/tools/{id}         删除
 *   POST   /api/ai/admin/tools/{code}/invoke  调用
 *
 * 数据源管理:
 *   GET    /api/ai/admin/datasources         列表
 *   POST   /api/ai/admin/datasources         新增
 *   PUT    /api/ai/admin/datasources/{id}    修改
 *   DELETE /api/ai/admin/datasources/{id}    删除
 *   POST   /api/ai/admin/datasources/{id}/test  测试连接
 *
 * 代码生成:
 *   POST   /api/ai/codegen                   生成项目
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/admin")
@RequiredArgsConstructor
@Tag(name = "AI 工具管理", description = "AI 工具注册 / 数据源管理 / 代码生成")
public class AiToolAdminController {

    private final AiToolMapper toolMapper;
    private final DataSourceMapper dataSourceMapper;
    private final AiToolRegistry registry;
    private final MultiDataSourceManager dsManager;
    private final ProjectCodeGenerator codeGenerator;

    // ============== AI 工具管理 ==============

    @GetMapping("/tools")
    @Operation(summary = "工具列表")
    public Map<String, Object> listTools(@RequestParam(required = false) String category) {
        QueryWrapper<AiTool> qw = new QueryWrapper<>();
        if (category != null) qw.eq("category", category);
        qw.orderByDesc("builtin").orderByAsc("category", "id");
        List<AiTool> tools = toolMapper.selectList(qw);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", tools.size());
        result.put("list", tools);
        return result;
    }

    @GetMapping("/tools/{code}")
    @Operation(summary = "工具详情")
    public AiTool getTool(@PathVariable String code) {
        return toolMapper.selectOne(new QueryWrapper<AiTool>().eq("code", code));
    }

    @PostMapping("/tools")
    @Operation(summary = "注册新工具")
    public AiTool createTool(@RequestBody AiTool tool) {
        tool.setId(null);
        tool.setBuiltin(0);
        if (tool.getEnabled() == null) tool.setEnabled(1);
        toolMapper.insert(tool);
        return tool;
    }

    @PutMapping("/tools/{id}")
    @Operation(summary = "更新工具")
    public AiTool updateTool(@PathVariable Long id, @RequestBody AiTool tool) {
        tool.setId(id);
        toolMapper.updateById(tool);
        return tool;
    }

    @DeleteMapping("/tools/{id}")
    @Operation(summary = "删除工具 (内置不可删)")
    public Map<String, Object> deleteTool(@PathVariable Long id) {
        AiTool tool = toolMapper.selectById(id);
        Map<String, Object> result = new LinkedHashMap<>();
        if (tool == null) {
            result.put("success", false);
            result.put("message", "工具不存在");
            return result;
        }
        if (tool.getBuiltin() != null && tool.getBuiltin() == 1) {
            result.put("success", false);
            result.put("message", "内置工具不能删除, 可禁用");
            return result;
        }
        toolMapper.deleteById(id);
        result.put("success", true);
        return result;
    }

    @PostMapping("/tools/{code}/invoke")
    @Operation(summary = "调用工具")
    public AiToolRegistry.ToolResult invokeTool(@PathVariable String code,
                                                 @RequestBody Map<String, Object> input) {
        Long userId = null, dsId = null;
        if (input.containsKey("dataSourceId")) {
            dsId = ((Number) input.get("dataSourceId")).longValue();
        }
        return registry.invoke(code, input, userId, null, dsId);
    }

    // ============== 数据源管理 ==============

    @GetMapping("/datasources")
    @Operation(summary = "数据源列表")
    public Map<String, Object> listDataSources() {
        List<DbDataSource> list = dataSourceMapper.selectList(
                new QueryWrapper<DbDataSource>().orderByDesc("id"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        // 隐藏密码
        list.forEach(ds -> ds.setPassword("******"));
        result.put("list", list);
        return result;
    }

    @PostMapping("/datasources")
    @Operation(summary = "新增数据源")
    public DbDataSource createDataSource(@RequestBody DbDataSource ds) {
        ds.setId(null);
        if (ds.getEnabled() == null) ds.setEnabled(1);
        if (ds.getPoolSize() == null) ds.setPoolSize(10);
        if (ds.getMinIdle() == null) ds.setMinIdle(2);
        if (ds.getMaxLifetime() == null) ds.setMaxLifetime(1800);
        ds.setTestStatus("UNKNOWN");
        dataSourceMapper.insert(ds);
        return ds;
    }

    @PutMapping("/datasources/{id}")
    @Operation(summary = "更新数据源")
    public DbDataSource updateDataSource(@PathVariable Long id, @RequestBody DbDataSource ds) {
        ds.setId(id);
        dataSourceMapper.updateById(ds);
        // 关闭旧连接
        dsManager.close(id);
        return ds;
    }

    @DeleteMapping("/datasources/{id}")
    @Operation(summary = "删除数据源")
    public Map<String, Object> deleteDataSource(@PathVariable Long id) {
        dsManager.close(id);
        dataSourceMapper.deleteById(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/datasources/{id}/test")
    @Operation(summary = "测试连接")
    public Map<String, Object> testDataSource(@PathVariable Long id) {
        DbDataSource ds = dataSourceMapper.selectById(id);
        Map<String, Object> result = new LinkedHashMap<>();
        if (ds == null) {
            result.put("success", false);
            result.put("message", "数据源不存在");
            return result;
        }
        MultiDataSourceManager.TestResult test = dsManager.testConnection(ds);

        // 更新状态
        DbDataSource update = new DbDataSource();
        update.setId(id);
        update.setTestStatus(test.success ? "OK" : "FAILED");
        update.setTestMessage(test.message);
        update.setLastTestAt(java.time.LocalDateTime.now());
        dataSourceMapper.updateById(update);

        result.put("success", test.success);
        result.put("message", test.message);
        result.put("driverAvailable", dsManager.isDriverAvailable(ds.getType()));
        return result;
    }

    // ============== 代码生成 ==============

    @PostMapping("/codegen")
    @Operation(summary = "项目代码生成")
    public CodeGenResponse generateCode(@RequestBody CodeGenRequest request) {
        return codeGenerator.generate(request);
    }
}
