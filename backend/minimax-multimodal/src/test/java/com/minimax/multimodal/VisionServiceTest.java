package com.minimax.multimodal;

import com.minimax.multimodal.service.VisionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VisionServiceTest {

    private VisionService service() {
        VisionService s = new VisionService();
        ReflectionTestUtils.setField(s, "mockMode", true);
        ReflectionTestUtils.setField(s, "apiKey", "");
        ReflectionTestUtils.setField(s, "baseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(s, "model", "gpt-4o");
        return s;
    }

    @Test
    void mockDescribeIncludesSize() {
        VisionService s = service();
        // 1x1 PNG 字节
        byte[] png = new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        String b64 = Base64.getEncoder().encodeToString(png);
        String r = s.describe(b64, "image/png", "看啥");
        assertTrue(r.contains("Mock"));
        assertTrue(r.contains("image/png"));
        assertTrue(r.contains("看啥"));
    }

    @Test
    void mockDescribeEmptyBase64() {
        VisionService s = service();
        String r = s.describe("", "image/png", "test");
        assertTrue(r.contains("Mock"));
    }

    @Test
    void inspectPng() {
        VisionService s = service();
        byte[] png = new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        String b64 = Base64.getEncoder().encodeToString(png);
        Map<String, Object> info = s.inspect(b64);
        assertEquals("png", info.get("format"));
    }

    @Test
    void inspectJpeg() {
        VisionService s = service();
        byte[] jpeg = new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x10};
        String b64 = Base64.getEncoder().encodeToString(jpeg);
        Map<String, Object> info = s.inspect(b64);
        assertEquals("jpeg", info.get("format"));
    }

    @Test
    void inspectGif() {
        VisionService s = service();
        byte[] gif = new byte[]{'G', 'I', 'F', '8', '9', 'a', 0x00, 0x00};
        String b64 = Base64.getEncoder().encodeToString(gif);
        Map<String, Object> info = s.inspect(b64);
        assertEquals("gif", info.get("format"));
    }

    @Test
    void inspectEmpty() {
        VisionService s = service();
        Map<String, Object> info = s.inspect("");
        assertTrue(info.containsKey("error"));
    }

    @Test
    void describeMultiTakesFirst() {
        VisionService s = service();
        byte[] png = new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        String b64 = Base64.getEncoder().encodeToString(png);
        var images = java.util.List.of(
                Map.of("base64", b64, "mimeType", "image/png"),
                Map.of("base64", "abc", "mimeType", "image/jpeg")
        );
        String r = s.describeMulti(images, "对比");
        assertTrue(r.contains("Mock"));
    }
}
