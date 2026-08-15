package com.minimax.rag.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析器注册表 — 按 sourceType 选择对应 parser。
 */
@Component
@RequiredArgsConstructor
public class ParserRegistry {

    private final List<DocumentParser> parsers;

    public DocumentParser resolve(String sourceType) {
        if (sourceType == null) {
            return parsers.stream()
                    .filter(p -> p.supports("txt"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("no txt parser"));
        }
        return parsers.stream()
                .filter(p -> p.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no parser for " + sourceType));
    }
}
