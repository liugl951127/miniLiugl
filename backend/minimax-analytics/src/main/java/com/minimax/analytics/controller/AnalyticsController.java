package com.minimax.analytics.controller;

import com.minimax.analytics.dto.DataSourceDTO;
import com.minimax.analytics.dto.Nl2SqlRequest;
import com.minimax.analytics.dto.QueryRequest;
import com.minimax.analytics.entity.DataSource;
import com.minimax.analytics.entity.IngestTask;
import com.minimax.analytics.entity.Nl2SqlHistory;
import com.minimax.analytics.entity.Report;
import com.minimax.analytics.service.datasource.DataSourceService;
import com.minimax.analytics.service.ingest.FileIngestService;
import com.minimax.analytics.service.nlsql.Nl2SqlService;
import com.minimax.analytics.service.query.QueryService;
import com.minimax.analytics.service.report.ReportService;
import com.minimax.analytics.service.schema.SchemaService;
import com.minimax.analytics.vo.*;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 数据智能分析 - 统一入口 (V5.31)
 *
 * 端点 18 个, 覆盖 6 大子模块:
 *   1. 数据源 (4) - CRUD + 测试连接
 *   2. Schema (3) - 列表/详情/画像
 *   3. 文件导入 (3) - 上传/状态/质量
 *   4. NL2SQL (4) - 提问/解释/反馈/历史
 *   5. SQL 执行 (2) - 执行/EXPLAIN
 *   6. 报告 (2) - 生成/详情
 */
