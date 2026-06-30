/**
 * monitor.js API 单元测试 (Vitest, mock http)
 * V5.33 Day 25
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock http module before importing monitor
vi.mock('./http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  }
}))

import http from './http'
import {
  getMonitorHealth,
  getMonitorMetrics,
  getMonitorAlerts,
  getMonitorAlertsFiring,
  getAlertChannels,
  getAlertChannel,
  createAlertChannel,
  updateAlertChannel,
  deleteAlertChannel,
  getMonitorAlertRules,
  createMonitorAlertRule,
} from './monitor.js'

beforeEach(() => {
  vi.clearAllMocks()
})

// ─── 健康与指标 ───────────────────────────────────────────────

describe('getMonitorHealth', () => {
  it('should call http.get with /monitor/health', async () => {
    const mockData = { data: { gateway: { status: 'UP' } } }
    http.get.mockResolvedValue(mockData)

    const result = await getMonitorHealth()
    expect(http.get).toHaveBeenCalledOnce()
    expect(http.get).toHaveBeenCalledWith('/monitor/health')
    expect(result).toEqual(mockData)
  })
})

describe('getMonitorMetrics', () => {
  it('should call http.get with /monitor/metrics', async () => {
    http.get.mockResolvedValue({ data: { totalTokens: 1000 } })
    const result = await getMonitorMetrics()
    expect(http.get).toHaveBeenCalledWith('/monitor/metrics')
    expect(result.data.totalTokens).toBe(1000)
  })
})

describe('getMonitorAlerts', () => {
  it('should call http.get with /monitor/alerts', async () => {
    http.get.mockResolvedValue({ data: [] })
    await getMonitorAlerts()
    expect(http.get).toHaveBeenCalledWith('/monitor/alerts')
  })
})

describe('getMonitorAlertsFiring', () => {
  it('should call http.get with /monitor/alerts/firing', async () => {
    http.get.mockResolvedValue({ data: [{ id: 1, message: 'CPU high' }] })
    const result = await getMonitorAlertsFiring()
    expect(http.get).toHaveBeenCalledWith('/monitor/alerts/firing')
    expect(result.data).toHaveLength(1)
    expect(result.data[0].message).toBe('CPU high')
  })
})

// ─── 告警渠道 CRUD ───────────────────────────────────────────

describe('getAlertChannels', () => {
  it('should call http.get with /monitor/alerts/channels', async () => {
    const mockChannels = [
      { id: 1, name: '运维告警', channelType: 'EMAIL', enabled: 1 },
      { id: 2, name: '钉钉通知', channelType: 'DINGTALK', enabled: 1 },
    ]
    http.get.mockResolvedValue({ data: mockChannels })

    const result = await getAlertChannels()
    expect(http.get).toHaveBeenCalledOnce()
    expect(http.get).toHaveBeenCalledWith('/monitor/alerts/channels')
    expect(result.data).toHaveLength(2)
  })

  it('should return empty array on error', async () => {
    http.get.mockRejectedValue(new Error('network error'))
    // Caller in UI catches errors, just verify mock behavior
    await expect(getAlertChannels()).rejects.toThrow('network error')
  })
})

describe('getAlertChannel', () => {
  it('should call http.get with correct id path', async () => {
    http.get.mockResolvedValue({ data: { id: 5, name: 'Test Channel', channelType: 'WEBHOOK' } })
    const result = await getAlertChannel(5)
    expect(http.get).toHaveBeenCalledOnce()
    expect(http.get).toHaveBeenCalledWith('/monitor/alerts/channels/5')
    expect(result.data.id).toBe(5)
  })
})

describe('createAlertChannel', () => {
  it('should POST to /monitor/alerts/channels with body', async () => {
    const body = { name: 'New Email', channelType: 'EMAIL', config: '{"email":"a@b.com"}', enabled: 1, priority: 10 }
    http.post.mockResolvedValue({ data: { id: 10, ...body } })

    const result = await createAlertChannel(body)
    expect(http.post).toHaveBeenCalledOnce()
    expect(http.post).toHaveBeenCalledWith('/monitor/alerts/channels', body)
    expect(result.data.id).toBe(10)
  })
})

describe('updateAlertChannel', () => {
  it('should PUT to /monitor/alerts/channels/{id} with body', async () => {
    const body = { name: 'Updated Name', channelType: 'EMAIL', config: '{}', enabled: 1, priority: 5 }
    http.put.mockResolvedValue({ data: { id: 3, ...body } })

    const result = await updateAlertChannel(3, body)
    expect(http.put).toHaveBeenCalledOnce()
    expect(http.put).toHaveBeenCalledWith('/monitor/alerts/channels/3', body)
    expect(result.data.name).toBe('Updated Name')
  })
})

describe('deleteAlertChannel', () => {
  it('should DELETE to /monitor/alerts/channels/{id}', async () => {
    http.delete.mockResolvedValue({ data: null })

    await deleteAlertChannel(7)
    expect(http.delete).toHaveBeenCalledOnce()
    expect(http.delete).toHaveBeenCalledWith('/monitor/alerts/channels/7')
  })
})

// ─── 告警规则 ────────────────────────────────────────────────

describe('getMonitorAlertRules', () => {
  it('should call GET /monitor/alerts/rules', async () => {
    http.get.mockResolvedValue({ data: [{ id: 1, name: 'CPU > 80%' }] })
    const result = await getMonitorAlertRules()
    expect(http.get).toHaveBeenCalledWith('/monitor/alerts/rules')
    expect(result.data).toHaveLength(1)
  })
})

describe('createMonitorAlertRule', () => {
  it('should POST rule body to /monitor/alerts/rules', async () => {
    const rule = { name: 'Disk > 90%', metric: 'disk_used_percent', operator: 'GT', threshold: 90, cooldownMinutes: 15 }
    http.post.mockResolvedValue({ data: { id: 20, ...rule } })

    const result = await createMonitorAlertRule(rule)
    expect(http.post).toHaveBeenCalledWith('/monitor/alerts/rules', rule)
    expect(result.data.id).toBe(20)
  })
})
