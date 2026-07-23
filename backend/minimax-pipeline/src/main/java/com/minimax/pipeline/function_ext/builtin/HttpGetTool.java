package com.minimax.pipeline.function_ext.builtin;

import com.minimax.pipeline.function_ext.executor.ToolFunction;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class HttpGetTool implements ToolFunction {
    @Override public String name() { return "http_get"; }

    private static final int MAX_BODY = 5000;
    private static final String BLOCK_HOST = "^(localhost|127\\.|0\\.0\\.0\\.|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.).*";

    @Override
    public String execute(Map<String, Object> args) throws Exception {
        if (args == null) return "{\"error\":\"missing url\"}";
        String url = (String) args.get("url");
        if (url == null || url.isBlank()) return "{\"error\":\"missing url\"}";
        Integer timeout = (Integer) args.getOrDefault("timeout_seconds", 10);
        if (timeout == null || timeout < 1) timeout = 10;
        if (timeout > 60) timeout = 60;

        // SSRF 防护: 阻止内网
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null) return "{\"error\":\"invalid url\"}";
        if (host.matches(BLOCK_HOST)) {
            return "{\"error\":\"internal host blocked: " + host + "\"}";
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            return "{\"error\":\"only http/https allowed\"}";
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(timeout))
                    .header("User-Agent", "MiniMax-Function-Caller/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            if (body != null && body.length() > MAX_BODY) {
                body = body.substring(0, MAX_BODY) + "...[truncated " + (resp.body().length() - MAX_BODY) + " chars]";
            }
            return "{\"status\":" + resp.statusCode() + ",\"body\":" + jsonEscape(body) + "}";
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String jsonEscape(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
