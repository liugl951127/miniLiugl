package com.minimax.admin.controller;

import com.minimax.admin.governance.GovernanceService;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 治理面板 API (V2.9.0 Admin 治理后台)
 *
 * <h3>端点</h3>
 * <pre>
 *   GET /api/v1/admin/governance/overview?from=&to=
 *   GET /api/v1/admin/governance/timeline?from=&to=
 *   GET /api/v1/admin/governance/anomalies?from=&to=
 *   GET /api/v1/admin/governance/compliance
 *   GET /api/v1/admin/governance/retention
 * </pre>
 *
 * @author MiniMax
 * @since V2.9.0
 */
@RestController
@RequestMapping("/api/v1/admin/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceService governanceService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from == null) from = LocalDateTime.now().minusDays(7);
        if (to == null) to = LocalDateTime.now();
        return Result.ok(governanceService.overview(from, to));
    }

    @GetMapping("/timeline")
    public Result<List<Map<String, Object>>> timeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from == null) from = LocalDateTime.now().minusDays(1);
        if (to == null) to = LocalDateTime.now();
        return Result.ok(governanceService.timeline(from, to));
    }

    @GetMapping("/anomalies")
    public Result<Map<String, Object>> anomalies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from == null) from = LocalDateTime.now().minusDays(7);
        if (to == null) to = LocalDateTime.now();
        return Result.ok(governanceService.anomalies(from, to));
    }

    @GetMapping("/compliance")
    public Result<Map<String, Object>> compliance() {
        return Result.ok(governanceService.compliance());
    }

    @GetMapping("/retention")
    public Result<List<Map<String, Object>>> retention() {
        return Result.ok(governanceService.retentionPolicies());
    }
}
