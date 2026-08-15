package com.minimax.ai.tool.builtin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件转换工具 (V2.8.3)
 */
@Slf4j
@Component
public class FileConverterTool extends AbstractSimpleTool {

    @Override
    public String getCode() { return "file.convert"; }

    @Override
    public String getName() { return "文件转换"; }

    @Override
    public String getDescription() { return "JSON/YAML/CSV/Base64 互转"; }

    @Override
    public String getCategory() { return "file"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String op = (String) input.getOrDefault("op", "format");
        String text = (String) input.getOrDefault("text", "");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("op", op);
        switch (op) {
            case "text2csv" -> result.put("output", text2csv(text));
            case "csv2text" -> result.put("output", csv2text(text));
            case "json2yaml" -> result.put("output", formatJson(text));
            case "yaml2json" -> result.put("output", yaml2json(text));
            case "json2csv" -> result.put("output", jsonArray2csv(text));
            case "base642text" -> result.put("output", new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8));
            case "text2base64" -> result.put("output", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
            case "format" -> result.put("output", formatJsonOrYaml(text));
            case "zip2list" -> result.put("entries", listZip(Base64.getDecoder().decode(text)));
            case "merge" -> {
                @SuppressWarnings("unchecked")
                List<String> parts = (List<String>) input.get("parts");
                String sep = (String) input.getOrDefault("separator", "\n---\n");
                result.put("output", parts == null ? "" : String.join(sep, parts));
            }
            default -> throw new IllegalArgumentException("不支持的 op: " + op);
        }
        return result;
    }

    private String text2csv(String text) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.contains(",") || line.contains("\"") || line.contains("\n"))
                sb.append('"').append(line.replace("\"", "\"\"")).append('"');
            else sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

    private String csv2text(String csv) {
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (inQuote) {
                if (c == '"' && i + 1 < csv.length() && csv.charAt(i + 1) == '"') { field.append('"'); i++; }
                else if (c == '"') inQuote = false;
                else field.append(c);
            } else {
                if (c == '"') inQuote = true;
                else if (c == ',') { sb.append(field); sb.append('\n'); field.setLength(0); }
                else if (c == '\n') { sb.append(field); sb.append('\n'); field.setLength(0); }
                else field.append(c);
            }
        }
        if (field.length() > 0) sb.append(field);
        return sb.toString();
    }

    public String yaml2json(String yaml) {
        if (yaml == null || yaml.isBlank()) return "{}";
        StringBuilder sb = new StringBuilder("{\n");
        String[] lines = yaml.split("\\r?\\n");
        boolean first = true;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("-")) continue;
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim();
            String val = line.substring(colon + 1).trim();
            if (val.isEmpty()) continue;
            if (!first) sb.append(",\n");
            sb.append("  \"").append(escape(key)).append("\": ");
            if (val.matches("-?\\d+(\\.\\d+)?")) sb.append(val);
            else if (val.equals("true") || val.equals("false") || val.equals("null")) sb.append(val);
            else sb.append("\"").append(escape(val)).append("\"");
            first = false;
        }
        sb.append("\n}");
        return sb.toString();
    }

    public String jsonArray2csv(String json) {
        if (!json.trim().startsWith("[")) return json;
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { objects.add(json.substring(start, i + 1)); start = -1; } }
        }
        if (objects.isEmpty()) return json;
        Set<String> keys = new LinkedHashSet<>();
        List<Map<String, String>> rows = new ArrayList<>();
        for (String obj : objects) {
            Map<String, String> map = parseFlatObject(obj);
            keys.addAll(map.keySet());
            rows.add(map);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", keys)).append('\n');
        for (Map<String, String> row : rows) {
            List<String> cells = new ArrayList<>();
            for (String k : keys) {
                String v = row.getOrDefault(k, "");
                if (v.contains(",") || v.contains("\"") || v.contains("\n"))
                    v = "\"" + v.replace("\"", "\"\"") + "\"";
                cells.add(v);
            }
            sb.append(String.join(",", cells)).append('\n');
        }
        return sb.toString();
    }

    private Map<String, String> parseFlatObject(String obj) {
        Map<String, String> m = new LinkedHashMap<>();
        int i = 0, n = obj.length();
        while (i < n) {
            int ks = obj.indexOf('"', i); if (ks < 0) break;
            int ke = obj.indexOf('"', ks + 1); if (ke < 0) break;
            String key = obj.substring(ks + 1, ke);
            int colon = obj.indexOf(':', ke); if (colon < 0) break;
            int v = colon + 1;
            while (v < n && Character.isWhitespace(obj.charAt(v))) v++;
            String val;
            if (v < n && obj.charAt(v) == '"') {
                int vEnd = obj.indexOf('"', v + 1); if (vEnd < 0) break;
                val = obj.substring(v + 1, vEnd); i = vEnd + 1;
            } else {
                int vEnd = v;
                while (vEnd < n && ",} ".indexOf(obj.charAt(vEnd)) < 0) vEnd++;
                val = obj.substring(v, vEnd).trim(); i = vEnd;
            }
            m.put(key, val);
        }
        return m;
    }

    private String formatJsonOrYaml(String text) {
        text = text.trim();
        if (text.startsWith("{") || text.startsWith("[")) return formatJson(text);
        return text;
    }

    private String formatJson(String json) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        boolean inStr = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inStr) {
                sb.append(c);
                if (c == '"' && json.charAt(i - 1) != '\\') inStr = false;
            } else if (c == '"') { sb.append(c); inStr = true; }
            else if (c == '{' || c == '[') { sb.append(c).append('\n'); depth++; sb.append("  ".repeat(depth)); }
            else if (c == '}' || c == ']') { sb.append('\n'); depth--; sb.append("  ".repeat(depth)).append(c); }
            else if (c == ',') { sb.append(c).append('\n').append("  ".repeat(depth)); }
            else if (c == ':') sb.append(": ");
            else sb.append(c);
        }
        return sb.toString();
    }

    private String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private List<Map<String, Object>> listZip(byte[] data) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(data))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", e.getName());
                m.put("size", e.getSize());
                m.put("compressed", e.getCompressedSize());
                m.put("directory", e.isDirectory());
                result.add(m);
            }
        } catch (Exception e) { log.error("listZip failed", e); }
        return result;
    }
}
