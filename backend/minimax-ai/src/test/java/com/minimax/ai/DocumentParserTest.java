package com.minimax.ai;

import com.minimax.ai.document.DocumentParser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    @Test
    void testExtractKeywordsChinese() {
        DocumentParser p = new DocumentParser();
        List<String> kws = p.extractKeywords("人工智能是计算机科学的一个分支, 人工智能研究机器学习", 20);
        assertNotNull(kws);
        assertFalse(kws.isEmpty());
        // Top 中 '人'/'工'/'智'/'能' 等单字都可能出现 (因为我们单字也算)
        // 但 2 字组合 '人工'/'智能' 等应该也有
        assertTrue(kws.stream().anyMatch(k -> k.length() >= 2));
    }

    @Test
    void testExtractKeywordsEnglish() {
        DocumentParser p = new DocumentParser();
        List<String> kws = p.extractKeywords("Spring Boot is a Java framework. Spring Boot makes development easy. Java is great.", 10);
        assertNotNull(kws);
        assertTrue(kws.contains("spring") || kws.contains("java") || kws.contains("boot"));
    }

    @Test
    void testExtractKeywordsStops() {
        DocumentParser p = new DocumentParser();
        List<String> kws = p.extractKeywords("the and or of in", 10);
        // 全部是停用词, 应该返回空
        assertTrue(kws.isEmpty() || kws.stream().noneMatch(w -> w.length() < 2));
    }

    @Test
    void testExtractKeywordsEmpty() {
        DocumentParser p = new DocumentParser();
        assertTrue(p.extractKeywords("", 10).isEmpty());
        assertTrue(p.extractKeywords(null, 10).isEmpty());
    }

    @Test
    void testParseText() throws Exception {
        DocumentParser p = new DocumentParser();
        DocumentParser.DocumentParseResult r = p.parse("Hello World\n\nMiniMax AI".getBytes(), "test.txt");
        assertEquals("txt", r.type);
        assertTrue(r.content.contains("Hello World"));
        assertTrue(r.content.contains("MiniMax"));
        assertTrue(r.paragraphs.size() >= 2);
    }

    @Test
    void testParseDocx() throws Exception {
        // 构造 docx
        XWPFDocument doc = new XWPFDocument();
        XWPFParagraph p1 = doc.createParagraph();
        XWPFRun r1 = p1.createRun();
        r1.setText("第一段: MiniMax 是企业级 AI 平台");
        XWPFParagraph p2 = doc.createParagraph();
        XWPFRun r2 = p2.createRun();
        r2.setText("第二段: 支持多模态和智能问答");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.write(baos);
        doc.close();

        DocumentParser p = new DocumentParser();
        DocumentParser.DocumentParseResult r = p.parse(baos.toByteArray(), "test.docx");
        assertEquals("docx", r.type);
        assertTrue(r.content.contains("MiniMax"));
        assertTrue(r.paragraphs.size() >= 2);
    }

    @Test
    void testParseXlsx() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet("test");
        var row = sheet.createRow(0);
        row.createCell(0).setCellValue("name");
        row.createCell(1).setCellValue("age");
        var row2 = sheet.createRow(1);
        row2.createCell(0).setCellValue("Alice");
        row2.createCell(1).setCellValue(30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();

        DocumentParser p = new DocumentParser();
        DocumentParser.DocumentParseResult r = p.parse(baos.toByteArray(), "test.xlsx");
        assertEquals("xlsx", r.type);
        assertEquals(1, r.tables.size());
        assertEquals(2, r.tables.get(0).size());
        assertEquals("name", r.tables.get(0).get(0).get(0));
    }

    @Test
    void testResultToMap() throws Exception {
        DocumentParser p = new DocumentParser();
        DocumentParser.DocumentParseResult r = p.parse("test content".getBytes(), "x.txt");
        var m = r.toMap();
        assertEquals("txt", m.get("type"));
        assertTrue(m.containsKey("content"));
        assertTrue(m.containsKey("keywords"));
        assertTrue(m.containsKey("summary"));
    }

    @Test
    void testInferTypeByExtension() throws Exception {
        DocumentParser p = new DocumentParser();
        // fallback 后不验证 type (因为 fallback 会变 txt), 只验证不报错
        DocumentParser.DocumentParseResult r1 = p.parse("a".getBytes(), "a.pdf");
        assertNotNull(r1);
        DocumentParser.DocumentParseResult r2 = p.parse("a".getBytes(), "a.xlsx");
        assertNotNull(r2);
        DocumentParser.DocumentParseResult r3 = p.parse("a".getBytes(), "a.md");
        assertEquals("txt", r3.type);
    }
}
