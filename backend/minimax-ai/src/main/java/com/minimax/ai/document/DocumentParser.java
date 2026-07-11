package com.minimax.ai.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 文档智能解析器 (V2.7.7)
 *
 * <h3>支持的格式</h3>
 * <ul>
 *   <li><b>PDF</b> - 文本提取 (Tika)</li>
 *   <li><b>Word (.docx)</b> - 段落 + 表格 (Apache POI)</li>
 *   <li><b>Excel (.xlsx)</b> - 行列数据 (Apache POI)</li>
 *   <li><b>纯文本 / Markdown / HTML</b> - Tika 自动检测</li>
 * </ul>
 *
 * <h3>输出结构</h3>
 * <pre>
 *   DocumentParseResult {
 *     type: "pdf/docx/xlsx/txt/..."
 *     title, author, pageCount, wordCount, charCount
 *     content: 纯文本
 *     paragraphs: [String]      // 段落列表
 *     tables: [List<List<String>>]  // 表格
 *     keywords: [String]        // 关键词 (频率 Top 20)
 *     summary: String           // 摘要 (前 200 字)
 *     metadata: { ... }         // 原始元数据
 *   }
 * </pre>
 *
 * <h3>算法</h3>
 * <p>1. 用 Tika AutoDetectParser 统一入口; 2. 写时按文件类型分支用 POI 拿结构化数据; 3. 关键词用 TF 词频统计 (排除停用词)</p>
 */
@Slf4j
@Component
public class DocumentParser {

