/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode, command }) => {
  const isTest = command === 'test'
  const env = loadEnv(mode, process.cwd(), '')
  const isProd = mode === 'production'

  const useGateway = env.VITE_USE_GATEWAY !== 'false'
  const gatewayTarget = env.VITE_GATEWAY_URL || 'http://localhost:8080'
  const directTarget = env.VITE_API_BASE || 'http://localhost:8080'

  if (isTest) {
    return {
      plugins: [
        vue(),
        AutoImport({ resolvers: [ElementPlusResolver()] }),
        Components({ resolvers: [ElementPlusResolver()] })
      ],
      resolve: {
        alias: {
          '@': fileURLToPath(new URL('./src', import.meta.url))
        }
      },
      test: {
        environment: 'happy-dom',
        globals: true,
        include: ['src/**/*.{test,spec}.{js,ts}'],
        exclude: ['**/e2e/**/*.spec.js', '**/e2e/**/*.test.js'],  // Playwright E2E excluded
        setupFiles: ['src/test/setup.js'],
        coverage: {
          provider: 'v8',
          reporter: ['text', 'lcov'],
          include: ['src/api/*.js', 'src/store/*.js', 'src/utils/*.js']
        }
      }
    }
  }

  return {
    plugins: [
      vue(),
      AutoImport({ resolvers: [ElementPlusResolver()] }),
      Components({ resolvers: [ElementPlusResolver()] })
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
          proxyTimeout: 60000,
          timeout: 60000,
        },
        '/ws': {
          target: gatewayTarget.replace('http', 'ws'),
          ws: true,
          changeOrigin: true,
        }
      } : {
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
      target: 'es2018',
      sourcemap: isProd ? false : 'eval',
      chunkSizeWarningLimit: 1500,
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
          entryFileNames: 'assets/[name].[hash].js',
          chunkFileNames: 'assets/[name].[hash].js',
          assetFileNames: 'assets/[name].[hash].[ext]',
        }
      },
      minify: isProd ? 'terser' : false,
      terserOptions: isProd ? {
        compress: {
          drop_console: true,
          drop_debugger: true,
          pure_funcs: ['console.info', 'console.debug'],
        },
      } : undefined,
    },
    esbuild: {
      target: 'es2018',
      drop: isProd ? ['console', 'debugger'] : [],
    }
  }
})
