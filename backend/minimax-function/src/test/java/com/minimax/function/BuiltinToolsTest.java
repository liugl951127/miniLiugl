package com.minimax.function;

import com.minimax.function.builtin.CalculatorTool;
import com.minimax.function.builtin.HttpGetTool;
import com.minimax.function.builtin.RandomNumberTool;
import com.minimax.function.builtin.TimeTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinToolsTest {

    private final TimeTool time = new TimeTool();
    private final CalculatorTool calc = new CalculatorTool();
    private final RandomNumberTool random = new RandomNumberTool();
    private final HttpGetTool http = new HttpGetTool();

    @Test
    void timeTool() {
        String r = time.execute(Map.of("timezone", "Asia/Shanghai"));
        assertTrue(r.contains("Asia/Shanghai"));
        assertTrue(r.contains("datetime"));
    }

    @Test
    void timeToolInvalidTimezone() {
        String r = time.execute(Map.of("timezone", "Mars/Olympus"));
        assertTrue(r.contains("error"));
    }

    @Test
    void calculatorAdd() {
        String r = calc.execute(Map.of("expression", "1+2"));
        assertTrue(r.contains("\"result\":3"), "got: " + r);
    }

    @Test
    void calculatorWithParens() {
        String r = calc.execute(Map.of("expression", "(1+2)*3"));
        assertTrue(r.contains("\"result\":9"));
    }

    @Test
    void calculatorSqrt() {
        String r = calc.execute(Map.of("expression", "sqrt(16)"));
        assertTrue(r.contains("\"result\":4"));
    }

    @Test
    void calculatorPow() {
        String r = calc.execute(Map.of("expression", "pow(2,10)"));
        assertTrue(r.contains("\"result\":1024"));
    }

    @Test
    void calculatorEmpty() {
        String r = calc.execute(Map.of("expression", ""));
        assertTrue(r.contains("error"));
    }

    @Test
    void calculatorInvalidChars() {
        // 含 JavaScript 关键字 — 应被过滤
        String r = calc.execute(Map.of("expression", "eval(1)"));
        assertTrue(r.contains("error") || r.contains("invalid"));
    }

    @Test
    void randomInRange() {
        for (int i = 0; i < 50; i++) {
            String r = random.execute(Map.of("min", 10, "max", 20));
            // {"min":10,"max":20,"result":N}
            int n = Integer.parseInt(r.replaceAll(".*\"result\":(\\d+).*", "$1"));
            assertTrue(n >= 10 && n <= 20, "out of range: " + n);
        }
    }

    @Test
    void randomDefault() {
        String r = random.execute(Map.of());
        int n = Integer.parseInt(r.replaceAll(".*\"result\":(\\d+).*", "$1"));
        assertTrue(n >= 1 && n <= 100);
    }

    @Test
    void httpGetBlocksLocalhost() throws Exception {
        String r = http.execute(Map.of("url", "http://localhost:9999/api"));
        assertTrue(r.contains("blocked") || r.contains("error"));
    }

    @Test
    void httpGetInvalidScheme() throws Exception {
        String r = http.execute(Map.of("url", "ftp://example.com"));
        assertTrue(r.contains("error"));
    }

    @Test
    void httpGetMissingUrl() throws Exception {
        String r = http.execute(Map.of());
        assertTrue(r.contains("error"));
    }
}
