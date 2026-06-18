import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
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
      port: 5173,
      host: '0.0.0.0',
      open: false,
      proxy: {
        '/api/v1/auth': {
          target: 'http://localhost:8081',
          changeOrigin: true
        },
        '/api/v1/sessions': {
          target: 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/models': {
          target: 'http://localhost:8083',
          changeOrigin: true
        },
        '/api/v1/test': {
          target: 'http://localhost:8083',
          changeOrigin: true
        },
        '/api/v1/openai': {
          target: 'http://localhost:8083',
          changeOrigin: true
        },
        '/api/v1/imagegen': {
          target: 'http://localhost:8083',
          changeOrigin: true
        },
        '/api/v1/audio': {
          target: 'http://localhost:8083',
          changeOrigin: true
        },
        '/api/v1/leaderboard': {
          target: 'http://localhost:8083',
          changeOrigin: true
        },
        '/api/v1/memory': {
          target: 'http://localhost:8084',
          changeOrigin: true
        },
        '/api/v1/rag': {
          target: 'http://localhost:8085',
          changeOrigin: true
        },
        '/api/v1/function': {
          target: 'http://localhost:8086',
          changeOrigin: true
        },
        '/api/v1/admin': {
          target: 'http://localhost:8087',
          changeOrigin: true
        },
        '/api/v1/multimodal': {
          target: 'http://localhost:8088',
          changeOrigin: true
        },
        '/api/v1/prompts': {
          target: 'http://localhost:8091',
          changeOrigin: true
        },
        '/api/v1/agent': {
          target: 'http://localhost:8090',
          changeOrigin: true
        },
        '/ws': {
          target: 'ws://localhost:8090',
          ws: true,
          changeOrigin: true
        },
        '/api/v1/ws': {
          target: 'ws://localhost:8095',
          ws: true,
          changeOrigin: true
        },
        '/api': {
          target: env.VITE_API_BASE || 'http://localhost:8080',
          changeOrigin: true
        },
        '/sse': {
          target: env.VITE_API_BASE || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      target: 'es2018',
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            element: ['element-plus', '@element-plus/icons-vue'],
            echarts: ['echarts', 'vue-echarts']
          }
        }
      }
    }
  }
})
