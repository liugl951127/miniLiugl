/**
 * @file system.js - 系统管理 API
 *
 * 新增 V6.8: 动态菜单接口 (模块化子菜单)
 * 新增 T1-mock-fix: 系统设置 (minimax-system /api/v1/system/settings)
 */
import http from './http'

// ========== 动态菜单 ==========
/**
 * 获取用户菜单配置
 * 后端根据用户角色返回对应菜单树
 * @returns { menu: MenuItem[] }
 */
export const getMenu = () => http.get('/system/menu')

/**
 * 获取平台信息
 */
export const getPlatformInfo = () => http.get('/system/info')

/**
 * 获取系统公告
 */
export const getAnnouncements = () => http.get('/system/announcements')

// ========== 基础系统接口 ==========
export const systemHealth = () => http.get('/system/health')
export const systemPing = () => http.get('/system/ping')

// ========== 系统设置 (T1-mock-fix) ==========
/**
 * 系统设置 API
 * 后端模型 (SystemSettings entity):
 *   siteName, siteLogo, maintenanceMode (0/1), allowRegister (0/1),
 *   defaultModelCode, description, contactEmail
 *
 * 前端表单 (views/settings/Index.vue):
 *   siteName, maintenance (bool), allowRegister (bool), defaultModel
 */
export const systemApi = {
  /** 取全局系统设置 (单行) - GET /api/v1/system/settings */
  getSettings: () => http.get('/system/settings'),
  /** 保存系统设置 (upsert) - PUT /api/v1/system/settings */
  updateSettings: (form) => http.put('/system/settings', {
    siteName: form.siteName,
    maintenanceMode: form.maintenance ? 1 : 0,
    allowRegister: form.allowRegister ? 1 : 0,
    defaultModelCode: form.defaultModel,
  }),
  /** 把后端返回的 SystemSettings 解析回前端 form 格式 */
  parseToForm: (settings) => {
    if (!settings) {
      return { siteName: 'Liugl-AI', maintenance: false, allowRegister: true, defaultModel: 'minimax-01' }
    }
    return {
      siteName: settings.siteName || 'Liugl-AI',
      maintenance: settings.maintenanceMode === 1,
      allowRegister: settings.allowRegister === 1,
      defaultModel: settings.defaultModelCode || 'minimax-01',
    }
  },
}
