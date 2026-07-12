/**
 * monitor.js API 单元测试 (Vitest)
 * 覆盖: 健康检查 / 指标 / 告警规则 / 告警渠道 CRUD / 知识图谱
 *
 * 策略: 用 vi.hoisted 创建 mock axios 实例，确保 interceptors 属性存在
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// 创建一个带有 interceptors 的完整 mock axios 实例
const mockAxiosInstance = {
  interceptors: {
    request: { use: vi.fn(), eject: vi.fn() },
    response: { use: vi.fn(), eject: vi.fn() },
  },
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  create: vi.fn(() => mockAxiosInstance),
}

const mockHttp = {
  get: mockAxiosInstance.get,
  post: mockAxiosInstance.post,
  put: mockAxiosInstance.put,
  delete: mockAxiosInstance.delete,
}

// Mock axios
vi.mock('axios', () => ({
  default: vi.fn(() => mockAxiosInstance),
  create: vi.fn(() => mockAxiosInstance),
}))

// Mock http.js
vi.mock('../api/http.js', () => ({
  default: mockHttp,
}))

// ── Helper ──
function mockResponse(data) {
  return { data: { code: 0, ...data } }
}

describe('Monitor API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ── 健康检查 ──

  describe('健康检查 API', () => {
    it('getMonitorHealth 应调用 GET /monitor/health', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { gateway: 'UP' } }))
      const { getMonitorHealth } = await import('../api/monitor.js')
      await getMonitorHealth()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/health')
    })

    it('getMonitorJvm 应调用 GET /monitor/health/jvm', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { heap: '512MB' } }))
      const { getMonitorJvm } = await import('../api/monitor.js')
      await getMonitorJvm()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/health/jvm')
    })

    it('getMonitorDisk 应调用 GET /monitor/health/disk', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { usage: '65%' } }))
      const { getMonitorDisk } = await import('../api/monitor.js')
      await getMonitorDisk()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/health/disk')
    })

    it('getMonitorDb 应调用 GET /monitor/health/database', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { pool: 'active:5' } }))
      const { getMonitorDb } = await import('../api/monitor.js')
      await getMonitorDb()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/health/database')
    })
  })

  // ── 指标 API ──

  describe('指标 API', () => {
    it('getMonitorMetrics 应调用 GET /monitor/metrics', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { getMonitorMetrics } = await import('../api/monitor.js')
      await getMonitorMetrics()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/metrics')
    })

    it('getMonitorTrend 应传入 hours 参数', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { getMonitorTrend } = await import('../api/monitor.js')
      await getMonitorTrend(48)
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/metrics/trend', { params: { hours: 48 } })
    })

    it('getMonitorSnapshot 应调用 GET /monitor/metrics/snapshot', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { getMonitorSnapshot } = await import('../api/monitor.js')
      await getMonitorSnapshot()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/metrics/snapshot')
    })
  })

  // ── 告警 API ──

  describe('告警 API', () => {
    it('getMonitorAlerts 应调用 GET /monitor/alerts/firing', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { getMonitorAlerts } = await import('../api/monitor.js')
      await getMonitorAlerts()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/alerts/firing')
    })

    it('getMonitorAlertsFiring 应调用 GET /monitor/alerts/firing', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { getMonitorAlertsFiring } = await import('../api/monitor.js')
      await getMonitorAlertsFiring()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/alerts/firing')
    })

    it('getMonitorAlertSummary 应调用 GET /monitor/alerts/summary', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { total: 3 } }))
      const { getMonitorAlertSummary } = await import('../api/monitor.js')
      const result = await getMonitorAlertSummary()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/alerts/summary')
      expect(result.data.data.total).toBe(3)
    })
  })

  // ── 告警规则 CRUD ──

  describe('告警规则 CRUD', () => {
    it('getMonitorAlertRules 应调用 GET /monitor/alerts/rules', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { getMonitorAlertRules } = await import('../api/monitor.js')
      await getMonitorAlertRules()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/alerts/rules')
    })

    it('createMonitorAlertRule 应调用 POST /monitor/alerts/rules', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({ data: { id: 1 } }))
      const { createMonitorAlertRule } = await import('../api/monitor.js')
      const body = { name: 'HighCPU', metric: 'cpu_usage', threshold: 80 }
      await createMonitorAlertRule(body)
      expect(mockHttp.post).toHaveBeenCalledWith('/monitor/alerts/rules', body)
    })

    it('updateMonitorAlertRule 应调用 PUT /monitor/alerts/rules/{id}', async () => {
      mockHttp.put.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { updateMonitorAlertRule } = await import('../api/monitor.js')
      await updateMonitorAlertRule(1, { name: 'Updated' })
      expect(mockHttp.put).toHaveBeenCalledWith('/monitor/alerts/rules/1', { name: 'Updated' })
    })

    it('deleteMonitorAlertRule 应调用 DELETE /monitor/alerts/rules/{id}', async () => {
      mockHttp.delete.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { deleteMonitorAlertRule } = await import('../api/monitor.js')
      await deleteMonitorAlertRule(5)
      expect(mockHttp.delete).toHaveBeenCalledWith('/monitor/alerts/rules/5')
    })
  })

  // ── 告警渠道 CRUD (Day 25) ──

  describe('告警渠道 CRUD', () => {
    it('getAlertChannels 应调用 GET /monitor/alerts/channels', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [{ id: 1, name: '运维邮件' }] }))
      const { getAlertChannels } = await import('../api/monitor.js')
      const result = await getAlertChannels()
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/alerts/channels')
      expect(result.data.data).toHaveLength(1)
    })

    it('getAlertChannel 应调用 GET /monitor/alerts/channels/{id}', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { id: 2, name: '钉钉群' } }))
      const { getAlertChannel } = await import('../api/monitor.js')
      await getAlertChannel(2)
      expect(mockHttp.get).toHaveBeenCalledWith('/monitor/alerts/channels/2')
    })

    it('createAlertChannel 应调用 POST /monitor/alerts/channels', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({ data: { id: 3 } }))
      const { createAlertChannel } = await import('../api/monitor.js')
      const body = { name: 'DingTalk', channelType: 'DINGTALK', config: '{"webhook":"https://..."}' }
      await createAlertChannel(body)
      expect(mockHttp.post).toHaveBeenCalledWith('/monitor/alerts/channels', body)
    })

    it('updateAlertChannel 应调用 PUT /monitor/alerts/channels/{id}', async () => {
      mockHttp.put.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { updateAlertChannel } = await import('../api/monitor.js')
      await updateAlertChannel(3, { name: 'Updated Channel', enabled: 0 })
      expect(mockHttp.put).toHaveBeenCalledWith('/monitor/alerts/channels/3', { name: 'Updated Channel', enabled: 0 })
    })

    it('deleteAlertChannel 应调用 DELETE /monitor/alerts/channels/{id}', async () => {
      mockHttp.delete.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { deleteAlertChannel } = await import('../api/monitor.js')
      await deleteAlertChannel(7)
      expect(mockHttp.delete).toHaveBeenCalledWith('/monitor/alerts/channels/7')
    })
  })

  // ── 知识图谱 API ──

  describe('知识图谱 API', () => {
    it('kgSearchEntities 应带 params 参数', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { kgSearchEntities } = await import('../api/monitor.js')
      await kgSearchEntities(1, 'CPU', 20)
      expect(mockHttp.get).toHaveBeenCalledWith('/agent/kg/entities/search', {
        params: { userId: 1, keyword: 'CPU', limit: 20 }
      })
    })

    it('kgGetEntity 应调用 GET /agent/kg/entities/{id}', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { id: 99 } }))
      const { kgGetEntity } = await import('../api/monitor.js')
      await kgGetEntity(99)
      expect(mockHttp.get).toHaveBeenCalledWith('/agent/kg/entities/99')
    })

    it('kgNeighbors 应调用 GET /agent/kg/entities/{id}/neighbors', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { kgNeighbors } = await import('../api/monitor.js')
      await kgNeighbors(10)
      expect(mockHttp.get).toHaveBeenCalledWith('/agent/kg/entities/10/neighbors')
    })

    it('kgTwoHop 应调用 GET /agent/kg/entities/{id}/2hop', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { kgTwoHop } = await import('../api/monitor.js')
      await kgTwoHop(5)
      expect(mockHttp.get).toHaveBeenCalledWith('/agent/kg/entities/5/2hop')
    })

    it('kgPath 应带 fromId 和 toId 参数', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: [] }))
      const { kgPath } = await import('../api/monitor.js')
      await kgPath(1, 10, 20)
      expect(mockHttp.get).toHaveBeenCalledWith('/agent/kg/path', {
        params: { userId: 1, fromId: 10, toId: 20 }
      })
    })
  })
})
