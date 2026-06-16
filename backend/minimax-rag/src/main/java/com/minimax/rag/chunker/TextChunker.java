package com.minimax.rag.chunker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器：滑动窗口。
 *
 * 策略:
 *  1) 优先按段落 (\n\n 或 \n) 切
 *  2) 若段落 > chunkSize: 切到 chunks
 *  3) 若相邻小段落累计 + overlap < chunkSize: 合并
 *
 * 参数:
 *  - chunkSize: 单块最大字符数 (默认 500)
 *  - overlap: 相邻块重叠字符 (默认 50)
 */
@Component
public class TextChunker {

    @Value("${minimax.rag.chunk.size:500}")
    private int chunkSize;

    @Value("${minimax.rag.chunk.overlap:50}")
    private int overlap;

    public List<Chunk> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<Chunk> out = new ArrayList<>();

        // 规范化空白
        String normalized = text.replaceAll("[\\r\\n]{3,}", "\n\n").trim();

        // 按段落切
        String[] paragraphs = normalized.split("\\n\\n+");

        StringBuilder buffer = new StringBuilder();
        int startPos = 0;
        int currentPos = 0;

        for (String p : paragraphs) {
            String para = p.trim();
            if (para.isEmpty()) { currentPos += p.length() + 2; continue; }

            // 单段超长: 强制切
            if (para.length() > chunkSize) {
                if (buffer.length() > 0) {
                    out.add(Chunk.of(buffer.toString().trim(), startPos, currentPos));
                    // overlap
                    String tail = tail(buffer.toString(), overlap);
                    buffer.setLength(0);
                    buffer.append(tail);
                    startPos = currentPos - tail.length();
                }
                int idx = 0;
                while (idx < para.length()) {
                    int end = Math.min(idx + chunkSize, para.length());
                    String piece = para.substring(idx, end);
                    int pStart = currentPos + idx;
                    int pEnd = currentPos + end;
                    out.add(Chunk.of(piece, pStart, pEnd));
                    if (end >= para.length()) break;
                    idx = end - overlap;
                    if (idx < 0) idx = end;
                }
                currentPos += para.length() + 2;
                continue;
            }

            // 累计超 chunkSize: 落块
            if (buffer.length() + para.length() + 1 > chunkSize) {
                out.add(Chunk.of(buffer.toString().trim(), startPos, currentPos));
                String tail = tail(buffer.toString(), overlap);
                buffer.setLength(0);
                buffer.append(tail);
                startPos = currentPos - tail.length();
            }
            if (buffer.length() > 0) buffer.append("\n");
            buffer.append(para);
            currentPos += para.length() + 2;
        }

        if (buffer.length() > 0) {
            out.add(Chunk.of(buffer.toString().trim(), startPos, currentPos));
        }
        return out;
    }

    private String tail(String s, int n) {
        if (s == null || s.length() <= n) return s == null ? "" : s;
        return s.substring(s.length() - n);
    }

    public record Chunk(String content, int startPos, int endPos) {
        public static Chunk of(String c, int s, int e) { return new Chunk(c, s, e); }
        public int charCount() { return content == null ? 0 : content.length(); }
    }
}
