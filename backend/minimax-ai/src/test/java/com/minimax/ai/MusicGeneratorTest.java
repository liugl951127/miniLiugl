package com.minimax.ai;

import com.minimax.ai.generation.MusicGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MusicGenerator 测试 (V2.7)
 */
class MusicGeneratorTest {

    private final MusicGenerator gen = new MusicGenerator();

    @Test
    void testGeneratePop() {
        MusicGenerator.MusicConfig cfg = MusicGenerator.MusicConfig.builder()
                .style("pop")
                .key("C")
                .bpm(120)
                .bars(4)
                .build();
        byte[] midi = gen.generate(cfg);
        assertNotNull(midi);
        assertTrue(midi.length > 50, "MIDI file should have content");
        // MIDI starts with "MThd"
        assertEquals('M', midi[0]);
        assertEquals('T', midi[1]);
        assertEquals('h', midi[2]);
        assertEquals('d', midi[3]);
    }

    @Test
    void testGenerateWithoutDrums() {
        MusicGenerator.MusicConfig cfg = MusicGenerator.MusicConfig.builder()
                .style("classical")
                .key("G")
                .bpm(80)
                .bars(2)
                .includeDrums(false)
                .build();
        byte[] midi = gen.generate(cfg);
        assertNotNull(midi);
    }

    @Test
    void testGenerateMinor() {
        MusicGenerator.MusicConfig cfg = MusicGenerator.MusicConfig.builder()
                .key("A")
                .scale("minor")
                .bpm(140)
                .bars(8)
                .build();
        byte[] midi = gen.generate(cfg);
        assertNotNull(midi);
    }

    @Test
    void testBuildScale() throws Exception {
        java.lang.reflect.Method m = MusicGenerator.class.getDeclaredMethod("buildScale", String.class, String.class);
        m.setAccessible(true);
        int[] scale = (int[]) m.invoke(gen, "C", "major");
        assertEquals(8, scale.length, "Major scale has 8 notes");
        assertEquals(60, scale[0], "C major starts at 60 (middle C)");
    }

    @Test
    void testBuildScaleMinor() throws Exception {
        java.lang.reflect.Method m = MusicGenerator.class.getDeclaredMethod("buildScale", String.class, String.class);
        m.setAccessible(true);
        int[] scale = (int[]) m.invoke(gen, "A", "minor");
        assertEquals(8, scale.length);
        assertEquals(60 + 9, scale[0], "A minor starts at A (60 + 9)");
    }

    @Test
    void testAllStyles() {
        for (MusicGenerator.Style s : MusicGenerator.Style.values()) {
            MusicGenerator.MusicConfig cfg = MusicGenerator.MusicConfig.builder()
                    .style(s)
                    .bpm(120)
                    .bars(4)
                    .build();
            byte[] midi = gen.generate(cfg);
            assertNotNull(midi, "Style " + s + " should generate MIDI");
        }
    }
}
