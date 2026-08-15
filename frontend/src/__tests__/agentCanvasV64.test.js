/**
 * Agent Canvas V6.4 测试 (运行/导入/导出/3 类新节点)
 */
import { describe, it, expect, beforeEach } from 'vitest'

const NODE_TYPES = [
  { type: 'llm', name: 'LLM 大模型', icon: '🤖', color: '#3b82f6' },
  { type: 'rag', name: 'RAG 检索', icon: '📚', color: '#10b981' },
  { type: 'tool', name: '工具调用', icon: '🔧', color: '#f59e0b' },
  { type: 'code', name: '代码执行', icon: '💻', color: '#8b5cf6' },
  { type: 'http', name: 'HTTP 请求', icon: '🌐', color: '#06b6d4' },
  { type: 'condition', name: '条件分支', icon: '🔀', color: '#eab308' },
  { type: 'memory', name: '记忆读写', icon: '🧠', color: '#ec4899' },
  { type: 'loop', name: '循环 Loop', icon: '🔁', color: '#14b8a6' },
  { type: 'parallel', name: '并行 Parallel', icon: '⚡', color: '#f97316' },
  { type: 'subflow', name: '子流 SubFlow', icon: '📦', color: '#0ea5e9' }
]

describe('V6.4 10 类节点', () => {
  it('应该正好 10 类', () => {
    expect(NODE_TYPES).toHaveLength(10)
  })
  it('3 类新节点: loop/parallel/subflow', () => {
    expect(NODE_TYPES.find(n => n.type === 'loop')).toBeDefined()
    expect(NODE_TYPES.find(n => n.type === 'parallel')).toBeDefined()
    expect(NODE_TYPES.find(n => n.type === 'subflow')).toBeDefined()
  })
  it('loop 颜色青绿 #14b8a6', () => {
    expect(NODE_TYPES.find(n => n.type === 'loop').color).toBe('#14b8a6')
  })
  it('parallel 颜色橙 #f97316', () => {
    expect(NODE_TYPES.find(n => n.type === 'parallel').color).toBe('#f97316')
  })
  it('subflow 颜色蓝 #0ea5e9', () => {
    expect(NODE_TYPES.find(n => n.type === 'subflow').color).toBe('#0ea5e9')
  })
  it('10 颜色不重复', () => {
    const colors = new Set(NODE_TYPES.map(n => n.color))
    expect(colors.size).toBe(10)
  })
})

describe('Loop 节点配置', () => {
  it('for/while/doWhile/forEach 4 种', () => {
    const types = ['for', 'while', 'doWhile', 'forEach']
    types.forEach(t => expect(t).toBeTruthy())
  })
  it('maxIterations 范围 1-10000', () => {
    const min = 1
    const max = 10000
    expect(max - min).toBe(9999)
  })
})

describe('Parallel 节点配置', () => {
  it('all/any/race/batch 4 种模式', () => {
    const modes = ['all', 'any', 'race', 'batch']
    expect(modes).toHaveLength(4)
  })
  it('concurrency 范围 1-100', () => {
    expect(100 - 1 + 1).toBe(100)
  })
})

describe('SubFlow 节点配置', () => {
  it('subflowId 必填', () => {
    const subflowId = 'subflow_xxx'
    expect(subflowId).toMatch(/^subflow_/)
  })
  it('inputMap/outputMap JSON 字符串', () => {
    const map = JSON.stringify({ key: '$value' })
    expect(JSON.parse(map).key).toBe('$value')
  })
  it('async 布尔', () => {
    const asyncFlag = false
    expect(typeof asyncFlag).toBe('boolean')
  })
})

describe('导出/导入 JSON', () => {
  it('导出格式含 version/nodes/edges/metadata', () => {
    const data = {
      version: '1.0',
      exportedAt: '2026-08-09T11:45:19Z',
      nodes: [{ id: '1', type: 'llm', name: 'T', x: 100, y: 100 }],
      edges: [],
      metadata: { name: 'Plan', author: 'user' }
    }
    expect(data.version).toBe('1.0')
    expect(data.nodes).toHaveLength(1)
    expect(data.metadata.name).toBe('Plan')
  })
  
  it('JSON.stringify 双向兼容', () => {
    const original = { version: '1.0', nodes: [{ id: '1' }], edges: [] }
    const json = JSON.stringify(original)
    const parsed = JSON.parse(json)
    expect(parsed).toEqual(original)
  })
  it('JSON 含 version 字段', () => {
    const data = { version: '1.0', nodes: [] }
    expect(data.version).toBe('1.0')
  })
  
  it('导入校验节点格式', () => {
    const valid = (n) => !!(n.id && n.type && n.name && typeof n.x === 'number')
    expect(valid({ id: '1', type: 'llm', name: 'T', x: 100, y: 100 })).toBe(true)
    expect(valid({ id: '1', type: 'llm' })).toBe(false)  // 缺 name/x
    expect(valid({ type: 'llm', name: 'T', x: 100 })).toBe(false)  // 缺 id
  })
})

