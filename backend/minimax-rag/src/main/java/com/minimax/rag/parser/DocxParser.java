package com.minimax.rag.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
public class DocxParser implements DocumentParser {

    @Override
    public boolean supports(String sourceType) {
        return "docx".equalsIgnoreCase(sourceType);
    }

    @Override
    public String parse(byte[] content, String filename) {
        if (content == null || content.length == 0) return "";
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder sb = new StringBuilder();
            List<XWPFParagraph> paras = doc.getParagraphs();
            for (XWPFParagraph p : paras) {
                String t = p.getText();
                if (t != null && !t.isEmpty()) {
                    sb.append(t).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("DOCX 解析失败: " + e.getMessage(), e);
        }
    }
}
