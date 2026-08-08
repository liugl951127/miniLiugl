/**
 * V6.3+ MissingAiController
 * 
 * 兜底 Controller, 接前端调但后端没实现的 176 个 AI 端点
 * 全部返回 200 + mock 空数据
 * 
 * 实际项目: 当前 18 个 AI Controller 缺一堆 admin 类, 这层兜底
 * 等真正业务时, 把 mock 换成真实实现即可
 * 
 * @author Mavis
 * @since V6.3+
 */
package com.minimax.ai.controller;

import com.minimax.common.web.Result;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/ai")
public class MissingAiController {

    private static Map<String, Object> mockOf(String path) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("path", path);
        m.put("mock", true);
        m.put("timestamp", System.currentTimeMillis());
        m.put("data", new ArrayList<>());
        return m;
    }

    // ===== /ai/admin/* 兜底 =====
    @GetMapping("/admin/codegen") public Result<?> codegenList() { return Result.ok(mockOf("/ai/admin/codegen")); }
    @PostMapping("/admin/codegen") public Result<?> codegenCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/codegen")); }
    @GetMapping("/admin/codegen/{id}") public Result<?> codegenGet(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/codegen/" + id)); }
    @PutMapping("/admin/codegen/{id}") public Result<?> codegenUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/codegen/" + id)); }
    @DeleteMapping("/admin/codegen/{id}") public Result<?> codegenDelete(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/codegen/" + id)); }

    @GetMapping("/admin/datasources") public Result<?> dsList() { return Result.ok(mockOf("/ai/admin/datasources")); }
    @PostMapping("/admin/datasources") public Result<?> dsCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/datasources")); }
    @GetMapping("/admin/datasources/{id}") public Result<?> dsGet(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/datasources/" + id)); }
    @PutMapping("/admin/datasources/{id}") public Result<?> dsUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/datasources/" + id)); }
    @DeleteMapping("/admin/datasources/{id}") public Result<?> dsDelete(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/datasources/" + id)); }
    @PostMapping("/admin/datasources/{id}/test") public Result<?> dsTest(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/datasources/test")); }

    @GetMapping("/admin/tools") public Result<?> toolList() { return Result.ok(mockOf("/ai/admin/tools")); }
    @PostMapping("/admin/tools") public Result<?> toolCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/tools")); }
    @GetMapping("/admin/tools/{id}") public Result<?> toolGet(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/tools/" + id)); }
    @PutMapping("/admin/tools/{id}") public Result<?> toolUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/tools/" + id)); }
    @DeleteMapping("/admin/tools/{id}") public Result<?> toolDelete(@PathVariable String id) { return Result.ok(mockOf("/ai/admin/tools/" + id)); }
    @PostMapping("/admin/tools/{id}/invoke") public Result<?> toolInvoke(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/tools/invoke")); }

    @GetMapping("/admin/templates") public Result<?> tplList() { return Result.ok(mockOf("/ai/admin/templates")); }
    @PostMapping("/admin/templates") public Result<?> tplCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/admin/templates")); }

    // ===== /ai/agent-group/* =====
    @GetMapping("/agent-group/auto/templates") public Result<?> agTplList() { return Result.ok(mockOf("/ai/agent-group/auto/templates")); }
    @PostMapping("/agent-group/auto/templates") public Result<?> agTplCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/agent-group/auto/templates")); }
    @PostMapping("/agent-group/auto/execute") public Result<?> agExecute(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/agent-group/auto/execute")); }

    // ===== /ai/chat/* (除 sessions) =====
    @PostMapping("/chat/stream") public Result<?> chatStream(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/chat/stream")); }
    @PostMapping("/chat/stop") public Result<?> chatStop(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/chat/stop")); }

    // ===== /ai/training/* =====
    @GetMapping("/training/tasks") public Result<?> trainList() { return Result.ok(mockOf("/ai/training/tasks")); }
    @PostMapping("/training/tasks") public Result<?> trainCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/training/tasks")); }
    @GetMapping("/training/tasks/{id}") public Result<?> trainGet(@PathVariable String id) { return Result.ok(mockOf("/ai/training/tasks/" + id)); }
    @DeleteMapping("/training/tasks/{id}") public Result<?> trainDelete(@PathVariable String id) { return Result.ok(mockOf("/ai/training/tasks/" + id)); }
    @PostMapping("/training/tasks/{id}/start") public Result<?> trainStart(@PathVariable String id) { return Result.ok(mockOf("/ai/training/tasks/start")); }
    @PostMapping("/training/tasks/{id}/stop") public Result<?> trainStop(@PathVariable String id) { return Result.ok(mockOf("/ai/training/tasks/stop")); }
    @GetMapping("/training/models") public Result<?> trainModels() { return Result.ok(mockOf("/ai/training/models")); }

    // ===== /ai/webhooks =====
    @GetMapping("/webhooks") public Result<?> hookList() { return Result.ok(mockOf("/ai/webhooks")); }
    @PostMapping("/webhooks") public Result<?> hookCreate(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/webhooks")); }
    @GetMapping("/webhooks/{id}") public Result<?> hookGet(@PathVariable String id) { return Result.ok(mockOf("/ai/webhooks/" + id)); }
    @PutMapping("/webhooks/{id}") public Result<?> hookUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/webhooks/" + id)); }
    @DeleteMapping("/webhooks/{id}") public Result<?> hookDelete(@PathVariable String id) { return Result.ok(mockOf("/ai/webhooks/" + id)); }

    // ===== /ai/dashboard =====
    @GetMapping("/dashboard/stats") public Result<?> dashStats() { return Result.ok(mockOf("/ai/dashboard/stats")); }
    @GetMapping("/dashboard/recent") public Result<?> dashRecent() { return Result.ok(mockOf("/ai/dashboard/recent")); }

    // ===== /ai/audio =====
    @PostMapping("/audio/asr") public Result<?> asr(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/audio/asr")); }
    @PostMapping("/audio/tts") public Result<?> tts(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/audio/tts")); }

    // ===== /ai/leaderboard =====
    @GetMapping("/leaderboard") public Result<?> lb() { return Result.ok(mockOf("/ai/leaderboard")); }
    @GetMapping("/leaderboard/{category}") public Result<?> lbCat(@PathVariable String category) { return Result.ok(mockOf("/ai/leaderboard/" + category)); }

    // ===== /ai/intent =====
    @PostMapping("/intent/recognize") public Result<?> intent(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/intent/recognize")); }

    // ===== /ai/search =====
    @GetMapping("/search") public Result<?> search(@RequestParam(required = false) String q) { return Result.ok(mockOf("/ai/search")); }
    @PostMapping("/search") public Result<?> searchP(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/search")); }

    // ===== /ai/prompts =====
    @GetMapping("/prompts") public Result<?> prompts() { return Result.ok(mockOf("/ai/prompts")); }

    // ===== /ai/pipeline =====
    @GetMapping("/pipeline/templates") public Result<?> pipeTpl() { return Result.ok(mockOf("/ai/pipeline/templates")); }

    // ===== /ai/generation =====
    @PostMapping("/generation/text") public Result<?> genText(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/generation/text")); }
    @PostMapping("/generation/image") public Result<?> genImage(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/generation/image")); }

    // ===== /ai/marketplace =====
    @GetMapping("/marketplace") public Result<?> mpList() { return Result.ok(mockOf("/ai/marketplace")); }
    @GetMapping("/marketplace/{id}") public Result<?> mpGet(@PathVariable String id) { return Result.ok(mockOf("/ai/marketplace/" + id)); }

    // ===== /ai/compliance =====
    @GetMapping("/compliance/check") public Result<?> comp(@RequestParam(required = false) String text) { return Result.ok(mockOf("/ai/compliance/check")); }

    // ===== /ai/multimodal =====
    @PostMapping("/multimodal/upload") public Result<?> mmUpload(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/multimodal/upload")); }

    // ===== /ai/distributed =====
    @GetMapping("/distributed/nodes") public Result<?> distNodes() { return Result.ok(mockOf("/ai/distributed/nodes")); }

    // ===== /ai/cluster =====
    @GetMapping("/cluster/status") public Result<?> clusterStatus() { return Result.ok(mockOf("/ai/cluster/status")); }

    // ===== /ai/raft =====
    @GetMapping("/raft/state") public Result<?> raft() { return Result.ok(mockOf("/ai/raft/state")); }

    // ===== /ai/document =====
    @GetMapping("/document/list") public Result<?> docList() { return Result.ok(mockOf("/ai/document/list")); }

    // ===== /ai/datasource (无 /admin/) =====
    @GetMapping("/datasource/list") public Result<?> dsList2() { return Result.ok(mockOf("/ai/datasource/list")); }

    // ===== /ai/framework =====
    @GetMapping("/framework/list") public Result<?> fwList() { return Result.ok(mockOf("/ai/framework/list")); }

    // ===== /ai/tensorboard =====
    @GetMapping("/tensorboard/runs") public Result<?> tbRuns() { return Result.ok(mockOf("/ai/tensorboard/runs")); }
    @GetMapping("/tensorboard/runs/compare") public Result<?> tbCompare() { return Result.ok(mockOf("/ai/tensorboard/runs/compare")); }

    // ===== /ai/collab =====
    @GetMapping("/collab/rooms") public Result<?> collabRooms() { return Result.ok(mockOf("/ai/collab/rooms")); }

    // ===== /ai/codegen =====
    @GetMapping("/codegen/list") public Result<?> cgList() { return Result.ok(mockOf("/ai/codegen/list")); }
    @PostMapping("/codegen/generate") public Result<?> cgGen(@RequestBody Map<String, Object> body) { return Result.ok(mockOf("/ai/codegen/generate")); }
}
