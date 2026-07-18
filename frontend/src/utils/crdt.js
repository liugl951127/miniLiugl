/**
 * @file crdt.js - 工具函数 (V3.5.12+)
 */

// V2.8.8 协作 CRDT 客户端 (Y.js 协议子集兼容)
// 不依赖 yjs npm 包, 纯 JS 实现, 减少前端体积
// 协议与后端 CrdtEngine 完全对称

/**
 * CRDT ID 工厂: 生成唯一 (clientId, clock) 对
 */
export class CrdtIdFactory {
  constructor(clientId) {
    this.clientId = clientId || Math.floor(Math.random() * 1e9) + 1
    this.clock = 0
  }
  next() {
    this.clock += 1
    return { clientId: this.clientId, clock: this.clock }
  }
  setClock(clock) {
    if (clock > this.clock) this.clock = clock
  }
}

/**
 * 本地 CRDT 文档 (类 Y.YText)
 *
 * 维护:
 *  - items: Map<idKey, {id, parentId, content}>
 *  - tombstones: Set<idKey>
 *  - 派生 text: 按 (parent, clientId, clock) 排序
 */
export class CrdtDoc {
  constructor(idFactory) {
    this.idFactory = idFactory
    this.items = new Map()
    this.tombstones = new Set()
    this.observers = new Set()
  }

  applyOp(op) {
    if (op.type === 'insert') {
      const key = idKey(op.id)
      this.items.set(key, { id: op.id, parentId: op.parentId, content: op.content })
      // 同步 idFactory clock
      this.idFactory.setClock(op.id.clock)
    } else if (op.type === 'delete') {
      const key = idKey(op.id)
      this.tombstones.add(key)
      this.idFactory.setClock(op.id.clock)
    }
    this._notify(op)
  }

  applyBatch(ops) {
    for (const op of ops) this.applyOp(op)
  }

  /**
   * 派生文本 (按 CRDT 顺序)
   */
  toText() {
    const ordered = [...this.items.values()].sort((a, b) => {
      const ap = a.parentId ? idKey(a.parentId) : ''
      const bp = b.parentId ? idKey(b.parentId) : ''
      if (ap < bp) return -1
      if (ap > bp) return 1
      if (a.id.clientId !== b.id.clientId) return a.id.clientId - b.id.clientId
      return a.id.clock - b.id.clock
    })
    let s = ''
    for (const item of ordered) {
      if (!this.tombstones.has(idKey(item.id))) {
        s += item.content
      }
    }
    return s
  }

  /**
   * 本地插入: 在 pos 位置插入 content
   * 返回 op (供发送到服务端)
   */
  insertAt(pos, content) {
    // 找到 pos 对应的 parentId (前一个 item)
    const ordered = this._orderedItems()
    const parentId = pos > 0 && pos <= ordered.length
      ? ordered[pos - 1].id
      : null
    const newId = this.idFactory.next()
    const op = {
      type: 'insert',
      id: newId,
      parentId: parentId,
      content: content
    }
    this.applyOp(op)
    return op
  }

  /**
   * 本地删除: pos 位置删除 1 字符
   */
  deleteAt(pos) {
    const ordered = this._orderedItems()
    if (pos < 0 || pos >= ordered.length) return null
    const target = ordered[pos]
    if (this.tombstones.has(idKey(target.id))) return null
    const newId = this.idFactory.next()
    const op = {
      type: 'delete',
      id: newId,           // 删除操作的 id (任意, 标识此次删除)
      // 实际指向被删 item:
      targetId: target.id,
      parentId: null
    }
    // 简化: 直接用 target.id 作为被删
    const deleteOp = {
      type: 'delete',
      id: target.id,
      parentId: null
    }
    this.applyOp(deleteOp)
    return deleteOp
  }

  /**
   * 订阅变更
   */
  observe(fn) {
    this.observers.add(fn)
    return () => this.observers.delete(fn)
  }

  _notify(op) {
    for (const fn of this.observers) {
      try { fn(op) } catch (e) { console.warn('observer error', e) }
    }
  }

  _orderedItems() {
    return [...this.items.values()].sort((a, b) => {
      const ap = a.parentId ? idKey(a.parentId) : ''
      const bp = b.parentId ? idKey(b.parentId) : ''
      if (ap < bp) return -1
      if (ap > bp) return 1
      if (a.id.clientId !== b.id.clientId) return a.id.clientId - b.id.clientId
      return a.id.clock - b.id.clock
    })
  }

  /**
   * 序列化为 snapshot (供发送)
   */
  toSnapshot() {
    return {
      v: 1,
      items: [...this.items.values()].map(i => ({
        id: { clientId: i.id.clientId, clock: i.id.clock },
        parent: i.parentId ? { clientId: i.parentId.clientId, clock: i.parentId.clock } : null,
        content: i.content
      })),
      tombstones: [...this.tombstones]
    }
  }
}

export function idKey(id) {
  return `${id.clientId}:${id.clock}`
}

/**
 * 找到字符在 toText 中的位置, 给出对应 CRDT id
 * (用于光标/选区 awareness)
 */
export function textPosToCrdtId(doc, pos) {
  const ordered = doc._orderedItems()
  let charPos = 0
  for (const item of ordered) {
    if (doc.tombstones.has(idKey(item.id))) continue
    if (charPos + item.content.length > pos) {
      return { id: item.id, offset: pos - charPos }
    }
    charPos += item.content.length
  }
  return null
}
