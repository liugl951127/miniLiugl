// e2e/global-setup.js
// 全局 setup：启动 dev server（如果需要），等待服务就绪
const { chromium } = require('@playwright/test')
const { spawn } = require('child_process')
const path = require('path')

module.exports = async function globalSetup() {
  // 在 CI 环境下，如果 BASE_URL 未指定，使用内置静态服务器 serve dist/
  if (process.env.CI) {
    console.log('[e2e globalSetup] CI 环境，跳过外部服务检查')
    return
  }
  console.log('[e2e globalSetup] 本地模式，需要先 npm run dev')
}