@Tag(name = "数据智能分析 (V5.31)")
@RestController
// V1.9.1: 改为相对路径, 跟其他模块一致 (gateway StripPrefix=2 后转发到 /analytics/...)
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final DataSourceService dataSourceService;
    private final SchemaService schemaService;
    private final FileIngestService ingestService;
    private final Nl2SqlService nlsqlService;
    private final QueryService queryService;
    private final ReportService reportService;

    // =================== 数据源 (4) ===================

    @Operation(summary = "新建数据源")
    @PostMapping("/datasources")
    public Result<Long> createDataSource(@AuthenticationPrincipal Long userId, @RequestBody DataSourceDTO dto) {
        return Result.ok(dataSourceService.create(userId, dto));
    }

    @Operation(summary = "数据源列表")
    @GetMapping("/datasources")
    public Result<List<DataSource>> listDataSources(@AuthenticationPrincipal Long userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(dataSourceService.listByUser(userId, page, size));
    }

    @Operation(summary = "数据源详情")
    @GetMapping("/datasources/{id}")
    public Result<DataSource> getDataSource(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return Result.ok(dataSourceService.getById(userId, id));
    }

    @Operation(summary = "测试连接 (不保存)")
    @PostMapping("/datasources/test")
    public Result<Boolean> testConnection(@RequestBody DataSourceDTO dto) {
        return Result.ok(dataSourceService.testConnection(dto));
    }

    // =================== Schema (3) ===================

    @Operation(summary = "列出数据库")
    @GetMapping("/datasources/{dsId}/databases")
    public Result<List<String>> listDatabases(@PathVariable Long dsId) {
        return Result.ok(schemaService.listDatabases(dsId));
    }

    @Operation(summary = "列出表 (支持 keyword)")
    @GetMapping("/datasources/{dsId}/databases/{db}/tables")
    public Result<List<TableInfo>> listTables(@PathVariable Long dsId, @PathVariable String db,
                                                @RequestParam(required = false) String keyword) {
        return Result.ok(schemaService.listTables(dsId, db, keyword));
    }

    @Operation(summary = "表结构详情 (列+索引+DDL+样本)")
    @GetMapping("/datasources/{dsId}/databases/{db}/tables/{table}")
    public Result<TableInfo> describeTable(@PathVariable Long dsId, @PathVariable String db, @PathVariable String table) {
        return Result.ok(schemaService.describeTable(dsId, db, table));
    }

    @Operation(summary = "数据画像 (每列空值率/分布)")
    @GetMapping("/datasources/{dsId}/databases/{db}/tables/{table}/profile")
    public Result<TableInfo> profileTable(@PathVariable Long dsId, @PathVariable String db, @PathVariable String table) {
        return Result.ok(schemaService.profileTable(dsId, db, table));
    }

    // =================== 文件导入 (3) ===================

    @Operation(summary = "上传文件 (csv/json/log)")
    @PostMapping("/ingest/upload")
    public Result<String> upload(@AuthenticationPrincipal Long userId, @RequestParam("file") MultipartFile file) {
        return Result.ok(ingestService.upload(userId, file));
    }

    @Operation(summary = "任务状态")
    @GetMapping("/ingest/tasks/{taskId}")
    public Result<IngestTask> ingestStatus(@PathVariable String taskId) {
        return Result.ok(ingestService.status(taskId));
    }

    @Operation(summary = "质量报告")
    @GetMapping("/ingest/tasks/{taskId}/quality")
    public Result<QualityReport> ingestQuality(@PathVariable String taskId) {
        return Result.ok(ingestService.quality(taskId));
    }

    // =================== NL2SQL (4) ===================

    @Operation(summary = "自然语言 → SQL")
    @PostMapping("/nlsql/ask")
    public Result<Nl2SqlResult> nlAsk(@AuthenticationPrincipal Long userId, @RequestBody Nl2SqlRequest req) {
        return Result.ok(nlsqlService.ask(userId, req));
    }

    @Operation(summary = "解释 SQL")
    @PostMapping("/nlsql/explain")
    public Result<String> nlExplain(@AuthenticationPrincipal Long userId, @RequestParam Long dataSourceId, @RequestBody String sql) {
        return Result.ok(nlsqlService.explain(userId, dataSourceId, sql));
    }

    @Operation(summary = "反馈修改 (训练样本)")
    @PostMapping("/nlsql/feedback")
    public Result<Void> nlFeedback(@AuthenticationPrincipal Long userId,
                                    @RequestParam Long historyId,
                                    @RequestParam(required = false) String correctedSql,
                                    @RequestParam(required = false) Integer rating) {
        nlsqlService.feedback(userId, historyId, correctedSql, rating);
        return Result.ok();
    }

    @Operation(summary = "NL2SQL 历史")
    @GetMapping("/nlsql/history")
    public Result<List<Nl2SqlHistory>> nlHistory(@AuthenticationPrincipal Long userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.ok(nlsqlService.history(userId, page, size));
    }

    // =================== SQL 执行 (2) ===================

    @Operation(summary = "安全执行 SELECT")
    @PostMapping("/query/execute")
    public Result<QueryResult> queryExecute(@RequestBody QueryRequest req) {
        return Result.ok(queryService.execute(req));
    }

    @Operation(summary = "EXPLAIN (仅校验不执行)")
    @PostMapping("/query/dry-run")
    public Result<QueryResult> queryDryRun(@RequestBody QueryRequest req) {
        return Result.ok(queryService.explain(req));
    }

    // =================== 报告 (2) ===================

    @Operation(summary = "生成报告")
    @PostMapping("/reports/generate")
    public Result<Report> generateReport(@AuthenticationPrincipal Long userId,
                                            @RequestParam Long dataSourceId,
                                            @RequestParam(required = false) String title,
                                            @RequestParam(required = false) String question,
                                            @RequestParam String sql,
                                            @RequestParam(required = false) Long queryRowCount,
                                            @RequestParam(required = false) Long queryDurationMs) {
        // 简化: 直接重跑 SQL 拿数据, 然后生成报告
        QueryRequest qreq = new QueryRequest();
        qreq.setDataSourceId(dataSourceId);
        qreq.setSql(sql);
        QueryResult qr = queryService.execute(qreq);
        Report r = reportService.generate(userId, dataSourceId, title, question, sql, qr);
        return Result.ok(r);
    }

    @Operation(summary = "报告详情")
    @GetMapping("/reports/{reportId}")
    public Result<Report> getReport(@AuthenticationPrincipal Long userId, @PathVariable String reportId) {
        return Result.ok(reportService.getById(userId, reportId));
    }
}
