package com.minimax.ai;

import com.minimax.ai.generation.StreamingMusicGen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreamingMusicGenTest {

    @Test
    void testCreateStream() throws Exception {
        StreamingMusicGen gen = new StreamingMusicGen();
        StreamingMusicGen.MusicStreamConfig cfg = new StreamingMusicGen.MusicStreamConfig();
        cfg.config = new com.minimax.ai.generation.MusicGenerator.MusicConfig();
        cfg.config.bars = 4;
        cfg.chunkBars = 2;
        cfg.barIntervalMs = 10;
        gen.stream(cfg);
        Thread.sleep(100);
        assertEquals(1, gen.tasks().size());
        assertEquals(4, gen.tasks().values().iterator().next().totalBars);
    }

    @Test
    void testTaskIdGenerated() {
        StreamingMusicGen gen = new StreamingMusicGen();
        StreamingMusicGen.MusicStreamConfig cfg = new StreamingMusicGen.MusicStreamConfig();
        cfg.config = new com.minimax.ai.generation.MusicGenerator.MusicConfig();
        cfg.config.bars = 2;
        gen.stream(cfg);
        String id = gen.tasks().keySet().iterator().next();
        assertTrue(id.startsWith("music-"));
    }

    @Test
    void testCancel() throws Exception {
        StreamingMusicGen gen = new StreamingMusicGen();
        StreamingMusicGen.MusicStreamConfig cfg = new StreamingMusicGen.MusicStreamConfig();
        cfg.config = new com.minimax.ai.generation.MusicGenerator.MusicConfig();
        cfg.config.bars = 30;
        cfg.barIntervalMs = 200;
        cfg.chunkBars = 1;
        gen.stream(cfg);
        Thread.sleep(150);
        String id = gen.tasks().keySet().iterator().next();
        assertTrue(gen.cancel(id));
        Thread.sleep(300);
        assertEquals("CANCELLED", gen.tasks().get(id).state);
    }

    @Test
    void testStreamInfo() {
        StreamingMusicGen.StreamInfo s = new StreamingMusicGen.StreamInfo();
        s.taskId = "m1";
        s.state = "RUNNING";
        s.totalBars = 10;
        s.doneBars = 5;
        s.percent = 50;
        s.elapsedMs = 1000;
        s.etaMs = 1000;
        var m = s.toMap();
        assertEquals("m1", m.get("taskId"));
        assertEquals(50, m.get("percent"));
    }

    @Test
    void testEndToEnd() throws Exception {
        StreamingMusicGen gen = new StreamingMusicGen();
        StreamingMusicGen.MusicStreamConfig cfg = new StreamingMusicGen.MusicStreamConfig();
        cfg.config = new com.minimax.ai.generation.MusicGenerator.MusicConfig();
        cfg.config.bars = 4;
        cfg.chunkBars = 2;
        cfg.barIntervalMs = 10;
        gen.stream(cfg);
        String id = gen.tasks().keySet().iterator().next();

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            var s = gen.tasks().get(id);
            if (s != null && (s.state.equals("COMPLETED") || s.state.equals("FAILED") || s.state.equals("CANCELLED"))) {
                break;
            }
            Thread.sleep(100);
        }
        assertEquals("COMPLETED", gen.tasks().get(id).state);
        assertEquals(4, gen.tasks().get(id).doneBars);
    }
}
