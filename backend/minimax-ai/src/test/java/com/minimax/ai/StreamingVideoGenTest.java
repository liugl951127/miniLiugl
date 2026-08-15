package com.minimax.ai;

import com.minimax.ai.generation.StreamingVideoGen;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StreamingVideoGenTest {

    @Test
    void testCreateStream() throws Exception {
        StreamingVideoGen gen = new StreamingVideoGen();
        StreamingVideoGen.StreamConfig cfg = new StreamingVideoGen.StreamConfig();
        cfg.title = "test";
        cfg.width = 100;
        cfg.height = 100;
        cfg.fps = 5;
        cfg.durationSeconds = 1;
        cfg.chunkSize = 1;
        cfg.frameIntervalMs = 10;

        var emitter = gen.stream(cfg);
        assertNotNull(emitter);

        // 等到任务加入 tasks
        Thread.sleep(100);
        assertEquals(1, gen.tasks().size());
        assertEquals(5, gen.tasks().values().iterator().next().progress.totalFrames);
    }

    @Test
    void testTaskIdGenerated() {
        StreamingVideoGen gen = new StreamingVideoGen();
        StreamingVideoGen.StreamConfig cfg = new StreamingVideoGen.StreamConfig();
        cfg.title = "auto";
        cfg.fps = 1;
        cfg.durationSeconds = 1;
        gen.stream(cfg);
        String id = gen.tasks().keySet().iterator().next();
        assertTrue(id.startsWith("stream-"));
    }

    @Test
    void testCancel() throws Exception {
        StreamingVideoGen gen = new StreamingVideoGen();
        StreamingVideoGen.StreamConfig cfg = new StreamingVideoGen.StreamConfig();
        cfg.title = "cancel-test";
        cfg.fps = 2;
        cfg.durationSeconds = 30;        // 足够长才能 cancel
        cfg.frameIntervalMs = 100;
        cfg.chunkSize = 1;
        gen.stream(cfg);
        Thread.sleep(150);                // 等任务进入 RUNNING
        String id = gen.tasks().keySet().iterator().next();
        assertTrue(gen.cancel(id));
        Thread.sleep(300);
        assertEquals("CANCELLED", gen.tasks().get(id).state);
    }

    @Test
    void testProgressInfo() {
        StreamingVideoGen.ProgressInfo p = new StreamingVideoGen.ProgressInfo();
        p.taskId = "t1";
        p.totalFrames = 10;
        p.doneFrames = 5;
        p.percent = 50;
        p.elapsedMs = 1000;
        p.etaMs = 1000;
        p.totalBytes = 5000;
        assertEquals("t1", p.taskId);
        assertEquals(50, p.percent);
    }

    @Test
    void testStreamStatusToMap() {
        StreamingVideoGen.StreamStatus s = new StreamingVideoGen.StreamStatus();
        s.taskId = "abc";
        s.state = "RUNNING";
        s.progress = new StreamingVideoGen.ProgressInfo();
        s.error = null;
        var m = s.toMap();
        assertEquals("abc", m.get("taskId"));
        assertEquals("RUNNING", m.get("state"));
    }

    @Test
    void testStreamEndToEnd() throws Exception {
        StreamingVideoGen gen = new StreamingVideoGen();
        StreamingVideoGen.StreamConfig cfg = new StreamingVideoGen.StreamConfig();
        cfg.title = "e2e";
        cfg.fps = 5;
        cfg.durationSeconds = 1;
        cfg.frameIntervalMs = 10;
        cfg.chunkSize = 1;
        gen.stream(cfg);
        String id = gen.tasks().keySet().iterator().next();

        // 等完成
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            var s = gen.tasks().get(id);
            if (s != null && (s.state.equals("COMPLETED") || s.state.equals("FAILED") || s.state.equals("CANCELLED"))) {
                break;
            }
            Thread.sleep(100);
        }
        assertEquals("COMPLETED", gen.tasks().get(id).state);
        assertEquals(5, gen.tasks().get(id).progress.doneFrames);
    }

    @Test
    void testRenderFrameNotEmpty() throws Exception {
        StreamingVideoGen gen = new StreamingVideoGen();
        java.lang.reflect.Method m = StreamingVideoGen.class.getDeclaredMethod(
                "renderFrame", StreamingVideoGen.StreamConfig.class, int.class, int.class);
        m.setAccessible(true);
        StreamingVideoGen.StreamConfig cfg = new StreamingVideoGen.StreamConfig();
        cfg.title = "frame";
        cfg.width = 100;
        cfg.height = 100;
        String s = (String) m.invoke(gen, cfg, 0, 10);
        assertNotNull(s);
        assertFalse(s.isEmpty());
        assertTrue(s.length() > 100, "Frame should be encoded PNG base64");
    }
}
