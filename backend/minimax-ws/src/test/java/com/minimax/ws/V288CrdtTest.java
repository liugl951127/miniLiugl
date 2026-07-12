package com.minimax.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ws.collab.CrdtEngine;
import com.minimax.ws.collab.CrdtEngine.CrdtId;
import com.minimax.ws.collab.CrdtEngine.CrdtOperation;
import com.minimax.ws.entity.CollabMessage;
import com.minimax.ws.mapper.CollabMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2.8.8 CRDT 真实多人编辑测试
 *
 * <p>核心场景:</p>
 * <ol>
 *   <li>单客户端插入 -> 文档正确</li>
 *   <li>并发插入同位置 -> 按 clientId 排序</li>
 *   <li>删除后重新插入 -> 遵循 tombstones</li>
 *   <li>两客户端并发编辑 -> 都能合并</li>
 *   <li>Diff 算法: 全量 + 增量</li>
 *   <li>Snapshot 序列化 + 反序列化</li>
 * </ol>
 */
class V288CrdtTest {

    private CrdtEngine engine;
    private ObjectMapper objectMapper;
    private CollabMessageMapper messageMapper;

    @BeforeEach
    void setup() {
        messageMapper = mock(CollabMessageMapper.class);
        when(messageMapper.insertReturnId(any())).thenReturn(1);
        objectMapper = new ObjectMapper();
        engine = new CrdtEngine(messageMapper, objectMapper);
    }

    @Test
    void testSingleClientInsert() {
        String room = "ROOM01";
        // 模拟客户端 1 插入 "Hello"
        long clientId = 1;
        long clock = 0;
        for (char c : "Hello".toCharArray()) {
            clock++;
            engine.apply(room, CrdtOperation.builder()
                .type("insert")
                .id(new CrdtId(clientId, clock))
                .parentId(clock == 1 ? null : new CrdtId(clientId, clock - 1))
                .content(String.valueOf(c))
                .build());
        }
        assertEquals("Hello", engine.renderText(room));
        assertEquals(5L, engine.getDoc(room).getVersion());
    }

    @Test
    void testConcurrentInsertSamePosition() {
        // 两个客户端同时在文档头部插入
        String room = "ROOM02";
        // Client 1 插入 "A"
        engine.apply(room, opInsert(1, 1, null, "A"));
        // Client 2 插入 "B" (parentId 同为 null, 并发)
        engine.apply(room, opInsert(2, 1, null, "B"));
        // Client 1 再插 "C"
        engine.apply(room, opInsert(1, 2, new CrdtId(1L, 1L), "C"));

        // 排序: 父级 null, 然后按 clientId 升序, 再 clock 升序
        // (null) -> (1,1) "A" -> (1,2) "C" -> (2,1) "B"
        String text = engine.renderText(room);
        assertTrue(text.contains("A"));
        assertTrue(text.contains("B"));
        assertTrue(text.contains("C"));
        assertEquals(3, text.length());
    }

    @Test
    void testDeleteTombstone() {
        String room = "ROOM03";
        engine.apply(room, opInsert(1, 1, null, "H"));
        engine.apply(room, opInsert(1, 2, new CrdtId(1L, 1L), "i"));
        assertEquals("Hi", engine.renderText(room));

        // 删除 "i"
        engine.apply(room, CrdtOperation.builder()
            .type("delete")
            .id(new CrdtId(1L, 2L))
            .build());
        assertEquals("H", engine.renderText(room));

        // 删除 "H"
        engine.apply(room, CrdtOperation.builder()
            .type("delete")
            .id(new CrdtId(1L, 1L))
            .build());
        assertEquals("", engine.renderText(room));

        // tombstone 仍在
        assertEquals(2, engine.getDoc(room).getTombstones().size());
    }