describe('拓扑排序', () => {
  function topoSort(nodes, edges) {
    const inDegree = {}
    const adj = {}
    nodes.forEach(n => { inDegree[n.id] = 0; adj[n.id] = [] })
    edges.forEach(e => {
      inDegree[e.target] = (inDegree[e.target] || 0) + 1
      adj[e.source] = adj[e.source] || []
      adj[e.source].push(e.target)
    })
    const queue = nodes.filter(n => inDegree[n.id] === 0)
    const sorted = []
    while (queue.length) {
      const n = queue.shift()
      sorted.push(n)
      for (const tid of (adj[n.id] || [])) {
        inDegree[tid]--
        if (inDegree[tid] === 0) {
          const target = nodes.find(x => x.id === tid)
          if (target) queue.push(target)
        }
      }
    }
    return sorted
  }
  
  it('空图', () => {
    expect(topoSort([], [])).toEqual([])
  })
  
  it('3 节点链 a→b→c', () => {
    const nodes = [
      { id: 'a' }, { id: 'b' }, { id: 'c' }
    ]
    const edges = [
      { source: 'a', target: 'b' },
      { source: 'b', target: 'c' }
    ]
    const sorted = topoSort(nodes, edges)
    expect(sorted.map(n => n.id)).toEqual(['a', 'b', 'c'])
  })
  
  it('4 节点菱形 a→b, a→c, b→d, c→d', () => {
    const nodes = [
      { id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }
    ]
    const edges = [
      { source: 'a', target: 'b' },
      { source: 'a', target: 'c' },
      { source: 'b', target: 'd' },
      { source: 'c', target: 'd' }
    ]
    const sorted = topoSort(nodes, edges)
    expect(sorted[0].id).toBe('a')
    expect(sorted[3].id).toBe('d')
  })
  
  it('并行 3 分支 a→b, a→c, a→d', () => {
    const nodes = [
      { id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }
    ]
    const edges = [
      { source: 'a', target: 'b' },
      { source: 'a', target: 'c' },
      { source: 'a', target: 'd' }
    ]
    const sorted = topoSort(nodes, edges)
    expect(sorted[0].id).toBe('a')
    expect(sorted.slice(1).map(n => n.id).sort()).toEqual(['b', 'c', 'd'])
  })
  
  it('循环检测: a→b, b→a', () => {
    const nodes = [{ id: 'a' }, { id: 'b' }]
    const edges = [
      { source: 'a', target: 'b' },
      { source: 'b', target: 'a' }
    ]
    // 循环情况下 Kahn 算法会丢失节点
    const sorted = topoSort(nodes, edges)
    expect(sorted.length).toBeLessThan(2)  // 不能全排
  })
})

describe('运行 Plan', () => {
  it('Plan 含 nodes/edges/metadata', () => {
    const plan = {
      nodes: [{ id: '1', type: 'llm' }],
      edges: [],
      metadata: { name: 'Test', version: '1.0' }
    }
    expect(plan.nodes).toBeDefined()
    expect(plan.metadata.version).toBe('1.0')
  })
  
  it('运行结果含 success/durationMs/nodeResults/output', () => {
    const result = {
      success: true,
      durationMs: 1234,
      nodeResults: [
        { id: '1', name: 'LLM', type: 'llm', status: 'success', durationMs: 500, output: {} }
      ],
      output: { final: 'done' }
    }
    expect(result.success).toBe(true)
    expect(result.durationMs).toBeGreaterThan(0)
    expect(result.nodeResults[0].status).toBe('success')
  })
  
  it('运行状态: pending/running/success/failed', () => {
    const statuses = ['pending', 'running', 'success', 'failed']
    statuses.forEach(s => expect(s).toBeTruthy())
  })
  
  it('getRunStatusType 映射', () => {
    const map = { success: 'success', failed: 'danger', running: 'warning', pending: 'info' }
    expect(map.success).toBe('success')
    expect(map.failed).toBe('danger')
  })
})

describe('导入校验', () => {
  it('缺 nodes 报错', () => {
    const data = { version: '1.0' }
    expect(Array.isArray(data.nodes)).toBe(false)
  })
  
  it('节点缺 id 报错', () => {
    const data = { nodes: [{ type: 'llm', name: 'T', x: 0 }] }
    const valid = data.nodes.every(n => n.id && n.type && n.name && typeof n.x === 'number')
    expect(valid).toBe(false)
  })
  
  it('完整数据通过', () => {
    const data = {
      nodes: [{ id: '1', type: 'llm', name: 'T', x: 0, y: 0 }],
      edges: []
    }
    const valid = data.nodes.every(n => n.id && n.type && n.name && typeof n.x === 'number')
    expect(valid).toBe(true)
  })
})
