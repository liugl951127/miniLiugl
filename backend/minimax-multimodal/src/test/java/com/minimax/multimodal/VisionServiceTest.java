package com.minimax.multimodal;

import com.minimax.multimodal.provider.*;
import com.minimax.multimodal.service.VisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VisionService 单元测试 (V3.0.1 重构)
 *
 * <p>覆盖场景:
 *   1. 默认 provider 调用
 *   2. 指定 model 路由
 *   3. provider 失败时降级
 *   4. 所有 provider 都失败的兜底
 *   5. inspect 调用
 *   6. 异常 provider 不影响其他
 */
class VisionServiceTest {

    /** 视觉服务实例 */
    private VisionService service;

    /** 注册表 (注入所有 provider) */
    private MultimodalModelRegistry registry;

    /**
     * 准备测试环境: 注入所有 provider
     */
    @BeforeEach
    void setUp() {
        // 1. 创建所有 provider 实例
        List<MultimodalModelProvider> providers = new ArrayList<>();
        providers.add(new MockVisionProvider());         // mock
        providers.add(new BuiltinVisionProvider());      // builtin
        providers.add(new OpenAIVisionProvider());       // openai (未配置 key, 未就绪)
        providers.add(new LocalOnnxVisionProvider());    // local-onnx (无模型, 未就绪)
        // 2. 构造 registry
        registry = new MultimodalModelRegistry(providers);
        // 3. 注入 @Value 字段 (单元测试无 Spring 容器, 手动注入默认值)
        ReflectionTestUtils.setField(registry, "defaultProvider", "builtin");
        ReflectionTestUtils.setField(registry, "fallbackChain", "builtin,mock");
        registry.init();
        // 4. 构造 service
        service = new VisionService(registry);
    }

    /**
     * 测试 1: 不指定 model 时, 用默认 (builtin, 因为没配 openai key)
     */
    @Test
    @DisplayName("1. 不指定 model → 用默认 builtin")
    void testDefaultProvider() {
        // 1. 准备: 一个真实 PNG 1x1 红色像素
        String b64 = pngRedPixel();
        // 2. 调用
        String result = service.describe(b64, "image/png", "什么颜色");
        // 3. 验证: 应该包含 builtin 标识
        assertNotNull(result);
        assertTrue(result.contains("内置"), "应使用 builtin provider, 实际: " + result);
    }

    /**
     * 测试 2: 指定 model=mock
     */
    @Test
    @DisplayName("2. 指定 model=mock → 走 mock")
    void testExplicitMock() {
        // 1. 任意 base64
        String b64 = pngRedPixel();
        // 2. 调用
        String result = service.describe(b64, "image/png", "test", "mock");
        // 3. 验证
        assertTrue(result.contains("Mock"), "应使用 mock, 实际: " + result);
    }

    /**
     * 测试 3: provider 失败时降级
     *
     * <p>用一个总是抛异常的 provider 测试降级链
     */
    @Test
    @DisplayName("3. 优先 provider 失败 → 降级到下一个")
    void testFallbackOnFailure() {
        // 1. 准备: 创建会失败的 provider (name=failing, 永远抛异常)
        MultimodalModelProvider failing = new MultimodalModelProvider() {
            @Override public String name() { return "failing"; }
            @Override public String description() { return "测试用, 永远失败"; }
            @Override public boolean isReady() { return true; }
            @Override
            public String describe(String b, String m, String p) {
                throw new RuntimeException("simulated failure");
            }
            @Override
            public Map<String, Object> inspect(String b) { return Map.of("format", "test"); }
        };
        // 2. 创建 registry with failing 在前
        List<MultimodalModelProvider> providers = new ArrayList<>();
        providers.add(failing);                                   // 失败 provider
        providers.add(new MockVisionProvider());                 // mock 兜底
        MultimodalModelRegistry r = new MultimodalModelRegistry(providers);
        ReflectionTestUtils.setField(r, "defaultProvider", "failing");
        ReflectionTestUtils.setField(r, "fallbackChain", "failing,mock");
        r.init();
        VisionService s = new VisionService(r);
        // 3. 调用
        String result = s.describe("xx", "image/png", "test", "failing");
        // 4. 验证: 应该降级到 mock
        assertTrue(result.contains("Mock"), "应降级到 mock, 实际: " + result);
    }

    /**
     * 测试 4: 所有 provider 都失败
     */
    @Test
    @DisplayName("4. 所有 provider 都失败 → 返回错误信息")
    void testAllProvidersFail() {
        // 1. 全部失败的 registry
        MultimodalModelProvider failing1 = failing("f1");
        MultimodalModelProvider failing2 = failing("f2");
        MultimodalModelRegistry r = new MultimodalModelRegistry(List.of(failing1, failing2));
        ReflectionTestUtils.setField(r, "defaultProvider", "f1");
        ReflectionTestUtils.setField(r, "fallbackChain", "f1,f2,mock");
        r.init();
        VisionService s = new VisionService(r);
        // 2. 调用
        String result = s.describe("xx", "image/png", "test");
        // 3. 验证
        assertTrue(result.contains("暂不可用"), "应返回兜底, 实际: " + result);
    }

    /**
     * 测试 5: inspect 委托给默认 provider
     */
    @Test
    @DisplayName("5. inspect → 委托默认 provider")
    void testInspect() {
        // 1. 一个 1x1 PNG
        String b64 = pngRedPixel();
        // 2. inspect
        Map<String, Object> info = service.inspect(b64);
        // 3. 验证
        assertNotNull(info);
        assertEquals("png", info.get("format"));
    }

    /**
     * 测试 6: 多图理解
     */
    @Test
    @DisplayName("6. 多图理解 → 降级到单图")
    void testDescribeMulti() {
        // 1. 准备 2 张图
        List<Map<String, String>> images = new ArrayList<>();
        Map<String, String> img1 = new HashMap<>();
        img1.put("base64", pngRedPixel());
        img1.put("mimeType", "image/png");
        images.add(img1);
        // 2. 调用
        String result = service.describeMulti(images, "对比");
        // 3. 验证
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    /**
     * 测试 7: 列出所有 provider
     */
    @Test
    @DisplayName("7. 列出所有 provider")
    void testListProviders() {
        List<Map<String, Object>> list = registry.listAll();
        // 至少 4 个 (mock, builtin, openai, local-onnx)
        assertTrue(list.size() >= 4, "至少 4 个 provider, 实际: " + list.size());
    }

    /**
     * 辅助: 生成失败 provider
     */
    private MultimodalModelProvider failing(String name) {
        return new MultimodalModelProvider() {
            @Override public String name() { return name; }
            @Override public String description() { return "test failing"; }
            @Override public boolean isReady() { return true; }
            @Override
            public String describe(String b, String m, String p) {
                throw new RuntimeException("intentional failure");
            }
            @Override
            public Map<String, Object> inspect(String b) { return Map.of(); }
        };
    }

    /**
     * 辅助: 1x1 红色 PNG 的 base64
     *
     * <p>用 Java ImageIO 动态生成 1x1 红色 PNG, 避免硬编码字节
     */
    private String pngRedPixel() {
        try {
            // 1. 创建 1x1 红色 BufferedImage
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0xFF0000);  // 红色
            // 2. 编码 PNG
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            // 3. base64
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
