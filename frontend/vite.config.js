import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const isProd = mode === 'production'

  // V5.3: 默认走 nginx 端口 3000 (同源访问, 无 CORS)
  const useGateway = env.VITE_USE_GATEWAY !== 'false'
  const gatewayTarget = env.VITE_GATEWAY_URL || 'http://localhost:8080'
  const directTarget = env.VITE_API_BASE || 'http://localhost:8080'

  return {
    plugins: [
      vue(),
      AutoImport({ resolvers: [ElementPlusResolver()] }),
      Components({ resolvers: [ElementPlusResolver()] })
      // V5.8 优化: 移除 vite-plugin-compression (含 brotli native 依赖, 沙箱装不上)
      // nginx 端已配置运行时 gzip + br 压缩 (scripts/nginx-minimax-3000.conf)
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 3000,
      host: '0.0.0.0',
      open: false,
      cors: true,
      proxy: useGateway ? {
        // 走 gateway (端口 8080) — 12 个微服务在 gateway 后
        '/api': {
          target: gatewayTarget,
          changeOrigin: true,
          cookieDomainRewrite: '',
          proxyTimeout: 30000,
          timeout: 30000,
        },
        '/sse': {
          target: gatewayTarget,
          changeOrigin: true,
          proxyTimeout: 60000,  // SSE 长连接
          timeout: 60000,
        },
        '/ws': {
          target: gatewayTarget.replace('http', 'ws'),
          ws: true,
          changeOrigin: true,
        }
      } : {
        // 直连微服务 (开发调试用)
        '/api/v1/auth': { target: 'http://localhost:8081', changeOrigin: true, proxyTimeout: 30000 },
        '/api/v1/sessions': { target: 'http://localhost:8082', changeOrigin: true },
        '/api/v1/messages': { target: 'http://localhost:8082', changeOrigin: true },
        '/api/v1/chat': { target: 'http://localhost:8082', changeOrigin: true },
        '/api/v1/models': { target: 'http://localhost:8083', changeOrigin: true },
        '/api/v1/test': { target: 'http://localhost:8083', changeOrigin: true },
        '/api/v1/openai': { target: 'http://localhost:8083', changeOrigin: true },
        '/api/v1/imagegen': { target: 'http://localhost:8083', changeOrigin: true },
        '/api/v1/audio': { target: 'http://localhost:8083', changeOrigin: true },
        '/api/v1/leaderboard': { target: 'http://localhost:8083', changeOrigin: true },
        '/api/v1/memory': { target: 'http://localhost:8084', changeOrigin: true },
        '/api/v1/rag': { target: 'http://localhost:8085', changeOrigin: true },
        '/api/v1/function': { target: 'http://localhost:8086', changeOrigin: true },
        '/api/v1/admin': { target: 'http://localhost:8087', changeOrigin: true },
        '/api/v1/monitor': { target: 'http://localhost:8089', changeOrigin: true },
        '/api/v1/multimodal': { target: 'http://localhost:8088', changeOrigin: true },
        '/api/v1/prompts': { target: 'http://localhost:8091', changeOrigin: true },
        '/api/v1/prompt': { target: 'http://localhost:8091', changeOrigin: true },
        '/api/v1/agent': { target: 'http://localhost:8090', changeOrigin: true },
        '/api/v1/ws': { target: 'ws://localhost:8095', ws: true, changeOrigin: true },
        '/ws': { target: 'ws://localhost:8095', ws: true, changeOrigin: true },
        '/api': { target: directTarget, changeOrigin: true },
        '/sse': { target: directTarget, changeOrigin: true }
      }
    },
    build: {
      // V3.0.0: target 降为 es2015, 兼容 Chrome 63+/Edge 79+/Firefox 60+/Safari 12+
      // package.json 中 browserslist 会覆盖
      target: 'es2015',
      // V5.8: sourcemap 关闭 (生产不暴露源码)
      sourcemap: isProd ? false : 'eval',
      // V3.0.0: 浏览器 polyfill 支持
      // V3.5.8+: polyfillModulePreload 弃用, 改用 modulePreload.polyfill
      modulePreload: {
        polyfill: true
      },
      cssCodeSplit: true,
      chunkSizeWarningLimit: 1500,
      // V5.8: 智能分包 (按依赖 + 路由)
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('element-plus') || id.includes('@element-plus')) return 'element'
              if (id.includes('echarts') || id.includes('vue-echarts')) return 'echarts'
              if (id.includes('axios') || id.includes('@element-plus/icons-vue')) return 'common'
              if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) return 'vue'
              if (id.includes('dayjs') || id.includes('markdown')) return 'utils'
              return 'vendor'
            }
            if (id.includes('/src/api/') || id.includes('/src/views/admin/')) return 'admin'
            if (id.includes('/src/views/agent/')) return 'agent'
            if (id.includes('/src/views/kg/')) return 'kg'
            if (id.includes('/src/views/showcase/')) return 'showcase'
          },
          // hash 文件名长期缓存
          entryFileNames: 'assets/[name].[hash].js',
          chunkFileNames: 'assets/[name].[hash].js',
          assetFileNames: 'assets/[name].[hash].[ext]',
        }
      },
      // V5.8: terser 压缩 (生产)
      minify: isProd ? 'terser' : false,
      terserOptions: isProd ? {
        compress: {
          drop_console: true,
          drop_debugger: true,
          pure_funcs: ['console.info', 'console.debug'],
        },
      } : undefined,
    },
    // V5.8: esbuild 优化
    esbuild: {
      target: 'es2018',
      drop: isProd ? ['console', 'debugger'] : [],
    },
    // V3.5.8+: 关闭 Sass legacy JS API 警告 (Dart Sass 2.0 移除)
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler',  // 用 modern API 替代 legacy JS API
          silenceDeprecations: ['legacy-js-api'],  // V3.5.8 兜底, 避免依赖警告
        }
      }
    }
  }
})