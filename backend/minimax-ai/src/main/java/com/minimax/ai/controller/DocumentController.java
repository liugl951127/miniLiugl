package com.minimax.ai.controller;

import com.minimax.ai.document.DocumentParser;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文档智能解析 API (V2.7.7)
 *
 * 端点:
 *   POST /api/ai/document/parse   上传解析 (multipart/form-data)
 *   POST /api/ai/document/keywords  提取关键词 (纯文本)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentParser parser;

    @PostMapping("/parse")
    public Result<DocumentParser.DocumentParseResult> parse(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.fail("文件为空");
        try {
            String name = file.getOriginalFilename();
            DocumentParser.DocumentParseResult r = parser.parse(file.getBytes(), name);
            return Result.ok(r);
        } catch (Exception e) {
            log.error("Document parse failed", e);
            return Result.fail("解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/keywords")
    public Result<List<String>> keywords(@RequestBody Map<String, Object> req) {
        String text = (String) req.get("text");
        int top = ((Number) req.getOrDefault("top", 20)).intValue();
        return Result.ok(parser.extractKeywords(text, top));
    }

    @GetMapping("/formats")
    public Result<String[]> formats() {
        return Result.ok(new String[]{"pdf", "docx", "xlsx", "txt", "md", "html"});
    }
}
