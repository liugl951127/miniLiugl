import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
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
    exclude: ['**/e2e/**', '**/node_modules/**'],
    setupFiles: ['src/test/setup.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/api/*.js', 'src/store/*.js', 'src/utils/*.js']
    }
  }
})
