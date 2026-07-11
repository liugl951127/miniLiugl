package com.minimax.ai;

import com.minimax.ai.multimodal.VideoAnalyzer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 视频分析器测试 (V2.6 多模态)
 */
class VideoAnalyzerTest {

    /**
     * 构造一个最小合法 MP4 文件 (含 ftyp + moov + mvhd + mdat)
     */
    private byte[] createMinimalMp4() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // ftyp
            writeBox(baos, "ftyp", new byte[]{
                    'i', 's', 'o', 'm',
                    0, 0, 0, 1,
                    'i', 's', 'o', 'm'
            });

            // moov > mvhd (version 0)
            ByteArrayOutputStream moov = new ByteArrayOutputStream();
            // mvhd 100 bytes
            ByteArrayOutputStream mvhd = new ByteArrayOutputStream();
            DataOutputStream mvhdDos = new DataOutputStream(mvhd);
            // version + flags
            mvhdDos.writeInt(0); // version 0 + 3 字节 flags
            // creation/modification time
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0);
            // timescale = 1000
            mvhdDos.writeInt(1000);
            // duration = 5000 (5秒)
            mvhdDos.writeInt(5000);
            mvhdDos.writeInt(0x00010000); // rate 1.0
            mvhdDos.writeShort((short) 0x0100); // volume 1.0
            mvhdDos.write(new byte[10]); // reserved
            // matrix 9 * 4 bytes
            mvhdDos.writeInt(0x00010000);
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0x00010000);
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0);
            mvhdDos.writeInt(0x40000000);
            mvhdDos.write(new byte[24]); // pre-defined
            mvhdDos.writeInt(2); // next track ID
            writeBox(moov, "mvhd", mvhd.toByteArray());
            writeBox(baos, "moov", moov.toByteArray());

            // mdat
            writeBox(baos, "mdat", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBox(ByteArrayOutputStream out, String type, byte[] data) throws Exception {
        int size = 8 + data.length;
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(size);
        dos.write(type.getBytes());
        dos.write(data);
    }

    @Test
    void testAnalyzeMp4() {
        VideoAnalyzer analyzer = new VideoAnalyzer();
        byte[] mp4 = createMinimalMp4();
        VideoAnalyzer.VideoAnalysisResult r = analyzer.analyze(mp4, "test.mp4");

        assertEquals("mp4", r.format);
        assertEquals(mp4.length, r.fileSize);
        assertTrue(r.hasFtyp, "应有 ftyp");
        assertTrue(r.hasMoov, "应有 moov");
        assertEquals(5000, r.durationMs, "5 秒视频");
    }

    @Test
    void testIsSupported() {
        VideoAnalyzer analyzer = new VideoAnalyzer();
        assertTrue(analyzer.isSupported("a.mp4"));
        assertTrue(analyzer.isSupported("a.mov"));
        assertFalse(analyzer.isSupported("a.avi"));
    }

    @Test
    void testExtractMetadata() {
        VideoAnalyzer analyzer = new VideoAnalyzer();
        byte[] mp4 = createMinimalMp4();
        var meta = analyzer.extractMetadata(mp4);

        assertEquals("mp4", meta.get("format"));
        assertEquals(5000L, meta.get("durationMs"));
        assertNotNull(meta.get("sha256"));
    }

    @Test
    void testRejectEmpty() {
        VideoAnalyzer analyzer = new VideoAnalyzer();
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(new byte[0], "a.mp4"));
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null, "a.mp4"));
    }

    @Test
    void testHandleInvalidMp4() {
        VideoAnalyzer analyzer = new VideoAnalyzer();
        // 不是 MP4
        byte[] data = "not a video file".getBytes();
        VideoAnalyzer.VideoAnalysisResult r = analyzer.analyze(data, "fake.mp4");
        // 应该不抛错, 但 hasFtyp = false
        assertFalse(r.hasFtyp);
    }
}
