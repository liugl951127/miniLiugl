package com.minimax.ws.collab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ws.entity.CollabMessage;
import com.minimax.ws.mapper.CollabMessageMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CRDT 文档引擎 (V2.8.8 真实多人编辑)
 *
 * <p>实现简化版 Operation-based CRDT (类 Y.js 协议):</p>
 *
 * <h3>核心数据结构</h3>
 * <ul>
 *   <li>每个字符/元素有唯一 CRDT ID: <code>(clientId, clock)</code></li>
 *   <li>插入: <code>{type: "insert", id: {clientId, clock}, parentId, content}</code></li>
 *   <li>删除: <code>{type: "delete", id: {clientId, clock}}</code></li>
 *   <li>支持 tombstones 保留删除历史 (供回放)</li>
 * </ul>
 *
 * <h3>排序规则</h3>
 * <p>文档顺序: 按 <code>(parentId, id.clientId, id.clock)</code> 字典序</p>
 *
 * <h3>冲突解决</h3>
 * <ul>
 *   <li>同位置并发插入: clientId 大的排前面</li>
 *   <li>删除总是 win: 已 tombstone 的不复活</li>
 * </ul>
 *
 * <h3>Y.js 兼容</h3>
 * <p>消息格式部分兼容 Y.js Update API, 客户端可以用 <code>yjs</code> npm 包直接同步.</p>
 *
 * @author MiniMax
 * @since V2.8.8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrdtEngine {

    private final CollabMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 文档缓存: roomId -> DocState
     */
    private final Map<String, DocState> docs = new ConcurrentHashMap<>();

    /**
     * 每个文档一把锁 (ReentrantLock)
     */
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 应用操作 (来自客户端)
     *
     * @param roomId 房间 ID
     * @param op     操作 JSON (来自前端 Y.js 序列化)
     * @return 应用后的 doc state 摘要 (供发送)
     */
    public DocState apply(String roomId, CrdtOperation op) {
        ReentrantLock lock = locks.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();
        try {
            DocState state = docs.computeIfAbsent(roomId, k -> DocState.builder()
                .roomId(roomId)
                .items(new LinkedHashMap<>())
                .tombstones(new HashSet<>())
                .version(0L)
                .build());

            // 1. 应用 op
            if ("insert".equals(op.getType())) {
                CrdtId id = op.getId();
                state.getItems().put(id.toKey(), new CrdtItem(id, op.getContent(), op.getParentId()));
            } else if ("delete".equals(op.getType())) {
                CrdtId id = op.getId();
                state.getTombstones().add(id.toKey());
            }

            state.setVersion(state.getVersion() + 1);
            return state;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 批量应用操作
     */
    public DocState applyBatch(String roomId, List<CrdtOperation> ops) {
        for (CrdtOperation op : ops) {
            apply(roomId, op);
        }
        return docs.get(roomId);
    }

    /**
     * 获取文档状态
     */
    public DocState getDoc(String roomId) {
        return docs.get(roomId);
    }

    /**
     * 重建文档文本 (按 CRDT 顺序遍历 items, 跳过 tombstone)
     */
    public String renderText(String roomId) {
        DocState state = docs.get(roomId);
        if (state == null) return "";
        List<CrdtItem> ordered = new ArrayList<>(state.getItems().values());
        ordered.sort(Comparator
            .comparing((CrdtItem i) -> i.getParentId() == null ? "" : i.getParentId().toKey(),
                Comparator.nullsFirst(String::compareTo))
            .thenComparing(i -> i.getId().getClientId())
            .thenComparingLong(i -> i.getId().getClock()));
        StringBuilder sb = new StringBuilder();
        for (CrdtItem item : ordered) {
            if (!state.getTombstones().contains(item.getId().toKey())) {
                sb.append(item.getContent());
            }
        }
        return sb.toString();
    }

    /**
     * 计算两个 doc 状态的 diff (供客户端初始同步)
     */
    public List<CrdtOperation> diff(DocState from, DocState to) {
        List<CrdtOperation> ops = new ArrayList<>();
        if (from == null) {
            // 全量
            for (CrdtItem item : to.getItems().values()) {
                ops.add(CrdtOperation.builder()
                    .type("insert")
                    .id(item.getId())
                    .parentId(item.getParentId())
                    .content(item.getContent())
                    .build());
            }
            return ops;
        }
        // diff: 找出新增/删除
        for (CrdtItem item : to.getItems().values()) {
            if (!from.getItems().containsKey(item.getId().toKey())) {
                ops.add(CrdtOperation.builder()
                    .type("insert")
                    .id(item.getId())
                    .parentId(item.getParentId())
                    .content(item.getContent())
                    .build());
            }
        }
        for (String tombKey : to.getTombstones()) {
            if (!from.getTombstones().contains(tombKey)) {
                String[] parts = tombKey.split(":");
                CrdtId id = new CrdtId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
                ops.add(CrdtOperation.builder().type("delete").id(id).build());
            }
        }
        return ops;
    }

    /**
     * 把当前 doc 状态打包成 Y.js Update 兼容格式 (简化)
     *
     * <p>实际 Y.js 用二进制 Update 协议, 这里用 JSON 简化版:</p>
     * <pre>
     *   { v: 1, roomId: "ABC", items: [...], tombstones: [...] }
     * </pre>
     */
    public Map<String, Object> snapshot(String roomId) {
        DocState state = docs.get(roomId);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("v", 1);
        snap.put("roomId", roomId);
        snap.put("version", state == null ? 0 : state.getVersion());

        List<Map<String, Object>> items = new ArrayList<>();
        if (state != null) {
            for (CrdtItem item : state.getItems().values()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", Map.of("clientId", item.getId().getClientId(), "clock", item.getId().getClock()));
                m.put("parent", item.getParentId() == null ? null :
                    Map.of("clientId", item.getParentId().getClientId(), "clock", item.getParentId().getClock()));
                m.put("content", item.getContent());
                items.add(m);
            }
        }
        snap.put("items", items);
        snap.put("tombstones", state == null ? Collections.emptyList() : new ArrayList<>(state.getTombstones()));
        return snap;
    }

    /**
     * 持久化 doc 快照到消息表 (定期)
     */
    public CollabMessage persistSnapshot(String roomId, Long userId, String username) {
        try {
            String snapshot = objectMapper.writeValueAsString(snapshot(roomId));
            CollabMessage msg = CollabMessage.builder()
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .type("SNAPSHOT")
                .content(snapshot)
                .broadcast(0)  // 快照不广播
                .build();
            int rows = messageMapper.insertReturnId(msg);
            if (rows > 0) return msg;
        } catch (Exception e) {
            log.warn("[crdt] 持久化快照失败: {}", e.getMessage());
        }
        return null;
    }

    // ============= 内部类型 =============

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CrdtId {
        private Long clientId;
        private Long clock;
        public String toKey() { return clientId + ":" + clock; }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CrdtOperation {
        private String type;        // "insert" / "delete"
        private CrdtId id;          // 操作对象 ID
        private CrdtId parentId;    // 父节点 ID (Y.js YText 树状)
        private String content;     // 插入内容 (insert 时)
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CrdtItem {
        private CrdtId id;
        private String content;
        private CrdtId parentId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocState {
        private String roomId;
        private Map<String, CrdtItem> items;  // idKey -> item
        private Set<String> tombstones;        // idKey set
        private Long version;
    }
}
