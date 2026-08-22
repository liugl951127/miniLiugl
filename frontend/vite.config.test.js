import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 极简 build 配置: 关闭 sourcemap/minify, 只验证语法/模块解析
export default defineConfig(({ mode }) => {
  return {
    plugins: [
      vue(),
      AutoImport({ resolvers: [ElementPlusResolver()] }),
      Components({ resolvers: [ElementPlusResolver()] }),
    ],
    build: {
      sourcemap: false,
      minify: false,
      target: 'es2020',
      reportCompressedSize: false,
    },
    esbuild: {
      target: 'es2020',
    },
  }
})
