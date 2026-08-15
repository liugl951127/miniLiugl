///**
// * @file GlobalMissingController.java - V6.3+ 全局兜底 Controller
// *
// * 接收所有前端调但后端没实现的 278 个 API 端点
// * 返回 200 + mock 空数据, 避免 404 影响前端体验
// *
// * 实际项目: 这层是过渡, 后续按模块补真实 Controller
// *
// * @author Mavis
// * @since V6.3+
// */
package com.minimax.common.web;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.*;
//import com.minimax.common.result.Result;
//
//import java.util.*;
//
//@Slf4j
//@RestController
//public class GlobalMissingController {
//
//    private Map<String, Object> mock(String path, String method) {
//        Map<String, Object> m = new LinkedHashMap<>();
//        m.put("path", path);
//        m.put("method", method);
//        m.put("mock", true);
//        m.put("timestamp", System.currentTimeMillis());
//        m.put("data", new ArrayList<>());
//        m.put("total", 0);
//        return m;
//    }
//
//    // ===== /api/v1/admin/* =====
//    @GetMapping("/api/v1/admin/users") public Result<?> adminUsers() { return Result.ok(mock("/admin/users", "GET")); }
//    @GetMapping("/api/v1/admin/models") public Result<?> adminModels() { return Result.ok(mock("/admin/models", "GET")); }
//    @GetMapping("/api/v1/admin/audit-ops/recent") public Result<?> auditRecent() { return Result.ok(mock("/admin/audit-ops/recent", "GET")); }
//    @GetMapping("/api/v1/admin/audit-ops/by-actor") public Result<?> auditByActor() { return Result.ok(mock("/admin/audit-ops/by-actor", "GET")); }
//    @GetMapping("/api/v1/admin/audit-ops/by-day") public Result<?> auditByDay() { return Result.ok(mock("/admin/audit-ops/by-day", "GET")); }
//
//    // ===== /api/v1/auth/* (微信 / 租户 / 通知) =====
//    @GetMapping("/api/v1/auth/admin/wechat/bindings") public Result<?> wechatBindings() { return Result.ok(mock("/auth/admin/wechat/bindings", "GET")); }
//    @GetMapping("/api/v1/auth/admin/wechat/find") public Result<?> wechatFind() { return Result.ok(mock("/auth/admin/wechat/find", "GET")); }
//    @PostMapping("/api/v1/auth/admin/wechat/bind") public Result<?> wechatBind(@RequestBody Map<String, Object> body) { return Result.ok(mock("/auth/admin/wechat/bind", "POST")); }
//    @DeleteMapping("/api/v1/auth/admin/wechat/bind/{userId}") public Result<?> wechatUnbind(@PathVariable String userId) { return Result.ok(mock("/auth/admin/wechat/bind/" + userId, "DELETE")); }
//    @DeleteMapping("/api/v1/auth/notifications/{id}") public Result<?> deleteNotification(@PathVariable String id) { return Result.ok(mock("/auth/notifications/" + id, "DELETE")); }
//    @DeleteMapping("/api/v1/auth/wechat/binding/me") public Result<?> wechatUnbindMe() { return Result.ok(mock("/auth/wechat/binding/me", "DELETE")); }
//    @PostMapping("/api/v1/auth/tenants/{id}/status") public Result<?> tenantStatus(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mock("/auth/tenants/" + id + "/status", "POST")); }
//    @PostMapping("/api/v1/auth/tenants/{id}/quota") public Result<?> tenantQuota(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mock("/auth/tenants/" + id + "/quota", "POST")); }
//
//    // ===== /api/v1/monitor/* =====
//    @GetMapping("/api/v1/monitor/alerts/channels") public Result<?> alertChannels() { return Result.ok(mock("/monitor/alerts/channels", "GET")); }
//    @PostMapping("/api/v1/monitor/alerts/channels") public Result<?> createChannel(@RequestBody Map<String, Object> body) { return Result.ok(mock("/monitor/alerts/channels", "POST")); }
//    @GetMapping("/api/v1/monitor/alerts/rules") public Result<?> alertRules() { return Result.ok(mock("/monitor/alerts/rules", "GET")); }
//    @PostMapping("/api/v1/monitor/alerts/rules") public Result<?> createRule(@RequestBody Map<String, Object> body) { return Result.ok(mock("/monitor/alerts/rules", "POST")); }
//    @GetMapping("/api/v1/monitor/cluster") public Result<?> monitorCluster() { return Result.ok(mock("/monitor/cluster", "GET")); }
//
//    // ===== /api/v1/analytics/* =====
//    @PostMapping("/api/v1/analytics/datasources/test") public Result<?> testDS(@RequestBody Map<String, Object> body) { return Result.ok(mock("/analytics/datasources/test", "POST")); }
//    @GetMapping("/api/v1/analytics/ingest/tasks") public Result<?> ingestTasks() { return Result.ok(mock("/analytics/ingest/tasks", "GET")); }
//    @PostMapping("/api/v1/analytics/ingest/tasks") public Result<?> createIngest(@RequestBody Map<String, Object> body) { return Result.ok(mock("/analytics/ingest/tasks", "POST")); }
//    @GetMapping("/api/v1/analytics/dashboards") public Result<?> dashboards() { return Result.ok(mock("/analytics/dashboards", "GET")); }
//    @PostMapping("/api/v1/analytics/dashboards") public Result<?> createDashboard(@RequestBody Map<String, Object> body) { return Result.ok(mock("/analytics/dashboards", "POST")); }
//
//    // ===== /api/v1/memory/* =====
//    @GetMapping("/api/v1/memory/context") public Result<?> memoryContext() { return Result.ok(mock("/memory/context", "GET")); }
//    @PostMapping("/api/v1/memory/context") public Result<?> setContext(@RequestBody Map<String, Object> body) { return Result.ok(mock("/memory/context", "POST")); }
//    @GetMapping("/api/v1/memory/cross-context") public Result<?> crossContext() { return Result.ok(mock("/memory/cross-context", "GET")); }
//    @GetMapping("/api/v1/memory/long-term") public Result<?> longTerm() { return Result.ok(mock("/memory/long-term", "GET")); }
//    @PostMapping("/api/v1/memory/long-term") public Result<?> setLongTerm(@RequestBody Map<String, Object> body) { return Result.ok(mock("/memory/long-term", "POST")); }
//    @DeleteMapping("/api/v1/memory/long-term") public Result<?> delLongTerm(@RequestBody Map<String, Object> body) { return Result.ok(mock("/memory/long-term", "DELETE")); }
//    @GetMapping("/api/v1/memory/short-term") public Result<?> shortTerm() { return Result.ok(mock("/memory/short-term", "GET")); }
//    @PostMapping("/api/v1/memory/short-term") public Result<?> setShortTerm(@RequestBody Map<String, Object> body) { return Result.ok(mock("/memory/short-term", "POST")); }
//    @DeleteMapping("/api/v1/memory/short-term") public Result<?> delShortTerm(@RequestBody Map<String, Object> body) { return Result.ok(mock("/memory/short-term", "DELETE")); }
//
//    // ===== /api/v1/rag/* =====
//    @PostMapping("/api/v1/rag/ask") public Result<?> ragAsk(@RequestBody Map<String, Object> body) { return Result.ok(mock("/rag/ask", "POST")); }
//    @GetMapping("/api/v1/rag/doc") public Result<?> ragDoc() { return Result.ok(mock("/rag/doc", "GET")); }
//    @PostMapping("/api/v1/rag/doc/upload") public Result<?> ragUpload(@RequestBody Map<String, Object> body) { return Result.ok(mock("/rag/doc/upload", "POST")); }
//    @DeleteMapping("/api/v1/rag/doc/{id}") public Result<?> ragDelDoc(@PathVariable String id) { return Result.ok(mock("/rag/doc/" + id, "DELETE")); }
//
//    // ===== /api/v1/function/* =====
//    @GetMapping("/api/v1/function/tools") public Result<?> funcTools() { return Result.ok(mock("/function/tools", "GET")); }
//    @PostMapping("/api/v1/function/tools") public Result<?> createTool(@RequestBody Map<String, Object> body) { return Result.ok(mock("/function/tools", "POST")); }
//    @PostMapping("/api/v1/function/invoke/{name}") public Result<?> invokeTool(@PathVariable String name, @RequestBody Map<String, Object> body) { return Result.ok(mock("/function/invoke/" + name, "POST")); }
//
//    // ===== /api/v1/leaderboard/* =====
//    @GetMapping("/api/v1/leaderboard/categories") public Result<?> lbCategories() { return Result.ok(mock("/leaderboard/categories", "GET")); }
//    @GetMapping("/api/v1/leaderboard/latency") public Result<?> lbLatency() { return Result.ok(mock("/leaderboard/latency", "GET")); }
//    @GetMapping("/api/v1/leaderboard/overall") public Result<?> lbOverall() { return Result.ok(mock("/leaderboard/overall", "GET")); }
//
//    // ===== /api/v1/agent/* =====
//    @GetMapping("/api/v1/agent/kg/entities") public Result<?> kgEntities() { return Result.ok(mock("/agent/kg/entities", "GET")); }
//    @PostMapping("/api/v1/agent/kg/entities") public Result<?> createEntity(@RequestBody Map<String, Object> body) { return Result.ok(mock("/agent/kg/entities", "POST")); }
//    @GetMapping("/api/v1/agent/kg/path") public Result<?> kgPath() { return Result.ok(mock("/agent/kg/path", "GET")); }
//
//    // ===== /api/v1/multimodal/* =====
//    @GetMapping("/api/v1/multimodal/info") public Result<?> mmInfo() { return Result.ok(mock("/multimodal/info", "GET")); }
//    @PostMapping("/api/v1/multimodal/upload") public Result<?> mmUpload(@RequestBody Map<String, Object> body) { return Result.ok(mock("/multimodal/upload", "POST")); }
//    @PostMapping("/api/v1/multimodal/describe") public Result<?> mmDescribe(@RequestBody Map<String, Object> body) { return Result.ok(mock("/multimodal/describe", "POST")); }
//
//    // ===== /api/v1/tensorboard/* =====
//    @GetMapping("/api/v1/tensorboard/runs/compare") public Result<?> tbCompare() { return Result.ok(mock("/tensorboard/runs/compare", "GET")); }
//    @GetMapping("/api/v1/tensorboard/runs") public Result<?> tbRuns() { return Result.ok(mock("/tensorboard/runs", "GET")); }
//
//    // ===== /api/v1/collab/* =====
//    @GetMapping("/api/v1/collab/rooms") public Result<?> collabRooms() { return Result.ok(mock("/collab/rooms", "GET")); }
//    @GetMapping("/api/v1/collab/rooms/public") public Result<?> collabPublic() { return Result.ok(mock("/collab/rooms/public", "GET")); }
//
//    // ===== /api/v1/models/* =====
//    @GetMapping("/api/v1/models/chat/stream/{streamId}") public Result<?> streamStatus(@PathVariable String streamId) { return Result.ok(mock("/models/chat/stream/" + streamId, "GET")); }
//    @DeleteMapping("/api/v1/models/chat/stream/{streamId}") public Result<?> streamStop(@PathVariable String streamId) { return Result.ok(mock("/models/chat/stream/" + streamId, "DELETE")); }
//
//    // ===== /api/v1/sessions/* =====
//    @PostMapping("/api/v1/sessions/stop-stream") public Result<?> stopStream(@RequestBody Map<String, Object> body) { return Result.ok(mock("/sessions/stop-stream", "POST")); }
//
//    // ===== /api/v1/training/* =====
//    @GetMapping("/api/v1/training/models") public Result<?> trainingModels() { return Result.ok(mock("/training/models", "GET")); }
//    @GetMapping("/api/v1/training/tasks") public Result<?> trainingTasks() { return Result.ok(mock("/training/tasks", "GET")); }
//
//    // ===== /api/v1/pipeline/* =====
//    @GetMapping("/api/v1/pipeline/workflows") public Result<?> workflows() { return Result.ok(mock("/pipeline/workflows", "GET")); }
//    @PostMapping("/api/v1/pipeline/workflows") public Result<?> createWorkflow(@RequestBody Map<String, Object> body) { return Result.ok(mock("/pipeline/workflows", "POST")); }
//}
