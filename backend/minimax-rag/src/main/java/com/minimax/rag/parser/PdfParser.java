package com.minimax.rag.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfParser implements DocumentParser {

    @Override
    public boolean supports(String sourceType) {
        return "pdf".equalsIgnoreCase(sourceType);
    }

    @Override
    public String parse(byte[] content, String filename) {
        if (content == null || content.length == 0) return "";
        try (PDDocument doc = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new RuntimeException("PDF 解析失败: " + e.getMessage(), e);
        }
    }
}