    private final Tika tika = new Tika();
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "在", "和", "与", "或", "及", "the", "a", "an", "is", "are", "was", "were",
            "to", "of", "in", "on", "at", "for", "by", "with", "and", "or"
    );

    public static class DocumentParseResult {
        public String type;             // pdf / docx / xlsx / txt
        public String mimeType;
        public String title;
        public String author;
        public Long pageCount;
        public int wordCount;
        public int charCount;
        public String content;          // 纯文本
        public List<String> paragraphs = new ArrayList<>();
        public List<List<List<String>>> tables = new ArrayList<>();  // 多张表 -> 行 -> 列
        public List<String> keywords = new ArrayList<>();
        public String summary;          // 前 200 字
        public Map<String, String> metadata = new LinkedHashMap<>();
        public long parseCostMs;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", type);
            m.put("mimeType", mimeType);
            m.put("title", title);
            m.put("author", author);
            m.put("pageCount", pageCount);
            m.put("wordCount", wordCount);
            m.put("charCount", charCount);
            m.put("paragraphCount", paragraphs.size());
            m.put("tableCount", tables.size());
            m.put("keywords", keywords);
            m.put("summary", summary);
            m.put("content", content);
            m.put("metadata", metadata);
            m.put("parseCostMs", parseCostMs);
            return m;
        }
    }

    /**
     * 主入口: 自动检测类型 + 解析
     */
    public DocumentParseResult parse(byte[] data, String fileName) throws Exception {
        long start = System.currentTimeMillis();
        DocumentParseResult r = new DocumentParseResult();
        r.mimeType = detectMime(data, fileName);
        r.type = inferType(fileName, r.mimeType);

        try (InputStream is = new ByteArrayInputStream(data)) {
            try {
                switch (r.type) {
                    case "pdf" -> parsePdf(is, r);
                    case "docx" -> parseDocx(is, r);
                    case "xlsx" -> parseXlsx(is, r);
                    default -> parseGeneric(is, r);
                }
            } catch (Exception e) {
                // 解析失败: 用原始字节作为 text (避免空内容)
                if (r.content == null || r.content.isEmpty()) {
                    r.content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    r.type = "txt";
                    log.warn("Parse {} failed, fallback to txt: {}", fileName, e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        r.wordCount = countWords(r.content);
        r.charCount = r.content.length();
        r.summary = buildSummary(r.content, 200);
        r.keywords = extractKeywords(r.content, 20);
        r.parseCostMs = System.currentTimeMillis() - start;
        log.info("Parsed {} ({}): {} chars, {} paras, {} tables, cost={}ms",
                fileName, r.type, r.charCount, r.paragraphs.size(), r.tables.size(), r.parseCostMs);
        return r;
    }

    private String detectMime(byte[] data, String fileName) {
        try {
            return tika.detect(data, fileName);
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private String inferType(String fileName, String mime) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".doc")) return "docx";
        if (lower.endsWith(".xlsx")) return "xlsx";
        if (lower.endsWith(".xls")) return "xlsx";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "txt";
        if (mime.contains("pdf")) return "pdf";
        if (mime.contains("word")) return "docx";
        if (mime.contains("sheet") || mime.contains("excel")) return "xlsx";
        return "txt";
    }

    private void parsePdf(InputStream is, DocumentParseResult r) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler(-1);  // 无限
        Metadata meta = new Metadata();
        Parser parser = new AutoDetectParser();
        parser.parse(is, handler, meta, new ParseContext());
        r.content = handler.toString();
        copyMeta(meta, r);
        splitParagraphs(r);
    }

    private void parseDocx(InputStream is, DocumentParseResult r) throws Exception {
        XWPFDocument doc = new XWPFDocument(is);
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = p.getText();
            if (text != null && !text.isBlank()) {
                r.paragraphs.add(text);
                sb.append(text).append("\n");
            }
        }
        // 表格
        doc.getTables().forEach(table -> {
            List<List<String>> rows = new ArrayList<>();
            table.getRows().forEach(row -> {
                List<String> cells = new ArrayList<>();
                row.getTableCells().forEach(cell -> cells.add(cell.getText()));
                rows.add(cells);
            });
            if (!rows.isEmpty()) r.tables.add(rows);
        });
        r.content = sb.toString();
        r.title = doc.getProperties().getCoreProperties().getTitle();
        r.author = doc.getProperties().getCoreProperties().getCreator();
    }

    private void parseXlsx(InputStream is, DocumentParseResult r) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook(is);
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            XSSFSheet sheet = wb.getSheetAt(s);
            List<List<String>> rows = new ArrayList<>();
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    cells.add(getCellString(cell));
                }
                rows.add(cells);
            }
            if (!rows.isEmpty()) {
                r.tables.add(rows);
                // 也写到 content (TSV 格式)
                for (List<String> row : rows) {
                    sb.append(String.join("\t", row)).append("\n");
                }
            }
        }
        r.content = sb.toString();
    }

    private String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private void parseGeneric(InputStream is, DocumentParseResult r) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata meta = new Metadata();
        new AutoDetectParser().parse(is, handler, meta, new ParseContext());
        r.content = handler.toString();
        copyMeta(meta, r);
        splitParagraphs(r);
    }

    private void copyMeta(Metadata meta, DocumentParseResult r) {
        for (String name : meta.names()) {
            String v = meta.get(name);
            if (v != null) r.metadata.put(name, v);
        }
        r.title = meta.get("dc:title");
        r.author = meta.get("dc:creator");
        if (meta.get("xmpTPg:NPages") != null) {
            try { r.pageCount = Long.parseLong(meta.get("xmpTPg:NPages")); } catch (Exception ignored) {}
        }
    }

    private void splitParagraphs(DocumentParseResult r) {
        if (r.paragraphs.isEmpty()) {
            String[] ps = r.content.split("[\\n\\r]{2,}");
            for (String p : ps) {
                if (!p.isBlank()) r.paragraphs.add(p.trim());
            }
        }
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        // 中英混合: 中文字符 + 英文单词
        int cn = 0;
        int en = 0;
        boolean inWord = false;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) { inWord = false; continue; }
            if (c >= 0x4E00 && c <= 0x9FA5) cn++;
            else if (Character.isLetter(c)) {
                if (!inWord) { en++; inWord = true; }
            }
        }
        return cn + en;
    }

    private String buildSummary(String text, int max) {
        if (text == null) return "";
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /**
     * 关键词提取: 简化版 TF
     * 1. 分词 (中英混合: 中文字符 1-gram, 英文按空格)
     * 2. 过滤停用词 + 长度 < 2
     * 3. 频率降序取 Top N
     */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.isBlank()) return List.of();
        Map<String, Integer> freq = new HashMap<>();

        // 1. 提取连续中文 2-gram (bigram)
        StringBuilder cn = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                cn.append(c);
            } else {
                if (cn.length() > 0) {
                    // 提取 2 字组合
                    for (int i = 0; i < cn.length() - 1; i++) {
                        String w = cn.substring(i, i + 2);
                        addWord(freq, w);
                    }
                    cn.setLength(0);
                }
            }
        }
        if (cn.length() > 0) {
            for (int i = 0; i < cn.length() - 1; i++) {
                String w = cn.substring(i, i + 2);
                addWord(freq, w);
            }
        }

        // 2. 提取英文/数字单词
        StringBuilder buf = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                buf.append(c);
            } else {
                if (buf.length() >= 2) addWord(freq, buf.toString().toLowerCase());
                buf.setLength(0);
            }
        }
        if (buf.length() >= 2) addWord(freq, buf.toString().toLowerCase());

        return freq.entrySet().stream()
                .filter(e -> e.getKey().length() >= 2 && !STOP_WORDS.contains(e.getKey()))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void addWord(Map<String, Integer> freq, String w) {
        if (w.length() >= 1) freq.merge(w, 1, Integer::sum);
    }
}
