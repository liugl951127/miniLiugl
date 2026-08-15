package com.minimax.ai;

import com.minimax.ai.generation.ImageGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageGeneratorTest {

    @Test
    void testGenerateAbstract() {
        ImageGenerator g = new ImageGenerator();
        ImageGenerator.ImageRequest req = new ImageGenerator.ImageRequest();
        req.prompt = "test abstract";
        req.type = "abstract";
        req.width = 256;
        req.height = 256;
        ImageGenerator.ImageResult r = g.generate(req);
        assertEquals("abstract", r.type);
        assertNotNull(r.base64);
        assertTrue(r.sizeBytes > 0);
    }

    @Test
    void testGenerateGradient() {
        ImageGenerator g = new ImageGenerator();
        ImageGenerator.ImageRequest req = new ImageGenerator.ImageRequest();
        req.prompt = "渐变背景";
        req.type = "gradient";
        req.width = 128;
        req.height = 128;
        ImageGenerator.ImageResult r = g.generate(req);
        assertNotNull(r.base64);
    }

    @Test
    void testGenerateAllTypes() {
        for (String type : new String[]{"abstract", "gradient", "pattern", "text", "scene", "logo", "infographic"}) {
            ImageGenerator g = new ImageGenerator();
            ImageGenerator.ImageRequest req = new ImageGenerator.ImageRequest();
            req.prompt = "p";
            req.type = type;
            req.width = 64;
            req.height = 64;
            ImageGenerator.ImageResult r = g.generate(req);
            assertNotNull(r.base64, "Type " + type + " produced no image");
            assertEquals(type, r.type);
        }
    }

    @Test
    void testInferType() {
        ImageGenerator g = new ImageGenerator();
        assertEquals("gradient", g.inferType("蓝色渐变"));
        assertEquals("logo", g.inferType("公司 logo"));
        assertEquals("scene", g.inferType("山景日落"));
        assertEquals("infographic", g.inferType("数据图表"));
        assertEquals("text", g.inferType("宣传海报"));
        assertEquals("pattern", g.inferType("花纹图案"));
        assertEquals("abstract", g.inferType("随便画点"));
    }

    @Test
    void testSeedDeterministic() {
        ImageGenerator g = new ImageGenerator();
        ImageGenerator.ImageRequest r1 = new ImageGenerator.ImageRequest();
        r1.prompt = "same";
        r1.type = "abstract";
        r1.seed = 42L;
        r1.width = 64; r1.height = 64;

        ImageGenerator.ImageRequest r2 = new ImageGenerator.ImageRequest();
        r2.prompt = "same";
        r2.type = "abstract";
        r2.seed = 42L;
        r2.width = 64; r2.height = 64;

        ImageGenerator.ImageResult a = g.generate(r1);
        ImageGenerator.ImageResult b = g.generate(r2);
        assertEquals(a.base64, b.base64, "Same seed should give same image");
    }
}
