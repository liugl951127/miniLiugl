/**
 * @file system.js - 系统管理 API
 *
 * 新增 V6.8: 动态菜单接口 (模块化子菜单)
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
