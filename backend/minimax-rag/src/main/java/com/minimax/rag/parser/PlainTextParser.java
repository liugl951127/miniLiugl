package com.minimax.rag.parser;

import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class PlainTextParser implements DocumentParser {

    @Override
    public boolean supports(String sourceType) {
        return "txt".equalsIgnoreCase(sourceType) || "md".equalsIgnoreCase(sourceType);
    }

    @Override
    public String parse(byte[] content, String filename) {
        if (content == null || content.length == 0) return "";
        // 简单 BOM 探测
        int offset = 0;
        Charset cs = StandardCharsets.UTF_8;
        if (content.length >= 3 && content[0] == (byte)0xEF && content[1] == (byte)0xBB && content[2] == (byte)0xBF) {
            offset = 3; // UTF-8 BOM
        } else if (content.length >= 2
                && ((content[0] == (byte)0xFF && content[1] == (byte)0xFE)
                || (content[0] == (byte)0xFE && content[1] == (byte)0xFF))) {
            cs = StandardCharsets.UTF_16;
            offset = 2;
        }
        return new String(content, offset, content.length - offset, cs);
    }
}
