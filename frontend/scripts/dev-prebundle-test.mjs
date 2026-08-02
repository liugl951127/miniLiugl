/**
 * 模拟 dev 启动 + 预编译 + 抓所有错
 */
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'

const server = await createServer({
  root: '.',
  plugins: [vue()],
  optimizeDeps: {
    include: [
      'vue', 'vue-router', 'pinia', 'axios', 'dayjs',
      'echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers',
    ],
  },
  logLevel: 'info',
  server: { middlewareMode: true }
})

// 强制 optimizeDeps
try {
  await server.optimizeDeps()
  console.log('optimizeDeps OK')
} catch (e) {
  console.log('optimizeDeps FAIL:', e.message)
}

// 拉一些模块触发 transform
const tests = [
  '/src/main.js',
  '/src/api/auth.js',
  '/src/api/ai.js',
  '/src/api/admin.js',
  '/src/api/monitor.js',
  '/src/api/analytics.js',
  '/src/api/chat.js',
  '/src/composables/usePwa.js',
  '/src/composables/useDemoMode.js',
  '/src/composables/useRoleDashboard.js',
  '/src/composables/useSSEStream.js',
  '/src/composables/useBusinessStream.js',
  '/src/store/user.js',
  '/src/store/notification.js',
  '/src/views/admin/Cluster.vue',
  '/src/views/admin/Dashboard.vue',
  '/src/views/admin/Audit.vue',
  '/src/views/kg/Index.vue',
  '/src/views/ai/Marketplace.vue',
  '/src/views/chat/Index.vue',
  '/src/views/agent/Multi.vue',
  '/src/views/showcase/Liugl-AIShowcase.vue',
  '/src/views/showcase/StreamShowcase.vue',
]
const fails = []
for (const t of tests) {
  try {
    const r = await server.transformRequest(t)
    if (!r) { console.log(`  null: ${t}`); fails.push(t) }
  } catch (e) {
    console.log(`  ERR: ${t} -> ${e.message.split('\n')[0]}`)
    fails.push(t)
  }
}
console.log(`\n${fails.length} failures / ${tests.length} tests`)
await server.close()