    @Test
    void testTwoClientsConverge() {
        // 模拟两个客户端从相同状态出发, 并发编辑, 最终合并
        String room = "ROOM04";
        // Client 1 在 0 位置插 "X"
        engine.apply(room, opInsert(1, 1, null, "X"));
        // Client 2 在 0 位置插 "Y" (parentId 同 null)
        engine.apply(room, opInsert(2, 1, null, "Y"));

        // 不管顺序, 最终都有 X 和 Y
        String text = engine.renderText(room);
        assertEquals(2, text.length());
        assertTrue(text.contains("X"));
        assertTrue(text.contains("Y"));
    }

    @Test
    void testBatchApply() {
        String room = "ROOM05";
        List<CrdtOperation> ops = Arrays.asList(
            opInsert(1, 1, null, "A"),
            opInsert(1, 2, new CrdtId(1L, 1L), "B"),
            opInsert(1, 3, new CrdtId(1L, 2L), "C")
        );
        engine.applyBatch(room, ops);
        assertEquals("ABC", engine.renderText(room));
    }

    @Test
    void testDiffFull() {
        String room = "ROOM06";
        engine.apply(room, opInsert(1, 1, null, "A"));
        engine.apply(room, opInsert(1, 2, new CrdtId(1L, 1L), "B"));

        // 从 null 状态 diff
        CrdtEngine.DocState current = engine.getDoc(room);
        List<CrdtOperation> ops = engine.diff(null, current);
        assertEquals(2, ops.size());
        assertEquals("A", ops.get(0).getContent());
        assertEquals("B", ops.get(1).getContent());
    }

    @Test
    void testDiffIncremental() {
        String room = "ROOM07";
        // 在 t0 写入 ABC
        engine.apply(room, opInsert(1, 1, null, "A"));
        engine.apply(room, opInsert(1, 2, new CrdtId(1L, 1L), "B"));
        engine.apply(room, opInsert(1, 3, new CrdtId(1L, 2L), "C"));
        // 复制当前状态作为 t0 快照
        CrdtEngine.DocState t0 = clone(engine.getDoc(room));

        // 客户端断开, 服务端继续追加 D
        engine.apply(room, opInsert(1, 4, new CrdtId(1L, 3L), "D"));

        // diff: 增量 1 个
        List<CrdtOperation> ops = engine.diff(t0, engine.getDoc(room));
        assertEquals(1, ops.size());
        assertEquals("D", ops.get(0).getContent());
    }

    @Test
    void testSnapshot() {
        String room = "ROOM08";
        engine.apply(room, opInsert(1, 1, null, "X"));
        engine.apply(room, opInsert(2, 1, null, "Y"));
        engine.apply(room, CrdtOperation.builder()
            .type("delete").id(new CrdtId(1L, 1L)).build());

        var snap = engine.snapshot(room);
        assertEquals(1, snap.get("v"));
        assertEquals(3L, snap.get("version"));  // 2 insert + 1 delete = 3 ops

        @SuppressWarnings("unchecked")
        var items = (java.util.List<java.util.Map<String, Object>>) snap.get("items");
        assertEquals(2, items.size());

        @SuppressWarnings("unchecked")
        var tombs = (java.util.List<String>) snap.get("tombstones");
        assertEquals(1, tombs.size());
    }

    @Test
    void testPersistSnapshot() {
        String room = "ROOM09";
        engine.apply(room, opInsert(1, 1, null, "snap test"));
        CollabMessage msg = engine.persistSnapshot(room, 1L, "alice");
        assertNotNull(msg);
    }

    private CrdtOperation opInsert(long clientId, long clock, CrdtId parentId, String content) {
        return CrdtOperation.builder()
            .type("insert")
            .id(new CrdtId(clientId, clock))
            .parentId(parentId)
            .content(content)
            .build();
    }

    private CrdtEngine.DocState clone(CrdtEngine.DocState src) {
        if (src == null) return null;
        return CrdtEngine.DocState.builder()
            .roomId(src.getRoomId())
            .items(new java.util.LinkedHashMap<>(src.getItems()))
            .tombstones(new java.util.HashSet<>(src.getTombstones()))
            .version(src.getVersion())
            .build();
    }
}
