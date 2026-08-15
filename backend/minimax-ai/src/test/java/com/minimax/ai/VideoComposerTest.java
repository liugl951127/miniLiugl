package com.minimax.ai;

import com.minimax.ai.generation.VideoComposer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VideoComposer 测试 (V2.7)
 */
class VideoComposerTest {

    private final VideoComposer composer = new VideoComposer(null);

    @Test
    void testRenderAllFrames() {
        VideoComposer.VideoConfig cfg = VideoComposer.VideoConfig.builder()
                .size(640, 360)
                .fps(10)
                .duration(1)  // 1 秒, 10 帧
                .title("测试视频")
                .dataSource("测试")
                .build();
        List<BufferedImage> frames = composer.renderAllFrames(cfg);
        assertEquals(10, frames.size());
        for (BufferedImage f : frames) {
            assertEquals(640, f.getWidth());
            assertEquals(360, f.getHeight());
        }
    }

    @Test
    void testRenderWithDataFrame() {
        VideoComposer.VideoConfig cfg = VideoComposer.VideoConfig.builder()
                .size(640, 360)
                .fps(10)
                .duration(2)
                .title("销售")
                .addFrame(VideoComposer.DataFrame.builder()
                        .time(0, 2)
                        .label("Q1 数据")
                        .addMetric(new VideoComposer.MetricCard("销售额", 100000, "元", 0.15))
                        .narrative("数据来源: 测试")
                        .build())
                .build();
        List<BufferedImage> frames = composer.renderAllFrames(cfg);
        assertEquals(20, frames.size());
    }

    @Test
    void testFormatNumber() throws Exception {
        java.lang.reflect.Method m = VideoComposer.class.getDeclaredMethod("formatNumber", double.class);
        m.setAccessible(true);
        assertEquals("1.5K", m.invoke(composer, 1500.0));
        assertEquals("2.3M", m.invoke(composer, 2_300_000.0));
        assertEquals("1.2K", m.invoke(composer, 1234.0));
        assertEquals("999", m.invoke(composer, 999.0));
    }

    @Test
    void testEaseProgress() throws Exception {
        java.lang.reflect.Method m = VideoComposer.class.getDeclaredMethod("computeProgress",
                VideoComposer.DataFrame.class, double.class);
        m.setAccessible(true);
        VideoComposer.DataFrame f = VideoComposer.DataFrame.builder().time(0, 10).build();
        // 开始时进度 0, 结束时 1
        double p0 = (double) m.invoke(composer, f, 0.0);
        double p1 = (double) m.invoke(composer, f, 10.0);
        double p5 = (double) m.invoke(composer, f, 5.0);
        assertTrue(p0 < 0.1);
        assertTrue(p1 > 0.9);
        assertTrue(p5 > 0.4 && p5 < 0.6, "Mid should be ~0.5 with ease");
    }

    @Test
    void testBarItems() {
        VideoComposer.BarItem b = new VideoComposer.BarItem("Q1", 100);
        assertEquals("Q1", b.label);
        assertEquals(100.0, b.value);
    }
}
